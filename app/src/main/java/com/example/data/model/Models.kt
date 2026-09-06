package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Immutable
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val lastUpdatedAt: Long,
    val isPinned: Boolean = false
)

@Immutable
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user" or "model"
    val text: String,
    val imageUri: String? = null, // Path to attached image if any
    val timestamp: Long,
    val replyToMessageId: String? = null,
    val selectedText: String? = null
)

@Immutable
@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val messageId: String,
    val mimeType: String,
    val localUri: String,
    val remoteUrl: String? = null,
    val storagePath: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String,
    val uploadStatus: String = "PENDING"
)

@Immutable
@Entity(tableName = "memory_insights")
data class MemoryInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Pattern", "Goal", "Theme", "Insight", "Driver"
    val content: String,
    val timestamp: Long
)

@Immutable
@Entity(tableName = "archived_insights")
data class ArchivedInsightEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val query: String,
    val introTitle: String,
    val jsonContent: String, // Raw JSON string representation of the layers & details
    val timestamp: Long
)

// UI and Business models below
@Immutable
data class ProbabilityMetrics(
    val confidence: Int = 78,
    val likelihood: Int = 65,
    val risk: Int = 42,
    val opportunity: Int = 71
)

@Immutable
data class ProbabilityAssessment(
    val likelihood: Int = 65,
    val confidence: String = "High",
    val reasoningFactors: List<String> = emptyList()
)

@Immutable
data class FuturePathway(
    val title: String,
    val probability: Int,
    val description: String = "",
    val drivers: String = "",
    val risks: String = "",
    val opportunities: String = ""
)

@Immutable
data class TimelineForecast(
    val shortTermProb: Int = 80,
    val shortTermDesc: String = "",
    val midTermProb: Int = 60,
    val midTermDesc: String = "",
    val longTermProb: Int = 40,
    val longTermDesc: String = "",
    val explanation: String = ""
)

@Immutable
data class DecisionImpact(
    val statusQuoProb: Int = 80,
    val statusQuoDesc: String = "",
    val actionProb: Int = 40,
    val actionDesc: String = "",
    val comparison: String = "",
    val risks: String = "",
    val benefits: String = "",
    val tradeoffs: String = ""
)

@Immutable
data class ForecastSummary(
    val mostLikelyOutcome: Int = 75,
    val keyRisk: Int = 60,
    val opportunityWindow: Int = 50,
    val predictionConfidence: String = "High"
)

fun sanitizeRawModelText(raw: String): String {
    if (raw.isBlank()) return ""
    var text = raw
    text = text.replace(Regex("""(?is)<confidence>.*?</confidence>"""), "")
    text = text.replace(Regex("""(?is)</?confidence>"""), "")
    val metaLinePatterns = listOf(
        Regex("""(?i)^\s*[-*•+]?\s*(?:\*\*|\[)?\s*confidence\s*(?:level|score|rating)?\s*[:=\-].*"""),
        Regex("""(?i)^\s*[-*•+]?\s*(?:\*\*|\[)?\s*prediction\s*confidence(?:\s*rating)?\s*[:=\-].*"""),
        Regex("""(?i)^\s*(?:\*\*|\[)?\s*(?:high|medium|low|critical|moderate)\s*confidence(?:\*\*|\])?\.?\s*$"""),
        Regex("""(?i)^\s*(?:\*\*|\[)?\s*confidence\s*level\s*[:=\-]?\s*(?:high|medium|low|critical|moderate)?(?:\*\*|\])?\.?\s*$""")
    )
    text = text.lines().filterNot { line ->
        val trimmed = line.trim()
        metaLinePatterns.any { pattern -> trimmed.matches(pattern) }
    }.joinToString("\n")
    text = text.replace(Regex("""(?i)\b(?:confidence\s*level|confidence\s*score|prediction\s*confidence)\s*[:=\-]\s*(?:high|medium|low|critical|moderate)[^.\n\r]*\.?"""), "")
    return text.trim()
}

fun sanitizeCleanResponseText(text: String): String {
    if (text.isBlank()) return ""
    var cleaned = text

    // 1. Strip XML tags including metadata tags and their contents
    cleaned = cleaned.replace(Regex("""(?is)<confidence>.*?</confidence>"""), "")
    cleaned = cleaned.replace(Regex("""(?is)<probability_metrics>.*?</probability_metrics>"""), "")
    cleaned = cleaned.replace(Regex("""(?is)<probability_assessment>.*?</probability_assessment>"""), "")
    cleaned = cleaned.replace(Regex("""(?is)<forecast_summary>.*?</forecast_summary>"""), "")
    cleaned = cleaned.replace(Regex("""<[^>]+>"""), "")

    // 2. Filter out standalone lines that contain confidence levels, scores, or metadata junk
    val metaLinePatterns = listOf(
        Regex("""(?i)^\s*[-*•+]?\s*(?:\*\*|\[)?\s*confidence\s*(?:level|score|rating)?\s*[:=\-].*"""),
        Regex("""(?i)^\s*[-*•+]?\s*(?:\*\*|\[)?\s*prediction\s*confidence(?:\s*rating)?\s*[:=\-].*"""),
        Regex("""(?i)^\s*[-*•+]?\s*(?:\*\*|\[)?\s*(?:likelihood|probability(?:\s*metrics)?|importance|priority|severity|certainty)\s*[:=\-]\s*(?:high|medium|low|critical|moderate|\d+%).*"""),
        Regex("""(?i)^\s*(?:\*\*|\[)?\s*(?:high|medium|low|critical|moderate)\s*confidence(?:\*\*|\])?\.?\s*$"""),
        Regex("""(?i)^\s*(?:\*\*|\[)?\s*confidence\s*level\s*[:=\-]?\s*(?:high|medium|low|critical|moderate)?(?:\*\*|\])?\.?\s*$""")
    )

    cleaned = cleaned.lines().filterNot { line ->
        val trimmed = line.trim()
        metaLinePatterns.any { pattern -> trimmed.matches(pattern) }
    }.joinToString("\n")

    // 3. Remove inline remnants of "Confidence Level : High" or similar phrases
    cleaned = cleaned.replace(Regex("""(?i)\b(?:confidence\s*level|confidence\s*score|prediction\s*confidence)\s*[:=\-]\s*(?:high|medium|low|critical|moderate)[^.\n\r]*\.?"""), "")
    cleaned = cleaned.replace(Regex("""(?i)\bconfidence\s*[:=\-]\s*(?:high|medium|low|critical|moderate)\b\.?"""), "")

    // 4. Clean leaked markdown metadata tags at the beginning of bullet points or sentences
    val leakedMetadataRegex = Regex(
        "^(?:(\\s*[-*+•]\\s*|\\s*\\d+\\.\\s*))?\\*?\\*?(?:(?:importance|emphasis|priority|confidence\\s*level|confidence|severity|level|reasoning)\\s*[:=\\-]\\s*)?(?:high|medium|low|critical)\\*?\\*?\\s*\\.?\\s*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )
    cleaned = cleaned.replace(leakedMetadataRegex, "$1")
        .replace(Regex("^(?:high|medium|low|critical)\\.\\s*", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), "")

    // 5. Clean excessive newlines and whitespace
    return cleaned.replace(Regex("""\n{3,}"""), "\n\n").trim()
}

@Immutable
data class ParsedResponse(
    val introduction: String = "",
    val executiveSummary: String? = null,
    val deepSynthesis: String? = null,
    val depthLayers: List<DepthLayerInsight> = emptyList(),
    val rootCauseReport: RootCauseReport? = null,
    val humanDrivers: HumanDriversReport? = null,
    val futureScenarios: List<FutureScenario> = emptyList(),
    val confidence: String? = null, // "Low" / "Medium" / "High"
    val suggestedQuestions: List<String> = emptyList(),
    val explorationPaths: List<String> = emptyList(),
    val probabilityMetrics: ProbabilityMetrics? = null,
    val probabilityAssessment: ProbabilityAssessment? = null,
    val futurePathways: List<FuturePathway> = emptyList(),
    val timelineForecast: TimelineForecast? = null,
    val decisionImpact: DecisionImpact? = null,
    val forecastSummary: ForecastSummary? = null,
    val isFollowUp: Boolean = false
) {
    fun exportText(): String {
        val cleanIntro = sanitizeCleanResponseText(introduction.trim())
        if (cleanIntro.isNotBlank()) {
            return cleanIntro
        }
        val summary = executiveSummary?.trim()
        if (!summary.isNullOrBlank()) {
            val cleanSummary = sanitizeCleanResponseText(summary)
            if (cleanSummary.isNotBlank()) return cleanSummary
        }
        val synthesis = deepSynthesis?.trim()
        if (!synthesis.isNullOrBlank()) {
            val cleanSynth = sanitizeCleanResponseText(synthesis)
            if (cleanSynth.isNotBlank()) return cleanSynth
        }
        return sanitizeCleanResponseText(introduction)
    }
}

@Immutable
data class DepthLayerInsight(
    val layerNumber: Int,
    val layerName: String,
    val description: String
)

@Immutable
data class RootCauseReport(
    val symptom: String = "",
    val immediateCause: String = "",
    val underlyingCause: String = "",
    val deeperCause: String = "",
    val rootCauseEstimate: String = "",
    val confidenceLevel: String = "",
    val supportingEvidence: String = "",
    val alternativeExplanation: String = ""
)

@Immutable
data class HumanDriversReport(
    val surfaceIntention: String = "",
    val emotionalDriver: String = "",
    val needDriver: String = "",
    val fearDriver: String = "",
    val incentiveDriver: String = "",
    val identityDriver: String = "",
    val hiddenMotives: String = "",
    val rawContent: String = ""
)

@Immutable
data class FutureScenario(
    val codeName: String, // e.g. "Scenario A"
    val displayName: String, // e.g. "Most Likely Path"
    val probability: Int, // e.g. 60
    val impactText: String,
    val earlyWarningSigns: List<String> = emptyList()
)

fun ParsedResponse.toJsonString(query: String): String {
    val obj = org.json.JSONObject()
    obj.put("query", query)
    obj.put("introduction", introduction)
    obj.put("executiveSummary", executiveSummary ?: "")
    obj.put("confidence", confidence ?: "High")
    
    val layersArray = org.json.JSONArray()
    depthLayers.forEach { layer ->
        val layerObj = org.json.JSONObject()
        layerObj.put("layerNumber", layer.layerNumber)
        layerObj.put("layerName", layer.layerName)
        layerObj.put("description", layer.description)
        layersArray.put(layerObj)
    }
    obj.put("layers", layersArray)
    
    if (rootCauseReport != null) {
        val rcObj = org.json.JSONObject()
        rcObj.put("symptom", rootCauseReport.symptom)
        rcObj.put("immediateCause", rootCauseReport.immediateCause)
        rcObj.put("underlyingCause", rootCauseReport.underlyingCause)
        rcObj.put("deeperCause", rootCauseReport.deeperCause)
        obj.put("rootCause", rcObj)
    }
    
    return obj.toString()
}

fun parseArchivedJson(jsonStr: String): ParsedResponse {
    try {
        val obj = org.json.JSONObject(jsonStr)
        val introduction = obj.optString("introduction", "")
        val executiveSummary = obj.optString("executiveSummary", "").ifEmpty { null }
        val confidence = obj.optString("confidence", "High")
        
        val depthLayers = mutableListOf<DepthLayerInsight>()
        val layersArray = obj.optJSONArray("layers")
        if (layersArray != null) {
            for (i in 0 until layersArray.length()) {
                val layerObj = layersArray.getJSONObject(i)
                depthLayers.add(
                    DepthLayerInsight(
                        layerNumber = layerObj.optInt("layerNumber", 1),
                        layerName = layerObj.optString("layerName", ""),
                        description = layerObj.optString("description", "")
                    )
                )
            }
        }
        
        var rootCauseReport: RootCauseReport? = null
        val rcObj = obj.optJSONObject("rootCause")
        if (rcObj != null) {
            rootCauseReport = RootCauseReport(
                symptom = rcObj.optString("symptom", ""),
                immediateCause = rcObj.optString("immediateCause", ""),
                underlyingCause = rcObj.optString("underlyingCause", ""),
                deeperCause = rcObj.optString("deeperCause", "")
            )
        }
        
        return ParsedResponse(
            introduction = introduction,
            executiveSummary = executiveSummary,
            depthLayers = depthLayers,
            rootCauseReport = rootCauseReport,
            confidence = confidence
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return ParsedResponse()
    }
}

@Immutable
data class ExportMessage(
    val role: String, // "user" or "model"
    val text: String, // Cleaned, final rendered visible text
    val imageUri: String? = null,
    val tables: List<String> = emptyList(),
    val lists: List<String> = emptyList(),
    val codeBlocks: List<String> = emptyList(),
    val charts: List<String> = emptyList()
)

@Immutable
data class ExportConversation(
    val title: String,
    val messages: List<ExportMessage>
)

