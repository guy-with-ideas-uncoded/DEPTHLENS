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
        
        // 1. Replicate cleanIntroDisplay logic from the UI
        val summaryText = executiveSummary ?: ""
        var intro = introduction.trim()
        
        if (summaryText.isNotBlank()) {
            if (intro == summaryText) {
                intro = ""
            } else if (intro.contains(summaryText)) {
                intro = intro.replace(summaryText, "").trim()
            } else {
                val paragraphs = intro.split("\n\n")
                val filtered = paragraphs.filter { p ->
                    val pClean = p.trim()
                    pClean.isNotBlank() && !summaryText.contains(pClean) && !pClean.contains(summaryText)
                }
                intro = filtered.joinToString("\n\n").trim()
            }
        }
        
        // 2. Strip leaked markdown metadata just like the UI
        val leakedMetadataRegex = Regex(
            "^(?:(\\s*[-*+•]\\s*|\\s*\\d+\\.\\s*))?\\*?\\*?(?:(?:importance|emphasis|priority|confidence|severity|level|reasoning)\\s*:\\s*)?(?:high|medium|low|critical)\\*?\\*?\\s*\\.?\\s*",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        )
        intro = intro.replace(leakedMetadataRegex, "$1").replace(Regex("^(?:high|medium|low|critical)\\\\.\\s*", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)), "").trim()
        
        // 3. AGGRESSIVELY strip ANY remaining XML tags to guarantee they never leak into the PDF
        // This is critical to prevent the raw backend payload from appearing in the exported PDF.
        val xmlTagRegex = Regex("<[^>]+>")
        intro = intro.replace(xmlTagRegex, "").trim()
        
        val isComplex = executiveSummary != null || rootCauseReport != null || depthLayers.isNotEmpty() || humanDrivers != null || futureScenarios.isNotEmpty()
        
        if (isComplex) {
            builder.append("=== DEPTHLENS ANALYSIS REPORT ===\n\n")
        }
        
        val cleanIntro = intro
        if (cleanIntro.isNotBlank()) {
            builder.append(cleanIntro).append("\n\n")
        }
        
        val summary = executiveSummary?.trim()
        if (!summary.isNullOrBlank()) {
            builder.append("EXECUTIVE SUMMARY\n")
            builder.append(summary).append("\n\n")
        }
        
        val synthesis = deepSynthesis?.trim()
        if (!synthesis.isNullOrBlank()) {
            builder.append("DEEP SYNTHESIS\n")
            builder.append(synthesis).append("\n\n")
        }

        if (depthLayers.isNotEmpty()) {
            builder.append("DEPTH LAYERS OF REALITY\n")
            depthLayers.forEach { layer ->
                builder.append("Layer ").append(layer.layerNumber).append(" - ").append(layer.layerName).append(": ").append(layer.description.trim()).append("\n")
            }
            builder.append("\n")
        }
        
        val rcr = rootCauseReport
        if (rcr != null) {
            builder.append("ROOT CAUSE REPORT (THE 'WHY')\n")
            builder.append("Surface Cause: ").append(rcr.symptom.trim()).append("\n")
            builder.append("Immediate Cause: ").append(rcr.immediateCause.trim()).append("\n")
            builder.append("Underlying Cause: ").append(rcr.underlyingCause.trim()).append("\n")
            builder.append("Deeper Cause: ").append(rcr.deeperCause.trim()).append("\n")
            builder.append("Root Cause Conclusion: ").append(rcr.rootCauseEstimate.trim()).append("\n")
            builder.append("Supporting Evidence: ").append(rcr.supportingEvidence.trim()).append("\n")
            if (rcr.alternativeExplanation.isNotBlank()) {
                builder.append("Alternative Explanation: ").append(rcr.alternativeExplanation.trim()).append("\n")
            }
            builder.append("\n")
        }
        
        val hd = humanDrivers
        if (hd != null) {
            builder.append("HUMAN DRIVERS (PSYCHOMOTIVE ANATOMY)\n")
            builder.append("Surface Intention: ").append(hd.surfaceIntention.trim()).append("\n")
            builder.append("Emotional Driver: ").append(hd.emotionalDriver.trim()).append("\n")
            builder.append("Core Need: ").append(hd.needDriver.trim()).append("\n")
            builder.append("Core Fear: ").append(hd.fearDriver.trim()).append("\n")
            builder.append("Incentives: ").append(hd.incentiveDriver.trim()).append("\n")
            builder.append("Identity Alignment: ").append(hd.identityDriver.trim()).append("\n")
            builder.append("Hidden Motives: ").append(hd.hiddenMotives.trim()).append("\n")
            builder.append("\n")
        }
        
        if (futureScenarios.isNotEmpty()) {
            builder.append("FUTURE SCENARIOS & PROBABILITIES\n")
            futureScenarios.forEach { scenario ->
                builder.append("- ").append(scenario.codeName.uppercase()).append(" - ").append(scenario.displayName).append(" (Prob: ").append(scenario.probability).append("%)\n")
                builder.append("  Outcome: ").append(scenario.impactText.trim()).append("\n")
                if (scenario.earlyWarningSigns.isNotEmpty()) {
                    builder.append("  Early Warning Signs:\n")
                    scenario.earlyWarningSigns.forEach { sign ->
                        builder.append("    * ").append(sign.trim()).append("\n")
                    }
                }
            }
            builder.append("\n")
        }
        
        if (probabilityMetrics != null) {
            builder.append("PROBABILITY METRICS\n")
            builder.append("Confidence: ").append(probabilityMetrics!!.confidence).append("%\n")
            builder.append("Likelihood: ").append(probabilityMetrics!!.likelihood).append("%\n")
            builder.append("Risk: ").append(probabilityMetrics!!.risk).append("%\n")
            builder.append("Opportunity: ").append(probabilityMetrics!!.opportunity).append("%\n\n")
        }

        if (probabilityAssessment != null) {
            builder.append("PROBABILITY ASSESSMENT\n")
            builder.append("Likelihood: ").append(probabilityAssessment!!.likelihood).append("%\n")
            builder.append("Confidence: ").append(probabilityAssessment!!.confidence).append("\n")
            if (probabilityAssessment!!.reasoningFactors.isNotEmpty()) {
                builder.append("Reasoning Factors:\n")
                probabilityAssessment!!.reasoningFactors.forEach { factor ->
                    builder.append("- ").append(factor.trim()).append("\n")
                }
            }
            builder.append("\n")
        }

        if (futurePathways.isNotEmpty()) {
            builder.append("FUTURE PATHWAYS\n")
            futurePathways.forEach { pathway ->
                builder.append("- ").append(pathway.title).append(" (Prob: ").append(pathway.probability).append("%)\n")
                builder.append("  Description: ").append(pathway.description.trim()).append("\n")
                if (pathway.drivers.isNotBlank()) builder.append("  Drivers: ").append(pathway.drivers.trim()).append("\n")
                if (pathway.risks.isNotBlank()) builder.append("  Risks: ").append(pathway.risks.trim()).append("\n")
                if (pathway.opportunities.isNotBlank()) builder.append("  Opportunities: ").append(pathway.opportunities.trim()).append("\n")
            }
            builder.append("\n")
        }

        if (timelineForecast != null) {
            builder.append("TIMELINE FORECAST\n")
            builder.append("Short Term: ").append(timelineForecast!!.shortTermDesc.trim()).append(" (Prob: ").append(timelineForecast!!.shortTermProb).append("%)\n")
            builder.append("Mid Term: ").append(timelineForecast!!.midTermDesc.trim()).append(" (Prob: ").append(timelineForecast!!.midTermProb).append("%)\n")
            builder.append("Long Term: ").append(timelineForecast!!.longTermDesc.trim()).append(" (Prob: ").append(timelineForecast!!.longTermProb).append("%)\n")
            builder.append("Explanation: ").append(timelineForecast!!.explanation.trim()).append("\n\n")
        }

        if (decisionImpact != null) {
            builder.append("DECISION IMPACT\n")
            builder.append("If Nothing Changes: ").append(decisionImpact!!.statusQuoDesc.trim()).append(" (Prob: ").append(decisionImpact!!.statusQuoProb).append("%)\n")
            builder.append("If Action Is Taken: ").append(decisionImpact!!.actionDesc.trim()).append(" (Prob: ").append(decisionImpact!!.actionProb).append("%)\n")
            if (decisionImpact!!.comparison.isNotBlank()) builder.append("Comparison: ").append(decisionImpact!!.comparison.trim()).append("\n")
            if (decisionImpact!!.risks.isNotBlank()) builder.append("Risks: ").append(decisionImpact!!.risks.trim()).append("\n")
            if (decisionImpact!!.benefits.isNotBlank()) builder.append("Benefits: ").append(decisionImpact!!.benefits.trim()).append("\n")
            if (decisionImpact!!.tradeoffs.isNotBlank()) builder.append("Tradeoffs: ").append(decisionImpact!!.tradeoffs.trim()).append("\n")
            builder.append("\n")
        }

        if (forecastSummary != null) {
            builder.append("FORECAST SUMMARY\n")
            builder.append("Most Likely Outcome: ").append(forecastSummary!!.mostLikelyOutcome).append("%\n")
            builder.append("Key Risk: ").append(forecastSummary!!.keyRisk).append("%\n")
            builder.append("Opportunity Window: ").append(forecastSummary!!.opportunityWindow).append("%\n")
            builder.append("Prediction Confidence: ").append(forecastSummary!!.predictionConfidence).append("\n\n")
        }

        if (suggestedQuestions.isNotEmpty()) {
            builder.append("SUGGESTED QUESTIONS\n")
            suggestedQuestions.forEach { q ->
                builder.append("- ").append(q.trim()).append("\n")
            }
            builder.append("\n")
        }

        if (explorationPaths.isNotEmpty()) {
            builder.append("EXPLORATION PATHS\n")
            explorationPaths.forEach { p ->
                builder.append("- ").append(p.trim()).append("\n")
            }
            builder.append("\n")
        }

        val conf = confidence
        if (!conf.isNullOrBlank() && isComplex) {
            builder.append("Confidence Level: ").append(conf.trim()).append("\n")
        }
        
        if (isComplex) {
            builder.append("=================================================")
        }
        
        return builder.toString().trim()
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

