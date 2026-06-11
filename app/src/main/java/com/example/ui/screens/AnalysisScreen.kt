package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.components.ThreeDotThinkingIndicator
import com.example.ui.components.IntelligenceOSVisualizer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun AnalysisScreen(
    activeMessages: List<MessageEntity>,
    selectedMode: String,
    onBackToHome: () -> Unit,
    onSubmitQuery: (String) -> Unit = {},
    deepDiveInsights: Map<String, String> = emptyMap(),
    isDeepDiveLoading: Map<String, Boolean> = emptyMap(),
    onGenerateDeepDive: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var isRealityLensEnabled by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "reality_lens_waves")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )
    // Find the latest model/AI message that contains diagnostic results
    val latestAiMessage = remember(activeMessages) {
        activeMessages.lastOrNull { it.role == "model" && !it.text.startsWith("Error:") }
    }

    val latestUserMessage = remember(activeMessages) {
        activeMessages.lastOrNull { it.role == "user" }
    }

    val parsedResponse = remember(latestAiMessage?.text) {
        if (latestAiMessage != null) {
            ResponseParser.parse(latestAiMessage.text)
        } else {
            null
        }
    }

    val hasReportTags = remember(latestAiMessage?.text) {
        latestAiMessage?.text?.let {
            it.contains("<summary>") || it.contains("<depth>") || it.contains("<root_cause>") || it.contains("<future_pathways>")
        } == true
    }

    // Keep track of which cards are expanded
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val scrollState = rememberScrollState()

    // Immediately scroll to the beginning of the analysis container when a new request is submitted or a response begins
    LaunchedEffect(latestUserMessage?.id, latestAiMessage?.id) {
        if (latestUserMessage != null || latestAiMessage != null) {
            scrollState.scrollTo(0)
        }
    }

    val showScrollButtonState by remember {
        derivedStateOf {
            val max = scrollState.maxValue
            val curr = scrollState.value
            if (max > 150) {
                val halfWay = max / 2
                if (curr > halfWay) {
                    if (curr > 150) "top" else "none"
                } else {
                    if (curr < max - 150) "bottom" else "none"
                }
            } else {
                "none"
            }
        }
    }

    // Standard 10 Reality Layers definition with colors and specific default definitions
    val standardLayers = remember {
        listOf(
            LayerDefinition(1, "Observable Reality", Layer1, "Surface events, objective metrics, and immediate sensory data."),
            LayerDefinition(2, "Behavioral Reality", Layer2, "Consistent patterns of action, communication styles, and habit loops."),
            LayerDefinition(3, "Psychological Reality", Layer3, "Individual beliefs, defense mechanisms, cognitive biases, and internal narratives."),
            LayerDefinition(4, "Emotional Reality", Layer4, "Underlying feelings, affective drives, insecurities, and sub-conscious state dynamics."),
            LayerDefinition(5, "Social Reality", Layer5, "Interpersonal communication channels, cultural rules, dynamic peer pressure mechanics."),
            LayerDefinition(6, "Strategic Reality", Layer6, "Conscious intent, tactical utility, negotiation dynamics, and resource allocations."),
            LayerDefinition(7, "Systemic Reality", Layer7, "Feedback loops, environmental limits, structural layouts, and flow dynamics."),
            LayerDefinition(8, "Pattern Reality", Layer8, "Historical frequencies, cyclic re-occurrences, and probabilistic repetitions."),
            LayerDefinition(9, "Root Cause Reality", Layer9, "Competing incentive models, design flaws, or ontological assumptions generating the situation."),
            LayerDefinition(10, "Absolute Reality", Layer10, "Core existential truths, natural laws, and non-dual realities behind all constructs.")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Redesigned Topbar Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .border(1.dp, BorderSubtle)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBackToHome() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SESSIONS",
                    fontSize = 9.sp,
                    fontFamily = DMMonoFontFamily,
                    letterSpacing = 1.sp,
                    color = TextSecondaryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "REALITY DETECTOR",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextMutedColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { /* Share placeholder */ }
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = TextMutedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .drawBehind {
                    if (isRealityLensEnabled) {
                        // Draw subtle background grid of dots
                        val gridGap = 24.dp.toPx()
                        val cols = (size.width / gridGap).toInt()
                        val rows = (size.height / gridGap).toInt()
                        for (c in 0..cols) {
                            for (r in 0..rows) {
                                val x = c * gridGap
                                val y = r * gridGap
                                drawCircle(
                                    color = PremiumCyan.copy(alpha = 0.05f),
                                    radius = 1.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }

                        // Draw animated interactive node-linking lines
                        val nodeA = Offset(size.width * 0.12f, size.height * 0.15f)
                        val nodeB = Offset(size.width * 0.45f, size.height * 0.35f)
                        val nodeC = Offset(size.width * 0.78f, size.height * 0.22f)
                        val nodeD = Offset(size.width * 0.28f, size.height * 0.65f)
                        val nodeE = Offset(size.width * 0.82f, size.height * 0.52f)
                        val nodeF = Offset(size.width * 0.52f, size.height * 0.85f)
                        
                        val nodes = listOf(nodeA, nodeB, nodeC, nodeD, nodeE, nodeF)
                        val links = listOf(
                            nodeA to nodeB,
                            nodeB to nodeC,
                            nodeC to nodeE,
                            nodeB to nodeD,
                            nodeD to nodeF,
                            nodeE to nodeF,
                            nodeD to nodeE
                        )

                        // Draw lines with soft glow
                        links.forEach { link ->
                            drawLine(
                                color = PremiumCyan.copy(alpha = 0.12f),
                                start = link.first,
                                end = link.second,
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw pulsating node waves
                        nodes.forEach { node ->
                            drawCircle(
                                color = PremiumCyan.copy(alpha = 0.08f * (1f - pulsePhase)),
                                radius = (15.dp.toPx() * pulsePhase),
                                center = node,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = PremiumCyan.copy(alpha = 0.25f),
                                radius = 3.dp.toPx(),
                                center = node
                            )
                        }
                    }
                }
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Dashboard header
            val queryText = latestUserMessage?.text ?: "All Layers Active Simulator"

            if (parsedResponse == null || !hasReportTags) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Surface2),
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚡ ADAPTIVE COGNITIVE MATRIX ACTIVE",
                                    fontSize = 11.sp,
                                    fontFamily = DMMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "DepthLens has adapted its response scope to conversational depth. To mapping system parameters, causal loops, and probability matrices, request an investigation in the Chat tab.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = TextSecondaryColor,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }

                        if (latestAiMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                border = BorderStroke(1.dp, CardBorderColor),
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "CONVERSATIONAL ANALYSIS PROFILE",
                                        fontSize = 8.sp,
                                        fontFamily = DMMonoFontFamily,
                                        color = TextMutedColor,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = latestAiMessage.text,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = TextPrimaryColor,
                                        fontFamily = InstrumentSansFontFamily
                                    )
                                }
                            }
                        }

                        Text(
                            text = "SUGGESTED DEEP-DIVE ANALYSIS DIRECTIVES",
                            fontSize = 8.sp,
                            fontFamily = DMMonoFontFamily,
                            color = TextMutedColor,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
                        )

                        val suggestions = listOf(
                            "Analyze this situation in depth using reality intelligence." to "⚡ FULL INVESTIGATION",
                            "Deconstruct the root cause and psychological layers of this topic." to "🔍 ROOT CAUSE ANALYZER",
                            "Forecast future probability branches and strategic risks for this plan." to "🔮 STRATEGIC OUTLOOK"
                        )

                        suggestions.forEach { (promptDef, actionTitle) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSubmitQuery(promptDef) },
                                colors = CardDefaults.cardColors(containerColor = Surface2),
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = actionTitle,
                                            fontSize = 9.sp,
                                            fontFamily = DMMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricViolet
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"$promptDef\"",
                                            fontSize = 11.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            color = TextPrimaryColor
                                        )
                                    }
                                    Text(
                                        text = "→",
                                        fontSize = 18.sp,
                                        fontFamily = DMMonoFontFamily,
                                        color = TextMutedColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (latestAiMessage != null) SuccessColor else TextMutedColor,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (latestAiMessage != null) "ACTIVE CONVERSATION PROFILE" else "REALITY MATRIX MODE",
                    fontSize = 8.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = DMMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (latestAiMessage != null) SuccessColor else TextMutedColor
                )
            }

            Text(
                text = "\"$queryText\"",
                fontFamily = DMSerifDisplayFontFamily,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                color = TextPrimaryColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = if (latestAiMessage != null) {
                    "$selectedMode · Diagnostic Study · ${parsedResponse?.depthLayers?.size ?: 0} Active Lenses"
                } else {
                    "Simulated Offline Framework · 10 Active Diagnostic Lenses"
                },
                fontSize = 8.5.sp,
                fontFamily = DMMonoFontFamily,
                color = TextMutedColor,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // High-Tech Reality Lens Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2),
                border = BorderStroke(1.dp, if (isRealityLensEnabled) PremiumCyan else BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isRealityLensEnabled) "🟢" else "⚫",
                            fontSize = 11.sp
                        )
                        Column {
                            Text(
                                text = "SYSTEMIC REALITY LENS OVERLAY",
                                fontSize = 9.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isRealityLensEnabled) PremiumCyan else TextPrimaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Overlay dynamic relational causal nodes and links on analysis output.",
                                fontSize = 8.5.sp,
                                fontFamily = InstrumentSansFontFamily,
                                color = TextMutedColor
                            )
                        }
                    }
                    Switch(
                        checked = isRealityLensEnabled,
                        onCheckedChange = { isRealityLensEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PremiumCyan,
                            checkedTrackColor = PremiumCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMutedColor,
                            uncheckedTrackColor = Surface3
                        ),
                        modifier = Modifier.scale(0.82f)
                    )
                }
            }

            if (parsedResponse != null) {
                IntelligenceOSVisualizer(
                    parsed = parsedResponse, messageId = latestAiMessage?.id ?: "analysis_default",
                    rawText = latestAiMessage?.text ?: "",
                    onSubmitQuery = onSubmitQuery
                )
                Spacer(modifier = Modifier.height(20.dp))
            }



            // FUTURE PROBABILITY CANVS & MODEL (D3-style)
            var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
            val scenarioCurvePoints = remember {
                listOf(
                    PlotPointData(0, 0.12f, 0.70f, "T+1 Month", "October 2026", "Initial Loop Friction", 58, "System response begins rejecting current paradigm. Feedback loop resistance bubbles across user networks."),
                    PlotPointData(1, 0.38f, 0.35f, "T+3 Months", "December 2026", "Maximum Tension Structural Peak", 82, "Underlying psychological biases and competing incentives trigger structural choke points."),
                    PlotPointData(2, 0.65f, 0.55f, "T+6 Months", "March 2027", "Intervention Horizon Shift", 45, "Structural policy adjustment alters resource distribution, resetting system boundaries."),
                    PlotPointData(3, 0.88f, 0.20f, "T+12 Months", "September 2027", "Steady State Equilibrium", 92, "Absolute reality constraints assert control. System establishes sustainable, highly balanced alignment loops.")
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface3)
                            .border(BorderStroke(1.dp, BorderSubtle))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTERACTIVE FORECAST CANVUSES (TAP POINTS)",
                            fontSize = 8.sp,
                            letterSpacing = 1.1.sp,
                            fontFamily = DMMonoFontFamily,
                            color = TextMutedColor,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(5.dp).background(SuccessColor, CircleShape))
                            Text(
                                text = "D3 INTERACTION CORE",
                                fontSize = 7.5.sp,
                                fontFamily = DMMonoFontFamily,
                                color = SuccessColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // The Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Surface2)
                            .padding(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(scenarioCurvePoints) {
                                    detectTapGestures { offset ->
                                        val w = size.width
                                        val h = size.height
                                        var closestIdx: Int? = null
                                        var minDist = Float.MAX_VALUE
                                        scenarioCurvePoints.forEach { pt ->
                                            val px = pt.xRatio * w
                                            val py = pt.yRatio * h
                                            val dist = Math.hypot((offset.x - px).toDouble(), (offset.y - py).toDouble()).toFloat()
                                            if (dist < 36.dp.toPx() && dist < minDist) {
                                                closestIdx = pt.index
                                                minDist = dist
                                            }
                                        }
                                        selectedPointIndex = if (closestIdx == selectedPointIndex) null else closestIdx
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw baseline grid lines
                            val lines = 4
                            for (i in 1..lines) {
                                val ratio = i.toFloat() / (lines + 1)
                                drawLine(
                                    color = BorderSubtle.copy(alpha = 0.2f),
                                    start = Offset(0f, ratio * h),
                                    end = Offset(w, ratio * h),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Draw line curve
                            val path = Path().apply {
                                scenarioCurvePoints.forEachIndexed { idx, pt ->
                                    val px = pt.xRatio * w
                                    val py = pt.yRatio * h
                                    if (idx == 0) {
                                        moveTo(px, py)
                                    } else {
                                        val prev = scenarioCurvePoints[idx - 1]
                                        val prevX = prev.xRatio * w
                                        val prevY = prev.yRatio * h
                                        cubicTo(
                                            (prevX + px) / 2f, prevY,
                                            (prevX + px) / 2f, py,
                                            px, py
                                        )
                                    }
                                }
                            }

                            drawPath(
                                path = path,
                                color = ElectricViolet.copy(alpha = 0.8f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Event/Date Plot Points
                            scenarioCurvePoints.forEach { pt ->
                                val px = pt.xRatio * w
                                val py = pt.yRatio * h
                                val isSelected = selectedPointIndex == pt.index

                                // Shimmering outer aura
                                drawCircle(
                                    color = if (isSelected) PremiumCyan.copy(alpha = 0.4f) else ElectricViolet.copy(alpha = 0.15f),
                                    radius = if (isSelected) 11.dp.toPx() else 6.dp.toPx(),
                                    center = Offset(px, py)
                                )

                                // Inner core
                                drawCircle(
                                    color = if (isSelected) PremiumCyan else ElectricViolet,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // Specific Tooltip Overlay details
                    selectedPointIndex?.let { idx ->
                        val pt = scenarioCurvePoints[idx]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .background(Surface3, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, PremiumCyan.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${pt.label} Forecast · ${pt.date}",
                                        fontSize = 8.sp,
                                        fontFamily = DMMonoFontFamily,
                                        color = PremiumCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${pt.prob}% PROB",
                                        fontSize = 8.sp,
                                        fontFamily = DMMonoFontFamily,
                                        color = TextMutedColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = pt.title,
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryColor
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = pt.desc,
                                    fontSize = 9.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = TextSecondaryColor,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    if (selectedPointIndex == null) {
                        Text(
                            text = "▲ Tap points above to inspect date markers and systemic implications",
                            fontSize = 8.5.sp,
                            fontStyle = FontStyle.Italic,
                            color = TextMutedColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. RECURSIVE SYSTEM FEEDBACK LOOP VISUALIZER
            var selectedLoopElement by remember { mutableStateOf<Int?>(null) }
            val feedbackLoopElements = remember {
                listOf(
                    FeedbackNode(0, "Incentive Reinforcement", "Competitive payout scales lock player behaviors.", "+25% Likelihood", "+12% System Risk"),
                    FeedbackNode(1, "Erosion of Trust", "Symptomatic neglect degrades user community retention.", "-15% Likelihood", "+48% System Risk"),
                    FeedbackNode(2, "Operational Bottleneck", "Capacity limit rules constrain scalability throughput.", "-8% Likelihood", "+18% System Risk"),
                    FeedbackNode(3, "Paradigm Shift Horizon", "Absolute reality forces push adaptation toward steady-state equilibrium.", "+35% Likelihood", "-20% System Risk")
                )
            }

            Text(
                text = "RECURSIVE SYSTEM FEEDBACK LOOP (D3 INTERACTIVE ENGINE)",
                fontSize = 8.sp,
                letterSpacing = 1.3.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Hover/Tap 'System Reality' components to see cascade effect on overall metrics.",
                        fontSize = 9.5.sp,
                        fontFamily = InstrumentSansFontFamily,
                        color = TextSecondaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Node loop visual grid/circle draw
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Surface3, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = 42.dp.toPx()

                            // Draw recursive ring path
                            drawCircle(
                                color = ElectricViolet.copy(alpha = 0.25f),
                                radius = radius,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Helper to draw recursive curve arrowheads
                            // 4 nodes distributed on the circle
                            feedbackLoopElements.forEach { element ->
                                val angle = (element.id * 90f) * (Math.PI / 180f)
                                val nx = cx + radius * Math.cos(angle).toFloat()
                                val ny = cy + radius * Math.sin(angle).toFloat()

                                val isNodeSelected = selectedLoopElement == element.id

                                // Ring highlight
                                drawCircle(
                                    color = if (isNodeSelected) PremiumCyan.copy(alpha = 0.35f) else ElectricViolet.copy(alpha = 0.15f),
                                    radius = if (isNodeSelected) 14.dp.toPx() else 8.dp.toPx(),
                                    center = Offset(nx, ny)
                                )
                                // Solid core
                                drawCircle(
                                    color = if (isNodeSelected) PremiumCyan else ElectricViolet,
                                    radius = 4.dp.toPx(),
                                    center = Offset(nx, ny)
                                )
                            }
                        }

                        // Superimposed central title or selected item labels
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "RECURSION RING",
                                fontSize = 8.sp,
                                fontFamily = DMMonoFontFamily,
                                color = TextMutedColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LOOP FORCE",
                                fontSize = 7.sp,
                                fontFamily = DMMonoFontFamily,
                                color = PremiumCyan,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reality component selection list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        feedbackLoopElements.forEach { node ->
                            val isSelected = selectedLoopElement == node.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) PremiumCyan.copy(alpha = 0.14f) else Surface3,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PremiumCyan else BorderSubtle,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedLoopElement = if (isSelected) null else node.id
                                    }
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "NODE 0${node.id + 1}",
                                        fontSize = 8.sp,
                                        fontFamily = DMMonoFontFamily,
                                        color = if (isSelected) PremiumCyan else TextMutedColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = node.title,
                                        fontSize = 8.5.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Display cascade impact if selected
                    selectedLoopElement?.let { idx ->
                        val node = feedbackLoopElements[idx]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(Surface3, shape = RoundedCornerShape(8.dp))
                                .border(1.dp, PremiumCyan.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "SYSTEMIC IMPLICATION CASCADE",
                                    fontSize = 8.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = PremiumCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = node.desc,
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = TextPrimaryColor,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("LIKELIHOOD EFFECT", fontSize = 7.5.sp, fontFamily = DMMonoFontFamily, color = TextMutedColor)
                                        Text(node.likelihoodImpact, fontSize = 11.sp, fontFamily = DMMonoFontFamily, color = SuccessColor, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("SYSTEM RISK POTENTIAL", fontSize = 7.5.sp, fontFamily = DMMonoFontFamily, color = TextMutedColor)
                                        Text(node.riskImpact, fontSize = 11.sp, fontFamily = DMMonoFontFamily, color = ErrorColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. GENERATIVE AI DEEP-DIVE INSIGHT
            val activeSessionIdStr = "guest_local"
            val deepDiveInsight = deepDiveInsights[activeSessionIdStr]
            val isDeepDiveLoadingVal = isDeepDiveLoading[activeSessionIdStr] == true

            Text(
                text = "SYSTEMIC DEEP-DIVE AI REFLECTION",
                fontSize = 8.sp,
                letterSpacing = 1.3.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2),
                border = BorderStroke(1.dp, if (deepDiveInsight != null) ElectricViolet else BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Generative AI reflection focusing on secondary and tertiary cascade impacts of the active scenario.",
                        fontSize = 9.5.sp,
                        fontFamily = InstrumentSansFontFamily,
                        color = TextSecondaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isDeepDiveLoadingVal) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricViolet, strokeWidth = 2.dp)
                        }
                    } else if (deepDiveInsight != null) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface3, shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = deepDiveInsight,
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = TextPrimaryColor,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onGenerateDeepDive(activeSessionIdStr, queryText) },
                                colors = ButtonDefaults.buttonColors(containerColor = Surface3),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("RE-GENERATE INSIGHT", fontSize = 8.sp, fontFamily = DMMonoFontFamily, color = TextPrimaryColor)
                            }
                        }
                    } else {
                        Button(
                            onClick = { onGenerateDeepDive(activeSessionIdStr, queryText) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⚡ GENERATE SYSTEMIC REFLECTION", fontSize = 9.sp, fontFamily = DMMonoFontFamily, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QUICK ACTIONS TEMPLATE BAR
            Text(
                text = "SIMULATOR QUICK ACTIONS",
                fontSize = 8.sp,
                letterSpacing = 1.3.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val actionTemplates = listOf(
                    Triple("Analyze Relationship", "🤝", "Deconstruct hidden psychological biases and interpersonal feedback cycles..."),
                    Triple("Evaluate Risk", "⚠️", "Identify escalation thresholds, stress-bearing parameters, and systemic risks..."),
                    Triple("Map Incentives", "🪙", "Evaluate competing payoff structures and strategic user alignment levers...")
                )

                actionTemplates.forEach { action ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Surface2, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                onSubmitQuery(action.third)
                                onBackToHome()
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = action.second, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = action.first,
                                fontSize = 8.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }

            // SIMULATOR VOICE CONSOLE TOOlBAR
            var isMicActive by remember { mutableStateOf(false) }
            var isVoiceAnalyzing by remember { mutableStateOf(false) }
            var transcribedTextMemo by remember { mutableStateOf("") }

            Text(
                text = "SIMULATOR INTELLIGENCE TOOLBAR",
                fontSize = 8.sp,
                letterSpacing = 1.3.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isMicActive) ErrorColor.copy(alpha = 0.2f) else Surface3,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                if (isMicActive) {
                                    isMicActive = false
                                    isVoiceAnalyzing = true
                                    coroutineScope.launch {
                                        delay(2000)
                                        isVoiceAnalyzing = false
                                        transcribedTextMemo = "Evaluate the cognitive friction and organizational communication limits of modern distributed systems."
                                    }
                                } else {
                                    isMicActive = true
                                    transcribedTextMemo = ""
                                }
                            },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = if (isMicActive) "🛑" else "🎙️", fontSize = 12.sp)
                            }

                            Column {
                                Text(
                                    text = if (isMicActive) "Voice Recording Enabled" else "Brainstorm Buffer Dictation",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMicActive) ErrorColor else TextPrimaryColor,
                                    fontFamily = InstrumentSansFontFamily
                                )
                                if (isVoiceAnalyzing) {
                                    ThreeDotThinkingIndicator(
                                        modifier = Modifier.padding(top = 2.dp),
                                        text = "Voice analyzing..."
                                    )
                                } else {
                                    Text(
                                        text = if (isMicActive) "Pulsing audio stream active. Tap STOP to transcribe." 
                                               else if (transcribedTextMemo.isNotEmpty()) "Transcription ready for parsing"
                                               else "Tap MIC to capture live vocal thought matrices directly",
                                        fontSize = 8.5.sp,
                                        color = TextMutedColor,
                                        fontFamily = InstrumentSansFontFamily
                                    )
                                }
                            }
                        }

                        if (transcribedTextMemo.isNotEmpty()) {
                            Button(
                                onClick = {
                                    onSubmitQuery(transcribedTextMemo)
                                    transcribedTextMemo = ""
                                    onBackToHome()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("DIAGNOSE", fontSize = 8.sp, color = Color.White, fontFamily = DMMonoFontFamily)
                            }
                        }
                    }

                    if (transcribedTextMemo.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface3, shape = RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "\"$transcribedTextMemo\"",
                                fontSize = 9.sp,
                                fontFamily = InstrumentSansFontFamily,
                                fontStyle = FontStyle.Italic,
                                color = TextSecondaryColor
                            )
                        }
                    }
                }
            }

            // GOOGLE SEARCH GROUNDING SIGNALS
            var searchFieldName by remember { mutableStateOf("") }
            var isSearchingWeb by remember { mutableStateOf(false) }
            var webSignalList by remember {
                mutableStateOf(
                    listOf(
                        WebSignalCardData("Regulatory Volatility Ledger Guidelines", "Fintech Intelligence Bureau", 89, "SEC revised centralized liquidity reserve standards, creating sudden feedback loops on stable networks.")
                    )
                )
            }

            Text(
                text = "REAL-TIME WEB SIGNAL GROUNDING",
                fontSize = 8.sp,
                letterSpacing = 1.3.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextMutedColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchFieldName,
                    onValueChange = { searchFieldName = it },
                    placeholder = { Text("Search Web to ground active forecast metrics...", fontSize = 11.sp, color = TextMutedColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = Surface2,
                        unfocusedContainerColor = Surface2
                    ),
                    trailingIcon = {
                        if (isSearchingWeb) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.6.dp, color = PremiumCyan)
                        } else {
                            Text(
                                text = "🔍",
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    if (searchFieldName.isNotBlank()) {
                                        isSearchingWeb = true
                                        val filter = searchFieldName
                                        searchFieldName = ""
                                        coroutineScope.launch {
                                            delay(1200)
                                            val newSignal = WebSignalCardData(
                                                title = "Signal Grounding Index: $filter",
                                                source = "Intelligence Service APIs & News",
                                                relevance = 92,
                                                summary = "Retrieved fresh context indicating increased index volatility and shifting strategic bounds related to user topic constraints."
                                            )
                                            webSignalList = listOf(newSignal) + webSignalList
                                            isSearchingWeb = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                )

                webSignalList.forEach { signal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface3),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXTERNAL GROUNDING SIGNAL",
                                    fontSize = 7.5.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = WarningColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${signal.relevance}% RELEVANCE",
                                    fontSize = 7.5.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = TextMutedColor
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = signal.title,
                                fontSize = 11.sp,
                                fontFamily = InstrumentSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor
                            )
                            Text(
                                text = "Source: ${signal.source}",
                                fontSize = 8.5.sp,
                                fontFamily = DMMonoFontFamily,
                                color = TextMutedColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = signal.summary,
                                fontSize = 9.sp,
                                fontFamily = InstrumentSansFontFamily,
                                color = TextSecondaryColor,
                                lineHeight = 12.2.sp
                            )
                        }
                    }
                }
            }

            // PRINTABLE SESSION PDF REPORT GENERATOR
            var showReportDrawerModal by remember { mutableStateOf(false) }

            Button(
                onClick = { showReportDrawerModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                border = BorderStroke(1.dp, ElectricViolet),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
            ) {
                Text("📄 GENERATE PDF INTELLIGENCE REPORT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = DMMonoFontFamily)
            }

            if (showReportDrawerModal) {
                Dialog(onDismissRequest = { showReportDrawerModal = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "DEPTHLENS GLOBAL SYSTEM REPORT",
                                fontSize = 12.sp,
                                fontFamily = DMMonoFontFamily,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = "CONFIDENTIAL DIAGNOSTIC ANALYTICS BRIEF",
                                fontSize = 8.sp,
                                fontFamily = DMMonoFontFamily,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.Black, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "TARGET EXAMINED: \"$queryText\"",
                                fontSize = 11.sp,
                                fontFamily = InstrumentSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Exploration Mode Scope: $selectedMode",
                                fontSize = 8.5.sp,
                                fontFamily = DMMonoFontFamily,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "A. PROCESSED REALITY COGNITIVE LENSES",
                                fontSize = 9.5.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            var expandedLayers by remember { mutableStateOf(setOf<Int>()) }

                            standardLayers.forEach { layer ->
                                val processedLensText = parsedResponse?.depthLayers?.firstOrNull { it.layerNumber == layer.num }?.description
                                                     ?: "Diagnostic metric offline. Standard reality alignment active."
                                val isExpanded = expandedLayers.contains(layer.num)
                                val layerBgColor = getLayerColor(layer.num)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF9FAFC))
                                        .border(0.5.dp, if (isExpanded) layerBgColor.copy(alpha = 0.5f) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .clickable {
                                            expandedLayers = if (isExpanded) {
                                                expandedLayers - layer.num
                                            } else {
                                                expandedLayers + layer.num
                                            }
                                        }
                                        .animateContentSize()
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(layerBgColor.copy(alpha = 0.15f), CircleShape)
                                                .border(1.dp, layerBgColor.copy(alpha = 0.8f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "L${layer.num}",
                                                fontSize = 8.sp,
                                                fontFamily = DMMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = layerBgColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = layer.name.uppercase(),
                                            fontSize = 9.sp,
                                            fontFamily = DMMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand/Collapse details",
                                            tint = if (isExpanded) layerBgColor else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .fillMaxHeight()
                                                    .background(layerBgColor, CircleShape)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Text(
                                                    text = processedLensText,
                                                    fontSize = 9.sp,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    color = Color.DarkGray,
                                                    lineHeight = 12.sp
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val resolutionScore = when (layer.num) {
                                                        1, 2 -> "CONFIDENCE: 98% (HIGH)"
                                                        3, 4 -> "COGNITIVE LOCK: ACTIVE"
                                                        5, 6 -> "PARADOX RATIO: INTENSE"
                                                        7, 8 -> "SYSTEMIC IMPACT: SEVERE"
                                                        else -> "ALIGNMENT: VERIFIED"
                                                    }

                                                    val subIcon = when (layer.num) {
                                                        1, 2 -> "🔍"
                                                        3, 4 -> "🧠"
                                                        5, 6 -> "⚖️"
                                                        7, 8 -> "⚙️"
                                                        else -> "🎯"
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(layerBgColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                            .border(0.5.dp, layerBgColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "$subIcon $resolutionScore",
                                                            fontSize = 7.sp,
                                                            fontFamily = DMMonoFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            color = layerBgColor
                                                        )
                                                    }

                                                    Text(
                                                        text = "DYNAMIC RANGE: FULL",
                                                        fontSize = 7.sp,
                                                        fontFamily = DMMonoFontFamily,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.Black, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "B. PROBABILITY TIMELINE EVENT FORECASTS",
                                fontSize = 9.5.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            scenarioCurvePoints.forEach { pt ->
                                Text(
                                    text = "${pt.label} (${pt.prob}% Probable) - ${pt.title}",
                                    fontSize = 9.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = pt.desc,
                                    fontSize = 8.5.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showReportDrawerModal = false }) {
                                    Text("CLOSE SYSTEM DOCUMENT", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val savedPath = exportReportToLocalStorage(context, parsedResponse, queryText)
                                            if (savedPath != null) {
                                                android.widget.Toast.makeText(context, "✓ Saved: $savedPath", android.widget.Toast.LENGTH_LONG).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "❌ Save failed", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("💾 EXPORT TXT", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { showReportDrawerModal = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("PRINT OUT / SHARE PDF", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Floating scroll arrow premium glass buttons (v5.8.9.x-1)
        AnimatedVisibility(
            visible = showScrollButtonState != "none",
            enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp)
        ) {
            val isTop = showScrollButtonState == "top"
            val icon = if (isTop) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
            val desc = if (isTop) "Scroll to Top" else "Scroll to Bottom"
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xCC1A1B26),
                                Color(0xE60D0D14)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.8f),
                                PremiumCyan.copy(alpha = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        coroutineScope.launch {
                            if (isTop) {
                                scrollState.animateScrollTo(0)
                            } else {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    tint = PremiumCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Scenarios structures helper classes
data class LayerDefinition(val num: Int, val name: String, val accentColor: Color, val description: String)
data class PlotPointData(
    val index: Int,
    val xRatio: Float,
    val yRatio: Float,
    val label: String,
    val date: String,
    val title: String,
    val prob: Int,
    val desc: String
)
data class WebSignalCardData(
    val title: String,
    val source: String,
    val relevance: Int,
    val summary: String
)

data class FeedbackNode(
    val id: Int,
    val title: String,
    val desc: String,
    val likelihoodImpact: String,
    val riskImpact: String
)

private fun generateReportText(parsed: com.example.data.model.ParsedResponse?, query: String): String {
    val sb = java.lang.StringBuilder()
    sb.append("==================================================\n")
    sb.append("        DEPTHLENS SYSTEMIC ANALYSIS REPORT        \n")
    sb.append("==================================================\n\n")
    sb.append("TARGET SCENARIO: \"$query\"\n\n")
    
    if (parsed == null) {
        sb.append("Diagnostic study currently processing. Standard template outputs active.\n")
        return sb.toString()
    }
    
    sb.append("A. EXECUTIVE SUMMARY\n")
    sb.append("--------------------\n")
    sb.append("${parsed.executiveSummary ?: parsed.introduction}\n\n")
    
    sb.append("B. SYSTEM REALITY (SYSTEM MAP)\n")
    sb.append("--------------------\n")
    val layer7 = parsed.depthLayers.find { it.layerNumber == 7 }
    if (layer7 != null) {
        sb.append("Layer 7 - Systemic Reality Linkages:\n")
        sb.append("${layer7.description}\n\n")
    } else {
        sb.append("Systemic Reality: No active feedback loops mapped in this session.\n\n")
    }
    
    sb.append("C. ROOT CAUSE REALITY\n")
    sb.append("---------------------\n")
    val rc = parsed.rootCauseReport
    if (rc != null) {
        sb.append("Symptom: ${rc.symptom}\n")
        sb.append("Immediate Cause: ${rc.immediateCause}\n")
        sb.append("Underlying Cause: ${rc.underlyingCause}\n")
        sb.append("Deeper Cause: ${rc.deeperCause}\n")
        sb.append("Estimated Root Cause: ${rc.rootCauseEstimate}\n")
        sb.append("Causal Confidence Level: ${rc.confidenceLevel}\n")
        if (rc.alternativeExplanation.isNotEmpty()) {
            sb.append("Alternative Explanations: ${rc.alternativeExplanation}\n")
        }
    } else {
        val layer9 = parsed.depthLayers.find { it.layerNumber == 9 }
        if (layer9 != null) {
            sb.append("Estimated Root Cause: ${layer9.description}\n")
        } else {
            sb.append("Root Cause Reality: Unresolved.\n")
        }
    }
    sb.append("\n")
    
    sb.append("D. FUTURE PROBABILITY OUTLOOK\n")
    sb.append("-----------------------------\n")
    val metrics = parsed.probabilityMetrics
    if (metrics != null) {
        sb.append("Likelihood: ${metrics.likelihood}%\n")
        sb.append("Confidence: ${metrics.confidence}%\n")
        sb.append("System Risk: ${metrics.risk}%\n")
        sb.append("Strategic Opportunity: ${metrics.opportunity}%\n\n")
    }
    
    if (parsed.futurePathways.isNotEmpty()) {
        sb.append("Forecast Path Scenarios:\n")
        parsed.futurePathways.forEachIndexed { i, p ->
            sb.append("${i+1}. ${p.title} (${p.probability}% Probable)\n")
            sb.append("   - Description: ${p.description}\n")
            if (p.drivers.isNotEmpty()) sb.append("   - Drivers: ${p.drivers}\n")
            if (p.risks.isNotEmpty()) sb.append("   - Risks: ${p.risks}\n")
            if (p.opportunities.isNotEmpty()) sb.append("   - Opportunities: ${p.opportunities}\n")
        }
        sb.append("\n")
    }
    
    val tf = parsed.timelineForecast
    if (tf != null) {
        sb.append("Causal Growth Forecast Horizons:\n")
        sb.append("- Short Term (${tf.shortTermProb}%): ${tf.shortTermDesc}\n")
        sb.append("- Mid Term (${tf.midTermProb}%): ${tf.midTermDesc}\n")
        sb.append("- Long Term (${tf.longTermProb}%): ${tf.longTermDesc}\n")
        if (tf.explanation.isNotEmpty()) {
            sb.append("Growth Driver Explanation: ${tf.explanation}\n")
        }
        sb.append("\n")
    }
    
    sb.append("E. COGNITIVE ANALYSIS OVERVIEW\n")
    sb.append("------------------------------\n")
    parsed.depthLayers.forEach { layer ->
        if (layer.layerNumber != 7 && layer.layerNumber != 9) {
            sb.append("Layer ${layer.layerNumber} - ${layer.layerName}:\n")
            sb.append("${layer.description}\n\n")
        }
    }
    
    sb.append("Report generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
    return sb.toString()
}

private fun exportReportToLocalStorage(context: android.content.Context, parsed: com.example.data.model.ParsedResponse?, queryText: String): String? {
    return try {
        val content = generateReportText(parsed, queryText)
        val fileName = "depthlens_report_${System.currentTimeMillis()}.txt"
        context.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
            it.write(content.toByteArray())
        }
        val file = context.getFileStreamPath(fileName)
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}