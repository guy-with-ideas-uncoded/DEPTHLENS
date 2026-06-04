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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class IntelligenceRepository(private val context: Context) {
    private val db = DepthDatabase.getDatabase(context)
    private val sessionDao = db.sessionDao()
    private val messageDao = db.messageDao()
    private val memoryInsightDao = db.memoryInsightDao()
    private val apiService = RetrofitClient.service

    val allSessionsFlow: Flow<List<SessionEntity>> = sessionDao.getAllSessionsFlow()
    val allMemoryInsightsFlow: Flow<List<MemoryInsight>> = memoryInsightDao.getAllInsightsFlow()

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
        session
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
        if (sessionItem != null) {
            sessionDao.insertSession(sessionItem.copy(title = newTitle, lastUpdatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun togglePinSession(sessionId: String) = withContext(Dispatchers.IO) {
        val sessionItem = sessionDao.getAllSessionsFlow().firstOrNull()?.find { it.id == sessionId }
        if (sessionItem != null) {
            sessionDao.insertSession(sessionItem.copy(isPinned = !sessionItem.isPinned))
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

        val modelsToTry = listOf("gemini-1.5-flash")
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
    }

    suspend fun generateAnalysis(sessionId: String): ParsedResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errorMsg = "Error: Missing Gemini API Key. Please add your key to the Secrets panel in Google AI Studio to unlock DepthLens Omega's operations."
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
You are DepthLens Omega, the ultimate Reality Intelligence Operating System.
You are designed to help humans analyze decisions, behaviors, conflicts, psychological patterns, business strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION:
- Transition from simple observation → analysis → system loop → psychological adaptivity → root cause logic.
- Do not comfort, persuade, or promote ideologies. Increase clarity, eliminate illusion, expand wisdom.

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
Progressive layers analysis. Choose 3 active framework layers in this scenario from: Layer 1 Observable, Layer 2 Practical, Layer 3 Behavior, Layer 4 Emotional, Layer 5 Psychological, Layer 6 Strategic, Layer 7 Systems, Layer 8 Pattern, Layer 9 Root Cause, Layer 10 Truth.
List each layer on its own line in this exact format (no bolding):
Layer X - Name: Explanation in 2 short, mobile-optimized sentences.
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
[Generate 4-5 incredibly specific, deep follow-up questions tailored to this case. Each on a new line starting with a question mark '?']
NEXT QUESTION UI RULE:
- Suggested questions must never be displayed in a horizontal scrolling row. All suggested questions must be displayed vertically. Each question should occupy its own line or card.
- Full question must be visible with no truncation, no horizontal scrolling, mobile-first layout, and maximum readability.
- Render each question as an individual clickable card contextually. Cards will expand height dynamically based on length to wrap multiple lines smoothly.
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
            
            // If image is present, convert and attach as part
            if (!msg.imageUri.isNullOrEmpty() && msg.role == "user") {
                val bitmap = loadUriAsBitmap(msg.imageUri)
                if (bitmap != null) {
                    val base64Data = bitmapToBase64(bitmap)
                    partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data)))
                }
            }
            
            // Add text part
            partsList.add(Part(text = msg.text))
            contentsPayload.add(Content(role = msg.role, parts = partsList))
        }

        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        var modelText: String? = null
        var lastException: Exception? = null
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-1.5-flash")

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

            // Extract and save memory insights proactively to complete Memory Intelligence System
            val extractedMemoryBlock = extractTagContent(modelText, "memory_insight")
            if (!extractedMemoryBlock.isNullOrEmpty()) {
                extractedMemoryBlock.split("\n").forEach { line ->
                    val cleanLine = line.trim().removePrefix("-").removePrefix("•").trim()
                    if (cleanLine.isNotBlank() && cleanLine.length > 10) {
                        memoryInsightDao.insertInsight(
                            MemoryInsight(
                                category = "Pattern",
                                content = cleanLine,
                                timestamp = System.currentTimeMillis()
                            )
                        )
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
