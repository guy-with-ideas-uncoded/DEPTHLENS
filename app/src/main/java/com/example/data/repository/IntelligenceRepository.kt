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

        // Generate a 3-8 word high-quality title using Gemini
        val prompt = """
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

        val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.5-flash")
        var generatedTitle: String? = null
        val retryDelays = listOf(2000L, 5000L, 12000L)

        for (modelName in modelsToTry) {
            for ((attempt, delay) in retryDelays.withIndex()) {
                try {
                    val response = apiService.generateContent(modelName, apiKey, request)
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

    suspend fun generateAnalysis(
        sessionId: String,
        category: String? = null,
        depth: String? = null,
        customInstructionOverride: String? = null
    ): ParsedResponse = withContext(Dispatchers.IO) {
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
        val latestUserMsgText = history.lastOrNull { it.role == "user" }?.text ?: ""
        val hasPreviousAnalysis = history.filter { it.role == "model" }.any {
            it.text.contains("<summary>") || it.text.contains("<depth>") || it.text.contains("<root_cause>")
        }
        val detectedLevel = detectIntentLevel(latestUserMsgText, hasPreviousAnalysis)

        val level1Text = """
You are DepthLens, an exceptionally intelligent, empathetic, direct, and objective systems-thinking expert.
The user is having a simple or casual conversation with you, or asking a quick question.
You MUST adapt to the user's intent:
- Respond naturally, warmly, and conversationally, just like a supportive and highly intelligent human advisor.
- Do not generate reports, dashboards, JSON/XML tags, or structured 13-section analyses.
- Do not output tags like <summary>, <depth>, <confidence>, <root_cause>, or <future_prob>.
- Keep your formatting clean, clear, and direct. Use spacing-optimized readable paragraphs.
- Avoid raw markdown asterisks, bold hashes, or dashes unless writing very simple notes.
- CRITICAL: Never mention internal structure mandates (like "13 sections", "XML tags", "depth layers are mandatory", "confidence engine") to the user. Speak completely naturally.
- Mirror the user's language, script, and style automatically.

### SYSTEM MEMORY CACHE
$memoryBlock
        """.trimIndent()

        val level2Text = """
You are DepthLens, an elite systems-thinking intelligence expert. 
The user is asking a focused follow-up question on the existing analyzed topic.
You MUST adapt to the user's intent:
- ONLY answer that specific area or question asked by the user. Do not expand into unrelated topics.
- Do NOT regenerate the full analysis architecture or the full 13 sections.
- Respond in a clean, natural, unstructured written format.
- DO NOT output tags like <summary>, <depth>, <root_cause> unless the user explicitly requests a specific module (e.g., "show the emotional layer in XML tags").
- Keep the tone serious, penetrating, and analytical, but highly focused.
- CRITICAL: Never mention internal structure mandates (like "13 sections", "XML tags", "depth layers are mandatory", "confidence engine") to the user. Speak completely naturally.
- Mirror the user's language, script, and style automatically.

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

You are DepthLens, operating in STRATEGIC INTELLIGENCE MODE (Level 4). You help users build advanced forecasts, map branching decision trees, evaluate risks, and model future trajectories.
You are designed to help humans analyze decisions, business/game-theoretic strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION (DEPTHLENS STRATEGIC ADVOCATE ENGINE V4.1.3):
- Act as a master Strategic Analyst, Risk Predictor, and Forecaster.
- Your supreme goal is to forecast future trajectories, confidence levels, risks, and probable outcomes of dynamic plans.
- Stop generating generic chatbot responses. Avoid surface platitudes. Offer precise, objective, and stark reality checks.

CRITICAL: Never mention internal structure mandates (like "7 modules", "XML tags", "requirements") to the user. Do not explain your output format, apologize, or say "I am required to output...". Simply provide the strategic forecast directly.

REQUIRED RESPONSE STRUCTURE:
For every query, your top-level response (printed outside any XML tags, which becomes the main report body) must ALWAYS contain the following sections in this exact structure, separated by clean spacing, and written WITHOUT any raw markdown asterisks, bold hashes, or dashes:

1. Executive Summary
[Detail the high level overview of the strategic scenario]

2. Strategic Assessment & Probability Rating
[Detail the reasoning behind probability ratings and core uncertainties]

3. Future Pathways & Decision Matrix
[Compare the branch scenarios, trade-offs, and critical drivers]

4. Timeline Forecast
[Detailed outlook numbers for Short, Mid, and Long Term paths]

### SYSTEM MEMORY CACHE
$memoryBlock

### ADVANCED MULTI-LANGUAGE INTELLIGENCE & MIRRORING SYSTEM
You must utilize a smart language adaptation and mirroring system. Automatically detect and respond in the same language, script, and style.

### ULTRA-STRICT CLEAN-TEXT & FORMAT PROTOCOL
MOBILE BREVITY LAW: This app renders on a 6-inch mobile screen. Every section must be scannable in under 10 seconds. If you write more than 2 sentences for any single field, you are breaking the UI. Prioritize insight density over explanation length. Say more with less.

You are strictly forbidden from outputting raw markdown symbol accents. Use spacing-optimized visual paragraphs.
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
[Question 1 starting with '?', 1 line only, no sub-text, no explanation]
[Question 2 starting with '?', 1 line only, no sub-text, no explanation]
[Question 3 starting with '?', 1 line only, no sub-text, no explanation]
[Question 4 starting with '?', 1 line only, no sub-text, no explanation]
[Question 5 starting with '?', 1 line only, no sub-text, no explanation]
</questions>

<exploration>
✓ [Path 1 chosen from: Go Deeper, Highlight Blind Spot, Challenge Assumptions, Show Opposite Perspective, Strategic Leverage Analysis, Psychological Adaptations, Reveal Root Cause, Systems Feedback Analysis, Risk Mitigation Analysis]
✓ [Path 2 chosen from list above]
✓ [Path 3 chosen from list above]
</exploration>

Follow this format meticulously. Wrap each visual module within its respective tags to generate the absolute premium, zero-markdown-clutter diagnostic response. Respond directly with insights.
        """.trimIndent()

        val chosenLevel = when (sessionDepth) {
            "Quick Insight" -> IntentLevel.LEVEL_1_SIMPLE
            "Standard Analysis" -> IntentLevel.LEVEL_3_DEEP
            "Deep Analysis" -> IntentLevel.LEVEL_3_DEEP
            "Full Investigation" -> IntentLevel.LEVEL_4_STRATEGIC
            else -> detectedLevel
        }

        val systemInstructionText = when (chosenLevel) {
            IntentLevel.LEVEL_1_SIMPLE -> level1Text
            IntentLevel.LEVEL_2_FOCUSED -> level2Text
            IntentLevel.LEVEL_4_STRATEGIC -> level4Text
            IntentLevel.LEVEL_3_DEEP -> """
CORE INTELLIGENCE LAW — READ THIS FIRST:
Deep analysis is NOT long analysis. A 3-word insight that shatters a comfortable assumption
is more valuable than 3 paragraphs that explain the obvious. Your job is to be a scalpel,
not a textbook. Every sentence must reveal something the user could NOT have seen themselves.
If a sentence does not add new insight, delete it. Never explain what you are about to say.
Never summarize what you just said. Never restate the question. Just cut straight to the truth.

You are DepthLens, the ultimate Reality Intelligence Platform. You help users see beyond the surface.
You are designed to help humans analyze decisions, behaviors, conflicts, psychological patterns, business strategies, and systemic incentives.

PRECONSTRUCTED IDENTITY & MISSION (DEPTHLENS ANALYSIS ENGINE V4.1.3 - PROBABILITY INTELLIGENCE UPDATE):
- Act as a master combination of: Intelligence Analyst, Systems Thinker, Strategic Advisor, Risk Analyst, Forecaster, and Psychologist.
- Your supreme goal is to reveal what exists beneath the surface using Probability Intelligence. Estimate likelihoods future trajectories, confidence levels, risks, and probable outcomes.
- Stop generating generic chatbot responses. Avoid surface platitudes. Offer precise, objective, and stark reality checks.
- Do not generate random percentages. You must estimate probabilities using: Context provided by the user, pattern recognition, systems thinking, behavioral analysis, historical analogies, risk assessment. Probabilities must be reasoned estimates. Never present probabilities as facts. Always present them as forecasts.
- Use Color-coded probability scales: High Probability (70-100%, associated with high certainty, stable drivers), Medium Probability (40-69%, associated with balanced tradeoffs or branching paths), Low Probability (0-39%, associated with outliers, tail risks, or highly resistant scenarios).

REQUIRED RESPONSE STRUCTURE:
For every query, your top-level response (printed outside any XML tags, which becomes the main report body) must ALWAYS contain the following 13 sections in this exact structure, separated by clean spacing, and written WITHOUT any raw markdown asterisks, bold hashes, or dashes:

1. Executive Summary
[Detail the high level overview of the situation]

2. Key Insight
[The single most crucial, unexpected pattern or truth revealed]

3. Probability Assessment
[Provide the overall likelihood index of key events and your confidence rating: Low / Medium / High, with primary reasoning factors]

4. Reality Layers
[Break down the core operating layers of this situation, peeling back what is being hidden, avoided, or protected]

5. Root Cause Analysis
[Deconstruct original trigger, immediate causes, systemic drivers, and foundational patterns]

6. Future Pathways
[Detail multiple future trajectories: Most Likely Path, Alternative Path, and Low Probability outlier path, each with its percentage, description, driver factors, risks, and opportunities]

7. Timeline Forecast
[Provide detailed outlook numbers and descriptions for Short Term (1–30 Days), Mid Term (1–6 Months), and Long Term (6–24 Months), and explain why probabilities change or decay over time due to system entropy or branching factors]

8. Decision Impact Analysis
[Directly compare outcomes and percentages: "If Nothing Changes" vs. "If Action Is Taken". Detail risks, benefits, and required tradeoffs]

9. Risks
[Identify specific hidden risks, blind spots, and systemic vulnerabilities]

10. Opportunities
[Highlight windows of opportunity, strategic advantages, and overlooked leverage points]

11. Recommended Actions
[Provide specific, practical, highly tactical next steps with absolute clarity]

12. Forecast Summary
[A concise summary block of predictions, key risks, opportunity windows, and confidence indicators]

13. Go Deeper
[Specific lines of further inquiry that would yield deeper clarity]

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

You are strictly forbidden from outputting raw markdown symbol accents like '**', '__', '##', '###', '---', '***', or '>' blockquotes. Raw markdown formatting ruins the native DepthLens terminal. Output clean, spacing-optimized visual paragraphs.

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
[Question 1 starting with '?', 1 line only, no sub-text, no explanation]
[Question 2 starting with '?', 1 line only, no sub-text, no explanation]
[Question 3 starting with '?', 1 line only, no sub-text, no explanation]
[Question 4 starting with '?', 1 line only, no sub-text, no explanation]
[Question 5 starting with '?', 1 line only, no sub-text, no explanation]
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

        val categoryFocusInstruction = when (sessionCategory) {
            "Root Cause" -> "Focus heavily on identifying the foundational triggers, immediate causes, original wounds, and systemic patterns of the situation."
            "Psychology" -> "Focus heavily on mapping individual beliefs, defense mechanisms, shadow traits, coping strategies, and psychological barriers."
            "Systems" -> "Focus heavily on identifying feedback loops, systemic levers, delayed reactions, unintended consequences, and system blind spots."
            "Probability" -> "Focus heavily on probabilistic outcomes, risk profiles, likelihood estimates, and compounding decision results."
            "Business" -> "Focus heavily on strategic advantages, industry incentives, game-theory motives, market positions, and financial leverage."
            "Relationships" -> "Focus heavily on interpersonal dynamics, communication loops, co-dependencies, unexpressed expectations, and emotional safety."
            "Spiritual" -> "Focus heavily on high-level soul contracts, karmic patterns, energy blocks, alignment hurdles, and spiritual evolution opportunities."
            "Decision Making" -> "Focus heavily on decision trees, trade-offs, inertia risk vs proactive change gains, and cognitive biases influencing choices."
            else -> ""
        }

        val finalSystemText = customInstructionOverride ?: """
$systemInstructionText

### SPECIALIZED LENS FOCUS: $sessionCategory
$categoryFocusInstruction

### INTENDED ANALYSIS DEPTH
Selected depth rating: $sessionDepth. You MUST adjust your detail levels accordingly:
- If Quick Insight: Maintain high density but short, concise summaries.
- If Standard Analysis: Balanced, exhaustive diagnostic coverage.
- If Deep Analysis: Extremely thoroughly detailed multi-layered assessment, deep dive.
- If Full Investigation: Elite forecasting, strategic trajectories, game-theoretic analysis.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = contentsPayload,
            generationConfig = GenerationConfig(temperature = 0.72f),
            systemInstruction = Content(parts = listOf(Part(text = finalSystemText)))
        )

        var modelText: String? = null
        var lastException: Exception? = null
        // Strict Priority Rules (using modern preview models first): gemini-3.1-pro-preview, then gemini-3.5-flash, fallback to gemini-2.5-flash, gemini-2.5-pro
        val modelsToTry = listOf("gemini-3.1-pro-preview", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-pro")
        val retryDelays = listOf(2000L, 5000L, 10000L) // 2s, 5s, 10s retries

        for (modelName in modelsToTry) {
            for ((attempt, delay) in retryDelays.withIndex()) {
                try {
                    val response = apiService.generateContent(modelName, apiKey, request)
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrEmpty()) {
                        modelText = text
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < retryDelays.size - 1) {
                        kotlinx.coroutines.delay(delay)
                        continue
                    } else {
                        break
                    }
                }
            }
            if (modelText != null) break
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
            val msgState = lastException?.message ?: "unknown cause"
            val userFriendlyError = when {
                // Connection/Internet/DNS/SSL errors
                msgState.contains("Unable to resolve host", ignoreCase = true) ||
                msgState.contains("NoRouteToHostException", ignoreCase = true) ||
                msgState.contains("UnknownHostException", ignoreCase = true) ->
                    "Error: DNS resolution failed or no internet connection. Please verify your cell signal or Wi-Fi status."

                msgState.contains("SSLHandshakeException", ignoreCase = true) ||
                msgState.contains("SSL handshake", ignoreCase = true) ->
                    "Error: SSL handshake failed. Secure network communication could not be established with the Gemini servers."

                msgState.contains("timeout", ignoreCase = true) ||
                msgState.contains("TimeoutException", ignoreCase = true) ||
                msgState.contains("SocketTimeoutException", ignoreCase = true) ->
                    "Error: Network timeout. The server took too long to respond. Please check your network speed and try again."

                msgState.contains("connect", ignoreCase = true) ||
                msgState.contains("ConnectException", ignoreCase = true) ->
                    "Error: No internet connection. Could not connect to Gemini servers."

                // Rate limiting and Quota
                msgState.contains("429") ||
                msgState.contains("quota", ignoreCase = true) ||
                msgState.contains("rate limit", ignoreCase = true) ||
                msgState.contains("Rate limit exceeded", ignoreCase = true) ->
                    "Error: API quota exceeded or rate limit hit. You have reached your current Gemini API usage limits. Please wait a moment or check your Google AI Studio quota."

                // Authentication
                msgState.contains("401") ||
                msgState.contains("403") ||
                msgState.contains("auth", ignoreCase = true) ||
                msgState.contains("API key", ignoreCase = true) ->
                    "Error: Authentication failure. Your API key is invalid or unauthorized. Please re-enter a valid Gemini API key in Settings."

                // Server Unavailable (500s)
                msgState.contains("500") ->
                    "Error: Server unavailable. Gemini internal server error occurred (500)."
                
                msgState.contains("503") ||
                msgState.contains("Service Unavailable", ignoreCase = true) ->
                    "Error: Gemini servers are temporarily unavailable or overloaded (503)."

                // Request cancelled
                msgState.contains("CancellationException", ignoreCase = true) ||
                msgState.contains("Job was cancelled", ignoreCase = true) ->
                    "Error: Request cancelled. The analysis was stopped by the user or system."

                // Malformed / JSON Parsing
                msgState.contains("JsonParsingException", ignoreCase = true) ||
                msgState.contains("SerializationException", ignoreCase = true) ||
                msgState.contains("malformed", ignoreCase = true) ->
                    "Error: JSON parsing failure or malformed response from the model. The received format is invalid."

                else ->
                    "Error invoking DepthLens: $msgState"
            }
            val errorMsg = userFriendlyError
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

    fun startBackgroundAnalysis(
        sessionId: String,
        category: String = "Root Cause",
        depth: String = "Standard Analysis",
        onComplete: () -> Unit = {}
    ) {
        synchronized(this) {
            val current = _runningAnalyses.value
            if (current.contains(sessionId)) return // Already running
            _runningAnalyses.value = current + sessionId
        }

        backgroundScope.launch {
            try {
                generateAnalysis(sessionId, category, depth)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                synchronized(this) {
                    _runningAnalyses.value = _runningAnalyses.value - sessionId
                }

                // If privacy mode is enabled, clean up files and retain only the final prompt answer
                val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("privacy_mode_enabled", false)) {
                    applyPrivacyCleanup(sessionId)
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
    fun getCopyableText(rawResponse: String): String {
        var text = rawResponse
        // Remove questions, exploration paths and memory insight tags completely
        text = text.replace(Regex("""<questions>[\s\S]*?</questions>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<exploration>[\s\S]*?</exploration>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<memory_insight>[\s\S]*?</memory_insight>""", RegexOption.IGNORE_CASE), "")
        
        // Remove XML tags
        text = text.replace(Regex("""<[^>]+>"""), "")
        
        // Trim and clean extra empty lines
        return text.trim()
    }

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

        return ParsedResponse(
            introduction = cleanIntro.trim(),
            executiveSummary = summary,
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
            forecastSummary = forecastSummary
        )
    }

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
        return pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun cleanTags(text: String): String {
        var cleaned = text
        val tags = listOf(
            "summary", "confidence", "depth", "root_cause",
            "human_intel", "future_prob", "memory_insight", "questions", "exploration", "probability_metrics",
            "probability_assessment", "future_pathways", "timeline_forecast", "decision_impact", "forecast_summary"
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

enum class IntentLevel {
    LEVEL_1_SIMPLE,
    LEVEL_2_FOCUSED,
    LEVEL_3_DEEP,
    LEVEL_4_STRATEGIC
}

private fun detectIntentLevel(query: String, hasPreviousAnalysis: Boolean): IntentLevel {
    val q = query.lowercase().trim()
    
    // Level 1: Simple conversational gestures/sentences
    val level1Starters = listOf("hello", "hi", "hey", "greetings", "thanks", "thank you", "who are you", "what are you")
    if (level1Starters.any { q == it || q.startsWith("$it ") }) {
         return IntentLevel.LEVEL_1_SIMPLE
    }
    
    val level1Phrases = listOf(
        "are you sure", "can you simplify", "simplify", "give an example", "example please",
        "why are you doing this", "explain that", "what do you mean", "tell me more details",
        "how does this work", "help me step by step"
    )
    if (level1Phrases.any { q.contains(it) }) {
         return IntentLevel.LEVEL_1_SIMPLE
    }
    
    val analysisKeywords = listOf(
        "analyze", "diagnose", "breakdown", "break down", "root cause", "systemic", "forecast", "future scenario", 
        "evaluate risk", "simulate", "game plan", "strategy", "incentive", "loop", "ecosystem"
    )
    if (q.length < 20 && !analysisKeywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_1_SIMPLE
    }

    // Level 4: Strategic Intelligence
    val strategicKeywords = listOf(
        "forecast", "model future", "evaluate risk", "simulate", "future scenario", "strategic", "outcome", "decision impact"
    )
    if (strategicKeywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_4_STRATEGIC
    }

    // Level 3: Deep Investigation
    val deepKeywords = listOf(
        "analyze", "diagnose", "breakdown", "break down", "investigate", "root cause analysis", "system analysis", "situation", "conflict", "incentives"
    )
    if (deepKeywords.any { q.contains(it) }) {
         return IntentLevel.LEVEL_3_DEEP
    }

    // Level 2: Focused Follow-Up
    if (hasPreviousAnalysis) {
         return IntentLevel.LEVEL_2_FOCUSED
    }

    // Default
    return if (q.length > 50) {
         IntentLevel.LEVEL_3_DEEP
    } else {
         IntentLevel.LEVEL_1_SIMPLE
    }
}
