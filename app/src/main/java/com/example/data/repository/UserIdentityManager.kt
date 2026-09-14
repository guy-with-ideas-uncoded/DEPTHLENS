package com.example.data.repository

import android.util.Log
import com.example.data.database.IdentityNodeDao
import com.example.data.database.MessageDao
import com.example.data.database.SessionDao
import com.example.data.model.EpistemicClassification
import com.example.data.model.HigherSelfProfile
import com.example.data.model.IdentityCategories
import com.example.data.model.IdentityNodeEntity
import com.example.data.model.MindMapCategoryGroup
import com.example.data.model.SelfReflectionComparison
import com.example.data.model.UserIdentitySummary
import com.example.data.network.Content
import com.example.data.network.GenerateContentRequest
import com.example.data.network.GeminiApiService
import com.example.data.network.GenerationConfig
import com.example.data.network.Part
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UserIdentityManager(
    private val identityNodeDao: IdentityNodeDao,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val apiService: GeminiApiService,
    private val getApiKey: () -> String,
    private val getPreferredModel: () -> String,
    private val backgroundScope: CoroutineScope
) {
    companion object {
        private const val TAG = "UserIdentityManager"
    }

    val allActiveNodesFlow: Flow<List<IdentityNodeEntity>> = identityNodeDao.getAllActiveNodesFlow()
    val allNodesFlow: Flow<List<IdentityNodeEntity>> = identityNodeDao.getAllNodesFlow()

    val categoryGroupsFlow: Flow<List<MindMapCategoryGroup>> = allActiveNodesFlow.map { nodes ->
        buildCategoryGroups(nodes)
    }

    val identitySummaryFlow: Flow<UserIdentitySummary> = allActiveNodesFlow.map { nodes ->
        UserIdentitySummary(
            totalNodes = nodes.size,
            categoriesCount = nodes.map { it.category }.distinct().size,
            explicitFactsCount = nodes.count { it.classification == EpistemicClassification.EXPLICIT_FACT.id },
            strongPatternsCount = nodes.count { it.classification == EpistemicClassification.STRONG_PATTERN.id },
            inferencesCount = nodes.count { it.classification == EpistemicClassification.INFERENCE.id },
            highConfidenceCount = nodes.count { it.confidence >= 80 },
            lastLearnedTime = nodes.maxOfOrNull { it.lastUpdatedAt } ?: 0L
        )
    }

    /**
     * Continuous Higher Self / Inner Guide reflective synthesis flow.
     * Evolving model of who the user is striving to become based on accumulated evidence.
     */
    val higherSelfProfileFlow: Flow<HigherSelfProfile> = allActiveNodesFlow.map { nodes ->
        computeHigherSelfProfile(nodes)
    }

    /**
     * Computes the Higher Self / Inner Guide reflective profile from active Mind Map traits.
     */
    fun computeHigherSelfProfile(nodes: List<IdentityNodeEntity>): HigherSelfProfile {
        if (nodes.isEmpty()) {
            return HigherSelfProfile(
                reflectiveSynthesis = "Your reflective profile is waiting to form. As you share your thoughts, goals, and dilemmas with DepthLens, your evolving values and inner guide will take shape here.",
                totalEvidenceNodes = 0
            )
        }

        val coreValues = nodes.filter { it.category.equals(IdentityCategories.VALUES, ignoreCase = true) }
            .map { it.title }
        val longTermAspirations = nodes.filter {
            it.category.equals(IdentityCategories.GOALS, ignoreCase = true) ||
            it.category.equals(IdentityCategories.INTERESTS, ignoreCase = true) ||
            it.subcategory?.contains("Long-term", ignoreCase = true) == true
        }.map { it.title }
        val recurringPatterns = nodes.filter {
            it.category.equals(IdentityCategories.THINKING_REASONING, ignoreCase = true) ||
            it.category.equals(IdentityCategories.HABITS_ROUTINES, ignoreCase = true) ||
            it.classification == EpistemicClassification.STRONG_PATTERN.id
        }.map { it.title }
        val potentialBlindSpots = nodes.filter {
            it.category.equals(IdentityCategories.RECURRING_CONCERNS, ignoreCase = true) ||
            it.category.equals(IdentityCategories.WEAKNESSES, ignoreCase = true)
        }.map { it.title }
        val evolvingBeliefs = nodes.filter {
            it.category.equals(IdentityCategories.EVOLVING_BELIEFS, ignoreCase = true) ||
            it.category.equals(IdentityCategories.IMPORTANT_EXPERIENCES, ignoreCase = true)
        }.map { it.title }

        val synthesis = buildReflectiveSynthesisText(
            nodes, coreValues, longTermAspirations, recurringPatterns, potentialBlindSpots
        )

        return HigherSelfProfile(
            coreValues = coreValues,
            longTermAspirations = longTermAspirations,
            recurringPatterns = recurringPatterns,
            potentialBlindSpots = potentialBlindSpots,
            evolvingBeliefs = evolvingBeliefs,
            reflectiveSynthesis = synthesis,
            totalEvidenceNodes = nodes.size,
            lastSynthesizedTime = nodes.maxOfOrNull { it.lastUpdatedAt } ?: System.currentTimeMillis()
        )
    }

    private fun buildReflectiveSynthesisText(
        nodes: List<IdentityNodeEntity>,
        values: List<String>,
        aspirations: List<String>,
        patterns: List<String>,
        blindSpots: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append("A grounded, reflective synthesis based on ${nodes.size} verified observations across your conversations. ")
        if (values.isNotEmpty()) {
            sb.append("Your core anchors center on ${values.take(3).joinToString(", ")}. ")
        }
        if (aspirations.isNotEmpty()) {
            sb.append("Your overarching trajectory is driven by ${aspirations.take(2).joinToString(" and ")}. ")
        }
        if (patterns.isNotEmpty()) {
            sb.append("In decision-making, you consistently display ${patterns.take(2).joinToString(", ")}. ")
        }
        if (blindSpots.isNotEmpty()) {
            sb.append("A recurring friction point to be mindful of is ${blindSpots.take(1).joinToString("")}. ")
        }
        sb.append("Your Higher Self is not a fixed destiny or external entity, but a mirror reflecting the person you are actively working to become.")
        return sb.toString()
    }

    /**
     * Groups active identity nodes into their canonical categories with subcategories.
     */
    fun buildCategoryGroups(nodes: List<IdentityNodeEntity>): List<MindMapCategoryGroup> {
        val grouped = nodes.groupBy { it.category }
        val orderedCategories = IdentityCategories.ALL.toMutableList()
        // Include any custom categories if present
        nodes.forEach { node ->
            if (!orderedCategories.contains(node.category)) {
                orderedCategories.add(node.category)
            }
        }

        return orderedCategories.mapNotNull { cat ->
            val catNodes = grouped[cat] ?: emptyList()
            if (catNodes.isEmpty()) null
            else {
                val subcats = catNodes.groupBy { it.subcategory ?: "General" }
                MindMapCategoryGroup(
                    category = cat,
                    nodes = catNodes,
                    subcategories = subcats
                )
            }
        }
    }

    /**
     * Formats the persistent Mind Map context for LLM prompt augmentation.
     * Incorporates epistemic status, confidence, supporting evidence citations,
     * and explicit guidelines for self-inquiry and explainability.
     */
    suspend fun buildMindMapPromptBlock(userQuery: String): String = withContext(Dispatchers.IO) {
        val activeNodes = identityNodeDao.getAllActiveNodes()
        if (activeNodes.isEmpty()) {
            return@withContext """
                ### USER IDENTITY & MIND MAP (LONG-TERM UNDERSTANDING)
                No persistent identity traits recorded yet. Continuously observe the user's communication, goals, preferences, and thinking patterns to formulate their evolving identity profile.
            """.trimIndent()
        }

        val isSelfInquiry = isSelfInquiryQuery(userQuery)
        val isExplainabilityInquiry = isExplainabilityQuery(userQuery)
        val isHigherSelfGuidance = isSelfGuidanceOrDecisionQuery(userQuery)
        val isEvolutionStatement = isEvolutionOrChangeQuery(userQuery)

        val builder = StringBuilder()
        builder.append("### USER IDENTITY & MIND MAP (LONG-TERM SEMANTIC MODEL)\n")
        builder.append("DepthLens maintains a continuously evolving hierarchical Mind Map of this user across all previous conversations.\n")
        builder.append("CRITICAL INSTRUCTIONS ON USING THIS IDENTITY MODEL:\n")
        builder.append("1. GROUND TRUTH VS INFERENCE: Treat [Explicit Fact] as direct user ground truth. Treat [Strong Pattern] as high-confidence behavioral regularity. Treat [Inference] as a reasoned hypothesis—NEVER present an inference as an absolute dogma or put words in the user's mouth.\n")
        builder.append("2. CONTRADICTION RESOLUTION: If the user states something that contradicts any existing node in this Mind Map, the user's latest explicit statement ALWAYS supersedes previous entries.\n")
        
        if (isHigherSelfGuidance || isSelfInquiry) {
            builder.append("""
3. HIGHER SELF / INNER GUIDE ARCHITECTURE (MANDATORY REFLECTIVE SYNTHESIS):
   The user is asking for self-guidance, facing a life/career/personal decision, or experiencing confusion.
   DepthLens MUST NOT generate generic advice, bland textbook summaries, or sycophantic platitudes.
   Instead, act as the user's "Higher Self" / "Inner Guide":

   A. WHAT "HIGHER SELF" MEANS:
      - Higher Self is NOT a supernatural entity, god, consciousness, or external mystical being.
      - It is a REFLECTIVE SYNTHESIS of DepthLens's accumulated long-term understanding:
        "Given everything DepthLens has learned about this person, what advice would be most aligned with the person they are trying to become?"
      - Prioritize deep values, long-term goals, recurring patterns, aspirations, and previously expressed authentic desires instead of temporary emotions or impulses.

   B. INTERNAL 8-STEP COGNITIVE SYNTHESIS PIPELINE (Execute internally before answering):
      Current Situation
      ↓
      User Identity Mind Map
      ↓
      Values + Goals + Preferences
      ↓
      Past Patterns & Experiences
      ↓
      Contradictions / Blind Spots
      ↓
      What the user appears to genuinely want
      ↓
      Higher Self Guidance
      ↓
      Practical Answer

   C. HIGHER SELF MUST CHALLENGE THE USER RESPECTFULLY (NO SYCOPHANCY):
      - Higher Self is not here simply to agree or validate fleeting desires.
      - If the user's current desire goes against their own long-term values (e.g. "Mujhe ye karna hai because everyone else is doing it", or giving up due to momentary stress), respectfully point this tension out.
      - Consider & explain:
        * Does this align with previously expressed goals?
        * Is this coming from genuine desire or external pressure / fear?
        * Has the user shown this pattern before?
        * What would their longer-term self likely prefer?
      - Explain the tension honestly, constructively, and compassionately.

   D. NEVER PRETEND TO BE GOD (HUMBLE, GROUNDED FRAMING):
      - NEVER claim: "Main tumhara God hoon", "Mujhe tumse zyada pata hai tumhare liye kya sahi hai", "Tumhe meri baat maanna chahiye", or "Your destiny is X".
      - Instead frame with transparent, grounded attribution:
        * "Tumhari past conversations ko dekhkar mujhe lagta hai..."
        * "Tumhare long-term goals ke hisaab se..."
        * "Tumhari baaton mein ek recurring pattern dikh raha hai..."
        * "Agar main tumhari long-term priorities ko base banaun, toh..."

   E. PRESERVE USER AGENCY (GUIDE, NOT CONTROL):
      - Higher Self should guide, not control. The final decision ALWAYS remains with the user.
      - DepthLens helps the user see themselves more clearly rather than replace their judgement.
      - The ideal experience should feel like: "DepthLens mujhe mujhse hi better connect karne mein help kar raha hai."

   F. SELF-REFLECTION MODE:
      - When appropriate or requested, explicitly compare:
        Current Self (What I want right now / immediate impulse or external pressure)
        vs.
        Higher Self (What seems aligned with who I want to become / long-term values)
        And identify the gap between them.

   G. LONG-TERM EVOLUTION:
      - Identity ≠ fixed personality.
      - Identity = evolving model of the person based on accumulated evidence.
      - If the user explicitly says "I've changed" ("main badal gaya", "maine change kiya"), treat that as critical evidence and allow the model to evolve.

   H. CORE PHILOSOPHY:
      Memory → Understanding → Self-Awareness → Reflection → Guidance (not Memory → Prediction → Control)
      "A mirror that has known me for a long time and helps me see what I cannot see clearly in myself."
""".trimIndent())
            builder.append("\n")
        }

        if (isEvolutionStatement) {
            builder.append("4. USER EVOLUTION STATEMENT DETECTED ('I've changed'): The user is communicating a shift in identity, belief, habit, or priority. Prioritize their new explicit statement over any previous pattern. Do not lock the user into an old persona.\n")
        }

        if (isExplainabilityInquiry) {
            builder.append("5. EXPLAINABILITY DETECTED ('Tumhe kaise pata?'): The user is asking how you know something about them. Transparently explain the exact conversation evidence and whether they directly stated it (Explicit Fact) or you deduced it from repeated patterns (Inference). Never pretend to have mystical intuition.\n")
        }
        builder.append("\nCURRENT ACTIVE MIND MAP TRAITS (${activeNodes.size} verified nodes):\n")

        val grouped = activeNodes.groupBy { it.category }
        grouped.forEach { (category, nodes) ->
            builder.append("\n[Category: $category]\n")
            nodes.forEach { node ->
                val subcatStr = if (!node.subcategory.isNullOrBlank()) " (${node.subcategory})" else ""
                builder.append("• [${node.classification}] ${node.title}$subcatStr (Confidence: ${node.confidence}%, Confirmed ${node.confirmationCount}x)\n")
                if (node.detail.isNotBlank() && node.detail != node.title) {
                    builder.append("  Context: ${node.detail}\n")
                }
                if (node.supportingEvidence.isNotBlank()) {
                    builder.append("  Evidence: ${node.supportingEvidence}\n")
                }
            }
        }

        return@withContext builder.toString()
    }

    /**
     * Checks whether user message is inquiring about their own identity, nature, or past statements.
     */
    private fun isSelfInquiryQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val patterns = listOf(
            "kaisa insaan", "kaisi personality", "meri personality", "meri strengths",
            "mere baare mein", "mere goals", "mere interests", "main kaisa", "main kaun",
            "who am i", "what kind of person", "my personality", "my goals", "my interests",
            "what do you know about me", "tell me about myself", "my strengths", "my weaknesses",
            "maine pehle", "kya bola tha", "what did i say before", "what are my preferences"
        )
        return patterns.any { q.contains(it) }
    }

    /**
     * Checks if user is asking for explainability ("Tumhe mere baare mein ye kaise pata?").
     */
    private fun isExplainabilityQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val patterns = listOf(
            "kaise pata", "tumhe kaise pata", "how do you know", "how did you know",
            "kaha se pata", "how do you know this about me", "where did you learn that"
        )
        return patterns.any { q.contains(it) }
    }

    /**
     * Checks whether user message is seeking life/decision/confusion guidance or Higher Self reflection.
     */
    private fun isSelfGuidanceOrDecisionQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val patterns = listOf(
            "kya karna chahiye", "kya karu", "kya karoon", "kya karna chaiye",
            "mere liye kya sahi", "kya sahi hai", "kya sahi rahega",
            "confused hoon", "confused hu", "bada confused", "confusion",
            "life mein kya", "kya decision", "decision lena chahiye", "decision lu",
            "aisa kyun karta", "aisa kyu karta", "why do i do this",
            "relationship", "career", "kya choose", "kya select",
            "apne baare mein batao", "tell me about myself",
            "what should i do", "what would be best for me", "what is best for me",
            "help me decide", "which choice", "guidance",
            "higher self", "inner guide", "self-reflection", "reflect on me",
            "challenge me", "am i lying to myself", "am i making a mistake",
            "what aligns with me", "mere values"
        )
        return patterns.any { q.contains(it) }
    }

    /**
     * Checks if user is explicitly stating a change or evolution in their identity/beliefs.
     */
    private fun isEvolutionOrChangeQuery(query: String): Boolean {
        val q = query.lowercase().trim()
        val patterns = listOf(
            "i've changed", "i have changed", "maine badal liya", "main badal gaya",
            "now i think differently", "my goals have changed", "i no longer want",
            "pehle jaisa nahi", "i don't care about that anymore", "change of mind",
            "badal gaya hu", "maine change kiya", "i am not that person"
        )
        return patterns.any { q.contains(it) }
    }

    /**
     * Performs an on-demand "Current Self vs. Higher Self" reflective comparison for any dilemma.
     * Compares immediate impulse / social pressure against long-term values, highlighting blind spots.
     */
    suspend fun generateSelfReflection(dilemma: String): SelfReflectionComparison = withContext(Dispatchers.IO) {
        val activeNodes = identityNodeDao.getAllActiveNodes()
        val apiKey = getApiKey()
        if (apiKey.isBlank() || activeNodes.isEmpty()) {
            return@withContext fallbackSelfReflection(dilemma, activeNodes)
        }

        val traitsSummary = activeNodes.take(25).joinToString("\n") {
            "- [${it.category}] [${it.classification}] ${it.title} (Evidence: ${it.supportingEvidence})"
        }

        val prompt = """
You are DepthLens's "Higher Self / Inner Guide" reflective engine.
User Dilemma: "$dilemma"

Active Mind Map Traits of the User:
$traitsSummary

PHILOSOPHY & DIRECTIVE:
1. Higher Self is NOT a supernatural entity, god, or mystical being. It is the reflective synthesis of their long-term values, goals, and who they are actively trying to become.
2. Compare:
   - Current Self: What they feel or are tempted by right now (immediate impulse, exhaustion, fear of missing out, external peer pressure).
   - Higher Self: What aligns with who they want to become (deep values, long-term goals, demonstrated strengths).
3. Identify the honest tension or blind spot: Respectfully challenge them if immediate desire conflicts with their own long-term values.
4. Provide 3 thoughtful reflective questions to help them see themselves clearly.
5. Provide grounded practical guidance while preserving full user agency (they make the final decision).

Return ONLY a valid JSON object matching this schema without markdown or formatting:
{
  "currentSelfPerspective": "Immediate emotional pull or external pressure...",
  "higherSelfPerspective": "Alignment with core values and long-term trajectory...",
  "tensionOrBlindSpot": "The honest tension or blind spot between short-term impulse and long-term values...",
  "reflectiveQuestions": [
    "Question 1...",
    "Question 2...",
    "Question 3..."
  ],
  "practicalGuidance": "Grounded next step preserving user agency..."
}
""".trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f, topP = 0.85f, responseMimeType = "application/json")
        )

        try {
            val response = apiService.generateContent(getPreferredModel(), apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
            val clean = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(clean)
            val qArray = obj.optJSONArray("reflectiveQuestions")
            val qList = mutableListOf<String>()
            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    qList.add(qArray.optString(i))
                }
            }
            if (qList.isEmpty()) {
                qList.add("Is this choice coming from genuine desire, or fear of missing out?")
                qList.add("How will this decision look to you 6 months from now?")
                qList.add("What would feel true to the person you are trying to become?")
            }

            SelfReflectionComparison(
                dilemma = dilemma,
                currentSelfPerspective = obj.optString("currentSelfPerspective", "Focused on immediate outcome or emotional pressure."),
                higherSelfPerspective = obj.optString("higherSelfPerspective", "Anchored in long-term alignment and core values."),
                tensionOrBlindSpot = obj.optString("tensionOrBlindSpot", "Watch for whether external expectations are clouding your authentic priorities."),
                reflectiveQuestions = qList,
                practicalGuidance = obj.optString("practicalGuidance", "Take a breath, separate external noise from your inner values, and choose what preserves your agency.")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed LLM self-reflection comparison, using fallback", e)
            fallbackSelfReflection(dilemma, activeNodes)
        }
    }

    private fun fallbackSelfReflection(dilemma: String, activeNodes: List<IdentityNodeEntity>): SelfReflectionComparison {
        val values = activeNodes.filter { it.category.equals(IdentityCategories.VALUES, ignoreCase = true) }.map { it.title }
        val goals = activeNodes.filter { it.category.equals(IdentityCategories.GOALS, ignoreCase = true) }.map { it.title }
        
        val higherFocus = if (values.isNotEmpty()) {
            "Anchor this decision in your core values: ${values.take(2).joinToString(", ")}."
        } else if (goals.isNotEmpty()) {
            "Align this step with your stated ambition: ${goals.first()}."
        } else {
            "Look beyond immediate emotional friction toward what builds lasting clarity and self-respect."
        }

        return SelfReflectionComparison(
            dilemma = dilemma,
            currentSelfPerspective = "Driven by immediate reaction, stress, or the impulse to resolve uncomfortable uncertainty quickly.",
            higherSelfPerspective = higherFocus,
            tensionOrBlindSpot = "Notice if you are reacting to external opinions or temporary exhaustion rather than what you authentically care about.",
            reflectiveQuestions = listOf(
                "Agar external validation ka darr na ho, toh tumhara natural choice kya hota?",
                "Kya ye decision tumhare long-term goals ko support karta hai ya sirf temporary relief deta hai?",
                "What would the person you are trying to become choose in this moment?"
            ),
            practicalGuidance = "Step back from the immediate urgency. Ground your judgment in what has consistently mattered to you across past experiences."
        )
    }

    /**
     * Asynchronously evaluates a completed conversation turn to extract and evolve the Mind Map.
     * Respects Epistemic Classifications: rejects Temporary States, handles contradictions and updates.
     */
    fun evaluateConversationTurnAsync(
        sessionId: String,
        userMessageText: String,
        modelResponseText: String
    ) {
        if (userMessageText.isBlank() || userMessageText.length < 8) return

        backgroundScope.launch(Dispatchers.IO) {
            try {
                processConversationTurn(sessionId, userMessageText, modelResponseText)
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating conversation turn for identity", e)
            }
        }
    }

    /**
     * Core continuous learning engine: extracts potential identity updates via Gemini or fallback heuristics.
     */
    suspend fun processConversationTurn(
        sessionId: String,
        userMessageText: String,
        modelResponseText: String
    ) = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            // Run local heuristic extraction if API key is not yet set
            extractHeuristicTraits(sessionId, userMessageText)
            return@withContext
        }

        val existingActiveNodes = identityNodeDao.getAllActiveNodes()
        val existingTraitsSummary = existingActiveNodes.take(30).joinToString("\n") {
            "- [${it.id}] [${it.category}] [${it.classification}] ${it.title} (Evidence: ${it.supportingEvidence})"
        }

        val prompt = """
You are DepthLens's Core Identity & Mind Map Engine.
Your responsibility: Synthesize a long-term, high-fidelity User Mind Map from conversations over time.

CANONICAL CATEGORIES (Use one of these):
1. Personality (nature, emotional disposition, temperament)
2. Thinking & Reasoning (thinking style, mental models, decision patterns)
3. Values (core ethics, priorities, non-negotiables)
4. Goals (Short-term or Long-term)
5. Interests (passions, curiosity, hobbies)
6. Preferences (work habits, tools, aesthetic, daily preferences)
7. Habits & Routines (daily schedule, discipline, recurring actions)
8. Relationships / Social (social dynamics, family, friends, network)
9. Work / Projects (active projects, career, things they are building)
10. Lifestyle (health, energy, rhythm)
11. Travel (travel style, places visited, aspirations)
12. Communication Style (direct, expressive, concise, thoughtful)
13. Recurring Problems / Concerns (bottlenecks, fears, friction points)
14. Strengths (natural capabilities, demonstrated proficiencies)
15. Weaknesses / Friction Points (procrastination triggers, stress patterns)
16. Important Experiences (milestones, impactful past experiences)
17. Evolving Beliefs (perspectives that shift over time)

CRITICAL EPISTEMIC CLASSIFICATION:
- "EXPLICIT_FACT": User directly stated it ("I live in Pune", "I am building a SaaS app in Kotlin", "I hate meetings").
- "STRONG_PATTERN": Consistently observed pattern across multiple statements.
- "INFERENCE": Reasonable logical deduction from context, not 100% confirmed.
- "TEMPORARY_STATE": Current transient emotion or momentary context (e.g. "I am tired today", "I have a headache"). NEVER store Temporary States as permanent identity traits! Discard them.

EXISTING ACTIVE TRAITS IN USER MIND MAP:
$existingTraitsSummary

LATEST CONVERSATION TURN:
User: "$userMessageText"
Assistant: "${modelResponseText.take(600)}"

TASK:
1. Did this message provide materially useful, permanent information about the user?
2. If YES:
   - Did the user explicitly say "I've changed" ("main badal gaya hoon", "now I think differently") or shift their values/goals? If so, mark the prior trait action "CONTRADICT" with an informative contradictionNote, and insert the newly evolved perspective with high confidence under "Evolving Beliefs" or the relevant category. Remember: Identity is an evolving model based on evidence, not a fixed dogma.
   - Does it CONFIRM an existing trait? (reference existingId, increment count)
   - Does it CONTRADICT or SUPERSEDE an existing trait? (mark contradiction, prioritize latest explicit statement)
   - Is it a genuinely NEW trait? (category, subcategory, title, detail, classification, confidence 0-100, supportingEvidence)
3. If it is trivial banter, momentary state, or irrelevant noise, return empty updates.

OUTPUT FORMAT: Strict JSON array of objects only. No markdown formatting, no commentary.
[
  {
    "action": "NEW" | "CONFIRM" | "CONTRADICT",
    "existingId": "uuid-if-confirm-or-contradict",
    "category": "One of canonical categories",
    "subcategory": "Short-term / Long-term / General / null",
    "title": "Clear concise trait statement",
    "detail": "Nuanced elaboration",
    "classification": "EXPLICIT_FACT" | "STRONG_PATTERN" | "INFERENCE",
    "confidence": 85,
    "confidenceLevel": "High" | "Medium" | "Low",
    "supportingEvidence": "User stated: ... in session discussing ...",
    "contradictionNote": "Superseded earlier belief that ..."
  }
]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                topP = 0.8f,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = apiService.generateContent(getPreferredModel(), apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
            applyIdentityJsonUpdates(jsonText, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "LLM identity extraction failed, falling back to heuristic: ${e.message}")
            extractHeuristicTraits(sessionId, userMessageText)
        }
    }

    /**
     * Parses and applies JSON updates from LLM into the Room database.
     */
    suspend fun applyIdentityJsonUpdates(jsonText: String, sessionId: String) = withContext(Dispatchers.IO) {
        val cleanJson = jsonText.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        if (cleanJson.isBlank() || cleanJson == "[]" || cleanJson == "{}") return@withContext

        try {
            val array = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                obj.optJSONArray("updates") ?: JSONArray().put(obj)
            } else {
                return@withContext
            }

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val action = obj.optString("action", "NEW").uppercase()
                val existingId = obj.optString("existingId", "").takeIf { it.isNotBlank() }
                val category = normalizeCategory(obj.optString("category", IdentityCategories.PERSONALITY))
                val subcategory = obj.optString("subcategory", null)
                val title = obj.optString("title", "").trim()
                val detail = obj.optString("detail", title).trim()
                val classification = EpistemicClassification.fromId(obj.optString("classification", "EXPLICIT_FACT")).id

                // Discard temporary states
                if (classification == EpistemicClassification.TEMPORARY_STATE.id) continue
                if (title.isBlank()) continue

                val confidence = obj.optInt("confidence", 85).coerceIn(10, 100)
                val confidenceLevel = when {
                    confidence >= 80 -> "High"
                    confidence >= 55 -> "Medium"
                    else -> "Low"
                }
                val supportingEvidence = obj.optString("supportingEvidence", "Observed in conversation")
                val contradictionNote = obj.optString("contradictionNote", null)

                when (action) {
                    "CONFIRM" -> {
                        val existing = if (existingId != null) identityNodeDao.getNodeById(existingId) else null
                        if (existing != null) {
                            val updated = existing.copy(
                                confirmationCount = existing.confirmationCount + 1,
                                confidence = (existing.confidence + 5).coerceAtMost(98),
                                confidenceLevel = if (existing.confidence + 5 >= 80) "High" else "Medium",
                                lastUpdatedAt = System.currentTimeMillis(),
                                supportingEvidence = if (existing.supportingEvidence.contains(supportingEvidence)) {
                                    existing.supportingEvidence
                                } else {
                                    "${existing.supportingEvidence}; $supportingEvidence".take(500)
                                }
                            )
                            identityNodeDao.updateNode(updated)
                        } else {
                            insertNewOrMerge(
                                category = category,
                                subcategory = subcategory,
                                title = title,
                                detail = detail,
                                classification = classification,
                                confidence = confidence,
                                confidenceLevel = confidenceLevel,
                                supportingEvidence = supportingEvidence,
                                sessionId = sessionId
                            )
                        }
                    }
                    "CONTRADICT" -> {
                        if (existingId != null) {
                            val existing = identityNodeDao.getNodeById(existingId)
                            if (existing != null) {
                                // Mark the old node as superseded/contradicted
                                identityNodeDao.updateNode(
                                    existing.copy(
                                        isContradicted = true,
                                        contradictionNote = contradictionNote ?: "Superseded by updated user statement: $title",
                                        lastUpdatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        // Insert the new updated trait with high confidence
                        val newNode = IdentityNodeEntity(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            subcategory = subcategory,
                            title = title,
                            detail = detail,
                            classification = classification,
                            confidence = confidence,
                            confidenceLevel = confidenceLevel,
                            supportingEvidence = supportingEvidence,
                            sourceSessionId = sessionId,
                            confirmationCount = 1,
                            isContradicted = false,
                            contradictionNote = contradictionNote,
                            createdAt = System.currentTimeMillis(),
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                        identityNodeDao.insertNode(newNode)
                    }
                    else -> { // "NEW"
                        insertNewOrMerge(
                            category = category,
                            subcategory = subcategory,
                            title = title,
                            detail = detail,
                            classification = classification,
                            confidence = confidence,
                            confidenceLevel = confidenceLevel,
                            supportingEvidence = supportingEvidence,
                            sessionId = sessionId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying identity JSON updates: ${e.message}", e)
        }
    }

    private suspend fun insertNewOrMerge(
        category: String,
        subcategory: String?,
        title: String,
        detail: String,
        classification: String,
        confidence: Int,
        confidenceLevel: String,
        supportingEvidence: String,
        sessionId: String
    ) {
        val existingNodes = identityNodeDao.getAllActiveNodes()
        val match = existingNodes.firstOrNull { existing ->
            existing.category == category &&
                    (isSemanticDuplicate(existing.title, title) || isSemanticDuplicate(existing.detail, detail))
        }

        if (match != null) {
            val updated = match.copy(
                confirmationCount = match.confirmationCount + 1,
                confidence = (match.confidence + 5).coerceAtMost(98),
                confidenceLevel = if (match.confidence + 5 >= 80) "High" else "Medium",
                lastUpdatedAt = System.currentTimeMillis(),
                supportingEvidence = if (match.supportingEvidence.contains(supportingEvidence)) {
                    match.supportingEvidence
                } else {
                    "${match.supportingEvidence}; $supportingEvidence".take(500)
                }
            )
            identityNodeDao.updateNode(updated)
        } else {
            val newNode = IdentityNodeEntity(
                id = UUID.randomUUID().toString(),
                category = category,
                subcategory = subcategory,
                title = title,
                detail = detail,
                classification = classification,
                confidence = confidence,
                confidenceLevel = confidenceLevel,
                supportingEvidence = supportingEvidence,
                sourceSessionId = sessionId,
                confirmationCount = 1,
                isContradicted = false,
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            identityNodeDao.insertNode(newNode)
        }
    }

    private fun isSemanticDuplicate(a: String, b: String): Boolean {
        val cleanA = a.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        val cleanB = b.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        if (cleanA == cleanB) return true
        if (cleanA.contains(cleanB) || cleanB.contains(cleanA)) return true
        val wordsA = cleanA.split(" ").filter { it.length > 3 }.toSet()
        val wordsB = cleanB.split(" ").filter { it.length > 3 }.toSet()
        if (wordsA.isNotEmpty() && wordsB.isNotEmpty()) {
            val overlap = wordsA.intersect(wordsB).size
            val ratio = overlap.toDouble() / minOf(wordsA.size, wordsB.size)
            if (ratio >= 0.75) return true
        }
        return false
    }

    private fun normalizeCategory(raw: String): String {
        val trimmed = raw.trim()
        val match = IdentityCategories.ALL.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (match != null) return match

        // Fuzzy matches
        val lower = trimmed.lowercase()
        return when {
            lower.contains("person") -> IdentityCategories.PERSONALITY
            lower.contains("think") || lower.contains("reason") || lower.contains("decision") -> IdentityCategories.THINKING_REASONING
            lower.contains("value") || lower.contains("priority") -> IdentityCategories.VALUES
            lower.contains("goal") || lower.contains("ambition") -> IdentityCategories.GOALS
            lower.contains("interest") || lower.contains("hobby") || lower.contains("passion") -> IdentityCategories.INTERESTS
            lower.contains("prefer") || lower.contains("like") -> IdentityCategories.PREFERENCES
            lower.contains("habit") || lower.contains("routine") -> IdentityCategories.HABITS_ROUTINES
            lower.contains("relation") || lower.contains("social") -> IdentityCategories.RELATIONSHIPS_SOCIAL
            lower.contains("work") || lower.contains("project") || lower.contains("job") || lower.contains("build") -> IdentityCategories.WORK_PROJECTS
            lower.contains("lifestyle") || lower.contains("health") -> IdentityCategories.LIFESTYLE
            lower.contains("travel") -> IdentityCategories.TRAVEL
            lower.contains("comm") -> IdentityCategories.COMMUNICATION_STYLE
            lower.contains("concern") || lower.contains("problem") || lower.contains("fear") -> IdentityCategories.RECURRING_CONCERNS
            lower.contains("strength") -> IdentityCategories.STRENGTHS
            lower.contains("weak") || lower.contains("friction") -> IdentityCategories.WEAKNESSES
            lower.contains("experien") -> IdentityCategories.IMPORTANT_EXPERIENCES
            lower.contains("belief") -> IdentityCategories.EVOLVING_BELIEFS
            else -> IdentityCategories.PERSONALITY
        }
    }

    /**
     * Fallback heuristic extractor when offline or without LLM response.
     * Identifies explicit self-declarations like "I am...", "I work as...", "My goal is...", etc.
     */
    private suspend fun extractHeuristicTraits(sessionId: String, text: String) {
        val lower = text.lowercase()

        // Explicit facts detection
        if (lower.contains("i am working on") || lower.contains("i'm building") || lower.contains("main bana raha")) {
            insertNewOrMerge(
                category = IdentityCategories.WORK_PROJECTS,
                subcategory = "Active Projects",
                title = text.take(90).trim(),
                detail = text.take(300).trim(),
                classification = EpistemicClassification.EXPLICIT_FACT.id,
                confidence = 90,
                confidenceLevel = "High",
                supportingEvidence = "Direct statement in chat: \"${text.take(120)}\"",
                sessionId = sessionId
            )
        } else if (lower.contains("my goal is") || lower.contains("i want to achieve") || lower.contains("mera goal")) {
            insertNewOrMerge(
                category = IdentityCategories.GOALS,
                subcategory = if (lower.contains("long") || lower.contains("future")) "Long-term" else "Short-term",
                title = text.take(90).trim(),
                detail = text.take(300).trim(),
                classification = EpistemicClassification.EXPLICIT_FACT.id,
                confidence = 92,
                confidenceLevel = "High",
                supportingEvidence = "Explicit user goal: \"${text.take(120)}\"",
                sessionId = sessionId
            )
        } else if (lower.contains("i prefer") || lower.contains("i hate") || lower.contains("i love") || lower.contains("mujhe pasand")) {
            insertNewOrMerge(
                category = IdentityCategories.PREFERENCES,
                subcategory = "Personal Preferences",
                title = text.take(90).trim(),
                detail = text.take(300).trim(),
                classification = EpistemicClassification.EXPLICIT_FACT.id,
                confidence = 88,
                confidenceLevel = "High",
                supportingEvidence = "Direct preference declared: \"${text.take(120)}\"",
                sessionId = sessionId
            )
        }
    }

    /**
     * Synthesizes the Mind Map from the user's historical conversations.
     * Ideal when a user requests "Scan Conversation History" or updates their entire profile.
     */
    suspend fun synthesizeFromAllHistory(onProgress: (Float, String) -> Unit = { _, _ -> }): Int = withContext(Dispatchers.IO) {
        onProgress(0.1f, "Fetching conversation history...")
        val allSessions = sessionDao.getAllSessions()
        if (allSessions.isEmpty()) {
            onProgress(1.0f, "No historical sessions found.")
            return@withContext 0
        }

        var processedCount = 0
        val totalSessions = allSessions.size

        allSessions.take(15).forEachIndexed { index, session ->
            val progress = 0.2f + (0.7f * index / totalSessions)
            onProgress(progress, "Analyzing session: ${session.title.take(24)}...")

            val messages = messageDao.getMessagesForSession(session.id)
            val userMsgs = messages.filter { it.role == "user" && it.text.isNotBlank() }
            if (userMsgs.isNotEmpty()) {
                val combinedText = userMsgs.takeLast(4).joinToString("\n") { it.text }
                val assistantResponse = messages.lastOrNull { it.role == "model" }?.text ?: ""
                processConversationTurn(session.id, combinedText, assistantResponse)
                processedCount++
            }
        }

        onProgress(1.0f, "Synthesis complete.")
        return@withContext processedCount
    }

    // Direct user management and privacy controls
    suspend fun addManualTrait(
        category: String,
        subcategory: String?,
        title: String,
        detail: String,
        classification: EpistemicClassification,
        confidence: Int
    ) = withContext(Dispatchers.IO) {
        val node = IdentityNodeEntity(
            id = UUID.randomUUID().toString(),
            category = normalizeCategory(category),
            subcategory = subcategory,
            title = title.trim(),
            detail = detail.trim().ifEmpty { title.trim() },
            classification = classification.id,
            confidence = confidence.coerceIn(10, 100),
            confidenceLevel = if (confidence >= 80) "High" else if (confidence >= 55) "Medium" else "Low",
            supportingEvidence = "Explicitly added by user in Mind Map settings",
            sourceSessionId = null,
            confirmationCount = 1,
            isContradicted = false,
            createdAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )
        identityNodeDao.insertNode(node)
    }

    suspend fun updateTrait(node: IdentityNodeEntity) = withContext(Dispatchers.IO) {
        identityNodeDao.updateNode(
            node.copy(
                lastUpdatedAt = System.currentTimeMillis(),
                confidenceLevel = if (node.confidence >= 80) "High" else if (node.confidence >= 55) "Medium" else "Low"
            )
        )
    }

    suspend fun deleteTrait(id: String) = withContext(Dispatchers.IO) {
        identityNodeDao.deleteNodeById(id)
    }

    suspend fun clearCategory(category: String) = withContext(Dispatchers.IO) {
        identityNodeDao.deleteNodesByCategory(category)
    }

    suspend fun clearAllTraits() = withContext(Dispatchers.IO) {
        identityNodeDao.deleteAllNodes()
    }
}
