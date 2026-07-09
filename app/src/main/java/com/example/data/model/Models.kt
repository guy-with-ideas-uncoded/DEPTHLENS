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
    val thumbnailUrl: String? = null,
    val fileName: String
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
        val builder = java.lang.StringBuilder()
        builder.append("=== DEPTHLENS STRATEGIC RECONSTRUCTION REPORT ===\n\n")
        
        fun sanitize(input: String): String {
            var text = input.trim()
            text = text.replace(Regex("""<questions>[\s\S]*?</questions>""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""<exploration>[\s\S]*?</exploration>""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""<memory_insight>[\s\S]*?</memory_insight>""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""System Instructions[\s\S]*?(?=\n\n|\z)""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""SYSTEM_PROMPT[\s\S]*?(?=\n\n|\z)""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""Developer Config[\s\S]*?(?=\n\n|\z)""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""<[^>]+>"""), "")
            text = text.replace(Regex("""applicationId\s*=[\s\S]*?(?=\n|\z)""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""BuildConfig[\s\S]*?(?=\n|\z)""", RegexOption.IGNORE_CASE), "")
            // Strip markdown formatting tokens
            text = text.replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            text = text.replace(Regex("""__(.*?)__"""), "$1")
            text = text.replace(Regex("""\*(.*?)\*"""), "$1")
            text = text.replace(Regex("""_(.*?)_"""), "$1")
            text = text.replace(Regex("""```[\s\S]*?```""")) { it.value.replace("```", "") }
            text = text.replace(Regex("""`([^`]+)`"""), "$1")
            text = text.replace(Regex("""^#+\s+""", RegexOption.MULTILINE), "")
            return text.trim()
        }

        val cleanIntro = sanitize(introduction)
        if (cleanIntro.isNotBlank()) {
            builder.append("INTRODUCTION\n")
            builder.append(cleanIntro).append("\n\n")
        }
        
        val summary = executiveSummary?.let { sanitize(it) }
        if (!summary.isNullOrBlank()) {
            builder.append("EXECUTIVE SUMMARY\n")
            builder.append(summary).append("\n\n")
        }

        val ds = deepSynthesis?.let { sanitize(it) }
        if (!ds.isNullOrBlank()) {
            builder.append("DEEP SYNTHESIS\n")
            builder.append(ds).append("\n\n")
        }

        if (depthLayers.isNotEmpty()) {
            builder.append("DEPTH LAYERS OF REALITY\n")
            depthLayers.forEach { layer ->
                builder.append("Layer ").append(layer.layerNumber).append(" - ").append(layer.layerName).append(": ").append(sanitize(layer.description)).append("\n")
            }
            builder.append("\n")
        }
        
        val rcr = rootCauseReport
        if (rcr != null) {
            builder.append("ROOT CAUSE REPORT (THE 'WHY')\n")
            builder.append("Surface Cause: ").append(sanitize(rcr.symptom)).append("\n")
            builder.append("Immediate Cause: ").append(sanitize(rcr.immediateCause)).append("\n")
            builder.append("Underlying Cause: ").append(sanitize(rcr.underlyingCause)).append("\n")
            builder.append("Deeper Cause: ").append(sanitize(rcr.deeperCause)).append("\n")
            builder.append("Root Cause Conclusion: ").append(sanitize(rcr.rootCauseEstimate)).append("\n")
            builder.append("Supporting Evidence: ").append(sanitize(rcr.supportingEvidence)).append("\n")
            if (rcr.alternativeExplanation.isNotBlank()) {
                builder.append("Alternative Explanation: ").append(sanitize(rcr.alternativeExplanation)).append("\n")
            }
            builder.append("\n")
        }
        
        val hd = humanDrivers
        if (hd != null) {
            builder.append("HUMAN DRIVERS (PSYCHOMOTIVE ANATOMY)\n")
            builder.append("Surface Intention: ").append(sanitize(hd.surfaceIntention)).append("\n")
            builder.append("Emotional Driver: ").append(sanitize(hd.emotionalDriver)).append("\n")
            builder.append("Core Need: ").append(sanitize(hd.needDriver)).append("\n")
            builder.append("Core Fear: ").append(sanitize(hd.fearDriver)).append("\n")
            builder.append("Incentives: ").append(sanitize(hd.incentiveDriver)).append("\n")
            builder.append("Identity Alignment: ").append(sanitize(hd.identityDriver)).append("\n")
            builder.append("Hidden Motives: ").append(sanitize(hd.hiddenMotives)).append("\n")
            builder.append("\n")
        }
        
        if (futureScenarios.isNotEmpty()) {
            builder.append("FUTURE SCENARIOS & PROBABILITIES\n")
            futureScenarios.forEach { scenario ->
                builder.append("- ").append(scenario.codeName.uppercase()).append(" - ").append(scenario.displayName).append(" (Prob: ").append(scenario.probability).append("%)\n")
                builder.append("  Outcome: ").append(sanitize(scenario.impactText)).append("\n")
                if (scenario.earlyWarningSigns.isNotEmpty()) {
                    builder.append("  Early Warning Signs:\n")
                    scenario.earlyWarningSigns.forEach { sign ->
                        builder.append("    * ").append(sanitize(sign)).append("\n")
                    }
                }
            }
            builder.append("\n")
        }
        
        val conf = confidence
        if (!conf.isNullOrBlank()) {
            builder.append("Confidence Level: ").append(sanitize(conf)).append("\n")
        }
        
        probabilityAssessment?.let { pa ->
            builder.append("\nPROBABILITY ASSESSMENT\n")
            builder.append("Confidence: ").append(sanitize(pa.confidence)).append("\n")
            builder.append("Overall Likelihood: ").append(pa.likelihood).append("%\n")
            if (pa.reasoningFactors.isNotEmpty()) {
                builder.append("Reasoning Factors:\n")
                pa.reasoningFactors.forEach { factor ->
                    builder.append("  * ").append(sanitize(factor)).append("\n")
                }
            }
        }
        
        probabilityMetrics?.let { pm ->
            builder.append("\nPROBABILITY METRICS\n")
            builder.append("Confidence: ").append(pm.confidence).append("%\n")
            builder.append("Likelihood: ").append(pm.likelihood).append("%\n")
            builder.append("Risk: ").append(pm.risk).append("%\n")
            builder.append("Opportunity: ").append(pm.opportunity).append("%\n")
        }
        
        timelineForecast?.let { tf ->
            builder.append("\nTIMELINE FORECAST\n")
            builder.append("Short Term (").append(tf.shortTermProb).append("%): ").append(sanitize(tf.shortTermDesc)).append("\n")
            builder.append("Mid Term (").append(tf.midTermProb).append("%): ").append(sanitize(tf.midTermDesc)).append("\n")
            builder.append("Long Term (").append(tf.longTermProb).append("%): ").append(sanitize(tf.longTermDesc)).append("\n")
            if (tf.explanation.isNotBlank()) builder.append("Explanation: ").append(sanitize(tf.explanation)).append("\n")
        }
        
        decisionImpact?.let { di ->
            builder.append("\nDECISION IMPACT ANALYSIS\n")
            builder.append("Status Quo (").append(di.statusQuoProb).append("%): ").append(sanitize(di.statusQuoDesc)).append("\n")
            builder.append("Action (").append(di.actionProb).append("%): ").append(sanitize(di.actionDesc)).append("\n")
            builder.append("Comparison: ").append(sanitize(di.comparison)).append("\n")
            builder.append("Risks: ").append(sanitize(di.risks)).append("\n")
            builder.append("Benefits: ").append(sanitize(di.benefits)).append("\n")
            builder.append("Tradeoffs: ").append(sanitize(di.tradeoffs)).append("\n")
        }
        
        forecastSummary?.let { fs ->
            builder.append("\nFORECAST SUMMARY\n")
            builder.append("Most Likely Outcome: ").append(fs.mostLikelyOutcome).append("%\n")
            builder.append("Key Risk: ").append(fs.keyRisk).append("%\n")
            builder.append("Opportunity Window: ").append(fs.opportunityWindow).append("%\n")
            builder.append("Prediction Confidence: ").append(sanitize(fs.predictionConfidence)).append("\n")
        }

        if (futurePathways.isNotEmpty()) {
            builder.append("\nFUTURE PATHWAYS\n")
            futurePathways.forEach { pathway ->
                builder.append("- ").append(sanitize(pathway.title)).append(" (Prob: ").append(pathway.probability).append("%)\n")
                if (pathway.description.isNotBlank()) builder.append("  Description: ").append(sanitize(pathway.description)).append("\n")
                if (pathway.drivers.isNotBlank()) builder.append("  Drivers: ").append(sanitize(pathway.drivers)).append("\n")
                if (pathway.risks.isNotBlank()) builder.append("  Risks: ").append(sanitize(pathway.risks)).append("\n")
                if (pathway.opportunities.isNotBlank()) builder.append("  Opportunities: ").append(sanitize(pathway.opportunities)).append("\n")
            }
        }
        
        builder.append("\n=================================================")
        return builder.toString()
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
