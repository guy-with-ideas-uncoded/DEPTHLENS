package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    
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
     * Placeholder File Upload to Firebase Storage REST (unused in general code)
     */
    suspend fun uploadToFirebaseStorage(
        localFile: File,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        val firebaseApp = try { com.google.firebase.FirebaseApp.getInstance() } catch(e: Exception) { null }
        val projectId = firebaseApp?.options?.projectId ?: "depthlens-prod"
        val bucketName = firebaseApp?.options?.storageBucket?.takeIf { it.isNotBlank() } ?: "$projectId.appspot.com"
        val fileName = "uploads/${UUID.randomUUID()}_${localFile.name}"
        
        try {
            val url = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o?name=${java.net.URLEncoder.encode(fileName, "UTF-8")}"
            val requestBody = localFile.readBytes().toRequestBody(mimeType.toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(bodyStr)
                val downloadToken = responseJson.optString("downloadTokens", "")
                return@withContext "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/${java.net.URLEncoder.encode(fileName, "UTF-8")}?alt=media&token=$downloadToken"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Create user profile in Firestore if not exist
     */
    suspend fun createProfileIfNotExist(userId: String, email: String, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            Log.d("USER_FETCH", "Fetching user profile from Firestore: uid=$userId")
            Log.d("SYNC", "Firestore read start: user profile verification")
            val userRef = db.collection("users").document(userId)
            val docSnap = com.google.android.gms.tasks.Tasks.await(userRef.get())
            Log.d("FIRESTORE_READ", "Firestore read success: retrieved user profile status for uid=$userId")
            Log.d("SYNC", "Firestore read success")
            if (!docSnap.exists()) {
                val profile = mapOf(
                    "uid" to userId,
                    "email" to email,
                    "name" to name,
                    "createdAt" to System.currentTimeMillis()
                )
                Log.d("USER_FETCH", "Creating/writing new user profile on Firestore for uid=$userId")
                com.google.android.gms.tasks.Tasks.await(userRef.set(profile))
                Log.d("FIRESTORE_WRITE", "Firestore write success: created user record")
                Log.d("SYNC", "Firestore write success")
            } else {
                Log.d("USER_FETCH", "User profile already exist on Firestore for uid=$userId")
            }
            true
        } catch (e: Exception) {
            Log.e("SYNC", "Error in createProfileIfNotExist: ${e.message}", e)
            Log.e("USER_FETCH", "Profile creation/fetch error", e)
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
        updatedAt: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to sessionId,
                "title" to title,
                "isPinned" to isPinned,
                "createdAt" to createdAt,
                "lastUpdatedAt" to updatedAt
            )
            Log.d("CHAT_SAVE", "Uploading/saving session item to cloud: sessionId=$sessionId for userId=$userId")
            val task = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .set(data, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task)
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
            
            // Upload file to Firebase Storage if we don't have a remote URL yet
            if (finalRemoteUrl.isNullOrBlank() && attachment.localUri.isNotBlank()) {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val storageRef = storage.reference.child(storagePath)
                val uri = android.net.Uri.parse(attachment.localUri)
                
                try {
                    val uploadTask = if (uri.scheme == "content" && context != null) {
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
                        finalRemoteUrl = com.google.android.gms.tasks.Tasks.await(storageRef.downloadUrl).toString()
                        
                        // Update local DB if possible (we do it in repository instead if we don't have DB instance here, but let's try if context is provided)
                        if (context != null) {
                            val dbLocal = com.example.data.database.DepthDatabase.getDatabase(context!!)
                            dbLocal.attachmentDao().insertAttachment(attachment.copy(remoteUrl = finalRemoteUrl))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SYNC", "Error uploading file to Firebase Storage: ${e.message}")
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
            com.google.android.gms.tasks.Tasks.await(task)
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
        selectedText: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "id" to messageId,
                "sessionId" to sessionId,
                "role" to role,
                "text" to text,
                "imageUri" to (imageUri ?: ""),
                "timestamp" to timestamp,
                "replyToMessageId" to (replyToMessageId ?: ""),
                "selectedText" to (selectedText ?: "")
            )
            Log.d("CHAT_SAVE", "Uploading message details to cloud: messageId=$messageId in sessionId=$sessionId")
            val task = db.collection("users").document(userId)
                .collection("chats").document(sessionId)
                .collection("messages").document(messageId)
                .set(data, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task)
            Log.d("FIRESTORE_WRITE", "Firestore write success: message $messageId details uploaded")
            Log.d("SYNC", "Firestore write success")
            
            // Touch chat lastUpdatedAt
            try {
                val touchTask = db.collection("users").document(userId)
                    .collection("chats").document(sessionId)
                    .update("lastUpdatedAt", timestamp)
                com.google.android.gms.tasks.Tasks.await(touchTask)
                Log.d("FIRESTORE_WRITE", "Firestore write success: touched lastUpdatedAt for session $sessionId")
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
            
            // Try finding the session in all possible paths (legacy + current)
            val subcollectionsToQuery = listOf(
                db.collection("users").document(userId).collection("chats"),
                db.collection("users").document(userId).collection("sessions"),
                db.collection("users").document(userId).collection("history"),
                db.collection("users").document(userId).collection("chatHistory")
            )
            
            var targetDocSnap: com.google.firebase.firestore.DocumentSnapshot? = null
            
            for (colRef in subcollectionsToQuery) {
                try {
                    val docRef = colRef.document(sessionId)
                    val docSnap = com.google.android.gms.tasks.Tasks.await(docRef.get(), 10, java.util.concurrent.TimeUnit.SECONDS)
                    if (docSnap.exists()) {
                        targetDocSnap = docSnap
                        break
                    }
                } catch (e: Exception) {
                    // Ignore and try next
                }
            }
            
            val docSnap = targetDocSnap
            if (docSnap == null) {
                Log.e("SYNC", "syncSingleSession failed: Session $sessionId not found in any remote collection")
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
            sessionDao.insertSessionIgnore(sEntity)
            
            var foundMessages = false

            // A. Load inline messages list/history if stored as a list inside the session document itself (Legacy fallback)
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
                                    if (item.containsKey("isUser")) {
                                        val isUser = item["isUser"] as? Boolean ?: true
                                        role = if (isUser) "user" else "model"
                                    } else if (item.containsKey("is_user")) {
                                        val isUser = item["is_user"] as? Boolean ?: true
                                        role = if (isUser) "user" else "model"
                                    } else if (item.containsKey("isModel")) {
                                        val isModel = item["isModel"] as? Boolean ?: false
                                        role = if (isModel) "model" else "user"
                                    } else if (item.containsKey("is_model")) {
                                        val isModel = item["is_model"] as? Boolean ?: false
                                        role = if (isModel) "model" else "user"
                                    }
                                }
                                var finalRole = "user"
                                if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) {
                                    finalRole = "model"
                                } else if (role.lowercase() in listOf("user", "human", "me")) {
                                    finalRole = "user"
                                }
                                
                                val text = (item["text"]
                                    ?: item["content"]
                                    ?: item["message"]
                                    ?: item["body"]
                                    ?: item["msg"]
                                    ?: item["prompt"]
                                    ?: item["response"]
                                    ?: item["input"]
                                    ?: item["output"]
                                    ?: "").toString()
                                val imageUri = (item["imageUri"] ?: item["imageUrl"] ?: item["image_uri"] ?: item["image_url"] ?: "").toString()
                                
                                val tVal = item["timestamp"] ?: item["time"] ?: item["createdAt"] ?: item["created_at"]
                                val timestamp = when (tVal) {
                                    is Number -> tVal.toLong()
                                    is String -> tVal.toLongOrNull() ?: System.currentTimeMillis()
                                    is com.google.firebase.Timestamp -> tVal.toDate().time
                                    is java.util.Date -> tVal.time
                                    is Map<*, *> -> {
                                        val sec = tVal["seconds"] ?: tVal["_seconds"]
                                        if (sec is Number) sec.toLong() * 1000L else System.currentTimeMillis()
                                    }
                                    else -> System.currentTimeMillis()
                                }
                                
                                val replyToMessageId = (item["replyToMessageId"] ?: item["replyTo"] ?: "").toString().takeIf { it.isNotBlank() }
                                val selectedText = (item["selectedText"] ?: "").toString().takeIf { it.isNotBlank() }
                                
                                val mEntity = com.example.data.model.MessageEntity(
                                    id = msgId,
                                    sessionId = sessionId,
                                    role = finalRole,
                                    text = text,
                                    imageUri = if (imageUri.isEmpty()) null else imageUri,
                                    timestamp = timestamp,
                                    replyToMessageId = replyToMessageId,
                                    selectedText = selectedText
                                )
                                messageDao.insertMessage(mEntity)
                                foundMessages = true
                            }
                        } catch (me: Exception) {
                            Log.e("SYNC", "Error parsing inline message in session $sessionId: ${me.message}", me)
                        }
                    }
                }
            } catch (ae: Exception) {
                Log.e("SYNC", "Failed to check or process inline messages array for session $sessionId: ${ae.message}")
            }

            // B. Fetch remote messages for this session from subcollections
            val nestedSubcollections = listOf("messages", "chats", "history", "chatHistory")
            for (subColName in nestedSubcollections) {
                try {
                    val msgsSnapTask = docRef.collection(subColName).get()
                    val msgsSnap = com.google.android.gms.tasks.Tasks.await(msgsSnapTask, 10, java.util.concurrent.TimeUnit.SECONDS)
                    if (!msgsSnap.isEmpty) {
                        foundMessages = true
                        for (msgDoc in msgsSnap.documents) {
                            try {
                                val msgId = msgDoc.id
                                var role = getStringSafely(msgDoc, "role", "")
                                if (role.isBlank()) {
                                    if (msgDoc.contains("isUser")) {
                                        role = if (getBooleanSafely(msgDoc, "isUser", true)) "user" else "model"
                                    } else {
                                        role = "user"
                                    }
                                }
                                var finalRole = "user"
                                if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) {
                                    finalRole = "model"
                                } else if (role.lowercase() in listOf("user", "human", "me")) {
                                    finalRole = "user"
                                }
                                
                                val text = getStringSafely(msgDoc, "text", "")
                                val imageUri = getStringSafely(msgDoc, "imageUri", "")
                                val timestamp = getLongSafely(msgDoc, "timestamp", System.currentTimeMillis())
                                val replyToMessageId = getStringSafely(msgDoc, "replyToMessageId", "").takeIf { it.isNotBlank() }
                                val selectedText = getStringSafely(msgDoc, "selectedText", "").takeIf { it.isNotBlank() }
                                
                                val mEntity = com.example.data.model.MessageEntity(
                                    id = msgId,
                                    sessionId = sessionId,
                                    role = finalRole,
                                    text = text,
                                    imageUri = if (imageUri.isEmpty()) null else imageUri,
                                    timestamp = timestamp,
                                    replyToMessageId = replyToMessageId,
                                    selectedText = selectedText
                                )
                                messageDao.insertMessage(mEntity)
                                
                                try {
                                    val attsSnap = docRef.collection(subColName).document(msgId).collection("attachments").get()
                                    val attsResult = com.google.android.gms.tasks.Tasks.await(attsSnap, 5, java.util.concurrent.TimeUnit.SECONDS)
                                    for (attDoc in attsResult.documents) {
                                        val attachment = com.example.data.model.AttachmentEntity(
                                            attachmentId = getStringSafely(attDoc, "attachmentId", attDoc.id),
                                            messageId = getStringSafely(attDoc, "messageId", msgId),
                                            mimeType = getStringSafely(attDoc, "mimeType", "application/octet-stream"),
                                            localUri = getStringSafely(attDoc, "localUri", ""),
                                            remoteUrl = getStringSafely(attDoc, "remoteUrl", "").takeIf { it.isNotBlank() } ?: getStringSafely(attDoc, "downloadUrl", "").takeIf { it.isNotBlank() },
                                            thumbnailUrl = getStringSafely(attDoc, "thumbnailUrl", "").takeIf { it.isNotBlank() },
                                            fileName = getStringSafely(attDoc, "fileName", "attachment")
                                        )
                                        attachmentDao.insertAttachment(attachment)
                                    }
                                } catch (e: Exception) {
                                    Log.e("SYNC", "Failed to sync attachments for message $msgId: ${e.message}")
                                }
                            } catch (e: Exception) {
                                Log.e("SYNC", "Error parsing message $msgDoc: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SYNC", "Error fetching subcollection $subColName: ${e.message}")
                }
            }
            // Always return true if docSnap exists, because a session might legally have 0 messages (newly created)
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
        attachmentDao: com.example.data.database.AttachmentDao
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            Log.i("SYNC_UID_VERIFICATION", "Verifying UID passed to Firebase query: '$userId' (Length: ${userId.length})")
            if (userId.isBlank() || userId == "guest_local") {
                Log.w("SYNC_UID_VERIFICATION", "Aborted cloud synchronization: UID is empty or belongs to local guest")
                return@withContext false
            }

            Log.d("SYNC_STATUS", "Starting CloudSync fetchAndSyncAll for user: $userId")
            Log.d("SYNC", "Firestore read start: getting all remote chats for uid=$userId")

            // 0. Fetch tombstones for deleted sessions to sync deletions across devices
            val deletedSessionIds = mutableSetOf<String>()
            try {
                val deletedSnapTask = db.collection("users").document(userId)
                    .collection("deleted_chats").get()
                val deletedSnap = com.google.android.gms.tasks.Tasks.await(deletedSnapTask, 8, java.util.concurrent.TimeUnit.SECONDS)
                Log.i("SYNC_DELETE", "Found ${deletedSnap.size()} deleted session tombstones")
                for (doc in deletedSnap.documents) {
                    deletedSessionIds.add(doc.id)
                }
            } catch (e: Exception) {
                Log.e("SYNC_DELETE", "Failed fetching deleted session tombstones: ${e.message}")
            }

            // Immediately purge any locally cached deleted sessions to sync deletion
            for (delSessionId in deletedSessionIds) {
                try {
                    Log.d("SYNC_DELETE", "Purging locally deleted session: $delSessionId")
                    attachmentDao.deleteAttachmentsForSession(delSessionId)
                    messageDao.deleteMessagesForSession(delSessionId)
                    sessionDao.deleteSessionById(delSessionId)
                } catch (pe: Exception) {
                    Log.e("SYNC_DELETE", "Failed purging locally deleted session $delSessionId: ${pe.message}")
                }
            }
            
            // 1. Fetch user's chats from remote Firestore (from multiple potential legacy and current collections)
            val allDocuments = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
            val processedDocIds = mutableSetOf<String>()

            // List of potential user subcollection paths where chats/sessions could be stored across app versions
            val subcollectionsToQuery = listOf(
                db.collection("users").document(userId).collection("chats")
            )

            // List of potential root-level queries with filters (in case chats/sessions were stored at root in old versions)
            val rootQueriesToQuery = emptyList<com.google.firebase.firestore.Query>()

            // Query subcollections with safety timeouts
            for (colRef in subcollectionsToQuery) {
                try {
                    Log.d("SYNC_QUERY", "Attempting subcollection query on path: '${colRef.path}' for user '$userId'")
                    val task = colRef.get()
                    val snap = com.google.android.gms.tasks.Tasks.await(task, 8, java.util.concurrent.TimeUnit.SECONDS)
                    Log.i("FIRESTORE_READ", "Queried path '${colRef.path}': found ${snap.size()} documents")
                    for (doc in snap.documents) {
                        if (deletedSessionIds.contains(doc.id)) {
                            Log.d("SYNC_DELETE", "Skipping document ${doc.id} since it was deleted.")
                            continue
                        }
                        if (processedDocIds.add(doc.id)) {
                            allDocuments.add(doc)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FIRESTORE_READ", "Failed or bypassed subcollection search on path '${colRef.path}': ${e.message}", e)
                }
            }

            // Query root collections with user-id-field filters
            for (query in rootQueriesToQuery) {
                try {
                    Log.d("SYNC_QUERY", "Attempting filtered root query for user '$userId'")
                    val task = query.get()
                    val snap = com.google.android.gms.tasks.Tasks.await(task, 8, java.util.concurrent.TimeUnit.SECONDS)
                    Log.i("FIRESTORE_READ", "Queried filtered root collection query: found ${snap.size()} documents")
                    for (doc in snap.documents) {
                        if (processedDocIds.add(doc.id)) {
                            allDocuments.add(doc)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FIRESTORE_READ", "Failed or bypassed filtered root execution: ${e.message}", e)
                }
            }

            Log.i("SYNC_RESULT", "Aggregate scan complete: found ${allDocuments.size} unique session documents across legacy/modern collections")
            Log.d("SYNC", "Firestore read success")
            
            val remoteSessionsMap = allDocuments.associateBy { it.id }
            
            // 2. Sync from Remote to Local
            val initialLocalSessions = sessionDao.getAllSessions()
            for (doc in allDocuments) {
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
                    
                    val sEntity = com.example.data.model.SessionEntity(
                        id = sessionId,
                        title = title,
                        isPinned = isPinned,
                        createdAt = createdAt,
                        lastUpdatedAt = lastUpdatedAt
                    )
                    Log.d("CHAT_LOAD", "Writing session descriptor to Room: $sessionId - $title")
                    sessionDao.insertSession(sEntity)
                    
                    // A. Load inline messages list/history if stored as a list inside the session document itself (Legacy fallback)
                    try {
                        val inlineMessages = doc.get("messages") ?: doc.get("history") ?: doc.get("chats")
                        if (inlineMessages is List<*>) {
                            Log.d("CHAT_LOAD", "Found inline messages list in session document $sessionId of size ${inlineMessages.size}")
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
                                            if (item.containsKey("isUser")) {
                                                val isUser = item["isUser"] as? Boolean ?: true
                                                role = if (isUser) "user" else "model"
                                            } else if (item.containsKey("is_user")) {
                                                val isUser = item["is_user"] as? Boolean ?: true
                                                role = if (isUser) "user" else "model"
                                            } else if (item.containsKey("isModel")) {
                                                val isModel = item["isModel"] as? Boolean ?: false
                                                role = if (isModel) "model" else "user"
                                            } else if (item.containsKey("is_model")) {
                                                val isModel = item["is_model"] as? Boolean ?: false
                                                role = if (isModel) "model" else "user"
                                            } else if (item.containsKey("isBot")) {
                                                val isBot = item["isBot"] as? Boolean ?: false
                                                role = if (isBot) "model" else "user"
                                            } else if (item.containsKey("is_bot")) {
                                                val isBot = item["is_bot"] as? Boolean ?: false
                                                role = if (isBot) "model" else "user"
                                            } else {
                                                role = "user"
                                            }
                                        }
                                        // Standardize role
                                        var finalRole = "user"
                                        if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) {
                                            finalRole = "model"
                                        } else if (role.lowercase() in listOf("user", "human", "me")) {
                                            finalRole = "user"
                                        }
                                        
                                        val text = (item["text"]
                                            ?: item["content"]
                                            ?: item["message"]
                                            ?: item["body"]
                                            ?: item["msg"]
                                            ?: item["prompt"]
                                            ?: item["response"]
                                            ?: item["input"]
                                            ?: item["output"]
                                            ?: "").toString()
                                        val imageUri = (item["imageUri"] ?: item["imageUrl"] ?: item["image_uri"] ?: item["image_url"] ?: "").toString()
                                        
                                        val tVal = item["timestamp"] ?: item["time"] ?: item["createdAt"] ?: item["created_at"]
                                        val timestamp = when (tVal) {
                                            is Number -> tVal.toLong()
                                            is String -> tVal.toLongOrNull() ?: System.currentTimeMillis()
                                            is com.google.firebase.Timestamp -> tVal.toDate().time
                                            is java.util.Date -> tVal.time
                                            is Map<*, *> -> {
                                                val sec = tVal["seconds"] ?: tVal["_seconds"]
                                                if (sec is Number) sec.toLong() * 1000L else System.currentTimeMillis()
                                            }
                                            else -> System.currentTimeMillis()
                                        }
                                        
                                        val replyToMessageId = (item["replyToMessageId"] ?: item["reply_to_message_id"] ?: "").toString().takeIf { it.isNotEmpty() }
                                        val selectedText = (item["selectedText"] ?: item["selected_text"] ?: "").toString().takeIf { it.isNotEmpty() }
                                        val mEntity = com.example.data.model.MessageEntity(
                                            id = msgId,
                                            sessionId = sessionId,
                                            role = finalRole,
                                            text = text,
                                            imageUri = if (imageUri.isEmpty()) null else imageUri,
                                            timestamp = timestamp,
                                            replyToMessageId = replyToMessageId,
                                            selectedText = selectedText
                                        )
                                        messageDao.insertMessage(mEntity)
                                    }
                                } catch (me: Exception) {
                                    Log.e("SYNC", "Error parsing inline message in session $sessionId: ${me.message}", me)
                                }
                            }
                        }
                    } catch (ae: Exception) {
                        Log.e("SYNC", "Failed to check or process inline messages array for session $sessionId: ${ae.message}")
                    }
                    
                    // B. Fetch remote messages for this session from multiple possible subcol names with strict timeouts
                    val nestedSubcollections = listOf("messages", "chats", "history", "chatHistory")
                    for (subColName in nestedSubcollections) {
                        try {
                            val subRef = doc.reference.collection(subColName)
                            Log.d("CHAT_LOAD", "Scanning subcollection '${subRef.path}' for historical messages")
                            val msgsSnapTask = subRef.get()
                            val msgsSnap = com.google.android.gms.tasks.Tasks.await(msgsSnapTask, 8, java.util.concurrent.TimeUnit.SECONDS)
                            if (msgsSnap.isEmpty) {
                                continue
                            }
                            Log.i("FIRESTORE_READ", "Found ${msgsSnap.size()} historical messages under nested subcollection '${subRef.path}'")
                            
                            for (msgDoc in msgsSnap.documents) {
                                try {
                                    val msgId = msgDoc.id
                                    
                                    var role = getStringSafely(msgDoc, "role", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "sender", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "author", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: ""
                                    if (role.isBlank()) {
                                        if (msgDoc.contains("isUser")) {
                                            val isUser = getBooleanSafely(msgDoc, "isUser", true)
                                            role = if (isUser) "user" else "model"
                                        } else if (msgDoc.contains("is_user")) {
                                            val isUser = getBooleanSafely(msgDoc, "is_user", true)
                                            role = if (isUser) "user" else "model"
                                        } else if (msgDoc.contains("isModel")) {
                                            val isModel = getBooleanSafely(msgDoc, "isModel", false)
                                            role = if (isModel) "model" else "user"
                                        } else if (msgDoc.contains("is_model")) {
                                            val isModel = getBooleanSafely(msgDoc, "is_model", false)
                                            role = if (isModel) "model" else "user"
                                        } else if (msgDoc.contains("isBot")) {
                                            val isBot = getBooleanSafely(msgDoc, "isBot", false)
                                            role = if (isBot) "model" else "user"
                                        } else if (msgDoc.contains("is_bot")) {
                                            val isBot = getBooleanSafely(msgDoc, "is_bot", false)
                                            role = if (isBot) "model" else "user"
                                        } else {
                                            role = "user"
                                        }
                                    }
                                    // Standardize role
                                    var finalRole = "user"
                                    if (role.lowercase() in listOf("bot", "ai", "model", "assistant", "system")) {
                                        finalRole = "model"
                                    } else if (role.lowercase() in listOf("user", "human", "me")) {
                                        finalRole = "user"
                                    }
                                    
                                    val text = getStringSafely(msgDoc, "text", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "content", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "message", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "body", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "msg", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "prompt", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "response", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "input", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "output", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: ""
                                    
                                    val imageUri = getStringSafely(msgDoc, "imageUri", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "imageUrl", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "image_uri", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: getStringSafely(msgDoc, "image_url", "")
                                        .takeIf { it.isNotBlank() }
                                        ?: ""
                                    
                                    val timestamp = getLongSafely(msgDoc, "timestamp", 0L)
                                        .takeIf { it > 0 }
                                        ?: getLongSafely(msgDoc, "time", 0L)
                                        .takeIf { it > 0 }
                                        ?: getLongSafely(msgDoc, "createdAt", 0L)
                                        .takeIf { it > 0 }
                                        ?: getLongSafely(msgDoc, "created_at", 0L)
                                        .takeIf { it > 0 }
                                        ?: System.currentTimeMillis()
                                    
                                    val replyToMessageId = (msgDoc.getString("replyToMessageId") ?: msgDoc.getString("reply_to_message_id") ?: "").takeIf { it.isNotEmpty() }
                                    val selectedText = (msgDoc.getString("selectedText") ?: msgDoc.getString("selected_text") ?: "").takeIf { it.isNotEmpty() }
                                    val mEntity = com.example.data.model.MessageEntity(
                                        id = msgId,
                                        sessionId = sessionId,
                                        role = finalRole,
                                        text = text,
                                        imageUri = if (imageUri.isEmpty()) null else imageUri,
                                        timestamp = timestamp,
                                        replyToMessageId = replyToMessageId,
                                        selectedText = selectedText
                                    )
                                    messageDao.insertMessage(mEntity)
                                    
                                    try {
                                        val attsSnap = db.collection("users").document(userId)
                                            .collection("chats").document(sessionId)
                                            .collection("messages").document(msgId)
                                            .collection("attachments").get()
                                        val attsResult = com.google.android.gms.tasks.Tasks.await(attsSnap)
                                        for (attDoc in attsResult.documents) {
                                            val attachment = com.example.data.model.AttachmentEntity(
                                                attachmentId = getStringSafely(attDoc, "attachmentId", attDoc.id),
                                                messageId = getStringSafely(attDoc, "messageId", msgId),
                                                mimeType = getStringSafely(attDoc, "mimeType", "application/octet-stream"),
                                                localUri = getStringSafely(attDoc, "localUri", ""),
                                                remoteUrl = getStringSafely(attDoc, "remoteUrl", "").takeIf { it.isNotBlank() } ?: getStringSafely(attDoc, "downloadUrl", "").takeIf { it.isNotBlank() },
                                                thumbnailUrl = getStringSafely(attDoc, "thumbnailUrl", "").takeIf { it.isNotBlank() },
                                                fileName = getStringSafely(attDoc, "fileName", "attachment")
                                            )
                                            attachmentDao.insertAttachment(attachment)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SYNC", "Failed to sync attachments for message $msgId: ${e.message}")
                                    }
                                } catch (me: Exception) {
                                    Log.e("SYNC", "Error parsing/inserting remote message ${msgDoc.id} under session $sessionId: ${me.message}", me)
                                }
                            }

                            // Delete local messages for this session that are missing in remote
                            val remoteMsgIds = msgsSnap.documents.map { it.id }.toSet()
                            val localMsgs = messageDao.getMessagesForSession(sessionId)
                            
                            // Check if remote is newer or equal to local. If local is newer, we have unsynced local messages!
                            val localSessionObj = initialLocalSessions.find { it.id == sessionId }
                            val remoteUpdatedAt = getLongSafely(doc, "lastUpdatedAt", 0L)
                                .takeIf { it > 0 } ?: getLongSafely(doc, "updatedAt", 0L)
                                
                            val isRemoteNewerOrEqual = localSessionObj == null || remoteUpdatedAt >= localSessionObj.lastUpdatedAt
                            
                            for (localMsg in localMsgs) {
                                if (!remoteMsgIds.contains(localMsg.id)) {
                                    // Only delete if we are SURE remote is newer, AND it's not a brand new local message
                                    val age = System.currentTimeMillis() - localMsg.timestamp
                                    if (isRemoteNewerOrEqual && age > 60000L) {
                                        Log.d("SYNC_DELETE", "Purging deleted local message: ${localMsg.id} (remote is newer)")
                                        attachmentDao.deleteAttachmentsForMessage(localMsg.id)
                                        messageDao.deleteMessage(localMsg.id)
                                    } else {
                                        Log.d("SYNC_KEEP", "Keeping unsynced local message: ${localMsg.id} (local is newer or message is recent)")
                                    }
                                }
                            }
                        } catch (se: Exception) {
                            Log.e("SYNC", "Failed fetching nested message subcollection '$subColName' for session $sessionId: ${se.message}")
                        }
                    }
                    
                    Log.d("CHAT_LOAD", "Finished restoring session $sessionId locally")
                } catch (se: Exception) {
                    Log.e("SYNC", "Error processing remote session descriptor: ${se.message}", se)
                }
            }
            
            // 3. Sync from Local to Remote (Bidirectional push for unsynced or newer local sessions)
            val currentLocalSessions = sessionDao.getAllSessions()
            for (localSession in currentLocalSessions) {
                if (deletedSessionIds.contains(localSession.id)) {
                    Log.d("SYNC_DELETE", "Skipping upload of deleted session: ${localSession.id}")
                    continue
                }
                try {
                    val localMessages = messageDao.getMessagesForSession(localSession.id)
                    if (localMessages.isEmpty()) {
                        continue
                    }
                    
                    val remoteDoc = remoteSessionsMap[localSession.id]
                    val remoteUpdatedAt = getLongSafely(remoteDoc, "lastUpdatedAt", 0L)
                    
                    if (remoteDoc == null || localSession.lastUpdatedAt > remoteUpdatedAt) {
                        val sessionData = mapOf(
                            "id" to localSession.id,
                            "title" to localSession.title,
                            "isPinned" to localSession.isPinned,
                            "createdAt" to localSession.createdAt,
                            "lastUpdatedAt" to localSession.lastUpdatedAt
                        )
                        
                        Log.d("CHAT_SAVE", "Local session ${localSession.id} is newer or unsynced. Merging to cloud...")
                        Log.d("SYNC", "Firestore write start: uploadSession meta for sessionId=${localSession.id}")
                        val uploadSessionTask = db.collection("users").document(userId)
                            .collection("chats").document(localSession.id)
                            .set(sessionData, SetOptions.merge())
                        com.google.android.gms.tasks.Tasks.await(uploadSessionTask)
                        Log.d("FIRESTORE_WRITE", "Firestore write success: synced session descriptor ${localSession.id}")
                        Log.d("SYNC", "Firestore write success")
                        
                        for (localMsg in localMessages) {
                            try {
                                val msgData = mapOf(
                                    "id" to localMsg.id,
                                    "sessionId" to localMsg.sessionId,
                                    "role" to localMsg.role,
                                    "text" to localMsg.text,
                                    "imageUri" to (localMsg.imageUri ?: ""),
                                    "timestamp" to localMsg.timestamp,
                                    "replyToMessageId" to (localMsg.replyToMessageId ?: ""),
                                    "selectedText" to (localMsg.selectedText ?: "")
                                )
                                Log.d("CHAT_SAVE", "Uploading local message ${localMsg.id} for session ${localSession.id} to cloud...")
                                Log.d("SYNC", "Firestore write start: uploadMessage for messageId=${localMsg.id}")
                                val uploadMsgTask = db.collection("users").document(userId)
                                    .collection("chats").document(localSession.id)
                                    .collection("messages").document(localMsg.id)
                                    .set(msgData, SetOptions.merge())
                                com.google.android.gms.tasks.Tasks.await(uploadMsgTask)
                                
                                val localAtts = attachmentDao.getAttachmentsForMessage(localMsg.id)
                                for (att in localAtts) {
                                    uploadAttachment(userId, localSession.id, localMsg.id, att)
                                }
                                
                                Log.d("FIRESTORE_WRITE", "Firestore write success: synced message ${localMsg.id}")
                                Log.d("SYNC", "Firestore write success")
                            } catch (me: Exception) {
                                Log.e("SYNC", "Error uploading message ${localMsg.id} for session ${localSession.id} to cloud: ${me.message}", me)
                            }
                        }
                    }
                } catch (le: Exception) {
                    Log.e("SYNC", "Error pushing local session ${localSession.id} to cloud: ${le.message}", le)
                }
            }
            Log.d("SYNC_STATUS", "fetchAndSyncAll completed successfully for uid=$userId")
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
        if (userId.isBlank() || userId == "guest_local") return@withContext false
        try {
            val db = FirebaseFirestore.getInstance()

            // Register in the deleted tombstones collection for multi-device sync
            try {
                val deletedDocRef = db.collection("users").document(userId)
                    .collection("deleted_chats").document(sessionId)
                val writeDeletedTask = deletedDocRef.set(mapOf("deletedAt" to System.currentTimeMillis()))
                com.google.android.gms.tasks.Tasks.await(writeDeletedTask)
                Log.d(TAG, "Registered deleted session $sessionId in tombstones")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register deleted session $sessionId tombstone: ${e.message}")
            }

            val docRef = db.collection("users").document(userId)
                .collection("chats").document(sessionId)

            // Delete nested subcollections (e.g. messages) first
            try {
                val messagesRef = docRef.collection("messages")
                val msgsSnap = com.google.android.gms.tasks.Tasks.await(messagesRef.get())
                for (doc in msgsSnap.documents) {
                    deleteMessage(userId, sessionId, doc.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting messages for session $sessionId: ${e.message}")
            }
            
            val nestedSubcollections = listOf("chats", "history", "chatHistory")
            for (subColName in nestedSubcollections) {
                try {
                    val subRef = docRef.collection(subColName)
                    val snapTask = subRef.get()
                    val snap = com.google.android.gms.tasks.Tasks.await(snapTask)
                    for (doc in snap.documents) {
                        try {
                            val deleteTask = doc.reference.delete()
                            com.google.android.gms.tasks.Tasks.await(deleteTask)
                        } catch (de: Exception) {
                            Log.e(TAG, "Error deleting remote document ${doc.id} under subcollection $subColName: ${de.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed deleting remote subcollection $subColName for session $sessionId: ${e.message}")
                }
            }

            // Finally, delete the session document itself
            val deleteDocTask = docRef.delete()
            com.google.android.gms.tasks.Tasks.await(deleteDocTask)
            Log.d(TAG, "Successfully deleted remote session $sessionId and all its subcollections")
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
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                
                for (doc in attsSnap.documents) {
                    val attId = doc.getString("attachmentId") ?: doc.id
                    val fileName = doc.getString("fileName") ?: "attachment"
                    
                    // Delete from storage
                    try {
                        val storageRefNew = storage.reference.child("$userId/$sessionId/$messageId/$fileName")
                        com.google.android.gms.tasks.Tasks.await(storageRefNew.delete())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting new storage path file: ${e.message}")
                    }
                    try {
                        val storageRefOld = storage.reference.child("uploads/$userId/$sessionId/$messageId/${attId}_$fileName")
                        com.google.android.gms.tasks.Tasks.await(storageRefOld.delete())
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
}
