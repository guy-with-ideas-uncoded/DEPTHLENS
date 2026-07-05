package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.data.model.SessionEntity
import com.example.data.model.MemoryInsight
import com.example.data.model.ArchivedInsightEntity
import com.example.data.model.FutureScenario
import com.example.data.model.parseArchivedJson
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.components.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown

enum class DashboardNavState {
    Main,
    Category,
    Subtopic
}

// Normalise an insight body so near-identical insights extracted from different chats
// collapse to a single entry (case / whitespace / trailing punctuation ignored).
private fun normalizeInsightKey(s: String): String =
    s.lowercase().trim().replace(Regex("\\s+"), " ").trimEnd('.', '!', '?', ',', ';', ':')

// Derive the relevant "signs" for a group of insights. Only meaningful signals are
// returned (max 2), so tags aren't slapped on every single row.
private fun signsFor(insights: List<MemoryInsight>): List<Pair<String, Color>> {
    val hay = insights.joinToString(" ") { (it.category + " " + it.content) }.lowercase()
    val out = mutableListOf<Pair<String, Color>>()
    val critical = listOf("risk", "fear", "critical", "threat", "danger", "urgent", "warning").any { hay.contains(it) }
    val blocker = listOf("block", "avoid", "stuck", "procrastinat", "resist", "sabotage").any { hay.contains(it) }
    val positive = listOf("strength", "progress", "growth", "breakthrough", "opportunit", "confiden", "momentum").any { hay.contains(it) }
    val recurring = insights.size >= 3

    if (critical) out += "CRITICAL" to ErrorColor
    if (blocker && out.size < 2) out += "BLOCKER" to WarningColor
    if (recurring && out.size < 2) out += "RECURRING" to PremiumCyan
    if (positive && out.size < 2) out += "POSITIVE" to SuccessColor
    return out.take(2)
}

@Composable
fun AnalysisScreen(
    activeMessages: List<MessageEntity>,
    selectedMode: String,
    onBackToHome: () -> Unit,
    onSubmitQuery: (String) -> Unit = {},
    deepDiveInsights: Map<String, String> = emptyMap(),
    isDeepDiveLoading: Map<String, Boolean> = emptyMap(),
    onGenerateDeepDive: (String, String) -> Unit = { _, _ -> },
    sessions: List<SessionEntity> = emptyList(),
    memoryInsights: List<MemoryInsight> = emptyList(),
    archivedInsights: List<ArchivedInsightEntity> = emptyList(),
    probabilityForecast: List<FutureScenario> = emptyList(),
    modifier: Modifier = Modifier
) {
    var navState by remember { mutableStateOf(DashboardNavState.Main) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubtopic by remember { mutableStateOf<String?>(null) }

    val handleBack = {
        when (navState) {
            DashboardNavState.Subtopic -> {
                navState = DashboardNavState.Category
                selectedSubtopic = null
            }
            DashboardNavState.Category -> {
                navState = DashboardNavState.Main
                selectedCategory = null
            }
            DashboardNavState.Main -> {
                onBackToHome()
            }
        }
    }

    // System back must retrace the drill-down (Subtopic → Category → Main) instead of
    // jumping straight out. Only intercepts while drilled in; at Main it's disabled so
    // the parent handles leaving the Insights tab.
    androidx.activity.compose.BackHandler(enabled = navState != DashboardNavState.Main) {
        handleBack()
    }

    // Theme-aware tokens (previously hard-coded Deep Sea → screen never changed with theme).
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor

    Column(
        modifier = modifier
            .fillMaxSize()
            // Background drawn once by the DashboardScreen root — no second layer here.
            .statusBarsPadding()
    ) {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .depthGlass(borderWidth = 1.dp, showSpecularHighlight = false)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navState != DashboardNavState.Main) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.96f) { handleBack() }
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 28.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(38.dp))
            }
            Text(
                text = when (navState) {
                    DashboardNavState.Main -> "Intelligence Dashboard"
                    DashboardNavState.Category -> selectedCategory ?: "Insights"
                    DashboardNavState.Subtopic -> selectedSubtopic ?: "Insights"
                },
                fontSize = 15.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.size(38.dp)) // Equalizer
        }

        // Dynamic statistics calculation
        val sessionsCount = sessions.size

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(44.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ElectricViolet.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(44.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◈", fontSize = 38.sp, color = ElectricViolet)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No insights yet",
                        fontSize = 20.sp,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a conversation to let DepthLens extract patterns, analyze root causes, and model potential future pathways.",
                        fontSize = 12.sp,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBackToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Start Conversation", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // De-duplicate memory insights extracted across different chats.
            val distinctInsights = remember(memoryInsights) {
                val seen = HashSet<String>()
                memoryInsights.filter { seen.add(normalizeInsightKey(it.content)) }
            }
            val groupedInsights = remember(distinctInsights) {
                distinctInsights.groupBy { it.category }
            }
            val insightsCount = distinctInsights.size
            val clarity = when {
                sessionsCount == 0 -> "None"
                insightsCount < 2 -> "Low"
                insightsCount < 5 -> "Medium"
                else -> "High"
            }

            AnimatedContent(
                targetState = navState,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(300)) { it } togetherWith
                            slideOutHorizontally(animationSpec = tween(300)) { -it / 2 }
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { -it / 2 } togetherWith
                            slideOutHorizontally(animationSpec = tween(300)) { it }
                    }
                },
                modifier = Modifier.weight(1f),
                label = "DashboardNav"
            ) { state ->
                val scrollStateInner = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollStateInner)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state) {
                        DashboardNavState.Main -> {
                            // 3-Card Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    value = sessionsCount.toString(),
                                    label = "Sessions",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    value = insightsCount.toString(),
                                    label = "Insights",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    value = clarity,
                                    label = "Clarity",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Section 1: MEMORY INSIGHTS
                            Text(
                                text = "MEMORY INSIGHTS",
                                fontSize = 10.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                color = labelViolet,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )

                            if (distinctInsights.isEmpty()) {
                                InsightCard(
                                    title = "No memory insights compiled yet",
                                    description = "Start a session and talk with DepthLens. Over time, recurring patterns, personal motivations, and strategic insights will automatically be extracted and indexed here."
                                )
                            } else {
                                groupedInsights.forEach { (category, insights) ->
                                    val countLabel = if (insights.size == 1) "1 insight" else "${insights.size} insights"
                                    val displayName = category.ifBlank { "General" }

                                    InsightOptionRow(
                                        title = "$displayName  ·  $countLabel",
                                        signs = signsFor(insights),
                                        onClick = {
                                            selectedCategory = category
                                            navState = DashboardNavState.Category
                                        }
                                    )
                                }
                            }

                            // Section 2: PROBABILITY FORECAST
                            // Merge scenarios that repeat across chats (same displayName).
                            val scenarios = remember(probabilityForecast) {
                                val seen = HashSet<String>()
                                probabilityForecast.filter { seen.add(it.displayName.trim().lowercase()) }
                            }

                            Text(
                                text = "PROBABILITY FORECAST",
                                fontSize = 10.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                color = labelViolet,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )

                            if (scenarios.isNotEmpty()) {
                                Text(
                                    text = "Probability of each way things could unfold from here. Tap a row to see what drives it.",
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = textMuted,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .depthGlass(cornerRadius = 20.dp, borderWidth = 1.dp)
                                        .padding(vertical = 4.dp)
                                ) {
                                    scenarios.forEachIndexed { index, scenario ->
                                        val color = when {
                                            scenario.displayName.contains("Most Likely", ignoreCase = true) || scenario.codeName.contains("Scenario A", ignoreCase = true) -> PremiumCyan
                                            scenario.displayName.contains("Positive", ignoreCase = true) || scenario.codeName.contains("Scenario B", ignoreCase = true) -> labelViolet
                                            scenario.displayName.contains("Risk", ignoreCase = true) || scenario.codeName.contains("Scenario C", ignoreCase = true) -> ErrorColor
                                            else -> textMuted
                                        }
                                        ForecastRow(
                                            label = scenario.displayName,
                                            percentage = "${scenario.probability}%",
                                            detail = scenario.impactText,
                                            color = color
                                        )
                                        if (index < scenarios.lastIndex) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .depthGlass(cornerRadius = 20.dp, borderWidth = 1.dp)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No forecast computed yet",
                                        fontSize = 13.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Ask DepthLens a future-oriented question in any session to generate branching trajectory projections and real-time probability forecasts.",
                                        fontSize = 11.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = textMuted,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                        DashboardNavState.Category -> {
                            val catInsights = groupedInsights[selectedCategory] ?: emptyList()
                            // Group by sub-topic, merging near-identical topic names
                            // (case/space-insensitive) so the same topic isn't listed twice.
                            val subtopics = catInsights.groupBy {
                                val raw = it.content.split("|", limit = 2).firstOrNull()?.trim().orEmpty()
                                    .ifBlank { it.category }
                                raw.lowercase().replace(Regex("\\s+"), " ")
                            }

                            subtopics.forEach { (_, insights) ->
                                val subtopicName = insights.first().content.split("|", limit = 2)
                                    .firstOrNull()?.trim().orEmpty().ifBlank { insights.first().category }
                                val countLabel = if (insights.size == 1) "1 insight" else "${insights.size} insights"

                                InsightOptionRow(
                                    title = "$subtopicName  ·  $countLabel",
                                    signs = signsFor(insights),
                                    onClick = {
                                        selectedSubtopic = subtopicName
                                        navState = DashboardNavState.Subtopic
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                        DashboardNavState.Subtopic -> {
                            val catInsights = groupedInsights[selectedCategory] ?: emptyList()
                            val selKey = (selectedSubtopic ?: "").lowercase().replace(Regex("\\s+"), " ")
                            val subtopicInsights = catInsights.filter {
                                val title = (it.content.split("|", limit = 2).firstOrNull()?.trim().orEmpty()
                                    .ifBlank { it.category })
                                    .lowercase().replace(Regex("\\s+"), " ")
                                title == selKey
                            }

                            // Dedupe the actual insight bodies again so the same point extracted
                            // from several chats isn't listed multiple times inside a subtopic.
                            val seenDesc = HashSet<String>()
                            subtopicInsights.forEach { insight ->
                                val parts = insight.content.split("|", limit = 2)
                                val description = if (parts.size > 1) parts[1].trim() else insight.content.trim()
                                if (!seenDesc.add(normalizeInsightKey(description))) return@forEach

                                val firstSentence = description.split(Regex("[.!?]")).firstOrNull()?.trim() ?: ""
                                val derivedTitle = if (firstSentence.length > 50) {
                                    firstSentence.take(47) + "..."
                                } else if (firstSentence.isNotBlank()) {
                                    firstSentence
                                } else {
                                    description.split(" ").take(5).joinToString(" ") + "..."
                                }

                                InsightCard(
                                    title = derivedTitle,
                                    description = description
                                )
                            }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

// Small pill tag shown on the right of a memory-insight option row.
@Composable
private fun InsightTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.5.sp,
            fontFamily = DMMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            color = color
        )
    }
}

// A memory-insight category/subtopic row with contextual side tags (only where relevant).
@Composable
private fun InsightOptionRow(
    title: String,
    signs: List<Pair<String, Color>>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleOnPress = 0.96f) { onClick() }
            .depthGlass(cornerRadius = 20.dp, borderWidth = 1.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontFamily = InstrumentSansFontFamily,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryColor,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            signs.forEach { (label, color) -> InsightTag(label, color) }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open",
                tint = TextMutedColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val accentGrad = Brush.linearGradient(listOf(ElectricViolet, GradientEnd))

    Box(
        modifier = modifier
            .depthGlass(cornerRadius = 20.dp, borderWidth = 1.dp)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(brush = accentGrad),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Medium,
                color = TextMutedColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .depthGlass(cornerRadius = 20.dp, borderWidth = 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontFamily = InstrumentSansFontFamily,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryColor
        )
        Text(
            text = description,
            fontSize = 11.sp,
            fontFamily = InstrumentSansFontFamily,
            fontWeight = FontWeight.Normal,
            color = TextMutedColor,
            lineHeight = 16.sp
        )
    }
}

// Tap-to-expand forecast row: shows the % and, on tap, reveals what that % is about.
@Composable
fun ForecastRow(
    label: String,
    percentage: String,
    detail: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "forecast_arrow"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Normal,
                color = TextPrimaryColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = percentage,
                fontSize = 13.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextMutedColor,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(arrowRotation)
            )
        }
        if (expanded && detail.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = detail,
                fontSize = 11.sp,
                fontFamily = InstrumentSansFontFamily,
                fontWeight = FontWeight.Normal,
                color = TextMutedColor,
                lineHeight = 16.sp
            )
        }
    }
}
