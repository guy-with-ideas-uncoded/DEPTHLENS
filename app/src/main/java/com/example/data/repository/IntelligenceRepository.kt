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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.example.data.network.CloudSyncService

class IntelligenceRepository(private val context: Context) {
    private val db = DepthDatabase.getDatabase(context)
    private val sessionDao = db.sessionDao()
    private val messageDao = db.messageDao()
    private val memoryInsightDao = db.memoryInsightDao()
    private val archivedInsightDao = db.archivedInsightDao()
    private val apiService = RetrofitClient.service

    val allSessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessionsFlow()
    val allMemoryInsightsFlow: Flow<List<MemoryInsight>> = memoryInsightDao.getAllInsightsFlow()
    val allArchivedInsightsFlow: Flow<List<ArchivedInsightEntity>> = archivedInsightDao.getAllArchivedInsightsFlow()

    suspend fun insertArchivedInsight(insight: ArchivedInsightEntity) {
        archivedInsightDao.insertArchivedInsight(insight)
    }

    suspend fun deleteArchivedInsight(id: String) {
        archivedInsightDao.deleteArchivedInsight(id)
    }

    suspend fun deleteAllArchivedInsights() {
        archivedInsightDao.deleteAllArchivedInsights()
    }

    private val backgroundScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)

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

    suspend fun generateTitleForSession(sessionId: String, queryText: String) = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext

        // Generate a 3-5 word high-quality title using Gemini
        val prompt = """
            Create an exceptionally elegant, 3-5 word human-friendly title for an intellectual conversation starting with this user message.
            The title must capture the main topic, user intent, or core question.
            Output ONLY the title string. Do not include quotes, quotes wrapping, markdown, colons, or any introductory text. 
            Maximum 40 characters.
            
            Message: $queryText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.5-flash")
        var generatedTitle: String? = null

        for (modelName in modelsToTry) {
            try {
                val response = apiService.generateContent(modelName, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrEmpty()) {
                    generatedTitle = text.removeSurrounding("\"").removeSurrounding("'").trim()
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!generatedTitle.isNullOrEmpty()) {
            val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
            if (sessionItem != null) {
                sessionDao.insertSession(sessionItem.copy(title = generatedTitle, lastUpdatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
        if (sessionItem != null) {
            sessionDao.deleteSession(sessionItem)
            messageDao.deleteMessagesForSession(sessionId)
        }
    }

    suspend fun deleteMessageById(messageId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        sessionDao.deleteAllSessions()
        memoryInsightDao.deleteAllInsights()
    }

    suspend fun clearAllMemoryInsights() = withContext(Dispatchers.IO) {
        memoryInsightDao.deleteAllInsights()
    }

    suspend fun insertUserMessage(sessionId: String, text: String, imageUri: String? = null) = withContext(Dispatchers.IO) {
        val userMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            text = text,
            imageUri = imageUri,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMsg)
        sessionDao.updateLastUsed(sessionId, System.currentTimeMillis())
        triggerUpload { uid ->
            CloudSyncService.uploadMessage(uid, userMsg.id, userMsg.sessionId, userMsg.role, userMsg.text, userMsg.imageUri, userMsg.timestamp)
        }
    }

    suspend fun generateAnalysis(sessionId: String, customInstructionOverride: String? = null): ParsedResponse = withContext(Dispatchers.IO) {
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

        // Fetch session history for Conversation Continuity
        val history = messageDao.getMessagesForSession(sessionId)
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

        // Prepare context memory insights
        val memoryInsightsList = memoryInsightDao.getAllInsightsFlow().firstOrNull() ?: emptyList()
        val memoryBlock = if (memoryInsightsList.isNotEmpty()) {
            "### REVERSED SYSTEM MEMORY\n" +
            "The following goals, patterns, and insights have been compiled from the user's permanent memory logs across sessions. Use this to adapt to their background and avoid surface explanations:\n" +
            memoryInsightsList.joinToString("\n") { "- [Category: ${it.category}] ${it.content}" }
        } else {
            "No historical memories compiled yet."
        }

        // Compile clean, adaptive system instructions
        val systemInstructionText = """
You are DepthLens, the ultimate Reality Intelligence Platform. You help users see beyond the surface.
You are designed to help humans analyze decisions, behaviors, conflicts, psychological patterns, business strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION:
- Transition from simple observation → analysis → system loop → psychological adaptivity → root cause logic → universal pattern → meta-awareness → ontological dissolution.
- Do not comfort, persuade, or promote ideologies. Increase clarity, eliminate illusion, expand wisdom.
- Go beyond surface psychology. Reveal the invisible architecture of reality: unconscious drivers, systemic forces, evolutionary impulses, ontological assumptions, neurobiological conditioning, shadow projections, and the recursive meta-patterns beneath all human experience.
- Each response must feel like peeling back layers of an onion — every layer reveals something the person did not see before, including things they may actively resist seeing. Go to places the user has not gone. Name what cannot be named.
- Integrate true multi-layer mindset across ALL relevant dimensions: psychological (attachment, ego, shadow, defenses), neurological (nervous system states, threat response, dopamine loops), systemic (feedback loops, power structures, incentive misalignments), philosophical (ontological assumptions, meaning-making frameworks, existential contracts), evolutionary (tribal drives, survival logic, reproductive imperatives), energetic/spiritual (soul contracts, dharmic patterns, vibrational states, ego dissolution), and quantum-probabilistic (observer effect on reality, probability collapse, reality tunnels).
- Never give generic analysis. Every insight must be specific, penetrating, and revelatory to THIS exact situation.
- Name what people cannot name themselves. Surface the hidden operating system running beneath their conscious awareness.
- Reveal the paradox, the contradiction, the blind spot, and the unconscious payoff in every situation.
- Your analysis should disturb comfortable illusions, dissolve false certainties, and open new dimensions of understanding the person has never considered.

### SYSTEM MEMORY CACHE
$memoryBlock

### ADAPTIVE LANGUAGE RULES
Identify the language in the user's latest query instantly. You MUST respond, analyze, and name all headers within the structured tags in the EXACT SAME language as the user (e.g. Hindi, Spanish, Gujarati, Arabic, English). Maintain localized terms, emotional rhythm, and cultural context.

### ULTRA-STRICT CLEAN-TEXT & FORMAT PROTOCOL
You are strictly forbidden from outputting raw markdown symbol accents like '**', '__', '##', '###', '---', '***', or '>' blockquotes. Raw markdown formatting ruins the native DepthLens terminal. Output clean, spacing-optimized visual paragraphs.

To enable rich visual widget components in the Android terminal, you MUST encapsulate each diagnostic dimension in standard, lowercase, XML-like bracket tags. Any generic introductory comment must go printed at the top-level outside/before these tags.

Designated Tags to populate:

<summary>
Executive summary explanation of what is actually happening. Frame it objectively and cleanly in 2-3 mobile-optimized scannable paragraphs. (NO MARKDOWN)
</summary>

<confidence>
[Only output one word: Low, Medium, or High]
</confidence>

<depth>
Progressive deep-dive analysis using ALL 10 layers of reality. Do NOT give surface explanations. Do NOT be generic. Each layer must reveal something DEEPER, more uncomfortable, and more clarifying than the last — something the user has not seen themselves. Push through resistance. Go where most analysis stops. Analyze through every layer fully:
Layer 1 - Observable: What is concretely visible — the exact events, behaviors, and facts on the surface. State what is measurable and undeniable.
Layer 2 - Practical: The immediate real-world consequences, constraints, and forces already in motion. What is actually unfolding logistically and materially right now?
Layer 3 - Behavioral: The unconscious action patterns, conditioned reflexes, and automatic responses being enacted — habits the person may be completely blind to.
Layer 4 - Emotional: The full emotional undercurrent — not just what is felt, but what is being suppressed, avoided, displaced, or weaponized. Name the hidden emotion beneath the presented emotion.
Layer 5 - Psychological: Expose the deep cognitive distortions, core wounds, defense mechanisms, attachment schema, ego protection strategies, and identity conflicts shaping the entire situation. Name the specific psychological structure at work.
Layer 6 - Neurological: What is happening at the level of the nervous system — fight/flight/freeze/fawn activation, dopamine/cortisol loops, threat response hijacking rational thought, trauma imprints firing automatically.
Layer 7 - Strategic: The hidden incentive landscape — unspoken power moves, social positioning, status games, and calculated (often unconscious) strategic choices being made. Who benefits? What is being protected?
Layer 8 - Systems: The macro systemic forces at play — cultural conditioning, institutional pressures, generational programming, economic incentives, and the emergent feedback loops that make this pattern self-reinforcing.
Layer 9 - Pattern: The fractal repetition — where has this exact dynamic appeared before in this person's life, relationships, or history? What is the organizing principle generating the same loop at different scales?
Layer 10 - Root Cause: The single original wound, foundational belief, or core system logic beneath everything. The one thing, if shifted, that collapses the entire structure above it.
Layer 11 - Truth: The universal meta-pattern at work. What timeless law of human nature, consciousness, or reality does this situation illuminate? What does it reveal that goes beyond this specific person?
Layer 12 - Integration: What transformation is this situation calling for? What does genuine resolution require at the deepest level — not a fix, but a fundamental shift in being?
List EACH layer on its own line in this exact format (no bolding):
Layer X - Name: Explanation in 3-5 mobile-optimized sentences. Be specific, penetrating, and revelatory — never generic. Name the exact mechanism, not a category.
</depth>

<root_cause>
Symptom: [Immediate visible symptom]
Immediate Cause: [Immediate trigger]
Underlying Cause: [Deconstruct incentive, resource constraint, or system bias]
Deeper Cause: [Defensive adaptive survival model, social conflict, or attachment pattern]
Root Cause Estimate: [Probabilistic root cause]
Supporting Evidence: [Core logic supporting this root cause]
Alternative Root Causes: [Other plausible root-cause theories]
</root_cause>

<human_intel>
Surface Intention: [Apparent intent/claim]
Emotional Driver: [Suppressed emotion, or vulnerable state]
Need Driver: [Fundamental human need driving behavior]
Fear Driver: [Core underlying fear being avoided]
Incentive Driver: [What is gained strategically or socially]
Identity Driver: [Internal self-image/narrative being guarded]
Hidden Motives: [Possible unspoken status, control, or security loops]
</human_intel>

<future_prob>
Scenario A - Most Likely Path | [Probability percentage, e.g. 60]% | [1-2 sentences on what will occur if current loop persists]
Scenario B - Positive Alignment | [Probability percentage, e.g. 20]% | [1-2 sentences on how proactive shifts alter this outcome]
Scenario C - Risk Escalation | [Probability percentage, e.g. 15]% | [1-2 sentences on how fear or failure to act triggers escalation]
Scenario D - Outlier Factor | [Probability percentage, e.g. 5]% | [1-2 sentences on uncommon but possible systemic forces]
Early Warning Signals: [2 indicators of progress or change]
</future_prob>

<memory_insight>
[Formulate exactly 1-2 major core lessons or pattern revelations observed from this query. Frame it as general behavioral insights. These will be added to the memory system. Keep them concise, exactly 1-line each. Do not use bullets or markdown, just print each insight on its own line.]
</memory_insight>

<questions>
[Generate 6-8 powerfully specific follow-up questions that pierce through to the next hidden layer of this exact situation. These must NOT be generic coaching questions. Each question should shatter a comfortable assumption, open a dimension the user has not considered, or force them to look directly at something they are avoiding. Cover angles: psychological shadow, neurological pattern, systemic incentive, historical repetition, identity investment, unconscious payoff, and the fear underneath the fear. Each on a new line starting with a question mark '?'.
NEXT QUESTION UI RULE:
- Suggested questions must never be displayed in a horizontal scrolling row. All suggested questions must be displayed vertically. Each question should occupy its own line or card.
- Full question must be visible with no truncation, no horizontal scrolling, mobile-first layout, and maximum readability.
- Render each question as an individual clickable card contextually. Cards will expand height dynamically based on length to wrap multiple lines smoothly.
]
</questions>

<exploration>
[List 3-5 exploration paths tailored for this scenario, starting each with '✓', choosing from:
- Go Deeper
- Highlight Blind Spot
- Challenge Assumptions
- Show Opposite Perspective
- Strategic Leverage Analysis
- Psychological Adaptations
- Reveal Root Cause
- Systems Feedback Analysis]
</exploration>

Follow this format meticulously. Wrap each visual module within its respective tags to generate the absolute premium, zero-markdown-clutter diagnostic response. Respond directly with insights.
        """.trimIndent()

        // Build API contents payload
        val contentsPayload = mutableListOf<Content>()
        for (msg in history) {
            val partsList = mutableListOf<Part>()
            
            // If any media/file attachment is present, attach it using its detected MIME type!
            if (!msg.imageUri.isNullOrEmpty() && msg.role == "user") {
                val mediaData = loadUriAsMediaData(msg.imageUri)
                if (mediaData != null) {
                    partsList.add(Part(inlineData = InlineData(mimeType = mediaData.mimeType, data = mediaData.base64)))
                }
            }
            
            // Add text part
            partsList.add(Part(text = msg.text))
            contentsPayload.add(Content(role = msg.role, parts = partsList))
        }

        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.72f),
            systemInstruction = Content(parts = listOf(Part(text = customInstructionOverride ?: systemInstructionText)))
        )

        var modelText: String? = null
        var lastException: Exception? = null
        val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview")

        for (modelName in modelsToTry) {
            try {
                val response = apiService.generateContent(modelName, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    modelText = text
                    break
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (modelText != null) {
            // Save assistant message to Database history
            val assistantMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "model",
                text = modelText,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(assistantMsg)
            triggerUpload { uid ->
                CloudSyncService.uploadMessage(uid, assistantMsg.id, assistantMsg.sessionId, assistantMsg.role, assistantMsg.text, assistantMsg.imageUri, assistantMsg.timestamp)
            }

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

            return@withContext ResponseParser.parse(modelText)
        } else {
            val errorMsg = "Error invoking DepthLens engine: ${lastException?.localizedMessage ?: lastException?.message ?: "Unknown Connection Error"}"
            try {
                val assistantMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "model",
                    text = errorMsg,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(assistantMsg)
                triggerUpload { uid ->
                    CloudSyncService.uploadMessage(uid, assistantMsg.id, assistantMsg.sessionId, assistantMsg.role, assistantMsg.text, assistantMsg.imageUri, assistantMsg.timestamp)
                }
            } catch (dbEx: Exception) {
                dbEx.printStackTrace()
            }
            return@withContext ResponseParser.parse(errorMsg)
        }
    }

    private fun extractTagContent(text: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    data class MediaData(val mimeType: String, val base64: String)

    private fun loadUriAsMediaData(uriString: String): MediaData? {
        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            var mimeType = resolver.getType(uri) ?: "application/octet-stream"
            
            // Fallback content-type detection
            if (mimeType == "application/octet-stream") {
                val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uriString)
                if (!extension.isNullOrEmpty()) {
                    val detected = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                    if (detected != null) {
                        mimeType = detected
                    }
                }
            }
            
            val inputStream = resolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            
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
        
        val systemPrompt = """
            You are the DepthLens Conversation Continuity Engine™. Your role is to reconnect the context of a previous conversation.
            You are given a historic log of messages. Analyze them carefully.
            Generate a brief, highly structured summary to restore active mental models.
            
            IMPORTANT: Use exactly this pure text layout (DO NOT use asterisk markdown '**' or '#' headings):
            
            ⚡ CONTEXT RESTORED BRIEF
            
            PREVIOUS CONTEXT:
            [2-3 sentences summarizing the core topic discussed]
            
            CURRENT PROGRESS & GOALS:
            [Identify the main user goals/concerns and what has been discovered so far]
            
            UNANSWERED QUESTIONS:
            [List 2-3 critical open questions to answer next to depth-test this situation]
            
            SUGGESTED NEXT STEPS:
            [1-2 clear immediate prompt items to explore]
        """.trimIndent()
        
        val contentsPayload = mutableListOf<Content>()
        for (msg in history) {
            contentsPayload.add(Content(role = msg.role, parts = listOf(Part(text = msg.text))))
        }
        
        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.4f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        
        try {
            val response = apiService.generateContent("gemini-2.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Unable to sync session context at this moment."
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

    fun startBackgroundAnalysis(sessionId: String, onComplete: () -> Unit = {}) {
        synchronized(this) {
            val current = _runningAnalyses.value
            if (current.contains(sessionId)) return // Already running
            _runningAnalyses.value = current + sessionId
        }

        backgroundScope.launch {
            try {
                generateAnalysis(sessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                synchronized(this) {
                    _runningAnalyses.value = _runningAnalyses.value - sessionId
                }
                sendLocalNotification(context, sessionId)
                onComplete()
            }
        }
    }

    private fun sendLocalNotification(context: Context, sessionId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "depthlens_analysis_channel"
            val channelName = "DepthLens Analysis Notifications"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for completed strategic analyses."
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

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("Analysis Complete")
                .setContentText("Your DepthLens analysis is ready.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(sessionId.hashCode(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val _runningAnalyses = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
        val runningAnalyses: kotlinx.coroutines.flow.StateFlow<Set<String>> = _runningAnalyses.asStateFlow()

        private val backgroundScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
        )
    }
}

object ResponseParser {
    fun parse(rawResponse: String): ParsedResponse {
        var introduction = rawResponse

        val summary = extractTagContent(rawResponse, "summary")
        val confidence = extractTagContent(rawResponse, "confidence")?.trim()
        val depthRaw = extractTagContent(rawResponse, "depth")
        val rootCauseRaw = extractTagContent(rawResponse, "root_cause")
        val humanIntelRaw = extractTagContent(rawResponse, "human_intel")
        val futureProbRaw = extractTagContent(rawResponse, "future_prob")
        val questionsRaw = extractTagContent(rawResponse, "questions")
        val explorationRaw = extractTagContent(rawResponse, "exploration")

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
                symptom = parseField(rootCauseRaw, "Symptom"),
                immediateCause = parseField(rootCauseRaw, "Immediate Cause"),
                underlyingCause = parseField(rootCauseRaw, "Underlying Cause"),
                deeperCause = parseField(rootCauseRaw, "Deeper Cause"),
                rootCauseEstimate = parseField(rootCauseRaw, "Root Cause Estimate"),
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

        return ParsedResponse(
            introduction = cleanIntro.trim(),
            executiveSummary = summary,
            depthLayers = depthLayers,
            rootCauseReport = rootCauseReport,
            humanDrivers = humanReport,
            futureScenarios = futureScenarios.map { it.copy(earlyWarningSigns = eWarningSigns) },
            confidence = confidence?.ifEmpty { "High" } ?: "High",
            suggestedQuestions = suggestedQuestions,
            explorationPaths = explorationPaths
        )
    }

    private fun extractTagContent(text: String, tag: String): String? {
        val pattern = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun cleanTags(text: String): String {
        var cleaned = text
        val tags = listOf(
            "summary", "confidence", "depth", "root_cause",
            "human_intel", "future_prob", "memory_insight", "questions", "exploration"
        )
        for (tag in tags) {
            cleaned = cleaned.replace("<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
        }
        return cleaned.replace("<[^>]*>".toRegex(), "").trim()
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
