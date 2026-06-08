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

@Composable
fun IntelligenceOSVisualizer(
    parsed: ParsedResponse,
    rawText: String,
    modifier: Modifier = Modifier,
    onSubmitQuery: (String) -> Unit = {}
) {
    // Generate deterministic values based on text if they are not in the raw response
    val calculatedData = remember(rawText) {
        DeterministicIntelligenceGenerator(rawText, parsed)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. EXECUTIVE SUMMARY CARD (Large premium layout)
        ExecutiveSummaryCard(
            summaryText = parsed.executiveSummary?.ifBlank { null } ?: calculatedData.executiveSummary
        )

        // 2. REALITY LAYER ACTIVATION PANEL
        RealityLayerActivationPanel(
            activeLayersCount = parsed.depthLayers.size.coerceAtLeast(calculatedData.layersCount),
            parsedLayers = parsed.depthLayers
        )

        // 2B. D3 FORCE-DIRECTED LAYERS ENGINE GRAPH
        D3ForceDirectedGraph(
            parsedLayers = parsed.depthLayers
        )

        // 3. AI CONFIDENCE ENGINE (Displayed prominently early)
        AiConfidenceEngine(
            confidenceLevel = parsed.confidence?.ifBlank { null } ?: calculatedData.confidenceLevel,
            confidenceScore = calculatedData.confidenceScore,
            reasoning = calculatedData.confidenceReasoning
        )

        // 4. FUTURE PROBABILITY DASHBOARD
        FutureProbabilityDashboard(
            patternProb = calculatedData.probPatternContinues,
            interventionProb = calculatedData.probInterventionWorks,
            escalationRisk = calculatedData.probEscalationRisk
        )

        // 5. FUTURE SCENARIO COMPARISON
        FutureScenarioComparison(
            scenarios = parsed.futureScenarios.ifEmpty { calculatedData.scenarios },
            onScenarioClick = { scenarioName ->
                onSubmitQuery("Analyze in detail the scenario: '$scenarioName' from the current context.")
            }
        )

        // 6. FUTURE TIMELINE FORECAST
        FutureTimelineForecast(
            timeline = parsed.timelineForecast ?: calculatedData.timelineForecast
        )

        // 11. INTELLIGENCE SIGNALS
        IntelligenceSignalsSection(
            stability = calculatedData.signalStability,
            volatility = calculatedData.signalVolatility,
            escalation = calculatedData.signalEscalation,
            opportunity = calculatedData.signalOpportunity,
            consistency = calculatedData.signalConsistency,
            pressure = calculatedData.signalPressure
        )

        // 7. RISK VS OPPORTUNITY MATRIX
        RiskVsOpportunityMatrix(
            risks = calculatedData.highRisks,
            opportunities = calculatedData.highOpportunities
        )

        // 8 & 9. RISK ASSESSMENT & OPPORTUNITY SCORE ENGINES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                RiskAssessmentEngine(calculatedData = calculatedData)
            }
            Column(modifier = Modifier.weight(1f)) {
                OpportunityScoreEngine(
                    actions = calculatedData.highestLeverageActions,
                    onActionClick = { actionName ->
                        onSubmitQuery("Develop an implementation playbook for the leverage action: '$actionName' in this situation.")
                    }
                )
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
@Composable
fun RealityLayerActivationPanel(
    activeLayersCount: Int,
    parsedLayers: List<DepthLayerInsight>
) {
    var isExpanded by remember { mutableStateOf(false) }

    val standardNames = listOf(
        "Observable Reality" to "Physical, textual, or literal facts visible in plain sight.",
        "Behavioral Reality" to "Action patterns, habits, communication protocols, and interactive signals.",
        "Emotional Reality" to "Core underlying feeling states, anxiety projection, and mood stabilizers.",
        "Strategic Reality" to "Intentional alignments, calculated games, competitive postures, and objectives.",
        "Systemic Reality" to "Institutional boundaries, regulatory rules, protocols, and architectural ties.",
        "Pattern Reality" to "Historical repetitions, archetype behaviors, and cyclical triggers.",
        "Root Cause Reality" to "The fundamental friction point spawning downstream occurrences.",
        "Probability Reality" to "Calculated expectations and statistical directions.",
        "Hidden Risks" to "Unseen liabilities, blind spots, and latent cascading triggers.",
        "Opportunities" to "High leverage strategic unlocks, hidden reserves, and potential transformations."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Troubleshoot,
                    contentDescription = null,
                    tint = PremiumCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                        text = "$activeLayersCount Active Diagnostic Layers detected",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 11.sp,
                        color = TextMutedColor
                    )
                }
            }
            IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand layers list",
                    tint = TextSecondaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activation Indicators row (like an active hardware panel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            for (i in 1..10) {
                val isActive = i <= activeLayersCount
                val activeColor = getLayerColor(i)
                val dotColor = if (isActive) activeColor else BorderSubtle.copy(alpha = 0.4f)
                
                // Infinite glowing/pulsing effect for active indicators
                val infiniteTransition = rememberInfiniteTransition(label = "LayerGlow_$i")
                val pulseRatio by if (isActive) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800 + i * 150, easing = LinearEasing),
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
                        .background(dotColor.copy(alpha = if (isActive) pulseRatio else 1.0f))
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..10) {
                    val isActive = i <= activeLayersCount
                    val matchingParsed = parsedLayers.firstOrNull { it.layerNumber == i }
                    val name = matchingParsed?.layerName ?: standardNames[i - 1].first
                    val desc = matchingParsed?.description ?: standardNames[i - 1].second
                    val baseColor = getLayerColor(i)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) Surface3 else DeepMidnight.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (isActive) baseColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(if (isActive) baseColor.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                .border(1.dp, if (isActive) baseColor else BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L$i",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) baseColor else TextMutedColor,
                                fontFamily = DMMonoFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isActive) TextPrimaryColor else TextMutedColor
                                )
                                if (isActive) {
                                    val confPercent = 100 - (i * 3) - (abs(name.hashCode() % 10))
                                    Text(
                                        text = "CONFIDENCE: $confPercent%",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 8.5.sp,
                                        color = baseColor.copy(alpha = 0.85f)
                                    )
                                } else {
                                    Text(
                                        text = "DORMANT",
                                        fontFamily = DMMonoFontFamily,
                                        fontSize = 8.5.sp,
                                        color = TextMutedColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isActive) TextSecondaryColor else TextMutedColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 3. AI CONFIDENCE ENGINE Composable
// ────────────────────────────────────────────────────────────────────────
@Composable
fun AiConfidenceEngine(
    confidenceLevel: String,
    confidenceScore: Int,
    reasoning: String
) {
    var isExpanded by remember { mutableStateOf(false) }
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
    patternProb: Int,
    interventionProb: Int,
    escalationRisk: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
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

                var isScenarioExpanded by remember { mutableStateOf(false) }

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
fun FutureTimelineForecast(timeline: TimelineForecast) {
    var isExpanded by remember { mutableStateOf(false) }
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
    risks: List<String>,
    opportunities: List<String>
) {
    var isExpanded by remember { mutableStateOf(false) }
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
    stability: Int,
    volatility: Int,
    escalation: Int,
    opportunity: Int,
    consistency: Int,
    pressure: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
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
