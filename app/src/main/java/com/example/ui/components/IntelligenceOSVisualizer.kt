package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun IntelligenceOSVisualizer(
    parsed: ParsedResponse,
    rawText: String,
    messageId: String,
    modifier: Modifier = Modifier,
    onSubmitQuery: (String) -> Unit = {}
) {
    // Generate deterministic values based on text if they are not in the raw response
    val calculatedData = remember(rawText) {
        DeterministicIntelligenceGenerator(rawText, parsed)
    }

    val coroutineScope = rememberCoroutineScope()
    val unlockedSections = remember { mutableStateMapOf<String, Boolean>() }
    val generatingSections = remember { mutableStateMapOf<String, Boolean>() }

    val relevantChips = remember(rawText) {
        val lowercaseText = rawText.lowercase()
        val chips = mutableListOf<String>()
        
        // Analyze context to select highly relevant dashboards on-the-fly
        val isBusiness = lowercaseText.contains("market") || lowercaseText.contains("startup") || 
                         lowercaseText.contains("business") || lowercaseText.contains("company") || 
                         lowercaseText.contains("diamond") || lowercaseText.contains("industry") || 
                         lowercaseText.contains("finance") || lowercaseText.contains("product") ||
                         lowercaseText.contains("revenue") || lowercaseText.contains("valuation") ||
                         lowercaseText.contains("strategy") || lowercaseText.contains("pricing")
                         
        val isRelationshipOrPersonal = lowercaseText.contains("relationship") || lowercaseText.contains("friend") || 
                                       lowercaseText.contains("love") || lowercaseText.contains("feel") || 
                                       lowercaseText.contains("marry") || lowercaseText.contains("her") || 
                                       lowercaseText.contains("him") || lowercaseText.contains("she") || 
                                       lowercaseText.contains("he") || lowercaseText.contains("family") || 
                                       lowercaseText.contains("leave") || lowercaseText.contains("job") ||
                                       lowercaseText.contains("career") || lowercaseText.contains("anxious")
        
        if (isRelationshipOrPersonal) {
            chips.add("Future Outcomes")
            chips.add("Risk Analysis")
            chips.add("Hidden Factors")
            chips.add("Deep Dive")
        } else if (isBusiness) {
            chips.add("Future Outcomes")
            chips.add("Risk Analysis")
            chips.add("Probability")
            chips.add("Strategic View")
            chips.add("Deep Dive")
        } else {
            chips.add("Future Outcomes")
            chips.add("Risk Analysis")
            chips.add("Probability")
            chips.add("Hidden Factors")
            chips.add("Strategic View")
            chips.add("Deep Dive")
        }
        chips.toList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // 1. EXECUTIVE SUMMARY CARD (Always visible summary brief)
            var animateSummary by remember { mutableStateOf(false) }
            LaunchedEffect(messageId) {
                animateSummary = true
            }
            RevealSection(visible = animateSummary, delayMillis = 50) {
                ExecutiveSummaryCard(
                    summaryText = parsed.executiveSummary?.ifBlank { null } ?: calculatedData.executiveSummary
                )
            }

            // Reality Layer Activation Panel removed from analysis per user request

            // ── SMART ADAPTIVE CHIPS ROW ──
            var animateChips by remember { mutableStateOf(false) }
            LaunchedEffect(messageId) {
                animateChips = true
            }
            RevealSection(visible = animateChips, delayMillis = 100) {
                Column {
                    Text(
                        text = "INTELLIGENCE DEEP DIVE PANELS (CHIP CONTROL)",
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedColor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relevantChips.forEach { chipName ->
                            val isUnlocked = unlockedSections[chipName] == true
                            val isGenerating = generatingSections[chipName] == true
                            val activeColor = when (chipName) {
                                "Future Outcomes" -> ElectricViolet
                                "Risk Analysis" -> ErrorColor
                                "Probability" -> PremiumCyan
                                "Hidden Factors" -> WarningColor
                                "Strategic View" -> SuccessColor
                                else -> ElectricViolet
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUnlocked) activeColor.copy(alpha = 0.15f) else Surface2)
                                    .border(
                                        width = 1.dp,
                                        color = if (isUnlocked) activeColor else BorderSubtle,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isGenerating) {
                                        if (isUnlocked) {
                                            unlockedSections[chipName] = false
                                        } else {
                                            generatingSections[chipName] = true
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1200)
                                                generatingSections[chipName] = false
                                                unlockedSections[chipName] = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .minimumInteractiveComponentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(
                                            color = activeColor,
                                            strokeWidth = 1.5.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = chipName.uppercase(),
                                        fontSize = 11.sp,
                                        fontFamily = DMMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isUnlocked) activeColor else TextPrimaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── OPTIONAL DYNAMIC CHIP-CONTROLLED PANELS ──

            relevantChips.forEach { chipName ->
                val isUnlocked = unlockedSections[chipName] == true
                val isGenerating = generatingSections[chipName] == true
                val activeColor = when (chipName) {
                    "Future Outcomes" -> ElectricViolet
                    "Risk Analysis" -> ErrorColor
                    "Probability" -> PremiumCyan
                    "Hidden Factors" -> WarningColor
                    "Strategic View" -> SuccessColor
                    else -> ElectricViolet
                }

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface2, RoundedCornerShape(12.dp))
                            .border(1.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = activeColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "DEPTHLENS SYNTHESIS ENGINE PROCESSING...",
                                    fontSize = 10.sp,
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = activeColor,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cross-referencing behavioral indicators and computing scenario factors for $chipName...",
                                fontSize = 12.sp,
                                fontFamily = InstrumentSansFontFamily,
                                color = TextSecondaryColor
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isUnlocked,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (chipName) {
                            "Future Outcomes" -> {
                                // 4. FUTURE PROBABILITY DASHBOARD
                                FutureProbabilityDashboard(
                                    messageId = messageId,
                                    patternProb = calculatedData.probPatternContinues,
                                    interventionProb = calculatedData.probInterventionWorks,
                                    escalationRisk = calculatedData.probEscalationRisk
                                )

                                // 5. FUTURE SCENARIO COMPARISON
                                FutureScenarioComparison(
                                    messageId = messageId,
                                    scenarios = parsed.futureScenarios.ifEmpty { calculatedData.scenarios },
                                    onScenarioClick = { scenarioName ->
                                        onSubmitQuery("Analyze in detail the scenario: '$scenarioName' from the current context.")
                                    }
                                )
                            }
                            "Risk Analysis" -> {
                                // 7. RISK VS OPPORTUNITY MATRIX
                                RiskVsOpportunityMatrix(
                                    messageId = messageId,
                                    risks = calculatedData.highRisks,
                                    opportunities = calculatedData.highOpportunities
                                )

                                RiskAssessmentEngine(calculatedData = calculatedData)
                            }
                            "Probability" -> {
                                // 3. AI CONFIDENCE ENGINE
                                AiConfidenceEngine(
                                    messageId = messageId,
                                    confidenceLevel = parsed.confidence?.ifBlank { null } ?: calculatedData.confidenceLevel,
                                    confidenceScore = calculatedData.confidenceScore,
                                    reasoning = calculatedData.confidenceReasoning
                                )

                                // 6. FUTURE TIMELINE FORECAST
                                FutureTimelineForecast(
                                    messageId = messageId,
                                    timeline = parsed.timelineForecast ?: calculatedData.timelineForecast
                                )
                            }
                            "Hidden Factors" -> {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Surface2),
                                    border = BorderStroke(1.dp, WarningColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Troubleshoot, contentDescription = null, tint = WarningColor, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "UNDERLYING DRIVERS & HIDDEN FACTORS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarningColor,
                                                letterSpacing = 1.sp,
                                                fontFamily = DMMonoFontFamily
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Psychological need focus: ${calculatedData.confidenceReasoning.split(".").firstOrNull() ?: "Protecting structural viability and core incentive alignments."}",
                                            fontSize = 12.sp,
                                            color = TextSecondaryColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                            "Strategic View" -> {
                                // 11. INTELLIGENCE SIGNALS
                                IntelligenceSignalsSection(
                                    messageId = messageId,
                                    stability = calculatedData.signalStability,
                                    volatility = calculatedData.signalVolatility,
                                    escalation = calculatedData.signalEscalation,
                                    opportunity = calculatedData.signalOpportunity,
                                    consistency = calculatedData.signalConsistency,
                                    pressure = calculatedData.signalPressure
                                )

                                OpportunityScoreEngine(
                                    actions = calculatedData.highestLeverageActions,
                                    onActionClick = { actionName ->
                                        onSubmitQuery("Develop an implementation playbook for the leverage action: '$actionName' in this situation.")
                                    }
                                )
                            }
                            "Deep Dive" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Surface2),
                                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cyclone,
                                                    contentDescription = null,
                                                    tint = ElectricViolet,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    "COGNITIVE DEEP DIVE SIMULATOR",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElectricViolet,
                                                    letterSpacing = 1.2.sp,
                                                    fontFamily = DMMonoFontFamily
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Initiate targeted systemic deconstructions across 10 distinct layers of hidden reality. Selecting an individual layer triggers focused cognitive inquiries, while launching the complete simulator performs an ultra-deep strategic synthesis.",
                                                fontSize = 12.sp,
                                                color = TextSecondaryColor,
                                                lineHeight = 17.sp,
                                                fontFamily = InstrumentSansFontFamily
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            // Primary action button (Full 10-Layer Deep Dive)
                                            Button(
                                                onClick = {
                                                    onSubmitQuery("Conduct a comprehensive, multi-layer Deep Dive on my previous scenario. For each of the 10 developmental layers (from Layer 1: Observable Reality to Layer 10: Outside Observer's perspective), uncover hidden assumptions, unconscious defense patterns, systemic contradictions, emotional drivers, and the single deepest point of leverage. Present your analysis with unmatched strategic and psychological depth, and conclude with a stark, penetrative DEEP SYNTHESIS.")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ElectricViolet,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(vertical = 12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Launch,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "LAUNCH 10-LAYER DEEP DIVE",
                                                    fontSize = 11.sp,
                                                    fontFamily = DMMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }

                                    // Display 10 interactive layer-specific exploration cards
                                    val deepDiveLayers = listOf(
                                        Pair("Layer 1 · Events", "Deconstruct explicit facts and verifiable surface occurrences."),
                                        Pair("Layer 2 · Triggers", "Deconstruct the systemic triggers, lock-ins, and institutional constraints."),
                                        Pair("Layer 3 · Drivers", "Deconstruct the core underlying feeling states and emotional projections."),
                                        Pair("Layer 4 · Patterns", "Deconstruct historical repetitions, scripts, and defense mechanisms."),
                                        Pair("Layer 5 · Assumptions", "Deconstruct artificial mental rules and unstated blind-spot commitments."),
                                        Pair("Layer 6 · Paradoxes", "Deconstruct active contradictions, double-binds, and paradoxical incentives."),
                                        Pair("Layer 7 · Consequences", "Deconstruct branching mid-to-long trajectories and second-order details."),
                                        Pair("Layer 8 · Leverage", "Deconstruct the single high-leverage corrective Archimedean catalyst."),
                                        Pair("Layer 9 · Truths", "Deconstruct critically excluded, denied, or uncomfortable raw facts."),
                                        Pair("Layer 10 · Observer", "Deconstruct objective third-party feedback that remains completely unseen.")
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        deepDiveLayers.forEachIndexed { index, (title, summary) ->
                                            val layerNumber = index + 1
                                            val layerColor = getLayerColor(layerNumber)
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Surface2, RoundedCornerShape(12.dp))
                                                    .border(1.dp, layerColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        val queryPrompt = when(layerNumber) {
                                                            1 -> "Deconstruct Layer 1 (Observable Reality) in my previous scenario. Look past the surface report: What systematic, physical occurrences or verifiable behaviors are concretely taking place that might easily be overlooked?"
                                                            2 -> "Deconstruct Layer 2 (Systemic Reality) in my previous scenario. Analyze the systemic triggers, feedback loops, structural dependencies, regulatory limits, or institutional boundaries that govern the situation."
                                                            3 -> "Deconstruct Layer 3 (Emotional Reality) in my previous scenario. What underlying feelings, anxieties, projecting fears, or core emotional needs are driving internal and external behaviors?"
                                                            4 -> "Deconstruct Layer 4 (Psychological/Pattern Reality) in my previous scenario. What unconscious defense mechanisms, cognitive biases, or historical repetitions are operating? Outline the repeating script."
                                                            5 -> "Deconstruct Layer 5 (Hidden Assumptions/Calculations) in my previous scenario. What unstated commitments or invisible, core mental assumptions exist? What rules are we assuming that are actually artificial?"
                                                            6 -> "Deconstruct Layer 6 (Paradoxes & Contradictions) in my previous scenario. What conflicting incentives, dual-binding situations, or systemic double-standards are operating underneath?"
                                                            7 -> "Deconstruct Layer 7 (Probability / Strategic Reality) in my previous scenario. What are the second-order effects, branching future trajectories, and long-term consequences of this situation continuing unchanged?"
                                                            8 -> "Deconstruct Layer 8 (Root Cause / Highest Leverage) in my previous scenario. What is the fundamental, highest-leverage corrective action? What tiny change here releases the biggest structural bottleneck?"
                                                            9 -> "Deconstruct Layer 9 (The Ignored Reality) in my previous scenario. What critical, uncomfortable truth or raw fact is actively being denied, ignored, or projected away?"
                                                            else -> "Deconstruct Layer 10 (Objective Observer's view) in my previous scenario. If a completely objective, non-invested third-party observer looked at this dynamic, what would they notice immediately that we are blind to?"
                                                        }
                                                        onSubmitQuery(queryPrompt)
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(layerColor.copy(alpha = 0.12f), CircleShape)
                                                        .border(1.dp, layerColor.copy(alpha = 0.6f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "L$layerNumber",
                                                        fontSize = 9.sp,
                                                        fontFamily = DMMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = layerColor
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = title.uppercase(),
                                                        fontSize = 11.sp,
                                                        fontFamily = DMMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimaryColor
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = summary,
                                                        fontSize = 11.sp,
                                                        fontFamily = InstrumentSansFontFamily,
                                                        color = TextSecondaryColor,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = layerColor.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

// ────────────────────────────────────────────────────────────────────────
// 1. EXECUTIVE SUMMARY CARD Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun ExecutiveSummaryCard(summaryText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Outer subtle glowing aura or trace border underlay
                drawRoundRect(
                    color = ElectricViolet.copy(alpha = 0.15f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface3),
        border = BorderStroke(1.5.dp, ElectricViolet.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(ElectricViolet.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, ElectricViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ElectricViolet, CircleShape)
                    )
                }
                Text(
                    text = "ROOT CAUSE IDENTIFIED",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricViolet,
                    letterSpacing = 1.4.sp
                )
            }

            Text(
                text = summaryText,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextPrimaryColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-status terminal line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepMidnight.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIAGNOSTIC STATUS:",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = TextMutedColor,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REALITY ACTIVATED [100%]",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 8.5.sp,
                    color = SuccessColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 2. REALITY LAYER ACTIVATION PANEL Composable
// ────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RealityLayerActivationPanel(
    messageId: String,
    activeLayersCount: Int,
    parsedLayers: List<DepthLayerInsight>
) {
    val standardNames = listOf(
        "Observable",
        "Behavioral",
        "Psychological",
        "Emotional",
        "Strategic",
        "Systemic",
        "Pattern",
        "Root Cause",
        "Probability",
        "Hidden Risks"
    )

    val activeList = remember(parsedLayers, activeLayersCount) {
        if (parsedLayers.isNotEmpty()) {
            parsedLayers.map { 
                val cleanName = it.layerName
                    .removeSuffix(" Reality")
                    .removeSuffix(" Insight")
                    .removeSuffix(" & Opportunities")
                    .trim()
                it.copy(layerName = cleanName)
            }
        } else {
            (1..activeLayersCount.coerceIn(1, 10)).map { i ->
                DepthLayerInsight(
                    layerNumber = i,
                    layerName = standardNames[i - 1],
                    description = ""
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Troubleshoot,
                    contentDescription = null,
                    tint = PremiumCyan,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = "REALITY LAYER ACTIVATION",
                        fontFamily = DMMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${activeList.size} Active Diagnostic Layers detected",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 11.sp,
                        color = TextMutedColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Activation Indicators row (pulsing hardware panel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (i in 1..10) {
                    val isActive = i <= activeLayersCount
                    val activeColor = getLayerColor(i)
                    val barColor = if (isActive) activeColor else BorderSubtle.copy(alpha = 0.3f)
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "LayerGlow_$i")
                    val pulseRatio by if (isActive) {
                        infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800 + i * 120, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_$i"
                        )
                    } else remember { mutableStateOf(1f) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor.copy(alpha = if (isActive) pulseRatio else 1.0f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beautiful FlowRow layout of visual diagnostic capsules
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..10) {
                    val isActive = i <= activeLayersCount
                    val matching = activeList.firstOrNull { it.layerNumber == i }
                    val name = matching?.layerName ?: standardNames.getOrNull(i - 1) ?: "L$i"
                    val layerColor = getLayerColor(i)
                    
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(layerColor.copy(alpha = 0.12f))
                                .border(1.dp, layerColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "L$i $name".uppercase(),
                                fontSize = 9.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = layerColor
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface3.copy(alpha = 0.4f))
                                .border(1.dp, BorderSubtle.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "L$i DORMANT".uppercase(),
                                fontSize = 9.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = TextMutedColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// DEEP SYNTHESIS PANEL Composable
// ────────────────────────────────────────────────────────────────────────
data class PerspectiveInsight(val title: String, val content: String)

@Composable
fun DeepSynthesisPanel(
    synthesisText: String,
    modifier: Modifier = Modifier
) {
    val parsedPerspectives = remember(synthesisText) {
        val headers = listOf(
            "PRACTICAL PERSPECTIVE",
            "STRATEGIC PERSPECTIVE",
            "PSYCHOLOGICAL PERSPECTIVE",
            "BUSINESS PERSPECTIVE",
            "PHILOSOPHICAL PERSPECTIVE",
            "LONG-TERM PERSPECTIVE",
            "CONTRARIAN PERSPECTIVE",
            "META PERSPECTIVE",
            "INTEGRATED SYNTHESIS",
            "INTRODUCTION"
        )
        
        val result = mutableListOf<PerspectiveInsight>()
        var currentTitle: String? = null
        val currentContent = StringBuilder()
        
        val lines = synthesisText.split("\n")
        for (line in lines) {
            val trimmedLine = line.trim()
            val foundHeader = headers.firstOrNull { trimmedLine.startsWith(it, ignoreCase = true) }
            if (foundHeader != null) {
                if (currentTitle != null && currentContent.isNotEmpty()) {
                    result.add(PerspectiveInsight(currentTitle, currentContent.toString().trim()))
                    currentContent.setLength(0)
                }
                currentTitle = foundHeader
                val rest = trimmedLine.removePrefix(foundHeader).trim().removePrefix(":").trim()
                if (rest.isNotEmpty()) {
                    currentContent.append(rest).append("\n")
                }
            } else {
                if (currentTitle != null) {
                    currentContent.append(line).append("\n")
                } else if (trimmedLine.isNotEmpty()) {
                    currentTitle = "INTRODUCTION"
                    currentContent.append(line).append("\n")
                }
            }
        }
        if (currentTitle != null && currentContent.isNotEmpty()) {
            result.add(PerspectiveInsight(currentTitle, currentContent.toString().trim()))
        }
        result.filter { it.content.isNotEmpty() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = ElectricViolet.copy(alpha = 0.25f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface3),
        border = BorderStroke(1.5.dp, ElectricViolet.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(ElectricViolet.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, ElectricViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ElectricViolet, CircleShape)
                    )
                }
                Text(
                    text = "DEEP MULTI-PERSPECTIVE SYNTHESIS",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    letterSpacing = 1.5.sp
                )
            }

            if (parsedPerspectives.isEmpty()) {
                Text(
                    text = synthesisText,
                    fontSize = 15.sp,
                    fontFamily = InstrumentSansFontFamily,
                    color = TextPrimaryColor,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                parsedPerspectives.forEach { p ->
                    val (accentColor, titleText) = when (p.title.uppercase()) {
                        "INTRODUCTION" -> Pair(TextSecondaryColor, "CONTEXT BRIEF")
                        "PRACTICAL PERSPECTIVE" -> Pair(Color(0xFF00D4FF), "PRACTICAL GROUNDING")
                        "STRATEGIC PERSPECTIVE" -> Pair(Color(0xFF7E65FF), "STRATEGIC LEVERAGE")
                        "PSYCHOLOGICAL PERSPECTIVE" -> Pair(Color(0xFFFF5E8A), "PSYCHOLOGICAL MOTIVES")
                        "BUSINESS PERSPECTIVE" -> Pair(Color(0xFFFFAA40), "ECONOMIC STRUCTURE")
                        "PHILOSOPHICAL PERSPECTIVE" -> Pair(Color(0xFFA855F7), "PHILOSOPHICAL MEANING")
                        "LONG-TERM PERSPECTIVE" -> Pair(Color(0xFF60A5FA), "LONG-TERM TRAJECTORY")
                        "CONTRARIAN PERSPECTIVE" -> Pair(Color(0xFFFF3366), "CONTRARIAN SHIFT")
                        "META PERSPECTIVE" -> Pair(Color(0xFF2EE8A0), "META REPEATING FRACTAL")
                        "INTEGRATED SYNTHESIS" -> Pair(ElectricViolet, "INTEGRATED COGNITIVE SYNTHESIS")
                        else -> Pair(PremiumCyan, p.title)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RichNavy.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Text(
                                text = titleText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 1.sp,
                                fontFamily = DMMonoFontFamily
                            )
                        }
                        Text(
                            text = p.content,
                            fontSize = 13.5.sp,
                            fontFamily = InstrumentSansFontFamily,
                            color = TextPrimaryColor,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// SUMMARY OF INQUIRY Composable (short / medium Summary relating to users question)
// ────────────────────────────────────────────────────────────────────────
@Composable
fun SummaryOfInquiryPanel(
    userQuery: String,
    introduction: String,
    modifier: Modifier = Modifier
) {
    val cleanQuery = remember(userQuery) {
        val trimmed = userQuery.trim()
        if (trimmed.isBlank()) {
            "Initial diagnostic scan and telemetry calibration request."
        } else if (trimmed.length > 120) {
            trimmed.take(117) + "..."
        } else {
            trimmed
        }
    }

    val cleanSummaryText = remember(introduction, userQuery) {
        val cleanIntro = introduction.trim()
        val sentences = cleanIntro.split(Regex("(?<=[.!?])\\s+"))
        val summaryCandidate = sentences.take(2).joinToString(" ").trim()
        if (summaryCandidate.length in 20..220) {
            summaryCandidate
        } else if (cleanIntro.length > 10) {
            if (cleanIntro.length > 200) {
                cleanIntro.take(197) + "..."
            } else {
                cleanIntro
            }
        } else {
            "Strategic analysis of the defined context, evaluating core drivers, behavioral patterns, and leverage vectors."
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = PremiumCyan.copy(alpha = 0.25f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface3),
        border = BorderStroke(1.5.dp, PremiumCyan.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(PremiumCyan.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, PremiumCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(PremiumCyan, CircleShape)
                    )
                }
                Text(
                    text = "SUMMARY OF INQUIRY",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.5.sp
                )
            }
            Text(
                text = "“$cleanQuery”",
                fontSize = 13.sp,
                fontFamily = DMMonoFontFamily,
                color = TextSecondaryColor,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = cleanSummaryText,
                fontSize = 14.sp,
                fontFamily = InstrumentSansFontFamily,
                color = TextPrimaryColor,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 3. AI CONFIDENCE ENGINE Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun AiConfidenceEngine(
    messageId: String,
    confidenceLevel: String,
    confidenceScore: Int,
    reasoning: String
) {
    var isExpanded by rememberSaveable(key = "${messageId}_confidence") { mutableStateOf(false) }
    val levelColor = when (confidenceLevel.uppercase()) {
        "HIGH", "HIGH CONFIDENCE" -> SuccessColor
        "MODERATE", "MODERATE CONFIDENCE" -> WarningColor
        else -> ErrorColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI CONFIDENCE ENGINE",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.2.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Confidence Details",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Circular Dial Gauge Indicator
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .drawBehind {
                                    // Dial background track
                                    drawArc(
                                        color = BorderSubtle.copy(alpha = 0.4f),
                                        startAngle = 135f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    // Filled Dial track
                                    drawArc(
                                        color = levelColor,
                                        startAngle = 135f,
                                        sweepAngle = 270f * (confidenceScore / 100f),
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$confidenceScore%",
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimaryColor
                                )
                                Text(
                                    text = "SCORE",
                                    fontFamily = DMMonoFontFamily,
                                    fontSize = 7.5.sp,
                                    color = TextMutedColor
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(levelColor, CircleShape)
                                )
                                Text(
                                    text = confidenceLevel.uppercase(),
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = levelColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reasoning,
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = TextSecondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 4. FUTURE PROBABILITY DASHBOARD & ASCII TERMINAL MODULE Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun FutureProbabilityDashboard(
    messageId: String,
    patternProb: Int,
    interventionProb: Int,
    escalationRisk: Int
) {
    var isExpanded by rememberSaveable(key = "${messageId}_probability") { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FUTURE PROBABILITY DASHBOARD",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.2.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Probability Details",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Dynamic Terminal-Style Progress Module
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProbabilityAsciiBar(label = "PATTERN CONTINUES", score = patternProb, primaryColor = ElectricViolet)
                        ProbabilityAsciiBar(label = "INTERVENTION WORKS", score = interventionProb, primaryColor = PremiumCyan)
                        ProbabilityAsciiBar(label = "ESCALATION RISK", score = escalationRisk, primaryColor = ErrorColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ProbabilityAsciiBar(label: String, score: Int, primaryColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextSecondaryColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$score%",
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = primaryColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Create the terminal style █████░░░░ bar
        val totalBlocks = 18
        val filledBlocks = remember(score) { ((score / 100f) * totalBlocks).toInt().coerceIn(1, totalBlocks) }
        val asciiString = remember(filledBlocks) {
            "█".repeat(filledBlocks) + "░".repeat(totalBlocks - filledBlocks)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepMidnight, RoundedCornerShape(6.dp))
                .border(0.5.dp, BorderSubtle, RoundedCornerShape(6.dp))
                .padding(vertical = 4.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = asciiString,
                fontFamily = DMMonoFontFamily,
                fontSize = 13.sp,
                color = primaryColor,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            // Cyber pulsing active indicator
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(primaryColor, CircleShape)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 5. FUTURE SCENARIO COMPARISON Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun FutureScenarioComparison(
    messageId: String,
    scenarios: List<FutureScenario>,
    onScenarioClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "FUTURE SCENARIO COMPARISON",
            fontFamily = DMMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SectionLabelColor,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scenarios.forEachIndexed { index, scenario ->
                val (rankingLabel, colorAccent) = when (index) {
                    0 -> "MOST LIKELY FUTURE" to ElectricViolet
                    1 -> "SECONDARY FUTURE" to PremiumCyan
                    else -> "LOW PROBABILITY FUTURE" to WarningColor
                }

                var isScenarioExpanded by rememberSaveable(key = "${messageId}_scenario_$index") { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isScenarioExpanded = !isScenarioExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface2),
                    border = BorderStroke(1.dp, if (index == 0) colorAccent.copy(alpha = 0.4f) else BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rankingLabel,
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.5.sp,
                                    color = colorAccent,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scenario.displayName.ifBlank { scenario.codeName },
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryColor
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(colorAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${scenario.probability}%",
                                        fontFamily = DMMonoFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = colorAccent
                                    )
                                }
                                Icon(
                                    imageVector = if (isScenarioExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Show details",
                                    tint = TextSecondaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isScenarioExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = scenario.impactText,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = TextSecondaryColor
                                )

                                if (scenario.earlyWarningSigns.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "EARLY WARNING INDICATORS:",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 8.5.sp,
                                        color = colorAccent,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        scenario.earlyWarningSigns.forEach { sign ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(colorAccent, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sign,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    fontSize = 11.5.sp,
                                                    color = TextPrimaryColor
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                TextButton(
                                    onClick = { onScenarioClick(scenario.displayName) },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.textButtonColors(contentColor = colorAccent)
                                ) {
                                    Icon(Icons.Default.ManageSearch, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate This Scenario", fontSize = 11.sp, fontFamily = DMMonoFontFamily)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 6. FUTURE TIMELINE FORECAST Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun FutureTimelineForecast(
    messageId: String,
    timeline: TimelineForecast
) {
    var isExpanded by rememberSaveable(key = "${messageId}_timeline") { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FUTURE TIMELINE FORECAST",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.2.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Timeline Details",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    val timelineSteps = remember(timeline) {
                        listOf(
                            Triple("NEXT 7 DAYS", timeline.shortTermProb to timeline.shortTermDesc, SuccessColor),
                            Triple("NEXT 30 DAYS", timeline.midTermProb to timeline.midTermDesc, WarningColor),
                            Triple("NEXT 180+ DAYS", timeline.longTermProb to timeline.longTermDesc, ErrorColor)
                        )
                    }

                    // Custom Timeline Draw Component
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        timelineSteps.forEachIndexed { index, (termLabel, metrics, color) ->
                            val (prob, desc) = metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Drawing line connecting nodes
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(color.copy(alpha = 0.15f), CircleShape)
                                            .border(2.dp, color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    if (index < 2) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(55.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(color, timelineSteps[index + 1].third)
                                                    )
                                                )
                                        )
                                    }
                                }

                                // Text block details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = termLabel,
                                            fontFamily = DMMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            color = color,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "CONFIDENCE: $prob%",
                                            fontFamily = DMMonoFontFamily,
                                            fontSize = 8.5.sp,
                                            color = TextMutedColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = TextPrimaryColor
                                    )
                                }
                            }
                        }

                        if (timeline.explanation.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface3, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "SYSTEM FORECAST RATIONALE:",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 8.5.sp,
                                        color = PremiumCyan,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = timeline.explanation,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = TextSecondaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 7. RISK VS OPPORTUNITY MATRIX Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun RiskVsOpportunityMatrix(
    messageId: String,
    risks: List<String>,
    opportunities: List<String>
) {
    var isExpanded by rememberSaveable(key = "${messageId}_matrix") { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RISK VS OPPORTUNITY MATRIX",
                fontFamily = DMMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SectionLabelColor,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Toggle Matrix Details",
                tint = TextSecondaryColor,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High Risks Column
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface2),
                    border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(13.dp))
                            Text(
                                text = "HIGH RISKS",
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = ErrorColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            risks.forEach { risk ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "•", color = ErrorColor, modifier = Modifier.padding(end = 6.dp), fontSize = 12.sp)
                                    Text(
                                        text = risk,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = TextSecondaryColor
                                    )
                                }
                            }
                        }
                    }
                }

                // High Opportunities Column
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface2),
                    border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(13.dp))
                            Text(
                                text = "HIGH OPPORTUNITIES",
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SuccessColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            opportunities.forEach { opp ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "•", color = SuccessColor, modifier = Modifier.padding(end = 6.dp), fontSize = 12.sp)
                                    Text(
                                        text = opp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = TextSecondaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 8. RISK ASSESSMENT ENGINE Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun RiskAssessmentEngine(calculatedData: DeterministicIntelligenceGenerator) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "RISK ASSESSMENT",
                fontFamily = DMMonoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumCyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val riskMeters = remember(calculatedData) {
                listOf(
                    Triple("Conflict Risk", calculatedData.riskConflict, ErrorColor),
                    Triple("Trust Risk", calculatedData.riskTrust, WarningColor),
                    Triple("Financial Risk", calculatedData.riskFinancial, PremiumCyan),
                    Triple("Reputation Risk", calculatedData.riskReputation, ElectricViolet)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                riskMeters.forEach { (riskName, value, color) ->
                    val severityLabel = when {
                        value >= 75 -> "CRITICAL"
                        value >= 50 -> "ELEVATED"
                        else -> "STABLE"
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = riskName,
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 11.5.sp,
                                color = TextPrimaryColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$severityLabel · ",
                                    fontFamily = DMMonoFontFamily,
                                    fontSize = 8.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$value%",
                                    fontFamily = DMMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TextPrimaryColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(BorderSubtle.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(value / 100f)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 9. OPPORTUNITY SCORE ENGINE Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun OpportunityScoreEngine(
    actions: List<Pair<String, Int>>,
    onActionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "HIGHEST LEVERAGE ACTIONS",
                fontFamily = DMMonoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumCyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEachIndexed { index, (actionName, score) ->
                    val bulletColor = when (index) {
                        0 -> SuccessColor
                        1 -> PremiumCyan
                        else -> ElectricViolet
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface3, RoundedCornerShape(8.dp))
                            .clickable { onActionClick(actionName) }
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(bulletColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = actionName,
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryColor,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(bulletColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$score%",
                                    fontFamily = DMMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bulletColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 11. INTELLIGENCE SIGNALS Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun IntelligenceSignalsSection(
    messageId: String,
    stability: Int,
    volatility: Int,
    escalation: Int,
    opportunity: Int,
    consistency: Int,
    pressure: Int
) {
    var isExpanded by rememberSaveable(key = "${messageId}_signals") { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRATEGIC INTELLIGENCE SIGNALS",
                fontFamily = DMMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SectionLabelColor,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Toggle Signals",
                tint = TextSecondaryColor,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val column1 = listOf(
                    Triple("Pattern Stability", stability, SuccessColor),
                    Triple("Escalation Momentum", escalation, ErrorColor),
                    Triple("Behavioral Consistency", consistency, PremiumCyan)
                )
                val column2 = listOf(
                    Triple("Volatility Index", volatility, WarningColor),
                    Triple("Opportunity Momentum", opportunity, SuccessColor),
                    Triple("Systemic Pressure", pressure, ElectricViolet)
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    column1.forEach { (signalName, score, color) ->
                        SignalMeterCard(name = signalName, score = score, accentColor = color)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    column2.forEach { (signalName, score, color) ->
                        SignalMeterCard(name = signalName, score = score, accentColor = color)
                    }
                }
            }
        }
    }
}

@Composable
fun SignalMeterCard(name: String, score: Int, accentColor: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = BorderStroke(0.5.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name.uppercase(),
                    fontFamily = DMMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$score%",
                    fontFamily = DMMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Signal track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(BorderSubtle.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(score / 100f)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// COLOR MAPPING UTILITY
// ────────────────────────────────────────────────────────────────────────
private fun getLayerColor(layerNumber: Int): Color = when (ThemeManager.themeName) {
    "Polar Dawn" -> when (layerNumber) {
        1 -> Color(0xFF0369A1)
        2 -> Color(0xFF047857)
        3 -> Color(0xFF581C87)
        4 -> Color(0xFFB91C1C)
        5 -> Color(0xFFB45309)
        6 -> Color(0xFFC2410C)
        7 -> Color(0xFF701A75)
        8 -> Color(0xFF1E3A8A)
        9 -> Color(0xFF9D174D)
        else -> Color(0xFF475569)
    }
    "Future" -> when (layerNumber) {
        1 -> Color(0xFF00FFCC)
        2 -> Color(0xFF00FF66)
        3 -> Color(0xFFD946EF)
        4 -> Color(0xFF38BDF8)
        5 -> Color(0xFFFBBF24)
        6 -> Color(0xFFFF5E8A)
        7 -> Color(0xFFA855F7)
        8 -> Color(0xFF60A5FA)
        9 -> Color(0xFFF472B6)
        else -> Color(0xFFE2E8F0)
    }
    else -> when (layerNumber) {
        1 -> Color(0xFF00D4FF)
        2 -> Color(0xFF2EE8A0)
        3 -> Color(0xFF7E65FF)
        4 -> Color(0xFFFF5E8A)
        5 -> Color(0xFFFFAA40)
        6 -> Color(0xFFFF7A5C)
        7 -> Color(0xFFA855F7)
        8 -> Color(0xFF60A5FA)
        9 -> Color(0xFFF472B6)
        else -> Color(0xFFE2E8F0)
    }
}

// ────────────────────────────────────────────────────────────────────────
// DETERMINISTIC INTELLIGENCE ENGINE BACKING MODEL
// ────────────────────────────────────────────────────────────────────────
class DeterministicIntelligenceGenerator(rawText: String, parsed: ParsedResponse) {
    val cleanSeed = rawText.hashCode()

    val confidenceScore = parsed.probabilityMetrics?.confidence ?: (75 + abs(cleanSeed % 18))
    
    val executiveSummary = if (parsed.executiveSummary.isNullOrBlank()) {
        "Strategic diagnostic identifies systemic feedback mechanisms and misaligned incentives creating current tension vectors."
    } else parsed.executiveSummary!!

    val confidenceLevel = if (confidenceScore >= 80) "HIGH CONFIDENCE" else "MODERATE CONFIDENCE"
    
    val confidenceReasoning = "Consistent causal patterns identified across " + 
            "${parsed.depthLayers.size.coerceAtLeast(6)} active reality diagnostic layers with strong behavioral and systemic indicator cross-referencing."

    val layersCount = 6 + (abs(cleanSeed % 4)) // 6 to 10

    // Probabilities
    val probPatternContinues = parsed.probabilityMetrics?.likelihood ?: (55 + abs(cleanSeed % 25))
    val probInterventionWorks = parsed.probabilityMetrics?.opportunity ?: (20 + abs(cleanSeed % 20))
    val probEscalationRisk = parsed.probabilityMetrics?.risk ?: (10 + abs(cleanSeed % 15))

    // Scenarios
    val scenarios = listOf(
        FutureScenario(
            codeName = "S1",
            displayName = "Status Quo Reinforcement",
            probability = probPatternContinues,
            impactText = "The underlying systems drivers remain active without strategic interventions. Structural friction and existing behaviors accelerate.",
            earlyWarningSigns = listOf("Accelerated communication loops", "Repetitive avoidance postures")
        ),
        FutureScenario(
            codeName = "S2",
            displayName = "Constructive Realignment",
            probability = probInterventionWorks,
            impactText = "Active systemic realignment of boundaries is initialized. Realized accountability loops and shared incentives neutralize trust deficit.",
            earlyWarningSigns = listOf("Boundary definition updates", "Active joint mediation requests")
        ),
        FutureScenario(
            codeName = "S3",
            displayName = "Escalated Vulnerability",
            probability = probEscalationRisk,
            impactText = "A cascade of defensive projections triggers critical boundary breaches. Secondary parties are systemically locked in friction loop.",
            earlyWarningSigns = listOf("Explicit defensive projections", "Secondary stakeholder friction")
        )
    )

    // Timeline Forecast
    val timelineForecast = TimelineForecast(
        shortTermProb = probPatternContinues + 5,
        shortTermDesc = "Status quo patterns persist or consolidate in current friction spaces.",
        midTermProb = probInterventionWorks + 10,
        midTermDesc = "Systemic pressure prompts active strategic pivots or incentive modifications.",
        longTermProb = abs(probPatternContinues - 25).coerceAtLeast(5),
        longTermDesc = "Complete stabilization or system restructuring occurs as primary resources exhaust.",
        explanation = "Temporal trajectories stabilize as emotional drivers exhaust and raw incentives shift toward structural settlement."
    )

    // Risks
    val highRisks = let {
        val list = mutableListOf<String>()
        if (rawText.contains("trust", ignoreCase = true)) {
            list.add("Trust deficits: Defensive barriers block direct negotiation.")
        } else {
            list.add("Boundary Erosion: Competing expectations lead to severe misalignments.")
        }
        if (rawText.contains("money", ignoreCase = true) || rawText.contains("financial", ignoreCase = true)) {
            list.add("Financial Exposure: Unclear liabilities and lock-in contracts create leakage.")
        } else {
            list.add("Escalation Momentum: Minor friction loops cascade into full systemic blockades.")
        }
        list.add("Resource Drain: High cognitive load and exhaustion reduce operational capacity.")
        list
    }

    // Opportunities
    val highOpportunities = let {
        val list = mutableListOf<String>()
        if (rawText.contains("boundary", ignoreCase = true) || rawText.contains("need", ignoreCase = true)) {
            list.add("Explicit Contracting: Establish highly detailed expectations early.")
        } else {
            list.add("Symmetric Incentives: Re-align shared benefits to neutralize defense walls.")
        }
        list.add("System Restructuring: Decouple dependencies to eliminate friction loops.")
        list.add("Feedback Interdiction: Actively disrupt circular projection loops early.")
        list
    }

    // Risk Scores
    val riskConflict = 35 + abs(cleanSeed % 55)
    val riskTrust = 40 + abs(cleanSeed % 50)
    val riskFinancial = 15 + abs(cleanSeed % 60)
    val riskReputation = 20 + abs(cleanSeed % 55)

    // Highest Leverage Actions
    val highestLeverageActions = listOf(
        "Establish Explicit Structural Boundaries" to (75 + abs(cleanSeed % 20)),
        "Initiate Symmetric Incentive Re-alignment" to (65 + abs(cleanSeed % 20)),
        "Decouple Systemic Cascade Dependencies" to (55 + abs(cleanSeed % 20))
    )

    // Strategic Signals
    val signalStability = 20 + abs(cleanSeed % 65)
    val signalVolatility = 30 + abs(cleanSeed % 60)
    val signalEscalation = 15 + abs(cleanSeed % 75)
    val signalOpportunity = 40 + abs(cleanSeed % 50)
    val signalConsistency = 50 + abs(cleanSeed % 45)
    val signalPressure = 35 + abs(cleanSeed % 60)
}
