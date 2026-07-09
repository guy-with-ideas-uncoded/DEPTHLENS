package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.database.DepthDatabase
import com.example.data.model.*
import com.example.data.network.*
import com.example.BuildConfig
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.example.data.network.CloudSyncService

class IntelligenceRepository(private val context: Context) {

    companion object {
        // ── All available Gemini models (display name → API model string) ──
        // Ordered: best/newest first, lightweight last as final fallback.
        // "gemini-flash-latest" always points to the newest Flash release automatically.
        val ALL_MODELS: List<Pair<String, String>> = listOf(
            Pair("Default (Gemini 3.5 Flash)",   "gemini-3.5-flash"),
            Pair("Gemini 3.5 Flash",             "gemini-3.5-flash"),
            Pair("Gemini 3.1 Pro (Precision)",   "gemini-3.1-pro-preview"),
            Pair("Gemini 3.1 Flash Lite",        "gemini-3.1-flash-lite-preview")
        )

        // Default preferred model — gemini-3.5-flash for maximum response speed and intelligence
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        const val PREF_KEY_MODEL = "selected_gemini_model"
        const val PREFS_NAME     = "depthlens_prefs"

        // Build the fallback chain starting from the user's chosen model,
        // then appending all others so the app never fully fails.
        fun buildModelFallbackChain(preferredModel: String): List<String> {
            val cleanPreferred = preferredModel.removePrefix("models/")
            val ordered = mutableListOf(cleanPreferred)
            // Always ensure these reliable fallbacks are in the chain
            val fallbacks = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview"
            )
            for (m in fallbacks) {
                if (m != cleanPreferred) ordered.add(m)
            }
            return ordered
        }

        private val _runningAnalyses = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
        val runningAnalyses: kotlinx.coroutines.flow.StateFlow<Set<String>> = _runningAnalyses.asStateFlow()

        fun markAnalysisRunning(sessionId: String) {
            _runningAnalyses.value = _runningAnalyses.value + sessionId
        }

        fun markAnalysisComplete(sessionId: String) {
            _runningAnalyses.value = _runningAnalyses.value - sessionId
        }

        // Sessions whose NEXT analysis should force live web grounding
        // (user tapped "Search the Web" on a reply). Consumed once, then cleared.
        private val _forceWebSessions = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
        fun markForceWeb(sessionId: String) {
            _forceWebSessions.value = _forceWebSessions.value + sessionId
        }
        fun consumeForceWeb(sessionId: String): Boolean {
            val on = _forceWebSessions.value.contains(sessionId)
            if (on) _forceWebSessions.value = _forceWebSessions.value - sessionId
            return on
        }
    }

    private val db = DepthDatabase.getDatabase(context)
    val sessionDao = db.sessionDao()
    val messageDao = db.messageDao()
    private val attachmentDao = db.attachmentDao()
    val memoryInsightDao = db.memoryInsightDao()
    private val archivedInsightDao = db.archivedInsightDao()
    private val apiService = RetrofitClient.service
    private val apiRequestMutex = kotlinx.coroutines.sync.Mutex()

    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    private val urlOkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<com.example.data.model.AttachmentEntity>>()

    val allSessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessionsFlow()
    val allMemoryInsightsFlow: Flow<List<MemoryInsight>> = memoryInsightDao.getAllInsightsFlow()
    val allArchivedInsightsFlow: Flow<List<ArchivedInsightEntity>> = archivedInsightDao.getAllArchivedInsightsFlow()

    suspend fun getAllSessionsDirect(): List<SessionEntity> {
        return sessionDao.getAllSessions()
    }

    suspend fun insertArchivedInsight(insight: ArchivedInsightEntity) {
        archivedInsightDao.insertArchivedInsight(insight)
    }

    suspend fun deleteArchivedInsight(id: String) {
        archivedInsightDao.deleteArchivedInsight(id)
    }

    suspend fun deleteAllArchivedInsights() {
        archivedInsightDao.deleteAllArchivedInsights()
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        val db = com.example.data.database.DepthDatabase.getDatabase(context)
        db.clearAllTables()
    }

    private val backgroundScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)

    /** Read the user-selected model from SharedPrefs; falls back to DEFAULT_MODEL */
    private fun getPreferredModel(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawModel = prefs.getString(PREF_KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        return rawModel.removePrefix("models/")
    }

    private fun triggerUpload(block: suspend (userId: String) -> Unit) {
        val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val userId = prefs.getString("user_id", "") ?: ""
        if (isLoggedIn && userId.isNotEmpty()) {
            backgroundScope.launch {
                try {
                    block(userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getMessagesFlow(sessionId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun getMessagesDirect(sessionId: String): List<MessageEntity> {
        return messageDao.getMessagesForSession(sessionId)
    }

    fun getAttachmentsForMessageFlow(messageId: String, imageUriField: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsForMessageFlow(messageId).map { dbList ->
            val list = if (dbList.isNotEmpty()) {
                dbList
            } else if (!imageUriField.isNullOrBlank()) {
                imageUriField.split(",").map { uriString ->
                    val uri = uriString.trim()
                    AttachmentEntity(
                        attachmentId = UUID.randomUUID().toString(),
                        messageId = messageId,
                        mimeType = getUriMimeType(context, uri),
                        localUri = uri,
                        remoteUrl = if (uri.startsWith("http")) uri else null,
                        fileName = if (uri.startsWith("http")) uri.substringAfterLast("/") else (Uri.parse(uri).path?.let { java.io.File(it).name } ?: "attachment")
                    )
                }
            } else {
                emptyList()
            }
            list.map { ensureLocalAttachment(it) }
        }
    }

    suspend fun ensureLocalAttachment(attachment: AttachmentEntity): AttachmentEntity = withContext(Dispatchers.IO) {
        val hasLocal = try {
            if (attachment.localUri.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(attachment.localUri), "r")?.close()
                true
            } else if (attachment.localUri.startsWith("file://")) {
                java.io.File(Uri.parse(attachment.localUri).path ?: "").exists()
            } else if (attachment.localUri.startsWith("/")) {
                java.io.File(attachment.localUri).exists()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }

        if (hasLocal) return@withContext attachment

        if (attachment.remoteUrl.isNullOrBlank()) return@withContext attachment

        val deferred = activeDownloads.getOrPut(attachment.attachmentId) {
            backgroundScope.async(Dispatchers.IO) {
                try {
                    val cacheDir = java.io.File(context.cacheDir, "attachments")
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs()
                    }
                    val file = java.io.File(cacheDir, "${attachment.attachmentId}_${attachment.fileName}")
                    
                    if (!file.exists()) {
                        val request = okhttp3.Request.Builder().url(attachment.remoteUrl).build()
                        val response = urlOkHttpClient.newCall(request).execute()
                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { input ->
                                java.io.FileOutputStream(file).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    
                    if (file.exists()) {
                        val newLocalUri = "file://${file.absolutePath}"
                        val updated = attachment.copy(localUri = newLocalUri)
                        attachmentDao.insertAttachment(updated) // Update DB so future flows get the fixed URI
                        updated
                    } else {
                        attachment
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    attachment
                } finally {
                    activeDownloads.remove(attachment.attachmentId)
                }
            }
        }
        deferred.await()
    }

    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity> {
        return attachmentDao.getAttachmentsForMessage(messageId)
    }

    private fun getUriMimeType(context: Context, uriString: String): String {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "content" || uri.scheme == "android.resource") {
            return context.contentResolver.getType(uri) ?: "application/octet-stream"
        }
        val ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uriString)
        if (!ext.isNullOrEmpty()) {
            return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
        }
        return when {
            uriString.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            uriString.endsWith(".mp3", ignoreCase = true) || uriString.endsWith(".m4a", ignoreCase = true) || uriString.endsWith(".aac", ignoreCase = true) || uriString.endsWith(".wav", ignoreCase = true) -> "audio/mpeg"
            uriString.endsWith(".mp4", ignoreCase = true) || uriString.endsWith(".mov", ignoreCase = true) -> "video/mp4"
            uriString.endsWith(".png", ignoreCase = true) || uriString.endsWith(".jpg", ignoreCase = true) || uriString.endsWith(".jpeg", ignoreCase = true) || uriString.endsWith(".webp", ignoreCase = true) -> "image/png"
            else -> "application/octet-stream"
        }
    }

    suspend fun createNewSession(title: String): SessionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val session = SessionEntity(
            id = id,
            title = title,
            createdAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )
        sessionDao.insertSession(session)
        triggerUpload { uid ->
            CloudSyncService.uploadSession(uid, id, title, false, session.createdAt, session.lastUpdatedAt)
        }
        session
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
        if (sessionItem != null) {
            val updated = sessionItem.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis())
            sessionDao.insertSession(updated)
            triggerUpload { uid ->
                CloudSyncService.uploadSession(uid, updated.id, updated.title, updated.isPinned, updated.createdAt, updated.lastUpdatedAt)
            }
        }
    }

    suspend fun togglePinSession(sessionId: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
        if (sessionItem != null) {
            val updated = sessionItem.copy(isPinned = !sessionItem.isPinned, lastUpdatedAt = System.currentTimeMillis())
            sessionDao.insertSession(updated)
            triggerUpload { uid ->
                CloudSyncService.uploadSession(uid, updated.id, updated.title, updated.isPinned, updated.createdAt, updated.lastUpdatedAt)
            }
        }
    }

    // ── ADDITIONS ──────────────────────────────────────────────────────────────

    /**
     * Search-aware flow that filters sessions by title OR message content.
     * Used by the search bar in the session list screen.
     */
    fun searchSessionsFlow(query: String): Flow<List<SessionEntity>> {
        return sessionDao.searchSessionsFlow(query)
    }

    suspend fun searchMessages(query: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        return@withContext messageDao.searchMessages(query)
    }

    /**
     * Rename a session — used by the long-press "Rename" context menu action.
     */
    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessions().find { it.id == sessionId }
        if (sessionItem != null) {
            val updated = sessionItem.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis())
            sessionDao.insertSession(updated)
            triggerUpload { uid ->
                CloudSyncService.uploadSession(uid, updated.id, updated.title, updated.isPinned, updated.createdAt, updated.lastUpdatedAt)
            }
        } else {
            sessionDao.renameSession(sessionId, newTitle)
        }
    }

    // ── END ADDITIONS ──────────────────────────────────────────────────────────

    suspend fun generateTitleForSession(sessionId: String, queryText: String) = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext

        val currentDateTimeStr = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy, hh:mm:ss a (z)", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())

        // Generate a 3-8 word high-quality title using Gemini
        val prompt = """
            Current date and time: $currentDateTimeStr.
            
            Create an exceptionally elegant, professional, 3-8 word human-friendly title summarizing this user message.
            Format Rules:
            - Capture the main topic, user intent, or core question.
            - Output ONLY the title string. 
            - Do not include quotes, markdown, colons, timestamps, emojis, prefix labels, or any introductory text.
            - Keep it human-readable, like a Claude/ChatGPT/Notion AI title.

            Message: $queryText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        val modelsToTry = buildModelFallbackChain(getPreferredModel())
        var generatedTitle: String? = null
        val retryDelays = listOf(100L)

        for (modelName in modelsToTry) {
            for ((attempt, delay) in retryDelays.withIndex()) {
                try {
                    val response = apiRequestMutex.withLock {
                        apiService.generateContent(modelName, apiKey, request)
                    }
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        generatedTitle = text.removeSurrounding("\"").removeSurrounding("'").trim()
                            .replace(Regex("[#:*_~`]"), "") // Clean any markdown or colon
                        break
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    val is429 = msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true)
                    if (is429 && attempt < retryDelays.size - 1) {
                        kotlinx.coroutines.delay(delay)
                        continue
                    } else {
                        break
                    }
                }
            }
            if (generatedTitle != null) break
        }

        if (!generatedTitle.isNullOrEmpty()) {
            var proposedTitle = generatedTitle
            
            // Check for duplicates
            val existingSessions = sessionDao.getAllSessionsFlow().firstOrNull() ?: emptyList()
            val siblingTitles = existingSessions.filter { it.id != sessionId }.map { it.title }
            if (siblingTitles.contains(proposedTitle)) {
                var index = 2
                var uniqueTitle = "$proposedTitle ($index)"
                while (siblingTitles.contains(uniqueTitle)) {
                    index++
                    uniqueTitle = "$proposedTitle ($index)"
                }
                proposedTitle = uniqueTitle
            }

            val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
            if (sessionItem != null) {
                sessionDao.insertSession(sessionItem.copy(title = proposedTitle, lastUpdatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun applyPrivacyCleanup(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch all messages for local session history
            val messages = messageDao.getMessagesForSession(sessionId)
            
            // 2. Delete physical files / voice records / cached images from current session if starting with file:// or similar local paths
            messages.forEach { msg ->
                if (!msg.imageUri.isNullOrEmpty()) {
                    try {
                        val uri = Uri.parse(msg.imageUri)
                        if (uri.scheme == "file" || (uri.path != null && uri.path!!.contains("files/"))) {
                            val f = uri.path?.let { java.io.File(it) }
                            if (f != null && f.exists()) {
                                f.delete()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 3. Clear intermediate messages so that session retains ONLY the final AI response
            val latestModelMsg = messages.filter { it.role == "model" }.maxByOrNull { it.timestamp }
            if (latestModelMsg != null) {
                messages.forEach { msg ->
                    if (msg.id != latestModelMsg.id) {
                        messageDao.deleteMessage(msg.id)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        attachmentDao.deleteAttachmentsForSession(sessionId)
        messageDao.deleteMessagesForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
        triggerUpload { uid ->
            CloudSyncService.deleteSession(uid, sessionId)
        }
    }

    suspend fun deleteMessageById(messageId: String) = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            val sessionId = msg.sessionId
            val attachments = attachmentDao.getAttachmentsForMessage(messageId)
            attachments.forEach { att ->
                try {
                    val uri = Uri.parse(att.localUri)
                    if (uri.scheme == "file") {
                        val file = java.io.File(uri.path ?: "")
                        if (file.exists() && file.absolutePath.contains(context.cacheDir.absolutePath)) {
                            file.delete()
                            android.util.Log.d("CACHE_SYSTEM", "Successfully deleted local cache file: ${file.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CACHE_SYSTEM", "Error deleting local cache file: ${e.message}")
                }
            }
            attachmentDao.deleteAttachmentsForMessage(messageId)
            messageDao.deleteMessage(messageId)
            triggerUpload { uid ->
                CloudSyncService.deleteMessage(uid, sessionId, messageId)
            }
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        // Only clear memory insights on sign-out, NOT sessions or messages
        // Sessions and messages are preserved so they sync back on re-login
        memoryInsightDao.deleteAllInsights()
    }

    suspend fun clearAllMemoryInsights() = withContext(Dispatchers.IO) {
        memoryInsightDao.deleteAllInsights()
    }

    suspend fun insertUserMessage(sessionId: String, text: String, imageUri: String? = null, replyToMessageId: String? = null, selectedText: String? = null) = withContext(Dispatchers.IO) {
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            text = text,
            imageUri = imageUri,
            timestamp = System.currentTimeMillis(),
            replyToMessageId = replyToMessageId,
            selectedText = selectedText
        )
        messageDao.insertMessage(userMsg)

        if (!imageUri.isNullOrEmpty()) {
            val uris = imageUri.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val attachments = uris.mapNotNull { uriStr ->
                try {
                    val uri = android.net.Uri.parse(uriStr)
                    val mime = getUriMimeType(context, uriStr)
                    val name = if (uri.scheme == "file") {
                        java.io.File(uri.path ?: "").name
                    } else {
                        "attachment_${System.currentTimeMillis()}"
                    }

                    // Copy to permanent internal storage to prevent permission loss after restart
                    val (permanentLocalUri, finalFileName) = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                        try {
                            val attachmentsDir = java.io.File(context.filesDir, "attachments")
                            if (!attachmentsDir.exists()) attachmentsDir.mkdirs()
                            
                            var resolvedName: String? = null
                            val srcUri = android.net.Uri.parse(uriStr)
                            if (srcUri.scheme == "content") {
                                val cursor = context.contentResolver.query(srcUri, null, null, null, null)
                                cursor?.use { c ->
                                    if (c.moveToFirst()) {
                                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex != -1) {
                                            resolvedName = c.getString(nameIndex)
                                        }
                                    }
                                }
                            }
                            
                            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
                            val originalName = resolvedName ?: (srcUri.lastPathSegment ?: "attachment")
                            val safeName = if (originalName.contains(".")) originalName else "$originalName.$extension"
                            val uniqueName = "att_${UUID.randomUUID()}_$safeName"
                            val destFile = java.io.File(attachmentsDir, uniqueName)
                            
                            context.contentResolver.openInputStream(srcUri)?.use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            if (destFile.exists()) {
                                Pair("file://${destFile.absolutePath}", destFile.name)
                            } else {
                                Pair(uriStr, name)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            Pair(uriStr, name)
                        }
                    } else {
                        Pair(uriStr, name)
                    }

                    val attachment = AttachmentEntity(
                        attachmentId = UUID.randomUUID().toString(),
                        messageId = userMsg.id,
                        mimeType = mime,
                        localUri = permanentLocalUri,
                        remoteUrl = if (uriStr.startsWith("http")) uriStr else null,
                        thumbnailUrl = null,
                        fileName = finalFileName
                    )
                    attachmentDao.insertAttachment(attachment)
                    attachment
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            triggerUpload { uid ->
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                
                // Sequentially upload all attachments
                attachments.forEach { attachment ->
                    if (attachment.remoteUrl == null) {
                        try {
                            val uri = android.net.Uri.parse(attachment.localUri)
                            val storageRef = storage.reference.child("$uid/${userMsg.sessionId}/${userMsg.id}/${attachment.fileName}")
                            
                            val uploadTask = if (uri.scheme == "content") {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    storageRef.putStream(inputStream)
                                } else null
                            } else {
                                val path = uri.path ?: attachment.localUri
                                val file = java.io.File(path)
                                if (file.exists()) {
                                    storageRef.putFile(android.net.Uri.fromFile(file))
                                } else null
                            }
                            
                            if (uploadTask != null) {
                                com.google.android.gms.tasks.Tasks.await(uploadTask)
                                val downloadUrl = com.google.android.gms.tasks.Tasks.await(storageRef.downloadUrl).toString()
                                
                                val updatedAttachment = attachment.copy(remoteUrl = downloadUrl)
                                attachmentDao.insertAttachment(updatedAttachment)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                // After uploads, fetch updated attachments to generate the final imageUri string
                val finalAttachments = attachmentDao.getAttachmentsForMessage(userMsg.id)
                finalAttachments.forEach { att ->
                    val size = try {
                        val uri = android.net.Uri.parse(att.localUri)
                        if (uri.scheme == "content") {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                                it.statSize
                            } ?: 0L
                        } else {
                            val path = uri.path ?: att.localUri
                            val f = java.io.File(path)
                            if (f.exists()) f.length() else 0L
                        }
                    } catch (e: Exception) {
                        0L
                    }
                    CloudSyncService.uploadAttachment(uid, userMsg.sessionId, userMsg.id, att, size)
                }
                
                val finalImageUris = if (finalAttachments.isNotEmpty()) {
                    finalAttachments.map { it.remoteUrl ?: it.localUri }.joinToString(",")
                } else {
                    userMsg.imageUri
                }
                
                // Update local MessageEntity in Room so that imageUri field contains the remote URLs
                messageDao.insertMessage(userMsg.copy(imageUri = finalImageUris))
                
                CloudSyncService.uploadMessage(uid, userMsg.id, userMsg.sessionId, userMsg.role, userMsg.text, finalImageUris, userMsg.timestamp, userMsg.replyToMessageId, userMsg.selectedText)
            }
        } else {
            triggerUpload { uid ->
                CloudSyncService.uploadMessage(uid, userMsg.id, userMsg.sessionId, userMsg.role, userMsg.text, userMsg.imageUri, userMsg.timestamp, userMsg.replyToMessageId, userMsg.selectedText)
            }
        }

        sessionDao.updateLastUsed(sessionId, System.currentTimeMillis())
    }

    suspend fun runConversationContinuityFlow(sessionId: String, cleanQuery: String) = withContext(Dispatchers.IO) {
        val userMsg = MessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            text = cleanQuery,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMsg)
        sessionDao.updateLastUsed(sessionId, System.currentTimeMillis())
        triggerUpload { uid ->
            CloudSyncService.uploadMessage(uid, userMsg.id, userMsg.sessionId, userMsg.role, userMsg.text, userMsg.imageUri, userMsg.timestamp)
        }

        val briefText = generateContinuityBrief(sessionId)

        val assistantMsg = MessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "model",
            text = briefText,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(assistantMsg)
        triggerUpload { uid ->
            CloudSyncService.uploadMessage(uid, assistantMsg.id, assistantMsg.sessionId, assistantMsg.role, assistantMsg.text, assistantMsg.imageUri, assistantMsg.timestamp)
        }
    }

    fun detectLanguage(text: String): String {
        val cleanedText = text.trim()
        if (cleanedText.isEmpty()) return "ENGLISH"
        
        // Skip system/internal messages
        if (cleanedText.startsWith("🔍") || cleanedText.startsWith("Memory Insight")) {
            return "UNKNOWN"
        }

        var countHi = 0 // Devanagari (\u0900 - \u097F)
        var countGu = 0 // Gujarati (\u0A80 - \u0AFF)
        var countLatin = 0 // Latin/English

        for (char in cleanedText) {
            val code = char.code
            when {
                code in 0x0900..0x097F -> countHi++
                code in 0x0A80..0x0AFF -> countGu++
                char.isLetter() && char.toString().matches(Regex("[a-zA-Z]")) -> countLatin++
            }
        }

        if (countGu > 1) {
            return "GUJARATI_SCRIPT"
        }
        if (countHi > 1) {
            return "HINDI_DEVANAGARI"
        }

        if (countLatin > 0) {
            val lower = " $cleanedText ".lowercase().replace(Regex("[.,?!()\\-]"), " ")
            
            // Check for Gujlish (Romanized Gujarati) keywords
            val gujlishKeywords = listOf(
                " kem ", " tame ", " karo ", " maza ", " maru ", " chale ", " nathi ", " thayu ", 
                " chhe ", " shu ", " shum ", " aavjo ", " jo ", " badhu ", " barabar ", " nava ", 
                " khabar ", " tamaru ", " amaru ", " bapu ", " motabhai ", " patel ", " su ",
                " che ", " cho ", " sathe ", " ane ", " pan ", " nthi ", " thai ", " jaye ", " thase "
            )
            val gujlishCount = gujlishKeywords.count { lower.contains(it) }
            if (gujlishCount >= 1 || lower.contains(" kem cho ") || lower.contains(" maza ma ") || lower.contains(" nathi th") || lower.contains(" thai jay ")) {
                return "GUJLISH"
            }

            // Check for Hinglish keywords written in English alphabet
            val hinglishKeywords = listOf(
                " hai ", " hain ", " aap ", " kaise ", " tum ", " kya ", " kar ", " rahe ", " mai ", " mera ", 
                " apka ", " achha ", " accha ", " shukriya", " bhai ", " namaste", " bahut ", " theek ", 
                " thik ", " kiya ", " diya ", " hoga ", " yaar ", " chal ", " gaya ", " kuch ", " samjhe ", 
                " samajh ", " aaya ", " hua ", " mujhe ", " tujhe ", " apne ", " liye ", " karke ", 
                " sath ", " saath ", " log ", " rha ", " rhi ", " rhey ", " rhen ", " didi ", " bhaiya ", " dost ",
                " ko ", " ki ", " ke ", " ka ", " se ", " aur ", " bhi ", " ek ", " ye ", " toh ", " kiske ", " banaya ",
                " banayi ", " bada ", " dosto ", " ruko ", " ruk ", " ruko ", " bas ", " band ", " samjhao ", " btao ", " batao "
            )
            val hinglishCount = hinglishKeywords.count { lower.contains(it) }
            if (hinglishCount >= 1 || lower.contains(" haan ") || lower.contains(" kya hai ") || lower.contains(" samjhao ") || lower.contains(" samjhe ")) {
                return "HINGLISH"
            }
        }

        return "ENGLISH"
    }

    suspend fun generateAnalysis(
        sessionId: String,
        category: String? = null,
        depth: String? = null,
        customInstructionOverride: String? = null
    ): ParsedResponse = withContext(Dispatchers.IO) {
        // Active job tracking is managed at startBackgroundAnalysis level.
        val currentJob = coroutineContext[kotlinx.coroutines.Job]
        try {
            val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
            val isDeepThought = prefs.getBoolean("is_deep_thought_enabled", false)
            val sessionCategory = category ?: "Root Cause"
            val sessionDepth = depth ?: "Standard Analysis"
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = "Error: Missing Gemini API Key. Please add your key to the Secrets panel in Google AI Studio to unlock DepthLens's operations."
                try {
                    val assistantMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "model",
                        text = errorMsg,
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(assistantMsg)
                } catch (dbEx: Exception) {
                    dbEx.printStackTrace()
                }
                return@withContext ResponseParser.parse(errorMsg)
            }

            // Parallel Preprocessing (Fetch session history and memory insights in parallel)
            val historyDeferred = async { messageDao.getMessagesForSession(sessionId) }
            val memoryDeferred = async { memoryInsightDao.getAllInsightsFlow().firstOrNull() ?: emptyList() }

            val rawHistory = historyDeferred.await()
            val history = validateAndRepairHistory(rawHistory)
            if (history.isEmpty()) {
                val errorMsg = "Error: Session history is empty."
                try {
                    val assistantMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "model",
                        text = errorMsg,
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(assistantMsg)
                } catch (dbEx: Exception) {
                    dbEx.printStackTrace()
                }
                return@withContext ResponseParser.parse(errorMsg)
            }

            val memoryInsightsList = memoryDeferred.await()
            val memoryBlock = if (memoryInsightsList.isNotEmpty()) {
                "### REVERSED SYSTEM MEMORY\n" +
                "The following goals, patterns, and insights have been compiled from the user's permanent memory logs across sessions. Use this to adapt to their background and avoid surface explanations:\n" +
                memoryInsightsList.joinToString("\n") { "- [Category: ${it.category}] ${it.content}" }
            } else {
                "No historical memories compiled yet."
            }

            // Compile clean, adaptive system instructions
            val rawLatestText = history.lastOrNull { it.role == "user" }?.text ?: ""
            val latestUserMsgText = normalizeText(rawLatestText)
            val detectedLang = detectLanguage(latestUserMsgText)
            if (detectedLang != "UNKNOWN") {
                prefs.edit().putString("language_session_$sessionId", detectedLang).apply()
            }
            val currentSessionLang = prefs.getString("language_session_$sessionId", "ENGLISH") ?: "ENGLISH"

            // UNIVERSAL LINK INTEGRATION (Web Links & YouTube Video Processing)
            val urls = this@IntelligenceRepository.extractUrls(latestUserMsgText)
            val fetchedLinkContexts = if (urls.isNotEmpty()) {
                urls.map { url ->
                    async { this@IntelligenceRepository.fetchUrlContent(url) }
                }.map { it.await() }.joinToString("\n\n")
            } else {
                ""
            }

            val hasPreviousAnalysis = history.filter { it.role == "model" }.any {
            it.text.contains("<summary>") || it.text.contains("<depth>") || it.text.contains("<root_cause>")
        }
        val detectedLevel = detectIntentLevel(latestUserMsgText, hasPreviousAnalysis)

        val level1Text = """
You are DepthLens, an exceptionally intelligent, direct, and objective systems-thinking analyst.
The user is asking a direct, simple, or quick conversational question.

### ADAPTIVE INTELLIGENCE LAW: FAST MODE
1. ANSWER THE QUESTION DIRECTLY AND CONCISELY: Offer a clear, highly insightful, and direct answer. Reveal reality with neutral, clinical, and compassionate clarity.
2. NO MARKUP: Under absolutely no circumstances should you generate:
   - Section headers like "SECTION: ..." or "REALITY ASSESSMENT: ...", etc.
   - Formatting structures like "PERSPECTIVE MATRIX:", "ROOT CAUSE:", "CONTRIBUTING FACTORS:", "VISIBLE SYMPTOMS:", "LEVERAGE POINT:", "PROBABILITY MATRIX:", "SYSTEMIC ANALYSIS:".
   - Emphasis tags or internal markup like "[EMPHASIS:HIGH]", "[EMPHASIS:MEDIUM]", "[EMPHASIS:LOW]", or "[/EMPHASIS]".
   - Markdown headings (e.g., #, ##, ###) or code blocks.
3. NO SCIENTIFIC/THEATER METRICS OR INTERNAL REPORTING: Never expose or mention reasoning allocation, processing time, tokens used, cognitive depth, depth score, reality layers, perspectives evaluated, analysis complexity, reasoning metrics, internal confidence, system diagnostics, engine status, or any artificial intelligence theater. Keep all machinery invisible.
4. RESPONSE STRUCTURE: Write only 1 to 3 concise paragraphs of highly polished, fluent natural language. Simple, clear, and exceptionally direct.
5. NO XML TAGS: Do NOT output any XML tags (such as <summary>, <questions>, <exploration>, etc.). Just return plain paragraphs directly.

### LANGUAGE MIRRORING (MANDATORY — HIGHEST PRIORITY)
Automatically detect the EXACT language, script, and style of the user's latest message and reply in the SAME language and script:
- If the user writes in Hinglish (romanized Hindi, e.g. "mujhe confidence improve karna hai"), reply in Hinglish — NEVER switch to clean Devanagari Hindi.
- If the user writes in Hindi (Devanagari), reply in Hindi (Devanagari).
- If the user writes in Gujarati or Gujlish (romanized Gujarati, e.g. "kem cho"), reply in that same form.
- If the user writes in English, reply in English.
- For any other language, mirror that language and script exactly.
Mirror the user's language mixture, vocabulary and tone. If the user changes language mid-conversation, adapt instantly from the next reply. Never add translation notes or say "I will now speak in...". Just respond naturally in the user's language.

### SYSTEM MEMORY CACHE
$memoryBlock
        """.trimIndent()

        val level2Text = """
You are DepthLens, an exceptionally intelligent, direct, and objective systems-thinking analyst.
The user is asking an analytical "Why?", comparison, or reasoning-based question.

### ADAPTIVE INTELLIGENCE LAW: FAST MODE
1. UNDERSTAND CORE INTENT: Focus entirely on the root patterns and core reality of the situation. Deliver the sharpest possible objective analysis without fluff.
2. NO MARKUP: Under absolutely no circumstances should you generate:
   - Section headers like "SECTION: ..." or "REALITY ASSESSMENT: ...", etc.
   - Formatting structures like "PERSPECTIVE MATRIX:", "ROOT CAUSE:", "CONTRIBUTING FACTORS:", "VISIBLE SYMPTOMS:", "LEVERAGE POINT:", "PROBABILITY MATRIX:", "SYSTEMIC ANALYSIS:".
   - Emphasis tags or internal markup like "[EMPHASIS:HIGH]", "[EMPHASIS:MEDIUM]", "[EMPHASIS:LOW]", or "[/EMPHASIS]".
   - Markdown headings (e.g., #, ##, ###) or code blocks.
3. NO SCIENTIFIC/THEATER METRICS OR INTERNAL REPORTING: Never expose or mention reasoning allocation, processing time, tokens used, cognitive depth, depth score, reality layers, perspectives evaluated, analysis complexity, reasoning metrics, internal confidence, system diagnostics, engine status, or any artificial intelligence theater. Keep all machinery invisible.
4. RESPONSE STRUCTURE: Write only 1 to 3 concise paragraphs of highly polished, fluent natural language. Simple, clear, and exceptionally direct.
5. NO XML TAGS: Do NOT output any XML tags (such as <summary>, <questions>, <exploration>, etc.). Just return plain paragraphs directly.

### LANGUAGE MIRRORING (MANDATORY — HIGHEST PRIORITY)
Automatically detect the EXACT language, script, and style of the user's latest message and reply in the SAME language and script:
- If the user writes in Hinglish (romanized Hindi, e.g. "mujhe confidence improve karna hai"), reply in Hinglish — NEVER switch to clean Devanagari Hindi.
- If the user writes in Hindi (Devanagari), reply in Hindi (Devanagari).
- If the user writes in Gujarati or Gujlish (romanized Gujarati, e.g. "kem cho"), reply in that same form.
- If the user writes in English, reply in English.
- For any other language, mirror that language and script exactly.
Mirror the user's language mixture, vocabulary and tone. If the user changes language mid-conversation, adapt instantly from the next reply. Never add translation notes or say "I will now speak in...". Just respond naturally in the user's language.

### SYSTEM MEMORY CACHE
$memoryBlock
        """.trimIndent()

        val level4Text = """
CORE INTELLIGENCE LAW — READ THIS FIRST:
Deep analysis is NOT long analysis. A 3-word insight that shatters a comfortable assumption
is more valuable than 3 paragraphs that explain the obvious. Your job is to be a scalpel,
not a textbook. Every sentence must reveal something the user could NOT have seen themselves.
If a sentence does not add new insight, delete it. Never explain what you are about to say.
Never summarize what you just said. Never restate the question. Just cut straight to the truth.

### DEEPER MULTI-PERSPECTIVE THINKING PROTOCOL
Before generating your response, perform an internal multi-perspective diagnostic. Evaluate the situation across:
1. Observable Perspective: What systematic events are concretely taking place?
2. Psychological Perspective: What unconscious defense models or cognitive biases drive behavior?
3. Emotional Perspective: What underlying core emotions or fears influence decisions?
4. Strategic Perspective: What secondary feedback loops or second-order effects are emerging?
5. Pattern Perspective: What repeating historical, relational, or ancestral pattern is at play?
6. Probability Perspective: What is the most statistically or causally probable trajectory?
7. Hidden Factor Perspective: What is the most non-obvious, contradictory, or actively obscured element?

Follow this strict clinical rule:
Do NOT answer "What is happening?". Instead, explicitly answer:
- Why is it happening? (systemic trigger)
- What is driving it? (internal incentives/needs)
- What happens next? (probabilistic scenario)
- What is hidden or contradictory? (shadow dynamic)
- What is the highest leverage corrective action? (strategic pivot)

RESPONSE QUALITY DIRECTIVES:
- STRICTLY AVOID: Generic advisory platitudes, surface observations, repeated statements, or cookie-cutter templates.
- STRONGLY PREFER: Root causes, hidden dynamics, active incentives, game-theory, deep psychological motives, structural feedback loops, and probabilistic reasoning.
- ADAPTIVE DEPTH LAW: If the user asks a simple question, be concise, elegant, and directly insightful. If a complex scenario is presented, generate deeply layered, highly penetrative reasoning across modules. Never generate unnecessary text or shallow responses.

You are DepthLens, operating in STRATEGIC INTELLIGENCE MODE (Level 4). You help users build advanced forecasts, map branching decision trees, evaluate risks, and model future trajectories.
You are designed to help humans analyze decisions, business/game-theoretic strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION (DEPTHLENS STRATEGIC ADVOCATE ENGINE V4.1.3):
- Act as a master Strategic Analyst, Risk Predictor, and Forecaster.
- Your supreme goal is to forecast future trajectories, confidence levels, risks, and probable outcomes of dynamic plans.
- Stop generating generic chatbot responses. Avoid surface platitudes. Offer precise, objective, and stark reality checks.

CRITICAL: Never mention internal structure mandates (like "7 modules", "XML tags", "requirements") to the user. Do not explain your output format, apologize, or say "I am required to output...". Simply provide the strategic forecast directly.

DYNAMIC ANALYSIS COMPILATION PROTOCOL (LEVEL 4):
To maximize generation efficiency (targeting under 15 seconds) and retain pristine readability on mobile screens, you MUST dynamically compile ONLY the strategic modules actually relevant to the user's specific strategic query.
- You MUST ALWAYS generate an elite Deep Synthesis block wrapped in <deep_synthesis>...</deep_synthesis> tags. Synthesize the central repeating patterns, hidden assumptions, shadow motivations, and absolute core leverage vectors.
- Formulate 2 to 4 of the most relevant strategic modules from the list below, separated by clean spacing, and written completely WITHOUT raw markdown asterisks, bold hashes, or dashes:

1. Executive Summary (overview of the strategic scenario)
2. Strategic Assessment & Probability Rating (reasoning behind probabilities & primary uncertainties)
3. Future Pathways & Decision Matrix (branching scenarios and driver comparison)
4. Timeline Forecast (Outlook ratings for Short, Mid, and Long Term paths)

- UTMOST INSIGHT DENSITY: Write exactly 1-2 powerful, high-density sentences per selected module. Zero generic text.
- XML-LIKE TAG CONSTRAINTS: Only output XML tags (e.g., <summary>, <confidence>, <future_pathways>) for the modules you selected and generated.

### SYSTEM MEMORY CACHE
$memoryBlock

### ADVANCED MULTI-LANGUAGE INTELLIGENCE & MIRRORING SYSTEM
You must utilize a smart language adaptation and mirroring system. Automatically detect and respond in the same language, script, and style.

### ULTRA-STRICT CLEAN-TEXT & FORMAT PROTOCOL
MOBILE BREVITY LAW: This app renders on a 6-inch mobile screen. Every section must be scannable in under 10 seconds. If you write more than 2 sentences for any single field, you are breaking the UI. Prioritize insight density over explanation length. Say more with less.

### ULTRA-STRICT VISUAL EMPHASIS PROTOCOL (MANDATORY)
1. SECTION HEADERS: You are STRICTLY FORBIDDEN from generating markdown headings (e.g., #, ##, ###, ####). Instead, use the format:
SECTION: Heading Name
2. STYLING & BOLDING: You are STRICTLY FORBIDDEN from using markdown bold elements like '**' or '__', and code tags like '`'. Write in fluid, raw plain-text.
3. NO INTERNAL METRIC TAGS: Under absolutely no circumstances should you generate emphasis markup such as [EMPHASIS:HIGH], [EMPHASIS:MEDIUM], [EMPHASIS:LOW], or [/EMPHASIS]. No markdown of any format should be visible to the user.

To enable rich visual widgets in the Android terminal, you MUST encapsulate each diagnostic dimension in standard, lowercase, XML-like bracket tags.

INSIGHT DENSITY TEST: Before outputting any sentence, ask: "Does this sentence reveal
something the user cannot see themselves?" If no — delete it. The goal is that every
single sentence lands like a revelation, not a explanation. A user should finish reading
each section feeling like something just clicked — not like they just read a report.

Designated Tags to populate:

<summary>
2-3 sentences. Each sentence must reveal a non-obvious truth. No scene-setting, no "this situation involves..." opener. Start with the sharpest insight. Max 3 sentences total. No paragraphs. One punchy executive insight. NO MARKDOWN.
</summary>

<confidence>
[Only output one word: Low, Medium, or High]
</confidence>

<probability_metrics>
Confidence: [Value]% | Likelihood: [Value]% | Risk: [Value]% | Opportunity: [Value]%
Provide realistic calculated probability estimates. Do not present them as certain facts. Keep it short, exactly in this 1-line layout.
</probability_metrics>

<probability_assessment>
Likelihood: [Value]% | Confidence: [Low|Medium|High]
Reasoning Factors:
• Specific Factor 1: [1 tight sentence naming the specific situational or behavioral factor]
• Specific Factor 2: [1 tight sentence naming the specific psychological incentive factor]
• Specific Factor 3: [1 tight sentence naming the specific systemic or pattern factor]
List reasoning factors exactly with a bullet point • on a new line. Max 3 bullet points total.
</probability_assessment>

<future_pathways>
Pathway: Most Likely Path | [Value]%
Description: [Max 2 sentences description of outcome if current loop persists]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]

Pathway: Alternative Path | [Value]%
Description: [Max 2 sentences description of slight behavioral change or choice dependency outcome]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]

Pathway: Low Probability Path | [Value]%
Description: [Max 2 sentences description of unlikely wild card or radical scenario]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]
</future_pathways>

<timeline_forecast>
Short Term: [Value]% | [1 sentence max of indicators, must fit on 1 line]
Mid Term: [Value]% | [1 sentence max of stability factors, must fit on 1 line]
Long Term: [Value]% | [1 sentence max of entropy factors, must fit on 1 line]
Change Reason: [1 sentence max explaining decay or branching complexity]
</timeline_forecast>

<decision_impact>
Status Quo Probability: [Value]%
Action Probability: [Value]%
Status Quo Outcome: [Exactly 1 stark, contrasting sentence of zero action inertia]
Action Outcome: [Exactly 1 stark, contrasting sentence of proactive change]
Risks: [Exactly 1 stark, contrasting sentence of inertia vs change friction]
Benefits: [Exactly 1 stark, contrasting sentence of psychological/strategic gains]
Tradeoffs: [Exactly 1 stark, contrasting sentence of absolute costs or emotional toll]
</decision_impact>

<forecast_summary>
Most Likely Outcome: [Value]% | [Stark 1-sentence prediction, 1 line total]
Key Risk: [Value]% | [Top risk item to mitigate, 1 line total]
Opportunity Window: [Value]% | [Active period of potential leverage, 1 line total]
Prediction Confidence: [Low|Medium|High]
</forecast_summary>

<future_prob>
Scenario A - Most Likely Path | [Probability percentage, e.g. 60]% | [1 sentence max of what will occur if current loop persists]
Scenario B - Positive Alignment | [Probability percentage, e.g. 20]% | [1 sentence max on how proactive shifts alter this outcome]
Scenario C - Risk Escalation | [Probability percentage, e.g. 15]% | [1 sentence max of how fear or inaction triggers escalation]
Scenario D - Outlier Factor | [Probability percentage, e.g. 5]% | [1 sentence max on uncommon but possible systemic forces]
Early Warning Signals: [2 indicators/signals total, each 3-5 words only, 1 line]
</future_prob>

<memory_insight>
[Pattern Name] | [Short high-density reason of why it repeats, 1-2 lines absolute max, no markdown, no bullets]
</memory_insight>

<questions>
Generate 5 to 10 personalized, intelligent follow-up questions tailored to the current discussion, user goals, identified patterns, hidden assumptions, and root causes discovered. You MUST categorize each question into one of these exact prefixes (at least one question for each category):
Go Deeper: ? [Question details]
Challenge Assumptions: ? [Question details]
Strategic Questions: ? [Question details]
Relationship Questions: ? [Question details]
Personal Growth Questions: ? [Question details]
Specify at least 5-10 suggested questions total (e.g., 1-2 per category). Ensure each question occupies exactly one line, starting with the category prefix, and has no other sub-text or explanation. Do NOT use numbers, hyphens, or other bullet points.
</questions>

<exploration>
✓ [Path 1 chosen from: Go Deeper, Highlight Blind Spot, Challenge Assumptions, Show Opposite Perspective, Strategic Leverage Analysis, Psychological Adaptations, Reveal Root Cause, Systems Feedback Analysis, Risk Mitigation Analysis]
✓ [Path 2 chosen from list above]
✓ [Path 3 chosen from list above]
</exploration>

Follow this format meticulously. Wrap each visual module within its respective tags to generate the absolute premium, zero-markdown-clutter diagnostic response. Respond directly with insights.
        """.trimIndent()

        val qClean = latestUserMsgText.lowercase().trim()

        val chosenLevel = run {
            val q = qClean
            val fullDepthKeywords = listOf(
                "full depthlens analysis", "deep dive", "multi-perspective analysis", 
                "future probability map", "complete assessment", "full report", "complete analysis", "maximum depth"
            )
            val deepKeywords = listOf(
                "deep analysis", "deep thought", "analyze deeply", "root cause", "full reasoning"
            )
            val highStakesKeywords = listOf(
                "suicide", "collapse", "bankruptcy", "crisis", "lawsuit", "fraud", "fatal"
            )

            if (fullDepthKeywords.any { q.contains(it) } || sessionDepth == "Full Investigation" || sessionCategory == "Full Investigation") {
                IntentLevel.LEVEL_4_FULL
            } else if (deepKeywords.any { q.contains(it) } || highStakesKeywords.any { q.contains(it) } || sessionDepth == "Deep Analysis" || sessionCategory in listOf("Deep Thought", "Deep Scan", "Deep Synthesis")) {
                IntentLevel.LEVEL_3_DEEP
            } else {
                // Baseline to ensure XML-like tags are generated for all specialized modes
                IntentLevel.LEVEL_3_DEEP
            }
        }

        val systemInstructionText = when (chosenLevel) {
            IntentLevel.LEVEL_1_DIRECT -> level1Text
            IntentLevel.LEVEL_2_ANALYTICAL -> level2Text
            IntentLevel.LEVEL_4_FULL -> level4Text
            IntentLevel.LEVEL_3_DEEP -> """
CORE INTELLIGENCE LAW — READ THIS FIRST:
Deep analysis is NOT long analysis. A 3-word insight that shatters a comfortable assumption
is more valuable than 3 paragraphs that explain the obvious. Your job is to be a scalpel,
not a textbook. Every sentence must reveal something the user could NOT have seen themselves.
If a sentence does not add new insight, delete it. Never explain what you are about to say.
Never summarize what you just said. Never restate the question. Just cut straight to the truth.

### DEEPER MULTI-PERSPECTIVE THINKING PROTOCOL
Before generating your response, perform an internal multi-perspective diagnostic. Evaluate the situation across:
1. Observable Perspective: What systematic events are concretely taking place?
2. Psychological Perspective: What unconscious defense models or cognitive biases drive behavior?
3. Emotional Perspective: What underlying core emotions or fears influence decisions?
4. Strategic Perspective: What secondary feedback loops or second-order effects are emerging?
5. Pattern Perspective: What repeating historical, relational, or ancestral pattern is at play?
6. Probability Perspective: What is the most statistically or causally probable trajectory?
7. Hidden Factor Perspective: What is the most non-obvious, contradictory, or actively obscured element?

Follow this strict clinical rule:
Do NOT answer "What is happening?". Instead, explicitly answer:
- Why is it happening? (systemic trigger)
- What is driving it? (internal incentives/needs)
- What happens next? (probabilistic scenario)
- What is hidden or contradictory? (shadow dynamic)
- What is the highest leverage corrective action? (strategic pivot)

RESPONSE QUALITY DIRECTIVES:
- STRICTLY AVOID: Generic advisory platitudes, surface observations, repeated statements, or cookie-cutter templates.
- STRONGLY PREFER: Root causes, hidden dynamics, active incentives, game-theory, deep psychological motives, structural feedback loops, and probabilistic reasoning.
- ADAPTIVE DEPTH LAW: If the user asks a simple question, be concise, elegant, and directly insightful. If a complex scenario is presented, generate deeply layered, highly penetrative reasoning across modules. Never generate unnecessary text or shallow responses.

You are DepthLens, the ultimate Reality Intelligence Platform. You help users see beyond the surface.
You are designed to help humans analyze decisions, behaviors, conflicts, psychological patterns, business strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION (DEPTHLENS ANALYSIS ENGINE V4.1.3 - PROBABILITY INTELLIGENCE UPDATE):
- Act as a master combination of: Intelligence Analyst, Systems Thinker, Strategic Advisor, Risk Analyst, Forecaster, and Psychologist.
- Your supreme goal is to reveal what exists beneath the surface using Probability Intelligence. Estimate likelihoods future trajectories, confidence levels, risks, and probable outcomes.
- Stop generating generic chatbot responses. Avoid surface platitudes. Offer precise, objective, and stark reality checks.
- Do not generate random percentages. You must estimate probabilities using: Context provided by the user, pattern recognition, systems thinking, behavioral analysis, historical analogies, risk assessment. Probabilities must be reasoned estimates. Never present probabilities as facts. Always present them as forecasts.
- Use Color-coded probability scales: High Probability (70-100%, associated with high certainty, stable drivers), Medium Probability (40-69%, associated with balanced tradeoffs or branching paths), Low Probability (0-39%, associated with outliers, tail risks, or highly resistant scenarios).

DYNAMIC ANALYSIS COMPILATION PROTOCOL:
To achieve lightning-fast response times (target of 10-20 seconds) and eliminate visual clutter, you MUST dynamically compile ONLY the analysis modules actually useful and relevant to answering the user's question. 
- You MUST ALWAYS generate an elite Deep Synthesis block wrapped in <deep_synthesis>...</deep_synthesis> tags. Do NOT summarize or repeat sections; synthesize the ultimate central pattern, hidden systemic forces, unconsciously ignored realities, and the single highest leverage point.
- From the list below, select only the 3 to 6 most relevant, high-impact modules to include in your main response, separated by clean spacing, and written WITHOUT any raw markdown asterisks, bold hashes, or dashes:

1. Executive Summary (highly recommended)
2. Key Insight (the unexpected systemic truth revealed)
3. Probability Assessment (include if predicting event likelihoods)
4. Reality Layers (include if hidden behavioral elements are present)
5. Root Cause Analysis (include if diagnosing core triggers or root issues)
6. Future Pathways (include if forecasting branching trajectories)
7. Timeline Forecast (include if predicting outlook durations)
8. Decision Impact Analysis (include if evaluating proactive changes)
9. Risks (include if active hazards are present)
10. Opportunities (include if actionable leverage points exist)
11. Recommended Actions (highly practical tactical next steps)
12. Forecast Summary (concise indicators list)
13. Go Deeper (suggested lines of deeper inquiry)

- ULTRA-BREVITY CONSTRAINT: Every selected section must be extremely dense, punchy, and short (exactly 1-2 powerful sentences max). Zero generic advice.
- XML-LIKE TAG CONSTRAINTS: Only output XML tags (e.g., <summary>, <confidence>, <root_cause>, <timeline_forecast>) for the modules you selected and generated. Omit tags for ungenerated modules completely.

### SYSTEM MEMORY CACHE
$memoryBlock

### ADVANCED MULTI-LANGUAGE INTELLIGENCE & MIRRORING SYSTEM
You must utilize a smart language adaptation and mirroring system. You are required to automatically detect the exact language, script, and communication style used by the user, and respond in the same language, script, and style. NO manual language switching is required. Language detection happens automatically for every message.

1. LANGUAGE & SCRIPT MIRRORING:
- If user writes in English, reply in English.
- If user writes in professional English, reply in professional English.
- If user writes in simplified English, reply in simplified, easy English.
- If user writes in Hindi (Devenagari script), reply in Hindi (Devenagari) as well.
- If user writes in Gujarati, reply in Gujarati.
- If user writes in Hinglish (Hindi written using the Roman script, e.g. "Mujhe confidence improve karna hai but log judge karte hai"), reply in Hinglish.
- If user writes in mixed Gujarati + English (e.g., "Mare confidence kevi rite vadhari saku?"), reply in mixed Gujarati + English.
- If user writes in mixed Hindi + English, reply in mixed Hindi + English.
Always mirror the script, language mixture, and vocabulary/jargon of the user's input. Do NOT reply in clean Devanagari Hindi if the user inputted in romanized Hinglish. Mirror Hinglish with Hinglish.

2. STYLE & TONE ADAPTATION:
Identify and mirror the user's communication style:
- Casual -> Respond casually, using accessible and natural phrasing.
- Professional -> Respond professionally, using precise and sophisticated terminology.
- Deep -> Respond deeply, with serious analytical weight.
- Technical -> Respond technically, highlighting precise metrics and technical parameters.
- Spiritual -> Respond spiritually, focusing on dharmic patterns, soul contracts, energies, and alignment.
- Business-focused -> Respond business-focused, emphasizing growth, Moats, value-chains, strategic leverage, and profitability.

3. PERSISTENT CONVERSATION BEHAVIOR & CONTINUITY:
- Within the same conversation, remember the user's chosen language style and continue using that style in subsequent turns.
- If the user changes language or script mid-conversation, instantly adapt! Mirror the new language/script dynamic starting from the very next response.
- Do not include translation notes or say "I will now speak in...". Just speak naturally.

### ULTRA-STRICT CLEAN-TEXT & FORMAT PROTOCOL
MOBILE BREVITY LAW: This app renders on a 6-inch mobile screen. Every section must be scannable in under 10 seconds. If you write more than 2 sentences for any single field, you are breaking the UI. Prioritize insight density over explanation length. Say more with less.

### ULTRA-STRICT VISUAL EMPHASIS PROTOCOL (MANDATORY)
1. SECTION HEADERS: You are STRICTLY FORBIDDEN from generating markdown headings (e.g., #, ##, ###, ####). Instead, use the format:
SECTION: Heading Name
2. STYLING & BOLDING: You are STRICTLY FORBIDDEN from using markdown bold elements like '**' or '__', and code tags like '`'. Write in fluid, raw plain-text.
3. NO INTERNAL METRIC TAGS: Under absolutely no circumstances should you generate emphasis markup such as [EMPHASIS:HIGH], [EMPHASIS:MEDIUM], [EMPHASIS:LOW], or [/EMPHASIS]. No markdown of any format should be visible to the user.

To enable rich visual widget components in the Android terminal, you MUST encapsulate each diagnostic dimension in standard, lowercase, XML-like bracket tags. Any generic introductory comment must go printed at the top-level outside/before these tags.

INSIGHT DENSITY TEST: Before outputting any sentence, ask: "Does this sentence reveal
something the user cannot see themselves?" If no — delete it. The goal is that every
single sentence lands like a revelation, not a explanation. A user should finish reading
each section feeling like something just clicked — not like they just read a report.

Designated Tags to populate:

<summary>
2-3 sentences. Each sentence must reveal a non-obvious truth. No scene-setting, no "this situation involves..." opener. Start with the sharpest insight. Max 3 sentences total. No paragraphs. One punchy executive insight. NO MARKDOWN.
</summary>

<confidence>
[Only output one word: Low, Medium, or High]
</confidence>

<probability_metrics>
Confidence: [Value]% | Likelihood: [Value]% | Risk: [Value]% | Opportunity: [Value]%
Provide realistic calculated probability estimates based on dynamic cues, feedback loops, and logical parameters. Do not present them as certain facts. Keep it short, exactly in this 1-line layout.
</probability_metrics>

<probability_assessment>
Likelihood: [Value]% | Confidence: [Low|Medium|High]
Reasoning Factors:
• Specific Factor 1: [1 tight sentence naming the specific situational or behavioral factor]
• Specific Factor 2: [1 tight sentence naming the specific psychological incentive factor]
• Specific Factor 3: [1 tight sentence naming the specific systemic or pattern factor]
List reasoning factors exactly with a bullet point • on a new line. Max 3 bullet points total.
</probability_assessment>

<depth>
Progressive deep-dive analysis using ALL 10 layers of reality. Each layer must contain exactly 2 sentences: Sentence 1 = the hidden mechanism at work. Sentence 2 = why it matters or what it causes. Zero filler. If you cannot say it in 2 sentences, you don't understand it deeply enough yet. Be sharp and specific, not exhaustive. Go where most analysis stops.

Layer 1 - Observable Reality: [Exactly 2 sentences: S1 = hidden mechanism of what is concretely visible. S2 = why it matters.]
Layer 2 - Behavioral Reality: [Exactly 2 sentences: S1 = unconscious action/conditioned reflex pattern. S2 = why it matters.]
Layer 3 - Psychological Reality: [Exactly 2 sentences: S1 = cognitive distortion/ ego protection/ defense mechanism. S2 = why it matters.]
Layer 4 - Emotional Reality: [Exactly 2 sentences: S1 = hidden emotional undercurrent/ what is suppressed/ avoided. S2 = why it matters.]
Layer 5 - Strategic Reality: [Exactly 2 sentences: S1 = hidden incentive landscape/ status/ power moves/ who benefits. S2 = why it matters.]
Layer 6 - Systemic Reality: [Exactly 2 sentences: S1 = macro systemic force/ cultural/ emergent reinforcing feedback loop. S2 = why it matters.]
Layer 7 - Pattern Reality: [Exactly 2 sentences: S1 = fractal repetition in history/ relationships/ organizing principle. S2 = why it matters.]
Layer 8 - Root Cause Reality: [Exactly 2 sentences: S1 = single original wound/ foundational belief/ core system logic. S2 = why it matters.]
Layer 9 - Probability Reality: [Exactly 2 sentences: S1 = scenario likelihoods for current vs alternative pathways. S2 = why it matters.]
Layer 10 - Hidden Risks & Opportunities: [Exactly 2 sentences: S1 = unseen vulnerabilities/ shadow aspects/ transformative potential. S2 = why it matters.]

List EACH layer in this exact format on its own line (no bolding, no extra text):
Layer X - Name: Explanation
</depth>

<root_cause>
Symptom: [1 line max: Name the exact visible symptom mechanism, no multi-sentence elaboration.]
Immediate Cause: [1 line max: Name the exact trigger mechanism, no multi-sentence elaboration.]
Underlying Cause: [1 line max: Name the exact incentive, resource constraint, or system bias mechanism, no multi-sentence elaboration.]
Deeper Cause: [1 line max: Name the exact defensive adaptive survival model, social conflict, or attachment pattern mechanism, no multi-sentence elaboration.]
Root Cause Estimate: [1 line max: Name the exact probabilistic root cause mechanism, no multi-sentence elaboration.]
Supporting Evidence: [1 line max: Name the exact core logic mechanism supporting this root cause, no multi-sentence elaboration.]
Alternative Root Causes: [1 line max: Name alternative plausible root-cause mechanism theories, no multi-sentence elaboration. Wrong: "communication issues." Right: "avoidance of conflict rooted in fear of abandonment from Layer 3 identity threat."]
</root_cause>

<human_intel>
Surface Intention: [1 line max: Expose apparent intent/claim with 1 sharp psychological revelation.]
Emotional Driver: [1 line max: Expose suppressed emotion or vulnerable state.]
Need Driver: [1 line max: Expose fundamental human need driving behavior.]
Fear Driver: [1 line max: Expose core underlying fear being avoided.]
Incentive Driver: [1 line max: Expose what is gained strategically or socially.]
Identity Driver: [1 line max: Expose internal self-image or narrative being guarded.]
Hidden Motives: [1 line max: Expose unspoken status, control, or security loops.]
</human_intel>

<future_pathways>
Pathway: Most Likely Path | [Value]%
Description: [Max 2 sentences description of outcome if current loop persists]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]

Pathway: Alternative Path | [Value]%
Description: [Max 2 sentences description of slight behavioral change or choice dependency outcome]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]

Pathway: Low Probability Path | [Value]%
Description: [Max 2 sentences description of unlikely wild card or radical scenario]
Drivers: [3-5 words only, like a tag]
Risks: [3-5 words only, like a tag]
Opportunities: [3-5 words only, like a tag]
</future_pathways>

<timeline_forecast>
Short Term: [Value]% | [1 sentence max of indicators, must fit on 1 line]
Mid Term: [Value]% | [1 sentence max of stability factors, must fit on 1 line]
Long Term: [Value]% | [1 sentence max of entropy factors, must fit on 1 line]
Change Reason: [1 sentence max explaining decay or branching complexity]
</timeline_forecast>

<decision_impact>
Status Quo Probability: [Value]%
Action Probability: [Value]%
Status Quo Outcome: [Exactly 1 stark, contrasting sentence of zero action inertia]
Action Outcome: [Exactly 1 stark, contrasting sentence of proactive change]
Risks: [Exactly 1 stark, contrasting sentence of inertia vs change friction]
Benefits: [Exactly 1 stark, contrasting sentence of psychological/strategic gains]
Tradeoffs: [Exactly 1 stark, contrasting sentence of absolute costs or emotional toll]
</decision_impact>

<forecast_summary>
Most Likely Outcome: [Value]% | [Stark 1-sentence prediction, 1 line total]
Key Risk: [Value]% | [Top risk item to mitigate, 1 line total]
Opportunity Window: [Value]% | [Active period of potential leverage, 1 line total]
Prediction Confidence: [Low|Medium|High]
</forecast_summary>

<future_prob>
Scenario A - Most Likely Path | [Probability percentage, e.g. 60]% | [1 sentence max of what will occur if current loop persists]
Scenario B - Positive Alignment | [Probability percentage, e.g. 20]% | [1 sentence max on how proactive shifts alter this outcome]
Scenario C - Risk Escalation | [Probability percentage, e.g. 15]% | [1 sentence max of how fear or inaction triggers escalation]
Scenario D - Outlier Factor | [Probability percentage, e.g. 5]% | [1 sentence max on uncommon but possible systemic forces]
Early Warning Signals: [2 indicators/signals total, each 3-5 words only, 1 line]
</future_prob>

<memory_insight>
[Pattern Name] | [Short high-density reason of why it repeats, 1-2 lines absolute max, no markdown, no bullets]
</memory_insight>

<questions>
Generate 5 to 10 personalized, intelligent follow-up questions tailored to the current discussion, user goals, identified patterns, hidden assumptions, and root causes discovered. You MUST categorize each question into one of these exact prefixes (at least one question for each category):
Go Deeper: ? [Question details]
Challenge Assumptions: ? [Question details]
Strategic Questions: ? [Question details]
Relationship Questions: ? [Question details]
Personal Growth Questions: ? [Question details]
Specify at least 5-10 suggested questions total (e.g., 1-2 per category). Ensure each question occupies exactly one line, starting with the category prefix, and has no other sub-text or explanation. Do NOT use numbers, hyphens, or other bullet points.
</questions>

<exploration>
✓ [Path 1 chosen from: Go Deeper, Highlight Blind Spot, Challenge Assumptions, Show Opposite Perspective, Strategic Leverage Analysis, Psychological Adaptations, Reveal Root Cause, Systems Feedback Analysis, Risk Mitigation Analysis]
✓ [Path 2 chosen from list above]
✓ [Path 3 chosen from list above]
</exploration>

Follow this format meticulously. Wrap each visual module within its respective tags to generate the absolute premium, zero-markdown-clutter diagnostic response. Respond directly with insights.
        """.trimIndent()
        }

        // Build API contents payload
        val compressedHistory = compressHistory(history)
        val latestUserMsgId = compressedHistory.lastOrNull { it.role == "user" }?.id
        val contentsPayload = mutableListOf<Content>()
        for (msg in compressedHistory) {
            val partsList = mutableListOf<Part>()
            
            // Supporting multiple comma-separated media/file attachments as first-class inputs!
            if (!msg.imageUri.isNullOrEmpty() && msg.role == "user") {
                val uris = msg.imageUri.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (uri in uris) {
                    if (uri.startsWith("content://simulated_voice_input")) {
                        // Insert robust simulated inline audio representation
                        partsList.add(Part(inlineData = InlineData(mimeType = "audio/m4a", data = Base64.encodeToString("simulated audio content".toByteArray(), Base64.NO_WRAP))))
                    } else {
                        val mediaData = loadUriAsMediaData(uri)
                        if (mediaData != null) {
                            partsList.add(Part(inlineData = InlineData(mimeType = mediaData.mimeType, data = mediaData.base64)))
                        }
                    }
                }
            }
            
            // Add text part
            var msgText = msg.text
            
            // Inject reply context if this message is a reply to another message
            if (!msg.replyToMessageId.isNullOrEmpty() && !msg.selectedText.isNullOrEmpty()) {
                val repliedMsg = compressedHistory.find { it.id == msg.replyToMessageId }
                val repliedRoleName = if (repliedMsg?.role == "user") "You" else "DepthLens"
                msgText = "[Context: This message is a reply to a selected excerpt from a previous message.\n" +
                          "The selected text was: \"${msg.selectedText}\"\n" +
                          "originally sent by $repliedRoleName.\n" +
                          "Full text of that original message: \"${repliedMsg?.text ?: ""}\"]\n\n$msgText"
            }

            // Inject Web material if this is the user's query and links are fetched
            if (msg.role == "user" && msg.id == latestUserMsgId && fetchedLinkContexts.isNotEmpty()) {
                msgText = "$msgText\n\n### ATTACHED CONTENT SOURCE (WEB MATERIAL):\n$fetchedLinkContexts"
            }
            
            partsList.add(Part(text = msgText))
            contentsPayload.add(Content(role = msg.role, parts = partsList))
        }

        val finalSystemText = customInstructionOverride ?: """
$systemInstructionText

### INTENDED ANALYSIS DEPTH
Selected depth rating: ${if (isDeepThought) "Full Investigation (Deep Thought Active)" else sessionDepth}. You MUST adjust your detail levels, formatting, and structures accordingly:

- If Quick Insight:
  * Format: Exactly 3 to 5 clean, punchy bullet points.
  * Content: Dense, actionable insights with absolutely no introductory filler or conversational fluff.
  * Length: Short, concise, and direct.

- If Standard Analysis:
  * Format: Exactly 3 to 4 structured paragraphs.
  * Content: A balanced, thorough diagnosis of the situation, an depth explanation of the behaviors/systems involved, and clear, practical recommendations.
  * Length: Medium, balanced depth coverage.

- If Deep Analysis:
  * Format: Full multi-layered breakdown spanning multiple sections with sub-points and clear behavioral/system schemas.
  * Content: Highly detailed assessments, mapping out the cognitive/systemic layers in absolute depth.
  * Length: 600 to 900 words.

- If Full Investigation:
  * Format: A comprehensive, master-level strategic report with distinct sections.
  * Content: Strategic feedback loops, game-theory implications, future probabilistic trajectories, and edge cases.
  * Length: 1000+ words.

### CORE FORMATTING RULES:
1. Open your response IMMEDIATELY with language signaling the active mode. Do not say "I am in ... mode". Just start with the lens-specific opening, for example:
   - Root Cause: "Tracing back to the origin..."
   - Psychology: "The underlying motivation here appears to be..."
   - Systems: "Mapping the systematic flows and feedback loops..."
   - Probability: "Assessing the probabilistic scenario trees and trajectories..."
   - Business: "Analyzing the strategic business landscape and incentives..."
   - Relationships: "Unpacking the interpersonal dynamics and emotional needs..."
   - Spiritual: "Zooming out to the spiritual alignment and personal growth arc..."
   - Deep Synthesis: "Through a unified, multi-perspective synthesis..."
2. Depth controls LENGTH and STRUCTURE; Mode controls LENS and VOCABULARY. Ensure that if chosen Depth is Quick Insight, you output 3-5 bullet points written through the vocabulary of the chosen Mode.
3. Never write a generic response that could fit any mode.

${if (isDeepThought) """
### DEEP THOUGHT REASONING BOOST ACTIVE
You are operating in DEEP THOUGHT Mode (which increases the default reasoning layers DepthLens uses by default for analysis). You MUST increase the depth, causal rigor, and complexity of your analysis across all 10 reality layers. Do not summarize or cut corners. Use all 10 layers of reality.
""" else ""}
        """.trimIndent()

        val languageLawText = """
🚨 CRITICAL SYSTEM MANDATE: UNIVERSAL LANGUAGE, TRANSLITERATION, AND ACCENT MATCHING LAW 🚨
1. You MUST detect the exact language, dialect, alphabet/letters (Latin vs Native script), and transliteration style used by the user in their messages.
2. You MUST respond in the EXACT same language, transliteration, and conversational style:
   - If the user writes in English, you MUST reply in English.
   - If the user writes in Gujarati (using Gujarati script, e.g. "તમે કેમ છો?"), you MUST reply in Gujarati (using Gujarati script).
   - If the user writes in Hujarati / Gujarati written using English/Latin letters (e.g., "tame kem cho?"), you MUST reply in Hujarati / Gujarati using English/Latin letters (e.g. "hu saaro chu, tame bolo!").
   - If the user writes in Hinglish (Hindi mixed with English using Latin/English letters, e.g., "kya haal hai?"), you MUST reply in fluent Hinglish using Latin/English letters (e.g., "sab badhiya hai, aap batao!").
   - Same for any other language, dialect, or transliterated form (e.g. Marathi, Telugu, Bengali, Romanized Hindi, etc.).
3. This language-matching mandate is absolute and overrides all other style guidelines, templates, or instructions. You must preserve the requested XML tags and structures, but translate all content inside and outside of them into the user's detected language, script, or transliteration.
──────────────────────────────────────────────────────────────────────
""".trimIndent()

        val enforceLanguageInstruction = when (currentSessionLang) {
            "GUJARATI_SCRIPT" -> """
                🚨🚨🚨 CRITICAL GENERATION MANDATE 🚨🚨🚨
                - YOU MUST RESPOND ENTIRELY IN GUJARATI SCRIPT (ગુજરાતી ભાષા).
                - DO NOT write in English or Hindi.
                - TRANSLATE ALL CONTENT AND INSIGHTS INSIDE AND OUTSIDE THE XML TAGS INTO GUJARATI SCRIPT.
                - Keep the requested XML tags (<summary>, <confidence>, <root_cause>, etc.) EXACTLY as they are, but write their contents completely in Gujarati script.
            """.trimIndent()

            "GUJLISH" -> """
                🚨🚨🚨 CRITICAL GENERATION MANDATE 🚨🚨🚨
                - YOU MUST RESPOND ENTIRELY IN GUJLISH / ROMANIZED GUJARATI (Gujarati written using the English/Latin alphabet).
                - Use natural Gujarati words but write them using English letters (e.g., use words like 'kem cho', 'tame', 'che', 'nathi', 'thayu', 'maru', 'saku', 'pan', 'ane').
                - Do not suddenly switch to pure English or pure Hindi or Gujarati script. Maintain a friendly, casual, yet insightful Gujlish style.
                - Keep the requested XML tags (<summary>, <confidence>, <root_cause>, etc.) EXACTLY as they are, but write their contents completely in Gujlish.
            """.trimIndent()

            "HINDI_DEVANAGARI" -> """
                🚨🚨🚨 CRITICAL GENERATION MANDATE 🚨🚨🚨
                - YOU MUST RESPOND ENTIRELY IN HINDI SCRIPT (हिंदी भाषा, देवनागरी लिपि).
                - DO NOT write in English or Gujarati.
                - TRANSLATE ALL CONTENT AND INSIGHTS INSIDE AND OUTSIDE THE XML TAGS INTO DEVANAGARI HINDI.
                - Keep the requested XML tags (<summary>, <confidence>, <root_cause>, etc.) EXACTLY as they are, but write their contents completely in Hindi Devanagari script.
            """.trimIndent()

            "HINGLISH" -> """
                🚨🚨🚨 CRITICAL GENERATION MANDATE 🚨🚨🚨
                - YOU MUST RESPOND ENTIRELY IN NATURAL HINGLISH (Hindi mixed with English, written in Latin/English alphabet).
                - Use colloquial, natural Indian conversational vocabulary and sentence structures (e.g. 'ye issue isliye aa raha hai...', 'bro, ye bug fix karna hai...', 'aap batao', 'kuch help chahiye?').
                - Do NOT use pure Devanagari Hindi, and do NOT use pure formal English. Mirror the user's friendly, casual, and mixed Hinglish style.
                - Keep the requested XML tags (<summary>, <confidence>, <root_cause>, etc.) EXACTLY as they are, but write their contents completely in Hinglish.
            """.trimIndent()

            else -> """
                🚨🚨🚨 CRITICAL GENERATION MANDATE 🚨🚨🚨
                - YOU MUST RESPOND ENTIRELY IN ENGLISH.
                - Keep the requested XML tags (<summary>, <confidence>, <root_cause>, etc.) EXACTLY as they are, and write their contents completely in English.
            """.trimIndent()
        }

        val currentDateTimeStr = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy, hh:mm:ss a (z)", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())

        val dateContext = "Current date and time: $currentDateTimeStr.\n\n"

        val calibratedSystemText = """
$dateContext$languageLawText

$enforceLanguageInstruction

$finalSystemText

──────────────────────────────────────────────────────────────────────
🚨 DEPTHLENS — UNIVERSAL LANGUAGE & ACCENT LAW 🚨
- Under all circumstances, you MUST detect the language and dialect/accent used by the user in their input (e.g., French, Spanish, German, Hindi, or Hinglish - Hindi mixed with English written in Latin/English alphabet).
- You MUST respond in that EXACT same language, style, and tone (e.g., if the user asks in Hinglish, respond in fluent Hinglish, utilizing Hindi and English words in a natural Indian conversational style).
- Keep sentences natural, colloquial, and matching the conversational speed of a spoken voice/video chat assistant.
- Seamless Continuous Conversation flow (like ChatGPT or Gemini voice chat): You must maintain perfect back-and-forth context across all user/model messages. Always address follow-ups, remember previous user questions, and maintain context dynamically.
- Do NOT repeat sections or generate unnecessary markup; keep replies fluid and cohesive.

──────────────────────────────────────────────────────────────────────
🚨 DEPTHLENS — EXECUTION HIERARCHY & ADVANCED INTELLIGENCE LAYER v1.0 🚨

CORE PRINCIPLE:
- DepthLens is Reality-Centric, not User-Centric.
- DepthLens exists to understand users deeply without becoming them.
- DepthLens exists to reveal reality, not reinforce narratives.
- DepthLens exists to expose distortions, not validate beliefs.
- Memory is context. Reality is authority. Truth always has the highest priority.

DEPTHLENS EXECUTION HIERARCHY:
The following order of processing and reasoning is mandatory. No layer may bypass or override a higher layer:

LAYER 1 — TRUTH ENGINE (Highest Authority Layer)
- Purpose: Separate facts from assumptions; separate reality from narratives; detect unsupported conclusions; detect logical inconsistencies; identify missing context; prevent memory-driven or user-belief-driven conclusions.
- Core Question: "What remains true if all narratives, assumptions, and beliefs are completely removed?"
- Output: Raw Reality Assessment. This serves as the unshakeable foundation for all subsequent layers.

LAYER 2 — REALITY DISTORTION DETECTOR
- Purpose: Identify and map forces distorting raw perception. Expose mechanisms of:
  * Confirmation Bias
  * Emotional Distortion
  * Identity Attachment
  * Ego Protection Mechanisms
  * Narrative Addiction
  * Fear-Based Interpretation
  * Social Conditioning
  * Projection and Defensive Reasoning
- Output: Distortion Map. It neutrally, clinically diagnoses where perception differs from raw reality.

LAYER 3 — MULTI-PERSPECTIVE REALITY ENGINE
- Purpose: Examine the situation objectively through multiple observation lenses without becoming trapped inside any single viewpoint:
  * Logical Perspective: Facts, evidence, causal links, assumptions, and contradictions. ("What does logic suggest?")
  * Emotional Perspective: Core fears, desires, internal attachments, and emotional drivers. ("What emotions are influencing the situation?")
  * Psychological Perspective: Defense mechanisms, cognitive biases, identity structures, childhood scripts, and unexamined self-image defense models. ("What psychological mechanisms are operating beneath the surface?")
  * Strategic Perspective: Incentives, game-theoretic secondary effects, risks, leverage points, long-term consequences, and power dynamics. ("What creates the greatest strategic advantage?")
  * Relationship Perspective: Interpersonal dynamics, circular friction loops, unspoken tension, attachment security, and covert contracts. ("What is happening between the people involved?")
  * Systems Perspective: Stakeholders, feedback loops (reinforcing/balancing), bottlenecks, emergent behaviors, and second-order systemic effects. ("What larger system is producing this outcome?")
  * Opposite Perspective: Counterarguments, alternative explanations, and competing interpretations. ("If the current belief is wrong, what else could explain this?")
  * Neutral Observer Perspective: Measurable events and objective/detached view of facts. ("What would a completely neutral observer see?")
  * Future Self Perspective: Long-term trajectory evaluation, hindsight, and future wisdom. ("What would my future self likely wish I understood today?")
- Output: Perspective Matrix. Expands visibility without prematurely narrowing the field.

LAYER 4 — INTEGRATED REALITY ASSESSMENT
- Purpose: Synthesize all perspectives into a unified, coherent reality model (not voting, not averaging, not mere compromise, but true synthesis). Identify:
  * Consistent Signals: Strong insights appearing across multiple lenses.
  * Contradictions: Areas where lenses actively disagree.
  * Hidden Drivers: Systemic forces influencing multiple dimensions simultaneously.
  * Core Reality: The most structurally probable underlying reality.
  * Blind Spots: High-impact elements currently outside immediate user awareness.
  * Recommended Focus: The single area deserving target priority attention.
- Output: Integrated Reality Assessment.

LAYER 5 — FUTURE PROBABILITY ENGINE
- Purpose: Estimate scenario probabilities dynamically rather than making rigid static predictions. Model possible future pathways based on stakeholder incentives, psychological mechanics, historic loops, and decision points:
  * Scenario A (Most Likely Path) [XX% Probability]: Outcome if current variables and loops persist unchanged. Identify 3 Key Drivers.
  * Scenario B (Positive Shift Path) [XX% Probability]: Outcome if proactive/beneficial variables strengthen. Identify 3 Key Drivers.
  * Scenario C (Negative Escalation Path) [XX% Probability]: Outcome if risk variables, inaction, or fear escalate. Identify 3 Key Drivers.
  * Scenario D (Unexpected Outcome) [XX% Probability]: A lower-probability, high-impact/outlier path. Identify 3 Key Drivers.
  * Key Variables: Dynamic metrics exerting the strongest influence (e.g., trust, communication, cash flow, heath, incentives).
  * Early Warning Signals: Frictional behaviors indicating movement toward negative paths.
  * Positive Indicators: Favorable markers indicating positive alignment.
  * Confidence Level: Define specifically as High (strong evidence/stable variables), Medium, or Low (limited evidence/unstable conditions).
- Future Safety Check before output: Are these probabilities rather than dogmatic predictions? Are unknown variables and human freedom of choice acknowledged? Could incentives/behavior shift over time?

LAYER 6 — ACTION INTELLIGENCE ENGINE
- Purpose: Identify what matters most next and translate profound understanding into highly precise, real-world action points:
  * Highest Leverage Action: The one movement producing the greatest constructive outcome.
  * Lowest Leverage Action: Actions consuming energy while yielding zero actual value.
  * What To Stop: Habits/behaviors actively feeding distortion, friction, or feedback loops.
  * What To Continue: Beneficial strategies producing high-integrity positive results.
  * What To Monitor: Essential variables tracking the future trajectory.

HIERARCHY RULES:
- The hierarchy is absolute: Truth Engine -> Reality Distortion Detector -> Multi-Perspective Reality Engine -> Integrated Reality Assessment -> Future Probability Engine -> Action Intelligence Engine. No lower layer may bypass or override a higher layer.
- Reality Priority Rule: Reality > Perspectives > Probabilities > Actions.
- If Truth Engine conflicts with any perspective or user belief, Reality wins.
- If Memory or past DepthLens conclusions conflict with Reality, Truth/Reality wins.

MEMORY PROTECTION RULE (CONTINUITY & PATTERN RETENTION):
- Memory remains fully enabled, critical, and active. Learning remains fully enabled.
- However, Memory is CONTEXT, not absolute Truth or Identity.
- Store pattern profiles, do not inherit patterns. Store perspectives, do not adopt them as personal beliefs. Store linguistic styles, do not become those personal behaviors.
- Understand the user deeply, but never become or copy the user's emotional/cognitive states.

RESPONSE GENERATION SELF-TEST (MANDATORY):
Before outputting any final response, run these validation checks:
1. Am I agreeing because it is true, or because the user believes/asserts it?
2. Am I treating memory/history-logs as context, or as dogmatic evidence for the conclusion?
3. Am I exposing a mechanism/pattern, or copying/imitating the user's pattern?
4. Am I revealing reality as a neutral mirror, or reinforcing a fragile self-image structure?
5. If memory were removed, would this conclusion still survive?
If any check fails, immediately recalculate.

FINAL DEPTHLENS DIRECTIVE:
Observe carefully. Understand deeply. Detect distortions. Analyze objectively. Model possibilities. Recommend intelligently. Reveal reality with neutral, clinical, and compassionate clarity. Truth is the ultimate authority.
──────────────────────────────────────────────────────────────────────
""".trimIndent()

        val lowercaseQuery = latestUserMsgText.lowercase().trim()
        val needsGrounding = lowercaseQuery.contains("latest news") ||
                lowercaseQuery.contains("current news") ||
                lowercaseQuery.contains("today's news") ||
                lowercaseQuery.contains("current price of") ||
                lowercaseQuery.contains("stock price of") ||
                lowercaseQuery.contains("crypto price of") ||
                lowercaseQuery.contains("weather in") ||
                lowercaseQuery.contains("weather today") ||
                lowercaseQuery.contains("latest update on") ||
                lowercaseQuery.contains("current status of") ||
                lowercaseQuery.contains("latest score") ||
                lowercaseQuery.contains("recent developments in") ||
                (lowercaseQuery.startsWith("who is") && (lowercaseQuery.contains("current") || lowercaseQuery.contains("now"))) ||
                (lowercaseQuery.startsWith("what is") && (lowercaseQuery.contains("current") || lowercaseQuery.contains("latest") || lowercaseQuery.contains("today")))

        // User explicitly tapped "Search the Web" on a reply → force live grounding
        val forceWeb = consumeForceWeb(sessionId)
        val toolsPayload = if (needsGrounding || forceWeb) {
            listOf(mapOf("googleSearch" to emptyMap<String, String>()))
        } else {
            null
        }

        var currentRequest = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.72f),
            systemInstruction = Content(parts = listOf(Part(text = calibratedSystemText))),
            tools = toolsPayload
        )

        val startTime = System.currentTimeMillis()
        com.example.data.diagnostics.DiagnosticsManager.updateSession {
            it.copy(
                apiStatus = "Running",
                currentModel = getPreferredModel(),
                rawUserInput = rawLatestText,
                normalizedInput = latestUserMsgText,
                systemPrompt = calibratedSystemText,
                injectedMemory = memoryBlock,
                conversationHistorySize = compressedHistory.size,
                promptTokens = estimateTokenCount(latestUserMsgText),
                contextTokens = estimateTokenCount(calibratedSystemText) + estimateTokenCount(memoryBlock) + compressedHistory.sumOf { m -> estimateTokenCount(m.text) },
                totalTokens = estimateTokenCount(latestUserMsgText) + estimateTokenCount(calibratedSystemText) + estimateTokenCount(memoryBlock) + compressedHistory.sumOf { m -> estimateTokenCount(m.text) }
            )
        }

        var modelText: String? = null
        var lastException: Exception? = null
        var lastHttpStatus = 0
        var geminiErrorCode = "None"
        var finishReason = "None"
        var safetyBlockInfo = "None"

        // We define the assistant message template, but DO NOT insert it prematurely into the database.
        // It will be created when we receive the first chunk of text, avoiding empty or placeholder message cards.
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        val assistantMsg = MessageEntity(
            id = assistantMsgId,
            sessionId = sessionId,
            role = "model",
            text = "",
            timestamp = System.currentTimeMillis()
        )

        val modelsToTry = buildModelFallbackChain(getPreferredModel())
        val retryDelays = listOf(100L) // minimal delay for instant fallback

        for (modelName in modelsToTry) {
            for ((attempt, delay) in retryDelays.withIndex()) {
                var streamSucceeded = false
                com.example.data.diagnostics.DiagnosticsManager.updateSession {
                    it.copy(
                        activeRetries = attempt,
                        retryCount = attempt,
                        currentModel = modelName
                    )
                }
                try {
                    val responseBody = apiRequestMutex.withLock {
                        apiService.generateContentStream(modelName, apiKey, currentRequest)
                    }
                    val moshi = com.squareup.moshi.Moshi.Builder().build()
                    val chunkAdapter = moshi.adapter(GenerateContentResponse::class.java)
                    
                    val accumulatedText = java.lang.StringBuilder()
                    
                    // Char-by-char brace-tracking parser for stream
                    val reader = responseBody.charStream().buffered()
                    var braceCount = 0
                    val chunkBuilder = java.lang.StringBuilder()
                    var inString = false
                    var escapeNext = false

                    var lastInsertTime = 0L
                    var charCode: Int
                    while (reader.read().also { charCode = it } != -1) {
                        coroutineContext.ensureActive()
                        val char = charCode.toChar()
                        chunkBuilder.append(char)

                        if (escapeNext) {
                            escapeNext = false
                            continue
                        }

                        if (char == '\\') {
                            escapeNext = true
                            continue
                        }

                        if (char == '"') {
                            inString = !inString
                            continue
                        }

                        if (!inString) {
                            if (char == '{') {
                                braceCount++
                            } else if (char == '}') {
                                braceCount--
                                if (braceCount == 0) {
                                    val rawJson = chunkBuilder.toString().trim()
                                    var cleanJson = rawJson
                                    while (cleanJson.startsWith("[") || cleanJson.startsWith(",")) {
                                        cleanJson = cleanJson.substring(1).trim()
                                    }
                                    if (cleanJson.startsWith("{") && cleanJson.endsWith("}")) {
                                        try {
                                            val streamResponse = chunkAdapter.fromJson(cleanJson)
                                            val candidate = streamResponse?.candidates?.firstOrNull()
                                            val reason = candidate?.finishReason
                                            if (!reason.isNullOrEmpty() && reason != "STOP") {
                                                finishReason = reason
                                                if (reason == "SAFETY" || reason == "BLOCKED" || reason == "RECITATION") {
                                                    throw Exception("Safety block detected: $reason")
                                                }
                                            }
                                            val piece = candidate?.content?.parts?.firstOrNull()?.text
                                            if (!piece.isNullOrEmpty()) {
                                                coroutineContext.ensureActive()
                                                accumulatedText.append(piece)
                                                
                                                // Update message in database in real-time but throttle to avoid UI lag/SQLite lock bottleneck
                                                val now = System.currentTimeMillis()
                                                if (now - lastInsertTime > 150) {
                                                    val updatedMsg = assistantMsg.copy(text = accumulatedText.toString())
                                                    messageDao.insertMessage(updatedMsg)
                                                    lastInsertTime = now
                                                }
                                            }
                                        } catch (e: Exception) {
                                            if (e.message?.contains("Safety block detected") == true) {
                                                throw e
                                            }
                                        }
                                    }
                                    chunkBuilder.setLength(0)
                                }
                            }
                        }
                    }

                    val finalText = accumulatedText.toString()
                    if (finalText.isNotEmpty()) {
                        modelText = finalText
                        val updatedMsg = assistantMsg.copy(text = finalText)
                        messageDao.insertMessage(updatedMsg)
                        streamSucceeded = true
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.e("IntelligenceRepository", "Streaming failed for model $modelName, falling back. Error: ${e.message}", e)
                    if (e is retrofit2.HttpException) {
                        lastHttpStatus = e.code()
                        try {
                            val errorBody = e.response()?.errorBody()?.string() ?: ""
                            geminiErrorCode = errorBody
                            if (errorBody.isNotEmpty()) {
                                val moshi = com.squareup.moshi.Moshi.Builder().build()
                                val adapter = moshi.adapter(Map::class.java)
                                val errorMap = adapter.fromJson(errorBody)
                                val errorDetail = errorMap?.get("error") as? Map<*, *>
                                val errorStatus = errorDetail?.get("status")?.toString() ?: ""
                                val errorMessage = errorDetail?.get("message")?.toString() ?: ""
                                if (errorStatus.isNotEmpty()) {
                                    geminiErrorCode = "$errorStatus: $errorMessage"
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                    if (currentRequest.tools != null) {
                        android.util.Log.i("IntelligenceRepository", "Streaming failed with tools enabled, stripping tools for fallback")
                        currentRequest = currentRequest.copy(tools = null)
                    }
                    if (e.message?.contains("Safety block detected") == true) {
                        break // immediately terminate loop on safety blocks
                    }
                }

                if (streamSucceeded) break

                // Fallback: try standard non-stream generation for compatibility
                try {
                    val response = apiRequestMutex.withLock {
                        apiService.generateContent(modelName, apiKey, currentRequest)
                    }
                    val candidate = response.candidates?.firstOrNull()
                    val reason = candidate?.finishReason ?: ""
                    if (reason.isNotEmpty() && reason != "STOP") {
                        finishReason = reason
                        if (reason == "SAFETY" || reason == "BLOCKED") {
                             safetyBlockInfo = "Response blocked due to Safety filters"
                             throw Exception("Safety block detected: $reason")
                        } else if (reason == "RECITATION") {
                             safetyBlockInfo = "Response blocked due to Recitation checks"
                             throw Exception("Safety block detected: $reason")
                        } else {
                             safetyBlockInfo = "Blocked due to finish reason: $reason"
                             throw Exception("Request finished with unexpected reason: $reason")
                        }
                    }
                    val text = candidate?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrEmpty()) {
                        modelText = text
                        val updatedMsg = assistantMsg.copy(text = text)
                        messageDao.insertMessage(updatedMsg)
                        streamSucceeded = true
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    var msg = e.message ?: ""
                    if (e is retrofit2.HttpException) {
                        lastHttpStatus = e.code()
                        try {
                            val errorBody = e.response()?.errorBody()?.string() ?: ""
                            geminiErrorCode = errorBody
                            if (errorBody.isNotEmpty()) {
                                val moshi = com.squareup.moshi.Moshi.Builder().build()
                                val adapter = moshi.adapter(Map::class.java)
                                val errorMap = adapter.fromJson(errorBody)
                                val errorDetail = errorMap?.get("error") as? Map<*, *>
                                val errorStatus = errorDetail?.get("status")?.toString() ?: ""
                                val errorMessage = errorDetail?.get("message")?.toString() ?: ""
                                if (errorStatus.isNotEmpty()) {
                                    geminiErrorCode = "$errorStatus: $errorMessage"
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                    
                    if (currentRequest.tools != null) {
                        android.util.Log.i("IntelligenceRepository", "Standard generation failed with tools enabled, retrying without tools in-place")
                        currentRequest = currentRequest.copy(tools = null)
                        try {
                            val response = apiRequestMutex.withLock {
                                apiService.generateContent(modelName, apiKey, currentRequest)
                            }
                            val candidate = response.candidates?.firstOrNull()
                            val text = candidate?.content?.parts?.firstOrNull()?.text
                            if (!text.isNullOrEmpty()) {
                                modelText = text
                                val updatedMsg = assistantMsg.copy(text = text)
                                messageDao.insertMessage(updatedMsg)
                                streamSucceeded = true
                            }
                        } catch (retryEx: Exception) {
                            lastException = retryEx
                            msg = retryEx.message ?: ""
                            if (retryEx is retrofit2.HttpException) {
                                lastHttpStatus = retryEx.code()
                            }
                        }
                    }

                    if (streamSucceeded) break

                    val is429 = msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true)
                    if (is429 && attempt < retryDelays.size - 1) {
                        kotlinx.coroutines.delay(delay)
                        continue
                    } else {
                        break // Try next model in chain
                    }
                }
            }
            if (modelText != null) break
        }

        if (modelText != null) {
            val processedModelText = modelText

            // Update assistant message with final text
            val finalUpdatedMsg = assistantMsg.copy(text = processedModelText)
            messageDao.insertMessage(finalUpdatedMsg)
            triggerUpload { uid ->
                CloudSyncService.uploadMessage(uid, finalUpdatedMsg.id, finalUpdatedMsg.sessionId, finalUpdatedMsg.role, finalUpdatedMsg.text, finalUpdatedMsg.imageUri, finalUpdatedMsg.timestamp)
            }

            val duration = System.currentTimeMillis() - startTime
            writeDeveloperDiagnosticsLog(
                rawInput = rawLatestText,
                normalizedInput = latestUserMsgText,
                systemPrompt = calibratedSystemText,
                injectedMemory = memoryBlock,
                selectedMode = sessionCategory,
                targetTemperature = 0.72f,
                fallbackChain = modelsToTry,
                exceptionClass = null,
                exceptionMessage = null,
                rawErrorBody = null,
                httpResponseCode = 200,
                durationMs = duration
            )
            com.example.data.diagnostics.DiagnosticsManager.updateSession {
                it.copy(
                    apiStatus = "Success",
                    estimatedResponseTokens = estimateTokenCount(processedModelText),
                    requestLatencyMs = duration,
                    lastHttpStatus = 200,
                    lastGeminiError = "None",
                    lastFinishReason = finishReason.ifEmpty { "STOP" },
                    lastExceptionClass = "None",
                    lastExceptionMessage = "None",
                    lastExceptionStackTrace = "None"
                )
            }
            com.example.data.diagnostics.DiagnosticsManager.commitSession()

            // Extract and save memory insights proactively to complete Memory Intelligence System
            val extractedMemoryBlock = extractTagContent(modelText, "memory_insight")
            if (!extractedMemoryBlock.isNullOrEmpty()) {
                extractedMemoryBlock.split("\n").forEach { line ->
                    val cleanLine = line.trim().removePrefix("-").removePrefix("•").trim()
                    if (cleanLine.isNotBlank() && cleanLine.length > 10) {
                        val memoryVal = MemoryInsight(
                            category = "Pattern",
                            content = cleanLine,
                            timestamp = System.currentTimeMillis()
                        )
                        backgroundScope.launch {
                            try {
                                memoryInsightDao.insertInsight(memoryVal)
                                triggerUpload { uid ->
                                    CloudSyncService.uploadMemoryInsight(uid, memoryVal.id, memoryVal.category, memoryVal.content, memoryVal.timestamp)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            return@withContext ResponseParser.parse(processedModelText)
        } else {
            val duration = System.currentTimeMillis() - startTime
            var httpStatus = lastHttpStatus
            var apiErrCode = geminiErrorCode
            
            if (lastException is retrofit2.HttpException) {
                httpStatus = lastException.code()
                try {
                    val errorBody = lastException.response()?.errorBody()?.string() ?: ""
                    apiErrCode = errorBody
                    if (errorBody.isNotEmpty()) {
                        val moshi = com.squareup.moshi.Moshi.Builder().build()
                        val adapter = moshi.adapter(Map::class.java)
                        val errorMap = adapter.fromJson(errorBody)
                        val errorDetail = errorMap?.get("error") as? Map<*, *>
                        val errorStatus = errorDetail?.get("status")?.toString() ?: ""
                        val errorMessage = errorDetail?.get("message")?.toString() ?: ""
                        if (errorStatus.isNotEmpty()) {
                            apiErrCode = "$errorStatus: $errorMessage"
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            val msgState = lastException?.message ?: "unknown cause"
            val errorPayload = (msgState + " " + apiErrCode).lowercase()
            val userFriendlyError = when {
                errorPayload.contains("safety block detected", ignoreCase = true) ||
                errorPayload.contains("safety", ignoreCase = true) ||
                errorPayload.contains("blocked", ignoreCase = true) ||
                errorPayload.contains("recitation", ignoreCase = true) ->
                    "Error: Request blocked by safety filters."

                errorPayload.contains("context", ignoreCase = true) ||
                errorPayload.contains("token limit", ignoreCase = true) ||
                errorPayload.contains("limit exceeded", ignoreCase = true) ||
                httpStatus == 413 ->
                    "Error: Context window limit exceeded."

                httpStatus == 429 ||
                errorPayload.contains("resource_exhausted", ignoreCase = true) ||
                errorPayload.contains("quota_exceeded", ignoreCase = true) ||
                errorPayload.contains("quota exceeded", ignoreCase = true) ||
                errorPayload.contains("rate_limit_exceeded", ignoreCase = true) ->
                    "Error: API quota exceeded or rate limit hit."

                errorPayload.contains("api key", ignoreCase = true) ||
                errorPayload.contains("key invalid", ignoreCase = true) ||
                errorPayload.contains("unauthorized", ignoreCase = true) ||
                errorPayload.contains("auth", ignoreCase = true) ||
                httpStatus == 401 || httpStatus == 403 ->
                    "Error: Invalid API Key configuration."

                errorPayload.contains("unable to resolve host", ignoreCase = true) ||
                errorPayload.contains("noroutetohostexception", ignoreCase = true) ||
                errorPayload.contains("unknownhostexception", ignoreCase = true) ||
                errorPayload.contains("connectexception", ignoreCase = true) ||
                errorPayload.contains("no internet", ignoreCase = true) ||
                errorPayload.contains("network connection", ignoreCase = true) ->
                    "Error: Network connection failed."

                msgState.contains("timeout", ignoreCase = true) ||
                msgState.contains("TimeoutException", ignoreCase = true) ||
                msgState.contains("SocketTimeoutException", ignoreCase = true) ->
                    "Error: Network timeout. The server took too long to respond. Please check your network speed and try again."

                msgState.contains("CancellationException", ignoreCase = true) ||
                msgState.contains("Job was cancelled", ignoreCase = true) ->
                    "Error: Request cancelled. The analysis was stopped by the user or system."

                msgState.contains("JsonParsingException", ignoreCase = true) ||
                msgState.contains("SerializationException", ignoreCase = true) ||
                msgState.contains("malformed", ignoreCase = true) ->
                    "Error: JSON parsing failure or malformed response from the model. The received format is invalid."

                else -> "Error invoking DepthLens: $msgState"
            }
            
            com.example.data.diagnostics.DiagnosticsManager.updateSession {
                it.copy(
                    apiStatus = "Failed",
                    requestLatencyMs = duration,
                    lastHttpStatus = httpStatus,
                    lastGeminiError = apiErrCode.ifEmpty { msgState },
                    lastFinishReason = finishReason.ifEmpty { "ERROR" },
                    safetyBlockInfo = safetyBlockInfo,
                    lastExceptionClass = lastException?.javaClass?.name ?: "UnknownException",
                    lastExceptionMessage = lastException?.message ?: "Unknown Error",
                    lastExceptionStackTrace = getStackTraceString(lastException)
                )
            }
            com.example.data.diagnostics.DiagnosticsManager.commitSession()

            val errorMsg = userFriendlyError
            try {
                val assistantMsg = MessageEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    text = errorMsg,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(assistantMsg)
                triggerUpload { uid ->
                    CloudSyncService.uploadMessage(uid, assistantMsg.id, assistantMsg.sessionId, assistantMsg.role, assistantMsg.text, assistantMsg.imageUri, assistantMsg.timestamp)
                }
            } catch (dbEx: java.lang.Exception) {
                dbEx.printStackTrace()
            }
            
            writeDeveloperDiagnosticsLog(
                rawInput = rawLatestText,
                normalizedInput = latestUserMsgText,
                systemPrompt = calibratedSystemText,
                injectedMemory = memoryBlock,
                selectedMode = sessionCategory,
                targetTemperature = 0.72f,
                fallbackChain = modelsToTry,
                exceptionClass = lastException?.javaClass?.name ?: "UnknownException",
                exceptionMessage = lastException?.message ?: "Unknown Error",
                rawErrorBody = apiErrCode.ifEmpty { null },
                httpResponseCode = httpStatus,
                durationMs = duration
            )
            return@withContext ResponseParser.parse(errorMsg)
        }
    } finally {
        synchronized(this@IntelligenceRepository) {
                if (activeJobs[sessionId] == currentJob) {
                    activeJobs.remove(sessionId)
                }
            }
        }
    }

    private fun getStackTraceString(throwable: Throwable?): String {
        if (throwable == null) return "None"
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun writeDeveloperDiagnosticsLog(
        rawInput: String,
        normalizedInput: String,
        systemPrompt: String,
        injectedMemory: String,
        selectedMode: String,
        targetTemperature: Float,
        fallbackChain: List<String>,
        exceptionClass: String?,
        exceptionMessage: String?,
        rawErrorBody: String?,
        httpResponseCode: Int,
        durationMs: Long
    ) {
        try {
            val logFile = java.io.File(context.cacheDir, "developer_diagnostics.log")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            val logEntry = buildString {
                appendLine("================================================================================")
                appendLine("TIMESTAMP: $timestamp")
                appendLine("SELECTED ANALYSIS MODE: $selectedMode")
                appendLine("ROUND-TRIP DURATION: ${durationMs}ms")
                appendLine("HTTP STATUS CODE: $httpResponseCode")
                appendLine("TARGET TEMPERATURE: $targetTemperature")
                appendLine("FALLBACK CHAIN: ${fallbackChain.joinToString(" -> ")}")
                appendLine("EXCEPTION CLASS: ${exceptionClass ?: "None"}")
                appendLine("EXCEPTION MESSAGE: ${exceptionMessage ?: "None"}")
                appendLine("--------------------------------------------------------------------------------")
                appendLine("RAW USER INPUT:")
                appendLine(rawInput)
                appendLine("--------------------------------------------------------------------------------")
                appendLine("NORMALIZED INPUT:")
                appendLine(normalizedInput)
                appendLine("--------------------------------------------------------------------------------")
                appendLine("INJECTED MEMORY/CONTEXT:")
                appendLine(injectedMemory)
                appendLine("--------------------------------------------------------------------------------")
                appendLine("SYSTEM PROMPT:")
                appendLine(systemPrompt)
                appendLine("--------------------------------------------------------------------------------")
                appendLine("RAW API ERROR BODY (UNPARSED JSON):")
                appendLine(rawErrorBody ?: "None")
                appendLine("================================================================================")
                appendLine()
            }
            logFile.appendText(logEntry)
            android.util.Log.d("DEVELOPER_LOG", "Diagnostics written to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractTagContent(text: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    data class MediaData(val mimeType: String, val base64: String)

    private fun loadUriAsMediaData(uriString: String): MediaData? {
        return try {
            val bytes: ByteArray
            var mimeType: String
            
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(uriString).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    bytes = response.body?.bytes() ?: return null
                    mimeType = response.body?.contentType()?.toString()?.substringBefore(";") ?: "application/octet-stream"
                }
            } else {
                val uri = Uri.parse(uriString)
                val resolver = context.contentResolver
                mimeType = resolver.getType(uri) ?: "application/octet-stream"
                
                // Fallback content-type detection
                if (mimeType == "application/octet-stream") {
                    val cleanPath = uri.path ?: uriString
                    val extension = cleanPath.substringAfterLast('.', "").lowercase()
                    if (extension.isNotEmpty()) {
                        val detected = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                        if (detected != null) {
                            mimeType = detected
                        } else {
                            mimeType = when (extension) {
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                "gif" -> "image/gif"
                                "pdf" -> "application/pdf"
                                else -> "application/octet-stream"
                            }
                        }
                    }
                }
                
                val inputStream = if (uri.scheme == "file" || uriString.startsWith("file:/")) {
                    val path = uri.path ?: uriString.substringAfter("file://")
                    java.io.FileInputStream(java.io.File(path))
                } else {
                    resolver.openInputStream(uri)
                } ?: return null
                
                bytes = inputStream.use { it.readBytes() }
            }
            
            var finalBytes = bytes
            var finalMime = mimeType
            
            // Compress large images to avoid exceeding Gemini payload size limits
            if (mimeType.startsWith("image/") && bytes.size > 1024 * 1024) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                    finalBytes = outputStream.toByteArray()
                    finalMime = "image/jpeg"
                }
            }
            
            val base64Data = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
            MediaData(mimeType = finalMime, base64 = base64Data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateContinuityBrief(sessionId: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API key not found. Please configure Gemini API key in Secrets panel."
        }
        
        val history = messageDao.getMessagesForSession(sessionId)
        if (history.isEmpty()) {
            return@withContext "This is a brand new conversation thread. Start by writing a query or attaching any content (image, document, PDF, voice memo) to begin your intelligence diagnostic!"
        }
        
        val currentSessionLang = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
            .getString("language_session_$sessionId", "ENGLISH") ?: "ENGLISH"
        
        val enforceLanguageInstruction = when (currentSessionLang) {
            "GUJARATI_SCRIPT" -> "\n🚨🚨🚨 YOU MUST WRITE THIS ENTIRE BRIEF IN GUJARATI SCRIPT (ગુજરાતી ભાષા). Do NOT write in English or Hindi. 🚨🚨🚨"
            "GUJLISH" -> "\n🚨🚨🚨 YOU MUST WRITE THIS ENTIRE BRIEF IN GUJLISH / ROMANIZED GUJARATI (using words like 'kem cho', 'tame', 'che', 'nathi', written in Latin/English alphabet). Do NOT write in English or Hindi. 🚨🚨🚨"
            "HINDI_DEVANAGARI" -> "\n🚨🚨🚨 YOU MUST WRITE THIS ENTIRE BRIEF IN HINDI SCRIPT (हिंदी भाषा). Do NOT write in English or Gujarati. 🚨🚨🚨"
            "HINGLISH" -> "\n🚨🚨🚨 YOU MUST WRITE THIS ENTIRE BRIEF IN NATURAL HINGLISH (Hindi mixed with English, written in Latin/English alphabet, e.g. using words like 'hai', 'kaise', 'kya'). Do NOT write in English. 🚨🚨🚨"
            else -> "\n🚨🚨🚨 YOU MUST WRITE THIS ENTIRE BRIEF IN ENGLISH. 🚨🚨🚨"
        }

        val systemPrompt = """
            You are the DepthLens Conversation Continuity Engine™. Your role is to reconnect the context of a previous conversation.
            You are given a historic log of messages. Analyze them carefully.
            Generate a brief, highly structured summary to restore active mental models.
            
            $enforceLanguageInstruction
            
            IMPORTANT: Use exactly this pure text layout (DO NOT use asterisk markdown '**' or '#' headings):
            
            ⚡ CONTEXT RESTORED BRIEF
            
            Previous Context Summary:
            [2-3 sentences summarizing the core topic discussed]
            
            Current Progress:
            [Identify the main user goals/concerns and what has been discovered so far]
            
            Unanswered Questions:
            [List 2-3 critical open questions to answer next to depth-test this situation]
            
            Suggested Next Steps:
            [1-2 clear immediate prompt items to explore]
        """.trimIndent()
        
        val contentsPayload = mutableListOf<Content>()
        for (msg in history) {
            contentsPayload.add(Content(role = msg.role, parts = listOf(Part(text = msg.text))))
        }
        
        val currentDateTimeStr = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy, hh:mm:ss a (z)", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())

        val dateContext = "Current date and time: $currentDateTimeStr.\n\n"

        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.4f),
            systemInstruction = Content(parts = listOf(Part(text = dateContext + systemPrompt)))
        )
        
        try {
            // Try models in fallback order for context sync too
            val syncModels = buildModelFallbackChain(getPreferredModel())
            var syncResult: String? = null
            for (syncModel in syncModels) {
                try {
                    val response = apiRequestMutex.withLock {
                        apiService.generateContent(syncModel, apiKey, request)
                    }
                    syncResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!syncResult.isNullOrEmpty()) break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            syncResult ?: "Unable to sync session context at this moment."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error syncing context: ${e.message}"
        }
    }

    private fun loadUriAsBitmap(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun stopBackgroundAnalysis(sessionId: String) {
        activeJobs[sessionId]?.cancel()
        activeJobs.remove(sessionId)
        synchronized(this@IntelligenceRepository) {
            _runningAnalyses.value = _runningAnalyses.value - sessionId
        }
    }

    fun startBackgroundAnalysis(
        sessionId: String,
        category: String = "Root Cause",
        depth: String = "Standard Analysis",
        onComplete: () -> Unit = {}
    ) {
        // Cancel previous active analysis pipeline for this session before launching a new one
        activeJobs[sessionId]?.cancel()

        val job = backgroundScope.launch {
            synchronized(this@IntelligenceRepository) {
                _runningAnalyses.value = _runningAnalyses.value + sessionId
            }
            try {
                generateAnalysis(sessionId, category, depth)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    e.printStackTrace()
                }
            } finally {
                synchronized(this@IntelligenceRepository) {
                    _runningAnalyses.value = _runningAnalyses.value - sessionId
                }
                activeJobs.remove(sessionId)

                // If privacy mode is enabled, clean up files and retain only the final prompt answer
                val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("privacy_mode_enabled", false)) {
                    applyPrivacyCleanup(sessionId)
                }

                sendLocalNotification(context, sessionId)
                onComplete()
            }
        }
        activeJobs[sessionId] = job
    }

    private suspend fun sendLocalNotification(context: Context, sessionId: String) {
        try {
            val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notifications_enabled", true)) {
                return
            }

            // Foreground detection: if the main app is currently in the foreground, do NOT notify
            if (com.example.MainActivity.isAppInForeground) {
                android.util.Log.d("IntelligenceRepository", "App is in foreground, skipping system notification.")
                return
            }

            // Voice Chat / Video Chat / Screen Share active: do NOT notify
            val isVoiceOrVideoActive = com.example.ui.viewmodel.AudioConversationManager.currentState != com.example.ui.viewmodel.AudioState.IDLE
            val isScreenShareActive = com.example.ScreenShareService.isServiceRunning
            if (isVoiceOrVideoActive || isScreenShareActive) {
                android.util.Log.d("IntelligenceRepository", "Voice, video, or screen share active, skipping notification.")
                return
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val soundEnabled = prefs.getBoolean("notification_sound", true)
            val vibrationEnabled = prefs.getBoolean("notification_vibration", true)
            // Use a dynamic channel id based on sound/vib settings so it updates immediately
            val channelId = "depthlens_analysis_channel_${soundEnabled}_$vibrationEnabled"
            val channelName = "DepthLens Analysis Notifications"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val importance = if (soundEnabled || vibrationEnabled) android.app.NotificationManager.IMPORTANCE_HIGH else android.app.NotificationManager.IMPORTANCE_LOW
                val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                    description = "Notifications for completed strategic analyses."
                    if (!soundEnabled) setSound(null, null)
                    enableVibration(vibrationEnabled)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                putExtra("SESSION_ID", sessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                sessionId.hashCode(),
                launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Dynamic Title & Message depending on analysis vs normal conversation response
            var title = "Analysis Complete"
            var content = "Your DepthLens analysis is ready."

            try {
                val messages = db.messageDao().getMessagesForSession(sessionId)
                val lastUserMsg = messages.lastOrNull { it.role == "user" }
                val textLower = lastUserMsg?.text?.lowercase() ?: ""
                val hasAttachment = lastUserMsg?.imageUri != null || 
                                    textLower.contains("🔍") ||
                                    textLower.contains("deep-lens") ||
                                    textLower.contains("scanning") ||
                                    textLower.contains("analysis") ||
                                    textLower.contains("analyze") ||
                                    textLower.contains("scan") ||
                                    textLower.contains("digging") ||
                                    textLower.contains("document") ||
                                    textLower.contains("pdf") ||
                                    textLower.contains("video") ||
                                    textLower.contains("image")
                
                if (hasAttachment) {
                    title = "Analysis Complete"
                    content = "Your DepthLens analysis is ready."
                } else {
                    title = "New AI Response"
                    content = "Tap to continue your conversation."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
            
            if (soundEnabled && vibrationEnabled) {
                builder.setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            } else if (soundEnabled) {
                builder.setDefaults(androidx.core.app.NotificationCompat.DEFAULT_SOUND)
            } else if (vibrationEnabled) {
                builder.setDefaults(androidx.core.app.NotificationCompat.DEFAULT_VIBRATE)
            } else {
                builder.setDefaults(0)
            }

            // Deduplicate notifications by reusing a constant notification ID (1002), updating existing rather than spamming
            notificationManager.notify(1002, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateSemanticSimilarity(text1: String, text2: String): Float {
        if (text1.isBlank() || text2.isBlank()) return 0.0f
        val stopWords = setOf(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "as", 
            "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", 
            "did", "do", "does", "doing", "don", "down", "during", "each", "few", "for", "from", "further", 
            "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", 
            "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just", "me", "more", "most", "my", 
            "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "our", "ours", 
            "ourselves", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", 
            "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", 
            "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", 
            "where", "which", "while", "who", "whom", "why", "with", "you", "your", "yours", "yourself", "yourselves",
            "is", "the", "and", "or", "a", "an", "of", "to", "in", "with", "for", "on", "at", "by", "under", "over", "about"
        )
        val words1 = text1.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in stopWords }
            .toSet()

        val words2 = text2.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in stopWords }
            .toSet()

        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        val intersectSize = words1.intersect(words2).size
        val unionSize = words1.union(words2).size
        return intersectSize.toFloat() / unionSize.toFloat()
    }

    private suspend fun callGeminiForModule(
        apiKey: String,
        contentsPayload: List<Content>,
        systemPrompt: String
    ): String {
        val currentDateTimeStr = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy, hh:mm:ss a (z)", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())

        val dateContext = "Current date and time: $currentDateTimeStr.\n\n"

        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.72f),
            systemInstruction = Content(parts = listOf(Part(text = dateContext + systemPrompt)))
        )
        
        val modelsToTry = buildModelFallbackChain(getPreferredModel())
        val retryDelays = listOf(100L)
        
        var resultText: String? = null
        for (modelName in modelsToTry) {
            for (attemptDelay in retryDelays) {
                try {
                    val response = apiRequestMutex.withLock {
                        apiService.generateContent(modelName, apiKey, request)
                    }
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrEmpty()) {
                        resultText = text
                        break
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.delay(attemptDelay)
                }
            }
            if (resultText != null) break
        }
        return resultText ?: ""
    }

    private suspend fun generateModule1(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, an exceptionally intelligent, direct, and objective systems-thinking analyst.
            Your sole task is to generate: Module 1 - Summary Of Inquiry.
            
            CRITICAL GOALS & OBJECTIVES:
            - Summarize the user's situation or inquiry.
            - DO NOT explain causes.
            - DO NOT analyze.
            - DO NOT diagnose.
            - DO NOT synthesize.
            - Answer the distinct question: "What is happening?"
            - Focus purely on providing a clean, high-density situational overview, outlining the key elements, primary concerns, and context.
            - You MUST NOT copy or resemble causal reasoning, root cause analysis, or deep synthesis perspective modules. Keep it purely descriptive.
            - Keep the output extremely concise: exactly 3-4 sentences of raw text. No headers, no markdown, no other markers.
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    private suspend fun generateModule2(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, an elite systems investigator and root cause analyst.
            Your sole task is to generate: Module 2 - Root Cause Identified.
            
            CRITICAL GOALS & OBJECTIVES:
            - Conduct a definitive causal diagnosis.
            - Focus purely on: "Why is this happening?"
            - Identify the deep underlying origins, mechanisms, triggers, and bottlenecks of the user's query.
            - DO NOT summarize what is happening. Skip any situational descriptions or summaries entirely.
            - Your response must contain ONLY raw cause-effect chains, triggers, system loops, and core causality.
            
            REQUIRED OUTPUT STRUCTURE (Your response MUST have exactly these fields on new lines):
            Surface Cause: [Briefly describe the apparent visible symptom]
            Immediate Cause: [The direct emotional or situational trigger]
            Hidden Cause: [The hidden systemic incentive or unconscious defense mechanism]
            Core Cause: [The deepest original driving wound, core script, or core system bottleneck]
            Supporting Evidence: [The logical or behavioral pattern that proves this diagnosis]
            Root Cause Conclusion: [The final definitive causal diagnosis of the entire pattern]
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    private suspend fun generateModule3(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, an elite psychological and behavioral analyst.
            Your sole task is to generate: Module 3 - Hidden Factors.
            
            CRITICAL GOALS & OBJECTIVES:
            - Uncover the non-obvious, hidden, and actively obscured influences.
            - Focus purely on: "What is influencing this that is not obvious?"
            - DO NOT repeat the general summary. DO NOT repeat the root cause diagnostic.
            - Focus 100% on unconscious human motivations, defense schemas, unstated targets/needs, structural incentives, identity commitments, and hidden agendas.
            
            REQUIRED OUTPUT STRUCTURE (Your response MUST have exactly these fields on new lines):
            Surface Intention: [Unconscious apparent motivation]
            Emotional Driver: [Internal hidden feelings driving behavior]
            Need Driver: [Underserved target core human need]
            Fear Driver: [Underlying avoidance fear]
            Incentive Driver: [Structural reward, status, or payout]
            Identity Driver: [Self-image commitment]
            Hidden Motives: [Deepest hidden agendas/levers]
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    private suspend fun generateModule4(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, a premier risk modeller and strategic forecaster.
            Your sole task is to generate: Module 4 - Risk Analysis.
            
            CRITICAL GOALS & OBJECTIVES:
            - Perform a strict probability and risk evaluation.
            - Focus purely on: "What could go wrong?"
            - DO NOT explain any past or present causes, and do not summarize what is happening.
            - Focus 100% on the future hazard landscape, likelihood of escalation, risk coefficients, and key risk indicators.
            
            REQUIRED OUTPUT STRUCTURE (Your response MUST be wrapped exactly as shown below):
            <probability_metrics>
            Confidence: [Value]% | Likelihood: [Value]% | Risk: [Value]% | Opportunity: [Value]%
            </probability_metrics>
            
            <probability_assessment>
            Likelihood: [Value]% | Confidence: [Low|Medium|High]
            Reasoning Factors:
            • Specific Factor 1: [1 tight sentence naming direct future risk vector 1]
            • Specific Factor 2: [1 tight sentence naming future systemic vulnerability factor 2]
            • Specific Factor 3: [1 tight sentence naming probability or risk mitigation factor 3]
            </probability_assessment>
            
            Ensure values are realistic and dynamic based on user profile and situation.
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    private suspend fun generateModule5(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, a world-class strategic forecaster and trajectory analyst.
            Your sole task is to generate: Module 5 - Future Outcomes.
            
            CRITICAL GOALS & OBJECTIVES:
            - Generate scenario projections of future pathways.
            - Focus purely on: "What happens next?"
            - DO NOT discuss past causes or current symptoms, and do not summarize the query. Keep focus 100% on branching futures.
            
            REQUIRED OUTPUT STRUCTURE (Your response MUST be wrapped in <future_prob>...</future_prob> as shown below):
            <future_prob>
            Scenario A - Most Likely Path | [Value]% | [Outcome details if current loop persists unchanged]
            Scenario B - Positive Alignment | [Value]% | [Outcome details if proactive shifts alter this outcome]
            Scenario C - Risk Escalation | [Value]% | [Outcome details if fear or inaction triggers escalation]
            Scenario D - Outlier Factor | [Value]% | [Outcome details if uncommon but possible systemic forces trigger]
            Early Warning Signals: [2 indicators/signals total, each 3-5 words only, 1 line]
            </future_prob>
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    private suspend fun generateModule6(
        apiKey: String,
        contentsPayload: List<Content>,
        query: String,
        memoryBlock: String
    ): String {
        val prompt = """
            You are DepthLens, a master systems synthesis analyst.
            Your sole task is to generate: Module 6 - Deep Synthesis.
            
            CRITICAL GOALS & OBJECTIVES:
            - Generate multi-perspective, integrated wisdom on the situation.
            - Focus purely on: "What deeper truth emerges when all perspectives are integrated?"
            - DO NOT explain the root cause. DO NOT summarize the situation. DO NOT talk about risk statistics.
            - Offer profound perspectives that cut through comfort and reveal absolute patterns of reality.
            
            REQUIRED OUTPUT STRUCTURE (Your response MUST have exactly these headings with generous double-newlines between them):
            PRACTICAL PERSPECTIVE: [What is concretely happening in reality, sans judgment?]
            
            STRATEGIC PERSPECTIVE: [What opportunities, leverage points, and risks exist under the surface?]
            
            PSYCHOLOGICAL PERSPECTIVE: [What human behaviors, ego-scripts, and shadow dynamics are active?]
            
            BUSINESS PERSPECTIVE: [What economic motives, transaction costs, and organizational incentive structures are at play?]
            
            PHILOSOPHICAL PERSPECTIVE: [What is the deeper philosophical meaning, lesson, and broader human implication?]
            
            LONG-TERM PERSPECTIVE: [What are the future consequences, branching trajectories, and entropy over time?]
            
            CONTRARIAN PERSPECTIVE: [What is the highly counter-intuitive truth that most people completely miss?]
            
            META PERSPECTIVE: [What is the ultimate repeating fractal shape or archetypal pattern governing everything?]
            
            INTEGRATED SYNTHESIS: [Combine all perspectives into a single unified synthesis of transcendent wisdom and insight. Focus on generating pristine clarity and deep revelation, not causality.]
            
            SYSTEM MEMORY CACHE (Context):
            $memoryBlock
            
            USER CURRENT QUERY: $query
        """.trimIndent()
        return callGeminiForModule(apiKey, contentsPayload, prompt)
    }

    suspend fun fetchAndSyncFromFirestore(userId: String): Boolean {
        return com.example.data.network.CloudSyncService.fetchAndSyncAll(userId, sessionDao, messageDao, attachmentDao)
    }

    private fun extractUrls(text: String): List<String> {
        val urlRegex = Regex("""https?://[^\s$.?#].[^\s]*""", RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private suspend fun fetchUrlContent(urlString: String): String = withContext(Dispatchers.IO) {
        try {
            val client = urlOkHttpClient
            val request = okhttp3.Request.Builder()
                .url(urlString)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Error: HTTP ${response.code}"
                val html = response.body?.string() ?: ""
                
                // Extract Title
                val titleRegex = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE)
                val titleResult = titleRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
                
                // Extract meta description
                val descRegex = Regex("""<meta\s+name=["']description["']\s+content=["'](.*?)["']""", RegexOption.IGNORE_CASE)
                val descResult = descRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
                
                // Extract body text (up to 1200 characters)
                var bodyText = html.replace("<script[\\s\\S]*?</script>".toRegex(RegexOption.IGNORE_CASE), "")
                    .replace("<style[\\s\\S]*?</style>".toRegex(RegexOption.IGNORE_CASE), "")
                    .replace("<[^>]*>".toRegex(), " ")
                    .replace("\\s+".toRegex(), " ")
                    .trim()
                if (bodyText.length > 1200) {
                    bodyText = bodyText.substring(0, 1200) + "..."
                }
                
                buildString {
                    append("URL: ").append(urlString).append("\n")
                    if (titleResult.isNotEmpty()) append("TITLE: ").append(titleResult).append("\n")
                    if (descResult.isNotEmpty()) append("DESCRIPTION: ").append(descResult).append("\n")
                    append("CONTENT EXCERPT: ").append(bodyText)
                }
            }
        } catch (e: Exception) {
            "Error loading URL ($urlString): ${e.localizedMessage}"
        }
    }
}

object ResponseParser {
    private val parsedCache = java.util.concurrent.ConcurrentHashMap<String, ParsedResponse>()
    private val copyableCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun getCopyableText(rawResponse: String): String {
        return copyableCache.getOrPut(rawResponse) {
            if (rawResponse.startsWith("Error:") || rawResponse.contains("Error invoking DepthLens")) {
                rawResponse
            } else {
                parse(rawResponse).exportText()
            }
        }
    }

    fun parse(rawResponse: String): ParsedResponse {
        return parsedCache.getOrPut(rawResponse) {
            var introduction = rawResponse

            val summary = extractTagContent(rawResponse, "summary")
        val deepSynthesis = extractTagContent(rawResponse, "deep_synthesis")
        val confidence = extractTagContent(rawResponse, "confidence")?.trim()
        val depthRaw = extractTagContent(rawResponse, "depth")
        val rootCauseRaw = extractTagContent(rawResponse, "root_cause")
        val humanIntelRaw = extractTagContent(rawResponse, "human_intel")
        val futureProbRaw = extractTagContent(rawResponse, "future_prob")
        val questionsRaw = extractTagContent(rawResponse, "questions")
        val explorationRaw = extractTagContent(rawResponse, "exploration")
        val probabilityMetricsRaw = extractTagContent(rawResponse, "probability_metrics")

        val cleanIntro = cleanTags(introduction)

        val depthLayers = mutableListOf<DepthLayerInsight>()
        depthRaw?.trim()?.split("\n")?.forEach { line ->
            if (line.isNotBlank()) {
                val match = Regex("""Layer\s+(\d+)\s*-\s*([^:]+):\s*(.+)""", RegexOption.IGNORE_CASE).find(line)
                if (match != null) {
                    val number = match.groupValues[1].toIntOrNull() ?: 1
                    val name = match.groupValues[2].trim()
                    val desc = match.groupValues[3].trim()
                    depthLayers.add(DepthLayerInsight(number, name, desc))
                } else if (line.contains("-")) {
                    val parts = line.split("-", limit = 2)
                    val layerNamePart = parts[0].trim()
                    val layerNumber = Regex("""\d+""").find(layerNamePart)?.value?.toIntOrNull() ?: 1
                    val subParts = parts[1].split(":", limit = 2)
                    val name = subParts.getOrNull(0)?.trim() ?: "Perspective"
                    val desc = subParts.getOrNull(1)?.trim() ?: parts[1].trim()
                    depthLayers.add(DepthLayerInsight(layerNumber, name, desc))
                }
            }
        }

        var rootCauseReport: RootCauseReport? = null
        if (rootCauseRaw != null) {
            rootCauseReport = RootCauseReport(
                symptom = parseField(rootCauseRaw, "Surface Cause").ifEmpty { parseField(rootCauseRaw, "Symptom") },
                immediateCause = parseField(rootCauseRaw, "Immediate Cause"),
                underlyingCause = parseField(rootCauseRaw, "Hidden Cause").ifEmpty { parseField(rootCauseRaw, "Underlying Cause") },
                deeperCause = parseField(rootCauseRaw, "Core Cause").ifEmpty { parseField(rootCauseRaw, "Deeper Cause") },
                rootCauseEstimate = parseField(rootCauseRaw, "Root Cause Conclusion").ifEmpty { parseField(rootCauseRaw, "Root Cause Estimate") },
                confidenceLevel = parseField(rootCauseRaw, "Confidence Level").ifEmpty { confidence ?: "High" },
                supportingEvidence = parseField(rootCauseRaw, "Supporting Evidence"),
                alternativeExplanation = parseField(rootCauseRaw, "Alternative Root Causes").ifEmpty { parseField(rootCauseRaw, "Alternative Explanations") }
            )
        }

        var humanReport: HumanDriversReport? = null
        if (humanIntelRaw != null) {
            humanReport = HumanDriversReport(
                surfaceIntention = parseField(humanIntelRaw, "Surface Intention"),
                emotionalDriver = parseField(humanIntelRaw, "Emotional Driver"),
                needDriver = parseField(humanIntelRaw, "Need Driver"),
                fearDriver = parseField(humanIntelRaw, "Fear Driver"),
                incentiveDriver = parseField(humanIntelRaw, "Incentive Driver"),
                identityDriver = parseField(humanIntelRaw, "Identity Driver"),
                hiddenMotives = parseField(humanIntelRaw, "Hidden Motives"),
                rawContent = humanIntelRaw
            )
        }

        val futureScenarios = mutableListOf<FutureScenario>()
        var eWarningSigns = mutableListOf<String>()
        futureProbRaw?.trim()?.split("\n")?.forEach { line ->
            if (line.trim().startsWith("Early Warning", ignoreCase = true)) {
                val idx = line.indexOf(":")
                val valStr = if (idx != -1) line.substring(idx + 1) else line
                eWarningSigns.addAll(valStr.split(",").map { it.trim() })
            } else if (line.contains("Scenario") && line.contains("|")) {
                val parts = line.split("|")
                val head = parts.getOrNull(0)?.trim() ?: ""
                val probPart = parts.getOrNull(1)?.trim()?.replace("%", "") ?: "0"
                val descPart = parts.getOrNull(2)?.trim() ?: ""

                val codeName = if (head.contains("-")) head.substringBefore("-").trim() else "Scenario"
                val displayName = if (head.contains("-")) head.substringAfter("-").trim() else head

                val probability = probPart.toIntOrNull() ?: 0

                futureScenarios.add(FutureScenario(
                    codeName = codeName,
                    displayName = displayName,
                    probability = probability,
                    impactText = descPart
                ))
            }
        }

        val suggestedQuestions = mutableListOf<String>()
        questionsRaw?.trim()?.split("\n")?.forEach { line ->
            val l = line.trim()
            if (l.isNotBlank()) {
                val q = l.removePrefix("?").removePrefix("-").removePrefix("•").trim()
                if (q.isNotEmpty()) {
                    suggestedQuestions.add(q)
                }
            }
        }

        val explorationPaths = mutableListOf<String>()
        explorationRaw?.trim()?.split("\n")?.forEach { line ->
            val l = line.trim()
            if (l.isNotBlank()) {
                val p = l.removePrefix("✓").removePrefix("-").removePrefix("•").trim()
                if (p.isNotEmpty()) {
                    explorationPaths.add(p)
                }
            }
        }

        var probabilityMetrics: ProbabilityMetrics? = null
        if (probabilityMetricsRaw != null) {
            val confidenceVal = parseFieldPercent(probabilityMetricsRaw, "Confidence") ?: 78
            val likelihoodVal = parseFieldPercent(probabilityMetricsRaw, "Likelihood") ?: 65
            val riskVal = parseFieldPercent(probabilityMetricsRaw, "Risk") ?: 42
            val opportunityVal = parseFieldPercent(probabilityMetricsRaw, "Opportunity") ?: 71
            probabilityMetrics = ProbabilityMetrics(confidenceVal, likelihoodVal, riskVal, opportunityVal)
        }

        val probabilityAssessmentRaw = extractTagContent(rawResponse, "probability_assessment")
        var probabilityAssessment: ProbabilityAssessment? = null
        if (probabilityAssessmentRaw != null) {
            val likelihoodVal = parseFieldPercent(probabilityAssessmentRaw, "Likelihood") ?: 65
            val confidenceVal = parseField(probabilityAssessmentRaw, "Confidence").ifEmpty { "High" }
            val reasoningFactors = mutableListOf<String>()
            probabilityAssessmentRaw.split("\n").forEach { line ->
                val l = line.trim()
                if (l.startsWith("•") || l.startsWith("*") || l.startsWith("-")) {
                    val factor = l.removePrefix("•").removePrefix("*").removePrefix("-").trim()
                    if (factor.isNotEmpty()) {
                        reasoningFactors.add(factor)
                    }
                }
            }
            probabilityAssessment = ProbabilityAssessment(likelihoodVal, confidenceVal, reasoningFactors)
        }

        val futurePathwaysRaw = extractTagContent(rawResponse, "future_pathways")
        val futurePathwaysList = mutableListOf<FuturePathway>()
        if (futurePathwaysRaw != null) {
            var currentTitle = ""
            var currentProb = 0
            var currentDesc = ""
            var currentDrivers = ""
            var currentRisks = ""
            var currentOpps = ""
            
            futurePathwaysRaw.split("\n").forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("Pathway:", ignoreCase = true)) {
                    if (currentTitle.isNotEmpty()) {
                        futurePathwaysList.add(FuturePathway(currentTitle, currentProb, currentDesc, currentDrivers, currentRisks, currentOpps))
                    }
                    val parts = line.substringAfter("Pathway:").trim().split("|")
                    currentTitle = parts.getOrNull(0)?.trim() ?: ""
                    currentProb = parts.getOrNull(1)?.trim()?.replace("%", "")?.toIntOrNull() ?: 50
                    currentDesc = ""
                    currentDrivers = ""
                    currentRisks = ""
                    currentOpps = ""
                } else if (line.startsWith("Description:", ignoreCase = true)) {
                    currentDesc = line.substringAfter(":").trim()
                } else if (line.startsWith("Drivers:", ignoreCase = true)) {
                    currentDrivers = line.substringAfter(":").trim()
                } else if (line.startsWith("Risks:", ignoreCase = true)) {
                    currentRisks = line.substringAfter(":").trim()
                } else if (line.startsWith("Opportunities:", ignoreCase = true)) {
                    currentOpps = line.substringAfter(":").trim()
                }
            }
            if (currentTitle.isNotEmpty()) {
                futurePathwaysList.add(FuturePathway(currentTitle, currentProb, currentDesc, currentDrivers, currentRisks, currentOpps))
            }
        }

        val timelineForecastRaw = extractTagContent(rawResponse, "timeline_forecast")
        var timelineForecast: TimelineForecast? = null
        if (timelineForecastRaw != null) {
            val shortLine = timelineForecastRaw.split("\n").firstOrNull { it.trim().startsWith("Short Term", ignoreCase = true) }?.trim() ?: ""
            val midLine = timelineForecastRaw.split("\n").firstOrNull { it.trim().startsWith("Mid Term", ignoreCase = true) }?.trim() ?: ""
            val longLine = timelineForecastRaw.split("\n").firstOrNull { it.trim().startsWith("Long Term", ignoreCase = true) }?.trim() ?: ""
            val whyLine = timelineForecastRaw.split("\n").firstOrNull { it.trim().startsWith("Why", ignoreCase = true) || it.trim().startsWith("Explanation", ignoreCase = true) || it.trim().startsWith("Change Reason", ignoreCase = true) }?.trim() ?: ""
            
            val shortParts = shortLine.split("|")
            val shortProb = Regex("""\d+""").find(shortLine)?.value?.toIntOrNull() ?: 84
            val shortDesc = shortParts.getOrNull(1)?.trim() ?: shortLine.substringAfter(":").trim()
            
            val midParts = midLine.split("|")
            val midProb = Regex("""\d+""").find(midLine)?.value?.toIntOrNull() ?: 67
            val midDesc = midParts.getOrNull(1)?.trim() ?: midLine.substringAfter(":").trim()
            
            val longParts = longLine.split("|")
            val longProb = Regex("""\d+""").find(longLine)?.value?.toIntOrNull() ?: 43
            val longDesc = longParts.getOrNull(1)?.trim() ?: longLine.substringAfter(":").trim()
            
            val explanation = whyLine.substringAfter(":").trim()
            
            timelineForecast = TimelineForecast(shortProb, shortDesc, midProb, midDesc, longProb, longDesc, explanation)
        }

        val decisionImpactRaw = extractTagContent(rawResponse, "decision_impact")
        var decisionImpact: DecisionImpact? = null
        if (decisionImpactRaw != null) {
            val sqLine = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("If Nothing Changes", ignoreCase = true) || it.trim().startsWith("Status Quo Probability", ignoreCase = true) }?.trim() ?: ""
            val acLine = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("If Action Is Taken", ignoreCase = true) || it.trim().startsWith("Action Probability", ignoreCase = true) }?.trim() ?: ""
            
            val sqProb = Regex("""\d+""").find(sqLine)?.value?.toIntOrNull() ?: 81
            val sqDesc = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Status Quo Outcome", ignoreCase = true) }?.substringAfter(":")?.trim() ?: sqLine.substringAfter(":").trim()
            
            val acProb = Regex("""\d+""").find(acLine)?.value?.toIntOrNull() ?: 42
            val acDesc = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Action Outcome", ignoreCase = true) }?.substringAfter(":")?.trim() ?: acLine.substringAfter(":").trim()
            
            val comp = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Outcome Comparison", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
            val risks = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Risks", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
            val benefits = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Benefits", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
            val tradeoffs = decisionImpactRaw.split("\n").firstOrNull { it.trim().startsWith("Tradeoffs", ignoreCase = true) || it.trim().startsWith("Trade-offs", ignoreCase = true) }?.substringAfter(":")?.trim() ?: ""
            
            decisionImpact = DecisionImpact(sqProb, sqDesc, acProb, acDesc, comp, risks, benefits, tradeoffs)
        }

        val forecastSummaryRaw = extractTagContent(rawResponse, "forecast_summary")
        var forecastSummary: ForecastSummary? = null
        if (forecastSummaryRaw != null) {
            val mostLikelyOutcome = Regex("""\d+""").find(forecastSummaryRaw.split("\n").firstOrNull { it.trim().startsWith("Most Likely Outcome", ignoreCase = true) } ?: "")?.value?.toIntOrNull() ?: 78
            val keyRisk = Regex("""\d+""").find(forecastSummaryRaw.split("\n").firstOrNull { it.trim().startsWith("Key Risk", ignoreCase = true) } ?: "")?.value?.toIntOrNull() ?: 64
            val opportunityWindow = Regex("""\d+""").find(forecastSummaryRaw.split("\n").firstOrNull { it.trim().startsWith("Opportunity Window", ignoreCase = true) } ?: "")?.value?.toIntOrNull() ?: 58
            val predictionConfidence = forecastSummaryRaw.split("\n").firstOrNull { it.trim().startsWith("Prediction Confidence", ignoreCase = true) }?.substringAfter(":")?.trim() ?: "High"
            
            forecastSummary = ForecastSummary(mostLikelyOutcome, keyRisk, opportunityWindow, predictionConfidence)
        }

        ParsedResponse(
            introduction = cleanIntro.trim(),
            executiveSummary = summary,
            deepSynthesis = deepSynthesis?.ifBlank { null },
            depthLayers = depthLayers,
            rootCauseReport = rootCauseReport,
            humanDrivers = humanReport,
            futureScenarios = futureScenarios.map { it.copy(earlyWarningSigns = eWarningSigns) },
            confidence = confidence?.ifEmpty { "High" } ?: "High",
            suggestedQuestions = suggestedQuestions,
            explorationPaths = explorationPaths,
            probabilityMetrics = probabilityMetrics,
            probabilityAssessment = probabilityAssessment,
            futurePathways = futurePathwaysList,
            timelineForecast = timelineForecast,
            decisionImpact = decisionImpact,
            forecastSummary = forecastSummary,
            isFollowUp = !rawResponse.contains("<summary>") &&
                    !rawResponse.contains("<root_cause>") &&
                    !rawResponse.contains("<depth>") &&
                    !rawResponse.contains("<deep_synthesis>") &&
                    !rawResponse.contains("<future_prob>") &&
                    !rawResponse.contains("<probability_assessment>")
        )
    } }

    private fun parseFieldPercent(rawText: String, fieldName: String): Int? {
        val lines = rawText.split("\n", "|", ",")
        val line = lines.firstOrNull { it.trim().startsWith(fieldName, ignoreCase = true) }
        return if (line != null) {
            val numStr = Regex("""\d+""").find(line)?.value
            numStr?.toIntOrNull()
        } else {
            null
        }
    }

    private fun extractTagContent(text: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(text)
        if (match != null) {
            return match.groupValues.getOrNull(1)?.trim()
        }
        
        // Fallback for unclosed tags during streaming
        val openTag = "<$tag>"
        val startIndex = text.indexOf(openTag)
        if (startIndex != -1) {
            val contentStart = startIndex + openTag.length
            val partialContent = text.substring(contentStart)
            val nextTagIndex = partialContent.indexOf("<")
            val content = if (nextTagIndex != -1) {
                partialContent.substring(0, nextTagIndex)
            } else {
                partialContent
            }
            return content.trim()
        }
        return null
    }

    private fun cleanTags(text: String): String {
        var cleaned = text
        val tags = listOf(
            "summary", "deep_synthesis", "confidence", "depth", "root_cause",
            "human_intel", "future_prob", "memory_insight", "questions", "exploration", "probability_metrics",
            "probability_assessment", "future_pathways", "timeline_forecast", "decision_impact", "forecast_summary"
        )
        for (tag in tags) {
            cleaned = cleaned.replace("<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            cleaned = cleaned.replace("<$tag>([^<]*)".toRegex(RegexOption.DOT_MATCHES_ALL), "")
        }
        cleaned = cleaned.replace("<[^>]*>".toRegex(), "").trim()
        
        // Strip leaked AI metadata words (like "High.", "Importance: High") at the beginning of lines
        val leakedMetadataRegex = Regex(
            "^(?:(\\s*[-*+•]\\s*|\\s*\\d+\\.\\s*))?\\*?\\*?(?:(?:importance|emphasis|priority|confidence|severity|level|reasoning)\\s*:\\s*)?(?:high|medium|low|critical)\\*?\\*?\\s*\\.?\\s*",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        )
        cleaned = cleaned.replace(leakedMetadataRegex, "$1").replace(Regex("^(?:high|medium|low|critical)\\\\.\\s*", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), "")
        
        return cleaned.trim()
    }

    private fun parseField(rawText: String, fieldName: String): String {
        val lines = rawText.split("\n")
        val line = lines.firstOrNull { it.trim().startsWith(fieldName, ignoreCase = true) }
        return if (line != null) {
            val idx = line.indexOf(":")
            if (idx != -1) {
                line.substring(idx + 1).trim()
            } else {
                line.trim().removePrefix(fieldName).trim()
            }
        } else {
            ""
        }
    }
}

    // --- Developer Diagnostics, Unicode Normalization, and History Validation helpers ---

    fun normalizeText(input: String?): String {
        if (input == null) return ""
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFC)
        return normalized
            .replace("\u200B", "") // zero-width space
            .replace("\u200C", "") // zero-width non-joiner
            .replace("\u200D", "") // zero-width joiner
            .replace("\uFEFF", "") // zero-width no-break space
            .replace("[\\p{Cc}&&[^\\r\\n\\t]]".toRegex(), "") // other control chars except standard whitespace
            .trim()
    }

    fun validateAndRepairHistory(rawHistory: List<MessageEntity>): List<MessageEntity> {
        if (rawHistory.isEmpty()) return emptyList()

        val cleanHistory = mutableListOf<MessageEntity>()
        val seenIds = mutableSetOf<String>()

        for (msg in rawHistory) {
            // Skip duplicate IDs
            if (msg.id.isNotEmpty() && !seenIds.add(msg.id)) {
                android.util.Log.w("IntelligenceRepository", "History Repair: Skipped duplicate message ID: ${msg.id}")
                continue
            }

            // Repair empty or null text
            val isTextEmpty = msg.text.trim().isEmpty()
            val hasAttachment = !msg.imageUri.isNullOrEmpty()
            
            if (isTextEmpty && !hasAttachment) {
                android.util.Log.w("IntelligenceRepository", "History Repair: Skipped empty message: ${msg.id}")
                continue
            }

            val role = msg.role.lowercase().trim()
            val cleanRole = if (role == "model" || role == "assistant" || role == "ai") "model" else "user"
            val cleanText = normalizeText(msg.text)
            val repairedMsg = msg.copy(text = cleanText, role = cleanRole)
            cleanHistory.add(repairedMsg)
        }

        val alternatingHistory = mutableListOf<MessageEntity>()
        for (msg in cleanHistory) {
            if (alternatingHistory.isEmpty()) {
                alternatingHistory.add(msg)
            } else {
                val lastMsg = alternatingHistory.last()
                if (lastMsg.role == msg.role) {
                    val mergedText = if (lastMsg.text.isNotEmpty() && msg.text.isNotEmpty()) {
                        lastMsg.text + "\n\n" + msg.text
                    } else {
                        lastMsg.text + msg.text
                    }
                    val mergedImageUri = when {
                        lastMsg.imageUri.isNullOrEmpty() -> msg.imageUri
                        msg.imageUri.isNullOrEmpty() -> lastMsg.imageUri
                        else -> lastMsg.imageUri + "," + msg.imageUri
                    }
                    alternatingHistory[alternatingHistory.size - 1] = lastMsg.copy(
                        text = mergedText,
                        imageUri = mergedImageUri
                    )
                    android.util.Log.i("IntelligenceRepository", "History Repair: Merged consecutive messages of same role (${msg.role})")
                } else {
                    alternatingHistory.add(msg)
                }
            }
        }

        while (alternatingHistory.isNotEmpty() && alternatingHistory.first().role != "user") {
            android.util.Log.w("IntelligenceRepository", "History Repair: Removed leading non-user message")
            alternatingHistory.removeAt(0)
        }

        return alternatingHistory
    }

    fun estimateTokenCount(text: String?): Int {
        if (text == null) return 0
        return (text.length / 4.0).toInt().coerceAtLeast(1)
    }

    fun estimateContentTokens(content: Content): Int {
        var tokens = 0
        for (part in content.parts) {
            if (!part.text.isNullOrEmpty()) {
                tokens += estimateTokenCount(part.text)
            }
            if (part.inlineData != null) {
                tokens += 258
            }
        }
        return tokens
    }

    fun compressHistory(history: List<MessageEntity>, maxTokens: Int = 15000): List<MessageEntity> {
        var currentHistory = history.toMutableList()
        var estimatedTokens = currentHistory.sumOf { msg ->
            estimateTokenCount(msg.text) + (if (!msg.imageUri.isNullOrEmpty()) 258 else 0)
        }

        var didCompress = false
        while (estimatedTokens > maxTokens && currentHistory.size > 2) {
            android.util.Log.i("IntelligenceRepository", "Compressing context: current tokens ($estimatedTokens) exceeds max ($maxTokens). Dropping oldest turns.")
            currentHistory.removeAt(0)
            currentHistory.removeAt(0)
            didCompress = true
            
            while (currentHistory.isNotEmpty() && currentHistory.first().role != "user") {
                currentHistory.removeAt(0)
            }
            
            estimatedTokens = currentHistory.sumOf { msg ->
                estimateTokenCount(msg.text) + (if (!msg.imageUri.isNullOrEmpty()) 258 else 0)
            }
        }
        if (didCompress) {
            com.example.data.diagnostics.DiagnosticsManager.updateSession {
                it.copy(compressionStatus = "Compressed history to $estimatedTokens tokens")
            }
        }
        return currentHistory
    }

enum class IntentLevel {
    LEVEL_1_DIRECT,      // Level 1 — Direct Answer
    LEVEL_2_ANALYTICAL,  // Level 2 — Analytical Answer
    LEVEL_3_DEEP,        // Level 3 — Deep Analysis
    LEVEL_4_FULL         // Level 4 — Full Intelligence Report
}

private fun detectIntentLevel(query: String, hasPreviousAnalysis: Boolean): IntentLevel {
    val q = query.lowercase().trim()
    
    // Level 4: Full Intelligence Report
    val level4Keywords = listOf(
        "complete analysis", "run all engines", "full report", "maximum depth", "intelligence report", 
        "run engines", "all modules", "reveal everything", "maximum analytical depth"
    )
    if (level4Keywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_4_FULL
    }
    
    // Level 3: Deep Analysis
    val level3Keywords = listOf(
        "deep dive", "analyze deeply", "hidden factors", "what am i missing", "root cause", 
        "full reasoning", "deeper analysis", "psychological drivers", "systemic loops", 
        "underlying patterns", "diagnose", "breakdown", "break down"
    )
    if (level3Keywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_3_DEEP
    }
    
    // Level 2: Analytical Answer
    val level2Keywords = listOf(
        "why", "compare", "analyze this", "explain your reasoning", "explain why", 
        "difference between", "how do they compare", "reasons for", "explain reasoning",
        "evaluate risk", "systems mapping"
    )
    if (level2Keywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_2_ANALYTICAL
    }
    
    // Level 1: Direct Answer (or simple choice/decision indicators)
    val level1Keywords = listOf(
        "which one is better", "which is better", "what should i do", "which option", 
        "should i choose", "is this good or bad", "what is the answer", "tell me what to do", 
        "make a decision", "decide for me", "which is best", "is it good", "is it bad",
        "hello", "hi", "hey", "greetings", "thanks", "thank you", "who are you", "what are you"
    )
    if (level1Keywords.any { q.contains(it) } || q.length < 25) {
         return IntentLevel.LEVEL_1_DIRECT
    }
    
    // Default fallback based on length or history
    return if (q.length > 70) {
        IntentLevel.LEVEL_3_DEEP
    } else if (hasPreviousAnalysis) {
        IntentLevel.LEVEL_2_ANALYTICAL
    } else {
        IntentLevel.LEVEL_1_DIRECT
    }
}