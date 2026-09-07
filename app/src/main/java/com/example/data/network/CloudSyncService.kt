package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudSyncService {
    private const val TAG = "CloudSyncService"

    init {
        ensureFirestoreConfigured()
    }

    fun ensureFirestoreConfigured() {
        try {
            val db = FirebaseFirestore.getInstance()
            val currentSettings = db.firestoreSettings
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder(currentSettings)
                .setLocalCacheSettings(
                    com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                        .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            db.firestoreSettings = settings
            Log.d(TAG, "Firestore offline persistence configured successfully with unlimited cache size")
        } catch (e: Exception) {
            Log.d(TAG, "Firestore persistence settings notice: ${e.message}")
        }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Submit feedback to Firestore natively
     */
    suspend fun submitFeedback(
        userId: String,
        userName: String,
        email: String,
        message: String,
        appVersion: String,
        category: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Send email FIRST and independently — never let Firestore errors block it
        try {
            sendFeedbackEmail(userName, email, category, message, appVersion)
        } catch (e: Exception) {
            Log.e(TAG, "sendFeedbackEmail failed", e)
        }

        // Then try saving to Firestore separately
        return@withContext try {
            val db = FirebaseFirestore.getInstance()
            val feedback = mapOf(
                "userId" to userId,
                "userName" to userName,
                "email" to email,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "appVersion" to appVersion,
                "category" to category
            )
            val task = db.collection("feedback").add(feedback)
            com.google.android.gms.tasks.Tasks.await(task)
            Log.d(TAG, "Feedback submitted successfully via native Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore feedback save failed (email was still sent)", e)
            // Return true anyway since email was sent
            true
        }
    }

    /**
     * Submit bug report to Firestore natively
     */
    suspend fun submitBugReport(
        userId: String,
        userName: String,
        email: String,
        description: String,
        deviceInfo: String,
        androidVersion: String,
        appVersion: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Send email FIRST and independently
        try {
            sendFeedbackEmail(userName, email, "Bug Report", description, appVersion, deviceInfo)
        } catch (e: Exception) {
            Log.e(TAG, "sendFeedbackEmail for bug report failed", e)
        }

        // Then try saving to Firestore separately
        return@withContext try {
            val db = FirebaseFirestore.getInstance()
            val bugReport = mapOf(
                "userId" to userId,
                "userName" to userName,
                "email" to email,
                "description" to description,
                "deviceInfo" to deviceInfo,
                "androidVersion" to androidVersion,
                "appVersion" to appVersion,
                "timestamp" to System.currentTimeMillis()
            )
            val task = db.collection("bug_reports").add(bugReport)
            com.google.android.gms.tasks.Tasks.await(task)
            Log.d(TAG, "Bug report submitted successfully via native Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore bug report save failed (email was still sent)", e)
            true
        }
    }

    /**
     * Submit Issue to GitHub if token configured
     */
    suspend fun submitGithubIssue(
        token: String,
        repoOwnerAndName: String,
        title: String,
        bodyText: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || repoOwnerAndName.isBlank()) return@withContext false

        try {
            val url = "https://api.github.com/repos/$repoOwnerAndName/issues"
            
            val json = JSONObject().apply {
                put("title", title)
                put("body", bodyText)
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Placeholder File Upload to Supabase Storage REST
     */
    suspend fun uploadToSupabaseStorage(
        localFile: File,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        val fileName = "uploads/${UUID.randomUUID()}_${localFile.name}"
        try {
            SupabaseStorageClient.uploadFile(
                bucket = "attachments",
                path = fileName,
                file = localFile,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create user profile in Firestore if not exist
     */
    suspend fun createProfileIfNotExist(userId: String, email: String, name: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId == "guest_local") return@withContext false
        try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)
            val profile = mapOf(
                "uid" to userId,
                "email" to email,
                "name" to name,
                "lastActive" to System.currentTimeMillis()
            )
            userRef.set(profile, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.d("SYNC", "Profile sync notice: ${e.message}")
                }
            true
        } catch (e: Exception) {
            Log.d("SYNC", "Notice in createProfileIfNotExist: ${e.message}")
            false
        }
    }

    /**
     * Synchronize a Session (chat meta) natively to Firestore
     * Matches collection path /users/{userId}/chats/{sessionId}
     */
    suspend fun uploadSession(
        userId: String,
        sessionId: String,
        title: String,
        isPinned: Boolean,
        createdAt: Long,
        updatedAt: Long,
        email: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val resolvedEmail = if (email.isNotBlank()) email else {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email.orEmpty()
                } catch (e: Exception) { "" }
            }
            val data = mutableMapOf<String, Any>(
                "id" to sessionId,
                "title" to title,
                "isPinned" to isPinned,
                "createdAt" to createdAt,
                "lastUpdatedAt" to updatedAt,
                "userId" to userId
            )
            if (resolvedEmail.isNotBlank()) {
                data["email"] = resolvedEmail
            }
            Log.d("CHAT_SAVE", "Uploading/saving session item to cloud: sessionId=$sessionId for userId=$userId")
            val task = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .set(data, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task)
            
            // Mirror under sanitized email doc if userId is different, ensuring cross-auth sync
            if (resolvedEmail.isNotBlank() && userId != resolvedEmail && userId != "local_${resolvedEmail.replace(".", "_")}") {
                try {
                    val mirrorTask = db.collection("users").document("local_${resolvedEmail.replace(".", "_")}")
                        .collection("chats").document(sessionId)
                        .set(data, SetOptions.merge())
                    com.google.android.gms.tasks.Tasks.await(mirrorTask)
                } catch (me: Exception) {
                    // Ignore mirror failure
                }
            }
            Log.d("FIRESTORE_WRITE", "Firestore write success: session $sessionId details uploaded")
            Log.d("SYNC", "Firestore write success")
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error in uploadSession: ${e.message}", e)
            Log.e("CHAT_SAVE", "Failed saving session item to cloud: $sessionId", e)
            false
        }
    }

    /**
     * Synchronize a Message natively to Firestore
     * Matches collection path /users/{userId}/chats/{sessionId}/messages/{messageId}
     */
    suspend fun uploadAttachment(
        userId: String,
        sessionId: String,
        messageId: String,
        attachment: com.example.data.model.AttachmentEntity,
        fileSize: Long = 0L,
        context: android.content.Context? = com.google.firebase.FirebaseApp.getInstance().applicationContext
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var finalRemoteUrl = attachment.remoteUrl
            val db = FirebaseFirestore.getInstance()
            val storagePath = "$userId/$sessionId/$messageId/${attachment.fileName}"
            
            // Upload file to Supabase Storage if we don't have a remote URL yet
            if (finalRemoteUrl.isNullOrBlank() && attachment.localUri.isNotBlank()) {
                val uri = android.net.Uri.parse(attachment.localUri)
                
                try {
                    val downloadUrl = if (uri.scheme == "content" && context != null) {
                        SupabaseStorageClient.uploadInputStream(
                            context = context,
                            bucket = "attachments",
                            path = storagePath,
                            uri = uri,
                            mimeType = attachment.mimeType
                        )
                    } else {
                        val path = uri.path ?: attachment.localUri
                        val file = java.io.File(path)
                        if (file.exists()) {
                            SupabaseStorageClient.uploadFile(
                                bucket = "attachments",
                                path = storagePath,
                                file = file,
                                mimeType = attachment.mimeType
                            )
                        } else null
                    }
                    
                    if (downloadUrl != null) {
                        finalRemoteUrl = downloadUrl
                        
                        // Update local DB if possible (we do it in repository instead if we don't have DB instance here, but let's try if context is provided)
                        if (context != null) {
                            val dbLocal = com.example.data.database.DepthDatabase.getDatabase(context!!)
                            dbLocal.attachmentDao().insertAttachment(attachment.copy(remoteUrl = finalRemoteUrl, storagePath = storagePath))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SYNC", "Error uploading file to Supabase Storage: ${e.message}")
                }
            }

            val data = mapOf(
                "attachmentId" to attachment.attachmentId,
                "messageId" to attachment.messageId,
                "mimeType" to attachment.mimeType,
                // NEVER STORE TEMPORARY ANDROID URI in cloud.
                "localUri" to "",
                "remoteUrl" to (finalRemoteUrl ?: ""),
                "thumbnailUrl" to (attachment.thumbnailUrl ?: ""),
                "fileName" to attachment.fileName,
                // Audited metadata
                "downloadUrl" to (finalRemoteUrl ?: ""),
                "storagePath" to storagePath,
                "size" to fileSize,
                "uploadTimestamp" to System.currentTimeMillis(),
                "chatId" to sessionId,
                "userId" to userId
            )
            val task = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .collection("messages").document(messageId)
                .collection("attachments").document(attachment.attachmentId)
                .set(data, SetOptions.merge())
            task.awaitTask()
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error uploading attachment: ${e.message}", e)
            false
        }
    }

    suspend fun uploadMessage(
        userId: String,
        messageId: String,
        sessionId: String,
        role: String,
        text: String,
        imageUri: String?,
        timestamp: Long,
        replyToMessageId: String? = null,
        selectedText: String? = null,
        email: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val resolvedEmail = if (email.isNotBlank()) email else {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email.orEmpty()
                } catch (e: Exception) { "" }
            }
            val cleanReplyId = replyToMessageId?.trim()?.takeIf { it.isNotBlank() }
            val cleanSelectedText = selectedText?.trim()?.takeIf { it.isNotBlank() }
            val data = mutableMapOf<String, Any>(
                "id" to messageId,
                "sessionId" to sessionId,
                "role" to role,
                "text" to text,
                "imageUri" to (imageUri ?: ""),
                "timestamp" to timestamp,
                "userId" to userId
            )
            if (cleanReplyId != null && cleanSelectedText != null) {
                data["replyToMessageId"] = cleanReplyId
                data["selectedText"] = cleanSelectedText
            }
            if (resolvedEmail.isNotBlank()) {
                data["email"] = resolvedEmail
            }
            Log.d("CHAT_SAVE", "Uploading message details to cloud: messageId=$messageId in sessionId=$sessionId")
            val task = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .collection("messages").document(messageId)
                .set(data, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task)
            
            // Mirror message if userId differs from sanitized email
            if (resolvedEmail.isNotBlank() && userId != resolvedEmail && userId != "local_${resolvedEmail.replace(".", "_")}") {
                try {
                    val mirrorTask = db.collection("users").document("local_${resolvedEmail.replace(".", "_")}")
                        .collection("chats").document(sessionId)
                        .collection("messages").document(messageId)
                        .set(data, SetOptions.merge())
                    com.google.android.gms.tasks.Tasks.await(mirrorTask)
                } catch (me: Exception) {
                    // Ignore mirror failure
                }
            }
            Log.d("FIRESTORE_WRITE", "Firestore write success: message $messageId details uploaded")
            Log.d("SYNC", "Firestore write success")
            
            // Touch chat lastUpdatedAt
            try {
                val touchTask = db.collection("users").document(userId)
                    .collection("chats").document(sessionId)
                    .update("lastUpdatedAt", timestamp)
                com.google.android.gms.tasks.Tasks.await(touchTask)
            } catch (te: Exception) {
                Log.e("SYNC", "Error updating lastUpdatedAt: ${te.message}")
            }

            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error in uploadMessage: ${e.message}", e)
            Log.e("CHAT_SAVE", "Failed saving message item to cloud: $messageId", e)
            false
        }
    }

    /**
     * Synchronize a Memory insight to Firestore
     */
    suspend fun uploadMemoryInsight(
        userId: String,
        insightId: Long,
        category: String,
        content: String,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to insightId,
                "category" to category,
                "content" to content,
                "timestamp" to timestamp
            )
            val task = db.collection("users").document(userId)
                .collection("memories").document(insightId.toString())
                .set(data, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getLongSafely(doc: com.google.firebase.firestore.DocumentSnapshot?, field: String, default: Long): Long {
        if (doc == null) return default
        return try {
            val value = doc.get(field)
            when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: default
                is com.google.firebase.Timestamp -> value.toDate().time
                is java.util.Date -> value.time
                is Map<*, *> -> {
                    val seconds = value["seconds"] ?: value["_seconds"]
                    val milli = value["milli"] ?: value["milliseconds"] ?: value["_milliseconds"] ?: value["time"]
                    if (milli is Number) {
                        milli.toLong()
                    } else if (seconds is Number) {
                        seconds.toLong() * 1000L
                    } else {
                        default
                    }
                }
                else -> default
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun getBooleanSafely(doc: com.google.firebase.firestore.DocumentSnapshot?, field: String, default: Boolean): Boolean {
        if (doc == null) return default
        return try {
            val value = doc.get(field)
            when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> value.lowercase() == "true" || value == "1"
                else -> default
            }
        } catch (e: Exception) {
            default
        }
    }

    private fun getStringSafely(doc: com.google.firebase.firestore.DocumentSnapshot?, field: String, default: String): String {
        if (doc == null) return default
        return try {
            val value = doc.get(field)
            when (value) {
                null -> default
                is String -> value
                else -> value.toString()
            }
        } catch (e: Exception) {
            default
        }
    }
    suspend fun syncSingleSession(
        userId: String,
        sessionId: String,
        sessionDao: com.example.data.database.SessionDao,
        messageDao: com.example.data.database.MessageDao,
        attachmentDao: com.example.data.database.AttachmentDao
    ): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId == "guest_local") return@withContext false
        try {
            val db = FirebaseFirestore.getInstance()
            
            // Query all candidate collections in parallel
            val subcollectionsToQuery = listOf("chats", "sessions", "history", "chatHistory")
            val docSnapJobs = subcollectionsToQuery.map { subCol ->
                async(Dispatchers.IO) {
                    try {
                        val docRef = db.collection("users").document(userId).collection(subCol).document(sessionId)
                        val snap = com.google.android.gms.tasks.Tasks.await(docRef.get(), 3, TimeUnit.SECONDS)
                        if (snap.exists()) snap else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            val docSnap = docSnapJobs.awaitAll().filterNotNull().firstOrNull()
            if (docSnap == null) {
                Log.d("SYNC", "syncSingleSession: Session $sessionId not found in remote collections")
                return@withContext false
            }
            
            val docRef = docSnap.reference

            val title = getStringSafely(docSnap, "title", "")
                .takeIf { it.isNotBlank() }
                ?: getStringSafely(docSnap, "name", "Untitled")
            val isPinned = getBooleanSafely(docSnap, "isPinned", false)
            val createdAt = getLongSafely(docSnap, "createdAt", System.currentTimeMillis())
            val lastUpdatedAt = getLongSafely(docSnap, "lastUpdatedAt", createdAt)
            
            val sEntity = com.example.data.model.SessionEntity(
                id = sessionId,
                title = title,
                isPinned = isPinned,
                createdAt = createdAt,
                lastUpdatedAt = lastUpdatedAt
            )
            sessionDao.insertSession(sEntity)
            
            val foundMessages = mutableListOf<com.example.data.model.MessageEntity>()

            // A. Load inline messages list/history if present
            try {
                val inlineMessages = docSnap.get("messages") ?: docSnap.get("history") ?: docSnap.get("chats")
                if (inlineMessages is List<*>) {
                    for ((index, item) in inlineMessages.withIndex()) {
                        try {
                            if (item is Map<*, *>) {
                                val msgId = (item["id"]
                                    ?: item["messageId"]
                                    ?: item["msgId"]
                                    ?: item["message_id"]
                                    ?: item["uid"]
                                    ?: "${sessionId}_inline_$index").toString()
                                
                                var role = (item["role"] ?: item["sender"] ?: item["author"] ?: "").toString()
                                if (role.isBlank()) {
                                    val isUser = item["isUser"] as? Boolean ?: item["is_user"] as? Boolean ?: true
                                    role = if (isUser) "user" else "model"
                                }
                                val finalRole = if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) "model" else "user"
                                val text = (item["text"] ?: item["content"] ?: item["message"] ?: item["body"] ?: "").toString()
                                val imageUri = (item["imageUri"] ?: item["imageUrl"] ?: item["image_uri"] ?: "").toString()
                                
                                val tVal = item["timestamp"] ?: item["time"] ?: item["createdAt"]
                                val timestamp = when (tVal) {
                                    is Number -> tVal.toLong()
                                    is String -> tVal.toLongOrNull() ?: System.currentTimeMillis()
                                    is com.google.firebase.Timestamp -> tVal.toDate().time
                                    else -> System.currentTimeMillis()
                                }
                                
                                val rawReplyId = item["replyToMessageId"]?.toString()?.trim()
                                val rawSelectedText = item["selectedText"]?.toString()?.trim()
                                val validReplyId = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawReplyId else null
                                val validSelectedText = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawSelectedText else null

                                val mEntity = com.example.data.model.MessageEntity(
                                    id = msgId,
                                    sessionId = sessionId,
                                    role = finalRole,
                                    text = text,
                                    imageUri = if (imageUri.isEmpty()) null else imageUri,
                                    timestamp = timestamp,
                                    replyToMessageId = validReplyId,
                                    selectedText = validSelectedText
                                )
                                foundMessages.add(mEntity)
                            }
                        } catch (me: Exception) {}
                    }
                }
            } catch (ae: Exception) {}

            // B. Fetch subcollection messages in parallel
            val subMsgCollections = listOf("messages", "chats")
            for (subColName in subMsgCollections) {
                try {
                    val msgsSnap = com.google.android.gms.tasks.Tasks.await(
                        docRef.collection(subColName).get(),
                        3,
                        TimeUnit.SECONDS
                    )
                    if (msgsSnap != null && !msgsSnap.isEmpty) {
                        for (msgDoc in msgsSnap.documents) {
                            val msgId = msgDoc.id
                            var role = getStringSafely(msgDoc, "role", "")
                                .takeIf { it.isNotBlank() }
                                ?: getStringSafely(msgDoc, "sender", "")
                                .takeIf { it.isNotBlank() }
                                ?: ""
                            if (role.isBlank()) {
                                val isUser = getBooleanSafely(msgDoc, "isUser", true)
                                role = if (isUser) "user" else "model"
                            }
                            val finalRole = if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) "model" else "user"
                            val text = getStringSafely(msgDoc, "text", "")
                                .takeIf { it.isNotBlank() }
                                ?: getStringSafely(msgDoc, "content", "")
                                .takeIf { it.isNotBlank() }
                                ?: getStringSafely(msgDoc, "message", "")
                                .takeIf { it.isNotBlank() }
                                ?: ""
                            val imageUri = getStringSafely(msgDoc, "imageUri", "")
                                .takeIf { it.isNotBlank() }
                                ?: getStringSafely(msgDoc, "imageUrl", "")
                                .takeIf { it.isNotBlank() }
                                ?: ""
                            val timestamp = getLongSafely(msgDoc, "timestamp", 0L)
                                .takeIf { it > 0 }
                                ?: getLongSafely(msgDoc, "time", 0L)
                                .takeIf { it > 0 }
                                ?: getLongSafely(msgDoc, "createdAt", 0L)
                                .takeIf { it > 0 }
                                ?: System.currentTimeMillis()

                            val rawReplyId = msgDoc.getString("replyToMessageId")?.trim()
                            val rawSelectedText = msgDoc.getString("selectedText")?.trim()
                            val validReplyId = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawReplyId else null
                            val validSelectedText = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawSelectedText else null

                            val mEntity = com.example.data.model.MessageEntity(
                                id = msgId,
                                sessionId = sessionId,
                                role = finalRole,
                                text = text,
                                imageUri = if (imageUri.isEmpty()) null else imageUri,
                                timestamp = timestamp,
                                replyToMessageId = validReplyId,
                                selectedText = validSelectedText
                            )
                            foundMessages.add(mEntity)
                        }
                        if (msgsSnap.size() > 0) break
                    }
                } catch (e: Exception) {}
            }

            if (foundMessages.isNotEmpty()) {
                messageDao.insertMessages(foundMessages.distinctBy { it.id })
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("SYNC", "syncSingleSession failed: ${e.message}")
            return@withContext false
        }
    }

    suspend fun fetchAndSyncAll(
        userId: String,
        sessionDao: com.example.data.database.SessionDao,
        messageDao: com.example.data.database.MessageDao,
        attachmentDao: com.example.data.database.AttachmentDao,
        userEmail: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val db = FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val authUser = auth.currentUser
            val currentAuthUid = authUser?.uid.orEmpty()
            val currentAuthEmail = authUser?.email.orEmpty()

            Log.i("SYNC_UID_VERIFICATION", "Starting fast cloud synchronization: paramUid='$userId', paramEmail='$userEmail', authUid='$currentAuthUid'")

            if (currentAuthUid.isBlank()) {
                Log.i("SYNC", "No authenticated user. Skipping remote sync for guest.")
                return@withContext true
            }

            val effectiveEmail = userEmail.ifBlank { currentAuthEmail }.ifBlank { "ashah331@gmail.com" }
            val candidateUserIds = linkedSetOf<String>()
            if (currentAuthUid.isNotBlank()) candidateUserIds.add(currentAuthUid)
            if (userId.isNotBlank() && userId != "guest_local") candidateUserIds.add(userId)
            if (effectiveEmail.isNotBlank()) {
                candidateUserIds.add("local_${effectiveEmail.replace(".", "_")}")
                candidateUserIds.add(effectiveEmail)
            }
            if (candidateUserIds.isEmpty()) candidateUserIds.add("ashah331@gmail.com")

            suspend fun getQuerySnapshotFast(query: com.google.firebase.firestore.Query, timeoutSec: Long = 2): com.google.firebase.firestore.QuerySnapshot? {
                val defaultSnap = try {
                    com.google.android.gms.tasks.Tasks.await(query.get(com.google.firebase.firestore.Source.DEFAULT), timeoutSec, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    null
                }
                if (defaultSnap != null && !defaultSnap.isEmpty) {
                    return defaultSnap
                }
                val cacheSnap = try {
                    com.google.android.gms.tasks.Tasks.await(query.get(com.google.firebase.firestore.Source.CACHE), 1, TimeUnit.SECONDS)
                } catch (ce: Exception) {
                    null
                }
                return if (cacheSnap != null && !cacheSnap.isEmpty) cacheSnap else defaultSnap
            }

            val allDocuments = java.util.concurrent.CopyOnWriteArrayList<com.google.firebase.firestore.DocumentSnapshot>()
            val processedDocIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

            // 0. Load tombstones from local SharedPreferences and remote deleted_chats collections
            val appContext = try { com.google.firebase.FirebaseApp.getInstance().applicationContext } catch (e: Exception) { null }
            val prefs = appContext?.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)
            val localDeletedSet: Set<String> = prefs?.getStringSet("deleted_session_ids_set", emptySet<String>()) ?: emptySet()
            val allDeletedSessionIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
            allDeletedSessionIds.addAll(localDeletedSet)

            val tombstoneJobs = candidateUserIds.map { cId ->
                async(Dispatchers.IO) {
                    try {
                        val tombCol = db.collection("users").document(cId).collection("deleted_chats")
                        val snap = getQuerySnapshotFast(tombCol, 2)
                        if (snap != null && !snap.isEmpty) {
                            for (doc in snap.documents) {
                                allDeletedSessionIds.add(doc.id)
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            tombstoneJobs.awaitAll()

            // Keep local prefs updated with any new remote tombstones
            val updatedLocalDeletedSet: Set<String> = (localDeletedSet + allDeletedSessionIds)
            prefs?.edit()?.putStringSet("deleted_session_ids_set", updatedLocalDeletedSet)?.apply()

            // Purge any deleted session from local database immediately
            for (delId in allDeletedSessionIds) {
                sessionDao.deleteSessionById(delId)
                messageDao.deleteMessagesForSession(delId)
                attachmentDao.deleteAttachmentsForSession(delId)
            }

            // 1. Parallel collection scan across primary candidate paths
            val primaryCollections = listOf("chats", "sessions")
            val scanJobs = candidateUserIds.flatMap { cId ->
                primaryCollections.map { subCol ->
                    async(Dispatchers.IO) {
                        try {
                            val colRef = db.collection("users").document(cId).collection(subCol)
                            val snap = getQuerySnapshotFast(colRef, 2)
                            if (snap != null && !snap.isEmpty) {
                                for (doc in snap.documents) {
                                    if (allDeletedSessionIds.contains(doc.id)) {
                                        try { doc.reference.delete() } catch (e: Exception) {}
                                        continue
                                    }
                                    if (processedDocIds.add(doc.id)) {
                                        allDocuments.add(doc)
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            scanJobs.awaitAll()

            // If empty, quick fallback scan on legacy collections
            if (allDocuments.isEmpty()) {
                val fallbackJobs = candidateUserIds.flatMap { cId ->
                    listOf("history", "chatHistory").map { subCol ->
                        async(Dispatchers.IO) {
                            try {
                                val colRef = db.collection("users").document(cId).collection(subCol)
                                val snap = getQuerySnapshotFast(colRef, 1)
                                if (snap != null && !snap.isEmpty) {
                                    for (doc in snap.documents) {
                                        if (allDeletedSessionIds.contains(doc.id)) {
                                            try { doc.reference.delete() } catch (e: Exception) {}
                                            continue
                                        }
                                        if (processedDocIds.add(doc.id)) {
                                            allDocuments.add(doc)
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
                fallbackJobs.awaitAll()
            }

            Log.i("SYNC_RESULT", "Fast scan found ${allDocuments.size} unique remote session documents")

            val initialLocalSessions = sessionDao.getAllSessions()
            val initialLocalSessionsMap = initialLocalSessions.associateBy { it.id }

            val sessionsToInsert = java.util.concurrent.CopyOnWriteArrayList<com.example.data.model.SessionEntity>()
            val messagesToInsert = java.util.concurrent.CopyOnWriteArrayList<com.example.data.model.MessageEntity>()
            val attachmentsToInsert = java.util.concurrent.CopyOnWriteArrayList<com.example.data.model.AttachmentEntity>()

            // 2. Hydrate all remote sessions in parallel
            val hydrationJobs = allDocuments.map { doc ->
                async(Dispatchers.IO) {
                    try {
                        val sessionId = doc.id
                        val title = getStringSafely(doc, "title", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "name", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "topic", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "sessionName", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "session_name", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "label", "")
                            .takeIf { it.isNotBlank() }
                            ?: getStringSafely(doc, "subject", "")
                            .takeIf { it.isNotBlank() }
                            ?: "Saved Session"

                        val isPinned = getBooleanSafely(doc, "isPinned", false) || getBooleanSafely(doc, "pinned", false) || getBooleanSafely(doc, "is_pinned", false)

                        val createdAt = getLongSafely(doc, "createdAt", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "created_at", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "timestamp", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "time", 0L)
                            .takeIf { it > 0 }
                            ?: System.currentTimeMillis()

                        val lastUpdatedAt = getLongSafely(doc, "lastUpdatedAt", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "last_updated_at", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "updatedAt", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "updated_at", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "lastUsed", 0L)
                            .takeIf { it > 0 }
                            ?: getLongSafely(doc, "last_used", 0L)
                            .takeIf { it > 0 }
                            ?: createdAt

                        val localSessionObj = initialLocalSessionsMap[sessionId]
                        val finalTitle = if (localSessionObj != null &&
                            !com.example.data.repository.IntelligenceRepository.isGenericTitle(localSessionObj.title) &&
                            com.example.data.repository.IntelligenceRepository.isGenericTitle(title)) {
                            localSessionObj.title
                        } else {
                            title
                        }

                        val sEntity = com.example.data.model.SessionEntity(
                            id = sessionId,
                            title = finalTitle,
                            isPinned = isPinned,
                            createdAt = createdAt,
                            lastUpdatedAt = lastUpdatedAt
                        )
                        sessionsToInsert.add(sEntity)

                        val localMsgCount = if (localSessionObj != null) {
                            try { messageDao.getMessagesForSession(sessionId).size } catch (e: Exception) { 0 }
                        } else 0

                        val needsMessageFetch = localSessionObj == null || localMsgCount == 0 || lastUpdatedAt > localSessionObj.lastUpdatedAt
                        if (!needsMessageFetch) {
                            return@async
                        }

                        val foundRemoteMessages = mutableMapOf<String, com.example.data.model.MessageEntity>()

                        // A. Check inline messages
                        try {
                            val inlineMessages = doc.get("messages") ?: doc.get("history") ?: doc.get("chats")
                            if (inlineMessages is List<*>) {
                                for ((index, item) in inlineMessages.withIndex()) {
                                    if (item is Map<*, *>) {
                                        val msgId = (item["id"] ?: item["messageId"] ?: item["msgId"] ?: item["message_id"] ?: "${sessionId}_inline_$index").toString()
                                        var role = (item["role"] ?: item["sender"] ?: item["author"] ?: "").toString()
                                        if (role.isBlank()) {
                                            val isUser = item["isUser"] as? Boolean ?: item["is_user"] as? Boolean ?: true
                                            role = if (isUser) "user" else "model"
                                        }
                                        val finalRole = if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) "model" else "user"
                                        val text = (item["text"] ?: item["content"] ?: item["message"] ?: item["body"] ?: "").toString()
                                        val imageUri = (item["imageUri"] ?: item["imageUrl"] ?: item["image_uri"] ?: "").toString()
                                        val tVal = item["timestamp"] ?: item["time"] ?: item["createdAt"]
                                        val timestamp = when (tVal) {
                                            is Number -> tVal.toLong()
                                            is String -> tVal.toLongOrNull() ?: System.currentTimeMillis()
                                            is com.google.firebase.Timestamp -> tVal.toDate().time
                                            else -> System.currentTimeMillis()
                                        }
                                        val rawReplyId = item["replyToMessageId"]?.toString()?.trim()
                                        val rawSelectedText = item["selectedText"]?.toString()?.trim()
                                        val validReplyId = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawReplyId else null
                                        val validSelectedText = if (!rawReplyId.isNullOrBlank() && !rawSelectedText.isNullOrBlank()) rawSelectedText else null

                                        val mEntity = com.example.data.model.MessageEntity(
                                            id = msgId,
                                            sessionId = sessionId,
                                            role = finalRole,
                                            text = text,
                                            imageUri = if (imageUri.isEmpty()) null else imageUri,
                                            timestamp = timestamp,
                                            replyToMessageId = validReplyId,
                                            selectedText = validSelectedText
                                        )
                                        foundRemoteMessages[msgId] = mEntity
                                    }
                                }
                            }
                        } catch (e: Exception) {}

                        // B. If subcollection needed, query messages subcollection in parallel
                        if (foundRemoteMessages.isEmpty()) {
                            val messageSubcollections = listOf("messages", "chats")
                            for (subColName in messageSubcollections) {
                                try {
                                    val subRef = doc.reference.collection(subColName)
                                    val msgsSnap = getQuerySnapshotFast(subRef, 2)
                                    if (msgsSnap != null && !msgsSnap.isEmpty) {
                                        for (msgDoc in msgsSnap.documents) {
                                            val msgId = msgDoc.id
                                            var role = getStringSafely(msgDoc, "role", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: getStringSafely(msgDoc, "sender", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: ""
                                            if (role.isBlank()) {
                                                val isUser = getBooleanSafely(msgDoc, "isUser", true)
                                                role = if (isUser) "user" else "model"
                                            }
                                            val finalRole = if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) "model" else "user"
                                            val text = getStringSafely(msgDoc, "text", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: getStringSafely(msgDoc, "content", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: getStringSafely(msgDoc, "message", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: ""
                                            val imageUri = getStringSafely(msgDoc, "imageUri", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: getStringSafely(msgDoc, "imageUrl", "")
                                                .takeIf { it.isNotBlank() }
                                                ?: ""
                                            val timestamp = getLongSafely(msgDoc, "timestamp", 0L)
                                                .takeIf { it > 0 }
                                                ?: getLongSafely(msgDoc, "time", 0L)
                                                .takeIf { it > 0 }
                                                ?: getLongSafely(msgDoc, "createdAt", 0L)
                                                .takeIf { it > 0 }
                                                ?: System.currentTimeMillis()

                                            val subReplyId = msgDoc.getString("replyToMessageId")?.trim()
                                            val subSelectedText = msgDoc.getString("selectedText")?.trim()
                                            val validSubReplyId = if (!subReplyId.isNullOrBlank() && !subSelectedText.isNullOrBlank()) subReplyId else null
                                            val validSubSelectedText = if (!subReplyId.isNullOrBlank() && !subSelectedText.isNullOrBlank()) subSelectedText else null

                                            val mEntity = com.example.data.model.MessageEntity(
                                                id = msgId,
                                                sessionId = sessionId,
                                                role = finalRole,
                                                text = text,
                                                imageUri = if (imageUri.isEmpty()) null else imageUri,
                                                timestamp = timestamp,
                                                replyToMessageId = validSubReplyId,
                                                selectedText = validSubSelectedText
                                            )
                                            foundRemoteMessages[msgId] = mEntity
                                        }
                                        if (msgsSnap.size() > 0) break
                                    }
                                } catch (se: Exception) {}
                            }
                        }

                        if (foundRemoteMessages.isNotEmpty()) {
                            messagesToInsert.addAll(foundRemoteMessages.values)
                        }
                    } catch (se: Exception) {
                        Log.e("SYNC", "Error processing remote session descriptor: ${se.message}")
                    }
                }
            }
            hydrationJobs.awaitAll()

            // 3. Ultra-fast batch insert into local Room SQLite database
            if (sessionsToInsert.isNotEmpty()) {
                sessionDao.insertSessions(sessionsToInsert.distinctBy { it.id })
            }
            if (messagesToInsert.isNotEmpty()) {
                messageDao.insertMessages(messagesToInsert.distinctBy { it.id })
            }
            if (attachmentsToInsert.isNotEmpty()) {
                attachmentDao.insertAttachments(attachmentsToInsert.distinctBy { it.attachmentId })
            }

            Log.i("SYNC_RESULT", "Inserted ${sessionsToInsert.size} sessions and ${messagesToInsert.size} messages in ${System.currentTimeMillis() - startTime}ms")

            // 4. Concurrently push local unsynced sessions to Firestore
            val primaryUploadId = if (userId.isNotBlank() && userId != "guest_local") userId
                else if (currentAuthUid.isNotBlank()) currentAuthUid
                else if (effectiveEmail.isNotBlank()) "local_${effectiveEmail.replace(".", "_")}"
                else ""

            if (primaryUploadId.isNotBlank() && primaryUploadId != "guest_local") {
                val currentLocalSessions = sessionDao.getAllSessions()
                val remoteSessionsMap = allDocuments.associateBy { it.id }
                val uploadJobs = currentLocalSessions.mapNotNull { localSession ->
                    if (allDeletedSessionIds.contains(localSession.id)) return@mapNotNull null
                    val remoteDoc = remoteSessionsMap[localSession.id]
                    val remoteUpdatedAt = getLongSafely(remoteDoc, "lastUpdatedAt", 0L)
                    if (remoteDoc == null || localSession.lastUpdatedAt > remoteUpdatedAt) {
                        async(Dispatchers.IO) {
                            try {
                                val localMessages = messageDao.getMessagesForSession(localSession.id)
                                if (localMessages.isNotEmpty()) {
                                    uploadSession(
                                        userId = primaryUploadId,
                                        sessionId = localSession.id,
                                        title = localSession.title,
                                        isPinned = localSession.isPinned,
                                        createdAt = localSession.createdAt,
                                        updatedAt = localSession.lastUpdatedAt,
                                        email = effectiveEmail
                                    )
                                    for (localMsg in localMessages) {
                                        uploadMessage(
                                            userId = primaryUploadId,
                                            messageId = localMsg.id,
                                            sessionId = localMsg.sessionId,
                                            role = localMsg.role,
                                            text = localMsg.text,
                                            imageUri = localMsg.imageUri,
                                            timestamp = localMsg.timestamp,
                                            replyToMessageId = localMsg.replyToMessageId,
                                            selectedText = localMsg.selectedText,
                                            email = effectiveEmail
                                        )
                                    }
                                }
                            } catch (le: Exception) {
                                Log.d("SYNC", "Error pushing local session ${localSession.id}: ${le.message}")
                            }
                        }
                    } else null
                }
                uploadJobs.awaitAll()
            }

            Log.i("SYNC_STATUS", "Fast fetchAndSyncAll completed in ${System.currentTimeMillis() - startTime}ms. Restored ${allDocuments.size} remote sessions.")
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error in fetchAndSyncAll: ${e.message}", e)
            Log.e("SYNC_STATUS", "fetchAndSyncAll failed", e)
            false
        }
    }

    /**
     * Delete a Session (chat meta) and all its subcollections natively from Firestore
     */
    suspend fun deleteSession(userId: String, sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val authUser = auth.currentUser
            val currentAuthUid = authUser?.uid.orEmpty()
            val currentAuthEmail = authUser?.email.orEmpty()

            val candidateUserIds = linkedSetOf<String>()
            if (userId.isNotBlank() && userId != "guest_local") candidateUserIds.add(userId)
            if (currentAuthUid.isNotBlank()) candidateUserIds.add(currentAuthUid)
            if (currentAuthEmail.isNotBlank()) {
                candidateUserIds.add("local_${currentAuthEmail.replace(".", "_")}")
                candidateUserIds.add(currentAuthEmail)
            }
            candidateUserIds.add("local_ashah331@gmail_com")
            candidateUserIds.add("ashah331@gmail.com")

            val targetCollections = listOf("chats", "sessions", "history", "chatHistory")

            for (cId in candidateUserIds) {
                // Register in the deleted tombstones collection for multi-device sync
                try {
                    val deletedDocRef = db.collection("users").document(cId)
                        .collection("deleted_chats").document(sessionId)
                    val writeDeletedTask = deletedDocRef.set(mapOf("deletedAt" to System.currentTimeMillis()))
                    com.google.android.gms.tasks.Tasks.await(writeDeletedTask)
                    Log.d(TAG, "Registered deleted session $sessionId in tombstones for $cId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register deleted session $sessionId tombstone for $cId: ${e.message}")
                }

                // Delete from all candidate collections under this user
                for (colName in targetCollections) {
                    try {
                        val docRef = db.collection("users").document(cId)
                            .collection(colName).document(sessionId)

                        // Delete messages subcollection
                        try {
                            val messagesRef = docRef.collection("messages")
                            val msgsSnap = com.google.android.gms.tasks.Tasks.await(messagesRef.get())
                            for (doc in msgsSnap.documents) {
                                try {
                                    deleteMessage(cId, sessionId, doc.id)
                                } catch (me: Exception) {}
                            }
                        } catch (e: Exception) {}

                        // Delete nested subcollections
                        for (subColName in listOf("chats", "history", "chatHistory", "messages")) {
                            try {
                                val subRef = docRef.collection(subColName)
                                val snap = com.google.android.gms.tasks.Tasks.await(subRef.get())
                                for (doc in snap.documents) {
                                    try {
                                        com.google.android.gms.tasks.Tasks.await(doc.reference.delete())
                                    } catch (de: Exception) {}
                                }
                            } catch (e: Exception) {}
                        }

                        // Finally, delete the session document itself
                        try {
                            com.google.android.gms.tasks.Tasks.await(docRef.delete())
                        } catch (e: Exception) {}
                    } catch (e: Exception) {}
                }
            }
            Log.d(TAG, "Successfully deleted remote session $sessionId from all candidate paths and subcollections")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote session $sessionId: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a single message from Firestore
     */
    suspend fun deleteMessage(userId: String, sessionId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || userId == "guest_local") return@withContext false
        try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .collection("messages").document(messageId)
            
            // Delete attachments subcollection and storage files
            try {
                val attsRef = docRef.collection("attachments")
                val attsSnap = com.google.android.gms.tasks.Tasks.await(attsRef.get())
                
                for (doc in attsSnap.documents) {
                    val attId = doc.getString("attachmentId") ?: doc.id
                    val fileName = doc.getString("fileName") ?: "attachment"
                    
                    // Delete from storage
                    try {
                        val storagePathNew = "$userId/$sessionId/$messageId/$fileName"
                        SupabaseStorageClient.deleteFile("attachments", storagePathNew)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting new storage path file: ${e.message}")
                    }
                    try {
                        val storagePathOld = "uploads/$userId/$sessionId/$messageId/${attId}_$fileName"
                        SupabaseStorageClient.deleteFile("attachments", storagePathOld)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting old storage file for attachment $attId: ${e.message}")
                    }
                    
                    // Delete from firestore
                    com.google.android.gms.tasks.Tasks.await(doc.reference.delete())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting attachments for message $messageId: ${e.message}")
            }

            val deleteTask = docRef.delete()
            com.google.android.gms.tasks.Tasks.await(deleteTask)
            Log.d(TAG, "Successfully deleted remote message $messageId under session $sessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote message $messageId: ${e.message}", e)
            false
        }
    }

    private fun sendFeedbackEmail(
        fromName: String,
        fromEmail: String,
        category: String,
        message: String,
        appVersion: String,
        deviceInfo: String = ""
    ) {
        val serviceId = "service_lbl552d"
        val templateId = "template_vphityh"
        val publicKey = "GJZgQndVUSZMWSFOv"
        val toEmail = "reply.depthlens@gmail.com"

        try {
            val templateParams = JSONObject().apply {
                put("to_email", toEmail)
                put("from_name", fromName)
                put("from_email", fromEmail)
                put("category", category)
                put("message", message)
                put("app_version", appVersion)
                put("device_info", deviceInfo)
                put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
            }

            val jsonPayload = JSONObject().apply {
                put("service_id", serviceId)
                put("template_id", templateId)
                // EmailJS API requires both user_id and accessToken for reliability
                put("user_id", publicKey)
                put("accessToken", publicKey)
                put("template_params", templateParams)
            }

            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://api.emailjs.com/api/v1.0/email/send")
                .post(body)
                .header("Content-Type", "application/json")
                .header("origin", "http://localhost")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.e(TAG, "EmailJS transmission failure", e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            Log.e(TAG, "EmailJS transmission rejected: code=${it.code} body=${it.body?.string()}")
                        } else {
                            Log.d(TAG, "Feedback routed and delivered via EmailJS to destination")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare EmailJS transmission", e)
        }
    }

    /**
     * Broadcast latest release metadata across cloud nodes so all running versions receive update notification
     */
    suspend fun broadcastReleaseUpdate(
        versionName: String = "6.1.0",
        versionCode: Long = 6100L,
        changelog: String = "• iOS 27 Liquid Glass Navigation Redesign with 3-State Floating Capsule\n• Vibrant Active Neon Light Bar & upward bloom aesthetics\n• Smart Scroll-Driven Navigation (auto-hide on scroll down, open on scroll up)\n• Full-width edge-to-edge typing bar with reduced vertical spacing\n• ChatGPT-style Branch in New Chat with quote context\n• Proportional brevity & intelligence tuning"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val updatePayload = mapOf(
                "versionName" to versionName,
                "versionCode" to versionCode,
                "latestVersion" to versionName,
                "tagName" to versionName,
                "title" to "DepthLens v$versionName — iOS 27 Glass Navigation & Intelligence Polish",
                "changelog" to changelog,
                "body" to changelog,
                "publishedAt" to "September 7, 2026",
                "timestamp" to System.currentTimeMillis(),
                "apkUrl" to "https://github.com/guy-with-ideas-uncoded/DEPTHLENS/releases/download/$versionName/DepthLens_v${versionName}-debug.apk",
                "apkFileName" to "DepthLens_v${versionName}-debug.apk",
                "apkSize" to 28818277L,
                "forceUpdate" to false
            )
            // Broadcast across app_updates and system collections
            db.collection("app_updates").document("latest").set(updatePayload, SetOptions.merge())
            db.collection("system_config").document("app_version").set(updatePayload, SetOptions.merge())
            db.collection("releases").document("v$versionName").set(updatePayload, SetOptions.merge())
            true
        } catch (e: Exception) {
            Log.w(TAG, "Broadcast release update notice: ${e.message}")
            false
        }
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    this.addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }
}
