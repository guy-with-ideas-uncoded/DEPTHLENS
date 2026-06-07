package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val lastUpdatedAt: Long,
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user" or "model"
    val text: String,
    val imageUri: String? = null, // Path to attached image if any
    val timestamp: Long
)

@Entity(tableName = "memory_insights")
data class MemoryInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Pattern", "Goal", "Theme", "Insight", "Driver"
    val content: String,
    val timestamp: Long
)

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
data class ProbabilityMetrics(
    val confidence: Int = 78,
    val likelihood: Int = 65,
    val risk: Int = 42,
    val opportunity: Int = 71
)

data class ProbabilityAssessment(
    val likelihood: Int = 65,
    val confidence: String = "High",
    val reasoningFactors: List<String> = emptyList()
)

data class FuturePathway(
    val title: String,
    val probability: Int,
    val description: String = "",
    val drivers: String = "",
    val risks: String = "",
    val opportunities: String = ""
)

data class TimelineForecast(
    val shortTermProb: Int = 80,
    val shortTermDesc: String = "",
    val midTermProb: Int = 60,
    val midTermDesc: String = "",
    val longTermProb: Int = 40,
    val longTermDesc: String = "",
    val explanation: String = ""
)

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

data class ForecastSummary(
    val mostLikelyOutcome: Int = 75,
    val keyRisk: Int = 60,
    val opportunityWindow: Int = 50,
    val predictionConfidence: String = "High"
)

data class ParsedResponse(
    val introduction: String = "",
    val executiveSummary: String? = null,
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
    val forecastSummary: ForecastSummary? = null
)

data class DepthLayerInsight(
    val layerNumber: Int,
    val layerName: String,
    val description: String
)

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
