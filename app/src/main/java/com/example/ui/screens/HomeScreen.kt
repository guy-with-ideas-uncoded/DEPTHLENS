package com.example.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Download
import androidx.core.content.FileProvider
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.components.ThreeDotThinkingIndicator
import com.example.ui.components.IntelligenceOSVisualizer
import com.example.ui.components.DeepSynthesisPanel
import com.example.ui.components.SummaryOfInquiryPanel
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.Calendar
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalLayoutApi::class)
// Strips raw markdown symbols that Compose Text() cannot render  
private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("""\*\*(.+?)\*\*""", setOf(RegexOption.DOT_MATCHES_ALL)), "$1")
        .replace(Regex("""\*(.+?)\*""", setOf(RegexOption.DOT_MATCHES_ALL)), "$1")
        .replace(Regex("""__(.+?)__""", setOf(RegexOption.DOT_MATCHES_ALL)), "$1")
        .replace(Regex("""_(.+?)_""", setOf(RegexOption.DOT_MATCHES_ALL)), "$1")
        .replace(Regex("""^#{1,6}\s+""", setOf(RegexOption.MULTILINE)), "")
        .replace(Regex("""^>\s+""", setOf(RegexOption.MULTILINE)), "")
        .replace(Regex("""^[-*+]\s""", setOf(RegexOption.MULTILINE)), "• ")
        .replace("---", "").replace("***", "")
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    sessions: List<SessionEntity>,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    selectedDepth: String = "Standard Analysis",
    onDepthSelected: (String) -> Unit = {},
    isDeepThoughtEnabled: Boolean = false,
    onDeepThoughtToggle: (Boolean) -> Unit = {},
    onSessionSelected: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onAddAttachment: (String) -> Unit = {},
    archivedInsights: List<com.example.data.model.ArchivedInsightEntity> = emptyList(),
    onDeleteArchivedInsight: (String) -> Unit = {},
    activeMessages: List<com.example.data.model.MessageEntity> = emptyList(),
    isLoading: Boolean = false,
    onRetryLastAnalysis: (String) -> Unit = {},
    onRegenerateLastAnalysis: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onCreateNewSession: () -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    isPrivacyModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean("has_completed_permission_onboarding", false)) }
    
    var showPermissionOnboardingDialog by remember { mutableStateOf(false) }
    var showSystemSettingsPrompt by remember { mutableStateOf(false) }
    var showUploadSecurityNotice by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showUploadSecurityNotice) {
        if (showUploadSecurityNotice) {
            kotlinx.coroutines.delay(2500)
            showUploadSecurityNotice = false
        }
    }

    var rawText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        try { SpeechRecognizer.createSpeechRecognizer(context) } catch (e: Exception) { null }
    }

    val recognitionListener = remember(speechRecognizer) {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spoken = matches[0]
                    if (!spoken.isNullOrBlank()) {
                        rawText = if (rawText.isEmpty()) spoken else "$rawText $spoken"
                    }
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    LaunchedEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && speechRecognizer != null) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            speechRecognizer.startListening(intent)
        } else {
            showSystemSettingsPrompt = true
        }
    }

    var showAttachBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var messageToExport by remember { mutableStateOf<com.example.data.model.MessageEntity?>(null) }

    val attachPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showAttachBottomSheet = true
        } else {
            showSystemSettingsPrompt = true
        }
    }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            in 17..20 -> "GOOD EVENING"
            else -> "GOOD NIGHT"
        }
    }

    var animateEntry by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntry = true
    }
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (animateEntry) 0.dp else 16.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "slide"
    )
    val opacity by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animateEntry) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "opacity"
    )

    var editingMessageId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .statusBarsPadding()
            .imePadding()
    ) {
        val scrollState = rememberScrollState()

        // Immediately scroll to the beginning of the analysis container when a new request starts
        var wasLoadingState by remember { mutableStateOf(false) }
        LaunchedEffect(isLoading) {
            if (!wasLoadingState && isLoading) {
                // Immediately snap to the top of the analysis container
                scrollState.scrollTo(0)
            }
            wasLoadingState = isLoading
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

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {

            // Symmetrical Sized Header Row (v4.1.5 Sleek Design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left aligned: Menu/Hamburger Button
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface2)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open sessions",
                            tint = TextSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center aligned: Sleek Symmetrical DepthLens Logo (Compact & aligned v4.1.5)
                Image(
                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                    contentDescription = "DepthLens",
                    modifier = Modifier
                        .height(56.dp)
                        .padding(vertical = 1.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                // Right aligned: Upgraded Glassmorphism New Chat button
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface2)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { onCreateNewSession() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New chat",
                            tint = TextSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Animated Center Greeting Hero (only shown if Chat Feed is empty)
            if (activeMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .offset(y = slideOffset)
                        .alpha(opacity),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isPolarDawn = ThemeManager.themeName == "Polar Dawn"
                    val greetingColor = if (isPolarDawn) Color(0xFF2F2F2F) else Color.White
                    val greetingShadow = if (isPolarDawn) {
                        androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFDCD6F7).copy(alpha = 0.2f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = 4f
                        )
                    } else {
                        androidx.compose.ui.graphics.Shadow(
                            color = ElectricViolet.copy(alpha = 0.45f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    }

                    Text(
                        text = greeting,
                        style = TextStyle(
                            fontFamily = DMMonoFontFamily,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = greetingColor,
                            shadow = greetingShadow
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "DepthLens will reveal what lies beneath",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        color = PremiumCyan.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Choose a mode or start typing",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        color = TextMutedColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode grid header row: label + Multi-Layer button + collapse toggle
            var modesExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MODES",
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp,
                    fontFamily = DMMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeManager.themeName == "Polar Dawn") Color(0xFF3A3A3A) else TextMutedColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Multi-Layer button (compact)
                    val isMultiLayer = selectedMode == "Multi-Layer"
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isMultiLayer)
                                    Brush.linearGradient(listOf(ElectricViolet, PremiumCyan.copy(alpha = 0.8f)))
                                else
                                    Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.15f), PremiumCyan.copy(alpha = 0.08f)))
                            )
                            .border(
                                1.dp,
                                if (isMultiLayer) ElectricViolet else ElectricViolet.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onModeSelected("Multi-Layer") }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✦", fontSize = 8.sp, color = if (isMultiLayer) Color.White else ElectricViolet)
                            Text(
                                text = "Multi-Layer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DMMonoFontFamily,
                                color = if (isMultiLayer) Color.White else ElectricViolet
                            )
                        }
                    }

                    // Collapse/expand toggle
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Surface2)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(5.dp))
                            .clickable { modesExpanded = !modesExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (modesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (modesExpanded) "Collapse modes" else "Expand modes",
                            tint = TextMutedColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Quick Mode Selection Grid — minimized cards (reduced padding, tighter layout)
            androidx.compose.animation.AnimatedVisibility(visible = modesExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modesList = listOf(
                    Triple("Root Cause", "🔍", "Trace back to the origin of any situation"),
                    Triple("Deep Synthesis", "⚛️", "Integrated multi-perspective strategic wisdom"),
                    Triple("Psychology", "🧠", "Unpack hidden motives & patterns"),
                    Triple("Systems", "🌐", "Feedback loops & incentives"),
                    Triple("Probability", "📈", "Map timeline outcomes forward"),
                    Triple("Business", "💼", "Corporate models, strategies & goals"),
                    Triple("Relationships", "⚓", "Interpersonal dynamics & attachments"),
                    Triple("Spiritual", "✨", "Higher principles, purpose & growth")
                )

                for (i in modesList.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (j in 0..1) {
                            if (i + j < modesList.size) {
                                val (mode, emoji, desc) = modesList[i + j]
                                val isActive = mode == selectedMode

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Surface2
                                    ),
                                    border = BorderStroke(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) ElectricViolet else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onModeSelected(mode) }
                                ) {
                                    Column(
                                        // Minimized padding for compact cards
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = mode,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor,
                                            fontFamily = InstrumentSansFontFamily,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            color = TextMutedColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section header for depth
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANALYSIS DEPTH",
                        fontSize = 8.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedColor,
                        fontFamily = DMMonoFontFamily
                    )

                    // Deep Thought Mode Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DEEP THOUGHT",
                            fontSize = 8.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeepThoughtEnabled) ElectricViolet else TextMutedColor,
                            fontFamily = DMMonoFontFamily
                        )
                        androidx.compose.material3.Switch(
                            checked = isDeepThoughtEnabled,
                            onCheckedChange = { onDeepThoughtToggle(it) },
                            modifier = Modifier.scale(0.6f),
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricViolet,
                                uncheckedThumbColor = TextMutedColor,
                                uncheckedTrackColor = Surface2
                            )
                        )
                    }
                }

                val depthsList = listOf(
                    Triple("Quick Insight", "⚡", "Concise snapshot & swift action points"),
                    Triple("Standard Analysis", "🔍", "Balanced diagnostic & structured overview"),
                    Triple("Deep Analysis", "🧠", "Maximum multi-layered, behavioral schemas"),
                    Triple("Full Investigation", "🌐", "Strategic loops, trajectories & game-theory")
                )

                for (i in depthsList.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (j in 0..1) {
                            if (i + j < depthsList.size) {
                                val (depth, emoji, desc) = depthsList[i + j]
                                val isActive = depth == selectedDepth

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Surface2
                                    ),
                                    border = BorderStroke(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) ElectricViolet else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onDepthSelected(depth) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = depth,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor,
                                            fontFamily = InstrumentSansFontFamily,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            color = TextMutedColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (i == 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            } // end AnimatedVisibility modesExpanded

            // ── Inline Analysis Results ──────────────────────────────────────
            if (activeMessages.isNotEmpty() || isLoading) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANALYSIS",
                        fontSize = 8.sp,
                        letterSpacing = 1.2.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedColor
                    )
                    
                    if (isPrivacyModeEnabled) {
                         Row(
                             horizontalArrangement = Arrangement.spacedBy(4.dp),
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier
                                 .background(Color(0xFF2C1010), RoundedCornerShape(4.dp))
                                 .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                 .padding(horizontal = 6.dp, vertical = 2.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Lock,
                                 contentDescription = "Privacy Mode Active",
                                 tint = Color(0xFFFF5252),
                                 modifier = Modifier.size(10.dp)
                             )
                             Text(
                                 text = "PRIVACY ACTIVE",
                                 fontSize = 8.sp,
                                 fontFamily = DMMonoFontFamily,
                                 fontWeight = FontWeight.Bold,
                                 color = Color(0xFFFF5252)
                             )
                         }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeMessages.forEach { message ->
                        if (message.role == "user") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                ElectricViolet.copy(alpha = 0.15f),
                                                RoundedCornerShape(topStart = 14.dp, topEnd = 3.dp, bottomEnd = 14.dp, bottomStart = 14.dp)
                                            )
                                            .border(
                                                1.dp,
                                                ElectricViolet.copy(alpha = 0.3f),
                                                RoundedCornerShape(topStart = 14.dp, topEnd = 3.dp, bottomEnd = 14.dp, bottomStart = 14.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            fontSize = 18.sp,
                                            lineHeight = 25.sp,
                                            color = TextPrimaryColor,
                                            fontFamily = InstrumentSansFontFamily
                                        )
                                    }
                                }
                                
                                // Subtle micro action buttons row
                                Row(
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val uContext = LocalContext.current
                                    val uClipboard = LocalClipboardManager.current
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Surface2, RoundedCornerShape(4.dp))
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                            .clickable {
                                                uClipboard.setText(AnnotatedString(message.text))
                                                android.widget.Toast.makeText(uContext, "Copied question", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy message",
                                            tint = TextMutedColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Surface2, RoundedCornerShape(4.dp))
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                            .clickable {
                                                rawText = message.text
                                                editingMessageId = message.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit message",
                                            tint = TextMutedColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val parsedResponse = remember(message.text) {
                                try { ResponseParser.parse(message.text) } catch (e: Exception) { null }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(ElectricViolet, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEPTHLENS" + (if ((parsedResponse?.depthLayers?.size ?: 0) > 0) " · ${parsedResponse!!.depthLayers.size} LAYERS" else ""),
                                        fontSize = 8.sp,
                                        color = ElectricViolet,
                                        fontFamily = DMMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                if (message.text.startsWith("Error:") || message.text.contains("Error invoking DepthLens")) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
                                        border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Analysis failed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336), fontFamily = InstrumentSansFontFamily)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(message.text, fontSize = 10.sp, color = TextSecondaryColor, fontFamily = InstrumentSansFontFamily, lineHeight = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(onClick = { onRetryLastAnalysis(message.id) }) {
                                                Text("Retry", fontSize = 11.sp, color = ElectricViolet)
                                            }
                                        }
                                    }
                                } else if (parsedResponse != null) {
                                    Card(
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                        border = BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (!parsedResponse.isFollowUp) {
                                                // Redesigned Future Intelligence Visual Dashboard (Summarizes BEFORE detailed explanations)
                                                IntelligenceOSVisualizer(
                                                    parsed = parsedResponse, messageId = message.id,
                                                    rawText = message.text,
                                                    onSubmitQuery = onSubmitQuery
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }

                                            val cleanIntroDisplay = remember(parsedResponse.introduction, parsedResponse.executiveSummary) {
                                                val summary = parsedResponse.executiveSummary ?: ""
                                                val rawIntro = parsedResponse.introduction
                                                var intro = rawIntro.trim()
                                                if (summary.isNotBlank()) {
                                                    if (intro == summary) {
                                                        ""
                                                    } else if (intro.contains(summary)) {
                                                        intro.replace(summary, "").trim()
                                                    } else {
                                                        val paragraphs = intro.split("\n\n")
                                                        val filtered = paragraphs.filter { p ->
                                                            val pClean = p.trim()
                                                            pClean.isNotBlank() && !summary.contains(pClean) && !pClean.contains(summary)
                                                        }
                                                        filtered.joinToString("\n\n").trim()
                                                    }
                                                } else {
                                                    intro
                                                }
                                            }

                                            Column {
                                                    if (cleanIntroDisplay.isNotEmpty()) {
                                                        SelectionContainer {
                                                            Text(
                                                                text = stripMarkdown(cleanIntroDisplay),
                                                                fontSize = 18.sp,
                                                                color = TextSecondaryColor,
                                                                lineHeight = 25.sp,
                                                                fontFamily = InstrumentSansFontFamily,
                                                                modifier = Modifier.padding(bottom = 10.dp)
                                                            )
                                                        }
                                                    }

                                                    val associatedUserQuery = remember(message.id, activeMessages) {
                                                        activeMessages
                                                            .subList(0, activeMessages.indexOfFirst { it.id == message.id }.coerceAtLeast(0))
                                                            .findLast { it.role == "user" }?.text ?: ""
                                                    }
                                                    if (!parsedResponse.isFollowUp) {
                                                        Spacer(modifier = Modifier.height(14.dp))
                                                        SummaryOfInquiryPanel(userQuery = associatedUserQuery, introduction = parsedResponse.introduction)
                                                        if (!parsedResponse.deepSynthesis.isNullOrBlank()) {
                                                            DeepSynthesisPanel(synthesisText = parsedResponse.deepSynthesis)
                                                        }
                                                        Spacer(modifier = Modifier.height(14.dp))
                                                    }

                                             // ── Action Row (Copy, Deeper, Share, Export) ──
                                             val clipboardManager = LocalClipboardManager.current
                                             var copied by remember { mutableStateOf(false) }

                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .padding(vertical = 8.dp),
                                                 horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 // 1. Compact Copy Button (Circle Shape)
                                                 Box(
                                                     modifier = Modifier
                                                         .size(34.dp)
                                                         .clip(CircleShape)
                                                         .background(Surface2)
                                                         .border(1.dp, if (copied) ElectricViolet else BorderSubtle, CircleShape)
                                                         .clickable {
                                                             val cleanText = ResponseParser.getCopyableText(message.text)
                                                             clipboardManager.setText(AnnotatedString(cleanText))
                                                             copied = true
                                                             android.widget.Toast.makeText(context, "Copied Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.ContentCopy,
                                                         contentDescription = "Copy analysis",
                                                         tint = if (copied) ElectricViolet else TextPrimaryColor,
                                                         modifier = Modifier.size(14.dp)
                                                     )
                                                 }

                                                 // 2. Compact Go Deeper Button (Circle Shape)
                                                 Box(
                                                     modifier = Modifier
                                                         .size(34.dp)
                                                         .clip(CircleShape)
                                                         .background(Surface2)
                                                         .border(1.dp, BorderSubtle, CircleShape)
                                                         .clickable {
                                                             val deeperPrompt = "Go Deeper on the previous analysis of '" + associatedUserQuery + "'. Reveal: assumptions, unconscious patterns, systemic forces, hidden incentives, and long-term trajectories. Provide genuinely new insights."
                                                             onSubmitQuery(if (associatedUserQuery.isNotEmpty()) deeperPrompt else "Go Deeper on previous situation")
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.TrendingUp,
                                                         contentDescription = "Dig Deeper",
                                                         tint = TextPrimaryColor,
                                                         modifier = Modifier.size(14.dp)
                                                     )
                                                 }

                                                 // 3. Compact Share Button (Circle Shape)
                                                 Box(
                                                     modifier = Modifier
                                                         .size(34.dp)
                                                         .clip(CircleShape)
                                                         .background(Surface2)
                                                         .border(1.dp, BorderSubtle, CircleShape)
                                                         .clickable {
                                                             val cleanText = ResponseParser.getCopyableText(message.text)
                                                             val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                 type = "text/plain"
                                                                 putExtra(Intent.EXTRA_TEXT, cleanText)
                                                             }
                                                             context.startActivity(Intent.createChooser(shareIntent, "Share Analysis"))
                                                              android.widget.Toast.makeText(context, "Shared Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Share,
                                                         contentDescription = "Share analysis",
                                                         tint = TextPrimaryColor,
                                                         modifier = Modifier.size(14.dp)
                                                     )
                                                 }

                                                 // 4. Compact Export Button (Circle Shape)
                                                 Box(
                                                     modifier = Modifier
                                                         .size(34.dp)
                                                         .clip(CircleShape)
                                                         .background(Surface2)
                                                         .border(1.dp, BorderSubtle, CircleShape)
                                                         .clickable {
                                                             messageToExport = message
                                                             showExportDialog = true
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Download,
                                                         contentDescription = "Export report options",
                                                         tint = TextPrimaryColor,
                                                         modifier = Modifier.size(14.dp)
                                                     )
                                                 }
                                             }

                                              var digDeeperExpanded by rememberSaveable { mutableStateOf(false) }
                                              Row(
                                                  modifier = Modifier
                                                      .fillMaxWidth()
                                                      .clickable { digDeeperExpanded = !digDeeperExpanded }
                                                      .padding(vertical = 4.dp),
                                                  horizontalArrangement = Arrangement.SpaceBetween,
                                                  verticalAlignment = Alignment.CenterVertically
                                              ) {
                                                  Text(
                                                      text = "DIG DEEPER",
                                                      fontSize = 11.sp,
                                                      letterSpacing = 1.sp,
                                                      fontFamily = DMMonoFontFamily,
                                                      fontWeight = FontWeight.Bold,
                                                      color = PremiumCyan
                                                  )
                                                  Icon(
                                                      imageVector = if (digDeeperExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                      contentDescription = "Toggle Dig Deeper Options",
                                                      tint = PremiumCyan,
                                                      modifier = Modifier.size(16.dp)
                                                  )
                                              }

                                              AnimatedVisibility(
                                                  visible = digDeeperExpanded,
                                                  enter = expandVertically() + fadeIn(),
                                                  exit = shrinkVertically() + fadeOut()
                                              ) {
                                                  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                 val digDeeperPaths = remember(associatedUserQuery) {
                                                     listOf(
                                                         "Go Deeper" to ("Go Deeper on the previous analysis of '" + associatedUserQuery + "'. Reveal: assumptions, unconscious patterns, systemic forces, hidden incentives, and long-term trajectories. Provide genuinely new insights."),
                                                         "Strategic Leverage Analysis" to ("Strategic Leverage Analysis on '" + associatedUserQuery + "'. Analyze: key leverage points to disrupt this pattern, strategic advantages, and overlooked growth options."),
                                                         "Challenge Assumptions" to ("Challenge Assumptions on '" + associatedUserQuery + "'. Deconstruct the underlying systemic assumptions, cognitive distortions, and blind spot projections."),
                                                         "Systems Feedback Analysis" to ("Systems Feedback Analysis on '" + associatedUserQuery + "'. Deconstruct the feedback loops, dependencies, power structures, lock-in behaviors, and stabilizing factors.")
                                                     )
                                                 }

                                                 digDeeperPaths.forEach { (label, queryText) ->
                                                     val associatedQuery = if (associatedUserQuery.isNotEmpty()) queryText else "$label on previous situation"
                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .background(Surface3, RoundedCornerShape(8.dp))
                                                             .border(1.dp, PremiumCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                             .clickable { onSubmitQuery(associatedQuery) }
                                                             .padding(horizontal = 10.dp, vertical = 8.dp),
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Box(
                                                             modifier = Modifier
                                                                 .size(18.dp)
                                                                 .background(PremiumCyan.copy(alpha = 0.12f), CircleShape),
                                                             contentAlignment = Alignment.Center
                                                         ) {
                                                             Text("?", fontSize = 9.sp, color = PremiumCyan, fontWeight = FontWeight.Bold)
                                                         }
                                                         Spacer(modifier = Modifier.width(8.dp))
                                                         Text(
                                                             text = label,
                                                             fontSize = 16.sp,
                                                             color = TextSecondaryColor,
                                                             lineHeight = 22.sp,
                                                             fontFamily = InstrumentSansFontFamily,
                                                             modifier = Modifier.weight(1f)
                                                         )
                                                     }
                                                 }

                                                 // Additional dynamic suggested questions if present
                                                 if (parsedResponse.suggestedQuestions.isNotEmpty()) {
                                                     parsedResponse.suggestedQuestions.forEach { q ->
                                                         Row(
                                                             modifier = Modifier
                                                                 .fillMaxWidth()
                                                                 .background(Surface3, RoundedCornerShape(8.dp))
                                                                 .border(1.dp, PremiumCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                                 .clickable { onSubmitQuery(q) }
                                                                 .padding(horizontal = 10.dp, vertical = 8.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Box(
                                                                 modifier = Modifier
                                                                     .size(18.dp)
                                                                     .background(PremiumCyan.copy(alpha = 0.12f), CircleShape),
                                                                 contentAlignment = Alignment.Center
                                                             ) {
                                                                 Text("?", fontSize = 9.sp, color = PremiumCyan, fontWeight = FontWeight.Bold)
                                                             }
                                                             Spacer(modifier = Modifier.width(8.dp))
                                                             Text(
                                                                 text = q,
                                                                 fontSize = 16.sp,
                                                                 color = TextSecondaryColor,
                                                                 lineHeight = 22.sp,
                                                                 fontFamily = InstrumentSansFontFamily,
                                                                 modifier = Modifier.weight(1f)
                                                             )
                                                         }
                                                     }
                                                 }
                                             }

                                              } // Close AnimatedVisibility for DIG DEEPER

                                              // ── Exploration paths ──────────────────────────
                                             if (parsedResponse.explorationPaths.isNotEmpty()) {
                                                 var exploreFurtherExpanded by rememberSaveable { mutableStateOf(false) }
                                                 Spacer(modifier = Modifier.height(10.dp))
                                                 Row(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .clickable { exploreFurtherExpanded = !exploreFurtherExpanded }
                                                         .padding(vertical = 4.dp),
                                                     horizontalArrangement = Arrangement.SpaceBetween,
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Text(
                                                         text = "EXPLORE FURTHER",
                                                         fontSize = 11.sp,
                                                         letterSpacing = 1.sp,
                                                         fontFamily = DMMonoFontFamily,
                                                         fontWeight = FontWeight.Bold,
                                                         color = ElectricViolet
                                                     )
                                                     Icon(
                                                         imageVector = if (exploreFurtherExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                         contentDescription = "Toggle Explore Further",
                                                         tint = ElectricViolet,
                                                         modifier = Modifier.size(16.dp)
                                                     )
                                                 }
                                                 AnimatedVisibility(
                                                     visible = exploreFurtherExpanded,
                                                     enter = expandVertically() + fadeIn(),
                                                     exit = shrinkVertically() + fadeOut()
                                                 ) {
                                                     Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                     parsedResponse.explorationPaths.forEach { path ->
                                                         Row(
                                                             modifier = Modifier
                                                                 .fillMaxWidth()
                                                                 .background(ElectricViolet.copy(alpha = 0.07f), RoundedCornerShape(7.dp))
                                                                 .border(1.dp, ElectricViolet.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                                                                 .clickable { onSubmitQuery("$path — explore this path specifically in reference to my situation.") }
                                                                 .padding(horizontal = 10.dp, vertical = 7.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Text("✓", fontSize = 14.sp, color = ElectricViolet)
                                                             Spacer(modifier = Modifier.width(7.dp))
                                                             Text(
                                                                 text = path,
                                                                 fontSize = 16.sp,
                                                                 color = TextSecondaryColor,
                                                                 lineHeight = 22.sp,
                                                                 fontFamily = InstrumentSansFontFamily,
                                                                 modifier = Modifier.weight(1f)
                                                              )
                                                          }
                                                      }
                                                  }
                                              }
                                                }
                                        }
                                    }
                                }
                                } else {
                                    Card(
                                         shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                        border = BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = message.text,
                                            fontSize = 18.sp,
                                            color = TextSecondaryColor,
                                            lineHeight = 25.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isLoading && activeMessages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCardColor, RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            ThreeDotThinkingIndicator(
                                text = "Analyzing..."
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Home level Compose Box at bottom of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .border(BorderStroke(1.dp, BorderSubtle))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showUploadSecurityNotice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Color(0xFF0F252C), RoundedCornerShape(12.dp))
                            .border(1.2.dp, PremiumCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Isolated local sandbox active. Secure transit guaranteed.",
                            fontSize = 14.sp,
                            color = PremiumCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }

                if (editingMessageId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Surface2, RoundedCornerShape(8.dp))
                            .border(1.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ElectricViolet, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Editing Question",
                                fontSize = 14.sp,
                                color = ElectricViolet,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DMMonoFontFamily
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = TextMutedColor,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    editingMessageId = null
                                    rawText = ""
                                }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2, shape = RoundedCornerShape(26.dp))
                        .border(1.dp, BorderSubtle, shape = RoundedCornerShape(26.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ElectricViolet.copy(alpha = 0.22f),
                                    PremiumCyan.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(ElectricViolet, PremiumCyan.copy(alpha = 0.7f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val mediaPerm = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                android.Manifest.permission.READ_MEDIA_IMAGES
                            } else {
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            val hasMediaPerm = ContextCompat.checkSelfPermission(context, mediaPerm) == PackageManager.PERMISSION_GRANTED
                            
                            pendingPermissionAction = "attach"
                            if (!hasCompletedOnboarding) {
                                showPermissionOnboardingDialog = true
                            } else if (!hasMediaPerm) {
                                showSystemSettingsPrompt = true
                            } else {
                                showAttachBottomSheet = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Attach files",
                        tint = ElectricViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val speechCtx = context

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                BasicTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier.weight(1f),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        if (ThemeManager.isDarkTheme) Color.White else Color.Black
                    ),
                    textStyle = TextStyle(
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 17.sp,
                        color = TextPrimaryColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    decorationBox = { innerTextField ->
                        if (isListening) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30).copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "Secure Audio Stream Active…",
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 17.sp,
                                    color = Color(0xFFFF3B30),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            if (rawText.isEmpty()) {
                                Text(
                                    text = "Describe any situation, decision, or pattern…",
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 17.sp,
                                    color = TextMutedColor,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (isListening) {
                            speechRecognizer?.stopListening(); isListening = false
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(speechCtx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            pendingPermissionAction = "mic"
                            if (!hasCompletedOnboarding) {
                                showPermissionOnboardingDialog = true
                            } else if (!hasPerm) {
                                audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            } else {
                                isListening = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                speechRecognizer?.startListening(intent)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isListening) ElectricViolet.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = if (isListening) "Stop" else "Voice to text",
                        tint = if (isListening) ErrorColor else TextSecondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (rawText.isNotBlank()) {
                            val textToSend = rawText
                            rawText = ""
                            if (editingMessageId != null) {
                                val editedId = editingMessageId!!
                                val currentEdited = activeMessages.find { m -> m.id == editedId }
                                if (currentEdited != null) {
                                    val toDelete = activeMessages.filter { it.timestamp >= currentEdited.timestamp }
                                    toDelete.forEach { msg ->
                                        onDeleteMessage(msg.id)
                                    }
                                }
                                editingMessageId = null
                            }
                            onSubmitQuery(textToSend)
                        }
                    },
                    modifier = Modifier.size(32.dp).background(ElectricViolet, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Text("↑", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            }
        }
    } // End of inner Column

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

    // Attachment bottom sheet overlay
    androidx.compose.animation.AnimatedVisibility(
        visible = showAttachBottomSheet,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { showAttachBottomSheet = false }
        )
    }

    val attachPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let { 
            onAddAttachment(it.toString())
            showUploadSecurityNotice = true
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showAttachBottomSheet,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.5.dp, BorderSubtle, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMidnight)
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 18.dp, horizontal = 24.dp)) {
                    Box(modifier = Modifier.size(40.dp, 4.dp).background(BorderSubtle, CircleShape).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Add to Conversation", fontSize = 16.sp, fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Bold, color = PremiumCyan, modifier = Modifier.padding(bottom = 12.dp))
                    val attachOptions = listOf(
                        Triple("Images", "🖼️", "image/*"),
                        Triple("Videos", "🎬", "video/*"),
                        Triple("Audio Files", "🎧", "audio/*"),
                        Triple("PDF Documents", "📑", "application/pdf"),
                        Triple("Any File", "🗂️", "*/*")
                    )
                    attachOptions.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Surface3).border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)).clickable { showAttachBottomSheet = false; attachPickerLauncher.launch(option.third) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(text = option.second, fontSize = 18.sp)
                            Text(text = option.first, fontSize = 13.sp, color = TextPrimaryColor, fontFamily = InstrumentSansFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showAttachBottomSheet = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel", fontSize = 13.sp, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPermissionOnboardingDialog) {
        Dialog(onDismissRequest = { showPermissionOnboardingDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.6f),
                                PremiumCyan.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEB0A0D14)) // Dark Glass Look
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛡️ Scan Setup",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "To enable features like analyzing local documents/media and dictating voice queries, DepthLens requires standard physical permissions. Everything runs within an isolated high-security sandbox.",
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                        color = TextSecondaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface2.copy(alpha = 0.6f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PremiumCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📁", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Secure Storage Scan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontFamily = InstrumentSansFontFamily)
                            Text("Allows attaching images, PDFs & voice memos.", fontSize = 14.sp, color = TextMutedColor, fontFamily = InstrumentSansFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface2.copy(alpha = 0.6f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricViolet.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎙️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Mic & Voice Analytics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor, fontFamily = InstrumentSansFontFamily)
                            Text("Allows dictating statements directly to AI.", fontSize = 14.sp, color = TextMutedColor, fontFamily = InstrumentSansFontFamily)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showPermissionOnboardingDialog = false
                                pendingPermissionAction = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Not Now", color = TextSecondaryColor, fontWeight = FontWeight.Medium, fontFamily = InstrumentSansFontFamily)
                        }
                        
                        Button(
                            onClick = {
                                prefs.edit().putBoolean("has_completed_permission_onboarding", true).apply()
                                hasCompletedOnboarding = true
                                showPermissionOnboardingDialog = false
                                if (pendingPermissionAction == "mic") {
                                    audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    val mediaPerm = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        android.Manifest.permission.READ_MEDIA_IMAGES
                                    } else {


                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    attachPermLauncher.launch(mediaPerm)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Allow", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InstrumentSansFontFamily)
                        }
                    }
                }
            }
        }
    }

    if (showSystemSettingsPrompt) {
        Dialog(onDismissRequest = { showSystemSettingsPrompt = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, brush = Brush.linearGradient(colors = listOf(ElectricViolet.copy(alpha = 0.5f), PremiumCyan.copy(alpha = 0.5f))), shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMidnight)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 Permissions Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device storage or audio recording permissions have been restricted. To proceed with scanning or dictating context, please enable them in Android Settings.",
                        fontSize = 12.sp,
                        color = TextSecondaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showSystemSettingsPrompt = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondaryColor)
                        }
                        
                        Button(
                            onClick = {
                                showSystemSettingsPrompt = false
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showExportDialog) {
            messageToExport?.let { message ->
                ExportOptionsDialog(
                    onDismiss = { showExportDialog = false },
                    messageText = message.text,
                    context = context
                )
            }
        }
    }
}

// ───────────────────────────────────────────
// EXPORTERS & EXPORT OPTION DIALOG FOR V4.1.5
// ───────────────────────────────────────────

private fun saveToDownloads(context: android.content.Context, fileName: String, mimeType: String, contentBytes: ByteArray): Boolean {
    return try {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(contentBytes)
                }
                true
            } else {
                false
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeBytes(contentBytes)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun generatePdfReport(titleText: String, textContent: String): ByteArray {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = android.graphics.Paint()
    
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    
    fun drawPageDecorations(canvas: android.graphics.Canvas, pageNum: Int, paint: android.graphics.Paint, timestamp: String) {
        // Draw elegant high-contrast decorative top band in Electric Violet
        paint.color = android.graphics.Color.rgb(126, 101, 255) // #7E65FF Electric Violet
        canvas.drawRect(0f, 0f, 595f, 15f, paint)
        
        // Draw elegant accent strip in Premium Cyan
        paint.color = android.graphics.Color.rgb(0, 229, 255) // Premium Cyan
        canvas.drawRect(0f, 15f, 595f, 18f, paint)
        
        // Header Brand Text
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.rgb(126, 101, 255)
        canvas.drawText("DEPTHLENS", 40f, 42f, paint)
        
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.rgb(0, 229, 255)
        canvas.drawText("INTELLIGENCE ENGINE", 125f, 38f, paint)
        
        // Header thin divider
        paint.color = android.graphics.Color.rgb(220, 225, 235)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 53f, 555f, 53f, paint)
        
        // Footer thin divider
        canvas.drawLine(40f, 795f, 555f, 795f, paint)
        
        // Footer details
        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Generated by DepthLens Intelligence Engine  |  $timestamp", 40f, 812f, paint)
        
        // Page Number to the right
        canvas.drawText("Page $pageNum", 520f, 812f, paint)
    }

    var pageNum = 1
    // Draw decorations of the first page
    drawPageDecorations(canvas, pageNum, paint, timestamp)
    
    // Render text with pages pagination
    var currentY = 85f
    val margin = 45f
    val maxWidth = 505f
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 10.5f
    
    val sections = textContent.replace("\r", "").split("\n")
    for (section in sections) {
        if (section.trim().isEmpty()) {
            currentY += 10f
            continue
        }
        
        if (section.startsWith("=== DEPTHLENS") || section.startsWith("===") || section.startsWith("==================")) {
            // Skips raw separator lines to keep look premium
            continue
        }
        
        if (section.startsWith("###") || section.startsWith("##") || section.startsWith("#") || 
            section.equals("INTRODUCTION") || section.equals("EXECUTIVE SUMMARY") || section.equals("DEEP SYNTHESIS (INTEGRATED VISIONS)") || 
            section.startsWith("ROOT CAUSE REPORT") || section.startsWith("HUMAN DRIVERS") || section.startsWith("FUTURE SCENARIOS")) {
            paint.isFakeBoldText = true
            paint.textSize = 11.5f
            paint.color = android.graphics.Color.rgb(88, 28, 135) // Deep purple section header
            val cleanSec = section.replace("#", "").trim()
            if (currentY > 775f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                drawPageDecorations(canvas, pageNum, paint, timestamp)
                currentY = 85f
            }
            canvas.drawText(cleanSec, margin, currentY, paint)
            currentY += 22f
            continue
        } else {
            paint.isFakeBoldText = false
            paint.textSize = 9.5f
            paint.color = android.graphics.Color.BLACK
        }
        
        val words = section.split(" ")
        val line = StringBuilder()
        for (word in words) {
            val spaceText = if (line.isNotEmpty()) " " + word else word
            if (paint.measureText(line.toString() + spaceText) < maxWidth) {
                line.append(spaceText)
            } else {
                if (currentY > 775f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                    canvas = page.canvas
                    drawPageDecorations(canvas, pageNum, paint, timestamp)
                    currentY = 85f
                }
                
                // If it is a list item or field-label, color the starting tag beautifully
                val lineStr = line.toString()
                if (lineStr.trim().startsWith("-") || lineStr.trim().startsWith("*")) {
                    paint.color = android.graphics.Color.rgb(126, 101, 255)
                    canvas.drawText(lineStr.take(1), margin, currentY, paint)
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText(lineStr.drop(1), margin + 12f, currentY, paint)
                } else if (lineStr.contains(":")) {
                    val label = lineStr.substringBefore(":") + ":"
                    val value = lineStr.substringAfter(":")
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.rgb(126, 101, 255) // Electric Violet label
                    canvas.drawText(label, margin, currentY, paint)
                    
                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.BLACK
                    val offset = paint.measureText(label) + 4f
                    canvas.drawText(value, margin + offset, currentY, paint)
                } else {
                    canvas.drawText(lineStr, margin, currentY, paint)
                }
                
                currentY += 16f
                line.setLength(0)
                line.append(word)
            }
        }
        if (line.isNotEmpty()) {
            if (currentY > 775f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                drawPageDecorations(canvas, pageNum, paint, timestamp)
                currentY = 85f
            }
            
            val lineStr = line.toString()
            if (lineStr.contains(":")) {
                val label = lineStr.substringBefore(":") + ":"
                val value = lineStr.substringAfter(":")
                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.rgb(126, 101, 255) // Electric Violet label
                canvas.drawText(label, margin, currentY, paint)
                
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK
                val offset = paint.measureText(label) + 4f
                canvas.drawText(value, margin + offset, currentY, paint)
            } else {
                canvas.drawText(lineStr, margin, currentY, paint)
            }
            currentY += 18f
        }
    }
    
    pdfDocument.finishPage(page)
    val outputStream = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(outputStream)
    pdfDocument.close()
    return outputStream.toByteArray()
}

@Composable
private fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    messageText: String,
    context: android.content.Context
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "EXPORT REPORT",
                fontSize = 14.sp,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = PremiumCyan,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Choose an output format to save to your local Downloads folder:",
                    fontSize = 11.sp,
                    color = TextSecondaryColor,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val options = listOf(
                    Triple("Plain Text Document", "depthlens_report.txt", "text/plain"),
                    Triple("Structured JSON Document", "depthlens_report.json", "application/json"),
                    Triple("Portable Document Format (PDF)", "depthlens_report.pdf", "application/pdf")
                )
                
                options.forEach { (label, fileNm, mime) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface2, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable {
                                val cleanText = ResponseParser.getCopyableText(messageText)
                                val finalBytes = when (mime) {
                                    "application/json" -> {
                                        try {
                                            val jsonObject = org.json.JSONObject().apply {
                                                put("platform", "DepthLens")
                                                put("version", "v4.1.5")
                                                put("timestamp", System.currentTimeMillis())
                                                put("analysis_report", cleanText)
                                            }
                                            jsonObject.toString(4).toByteArray()
                                        } catch (e: Exception) {
                                            cleanText.toByteArray()
                                        }
                                    }
                                    "application/pdf" -> {
                                        generatePdfReport("DepthLens Analysis", cleanText)
                                    }
                                    else -> cleanText.toByteArray()
                                }
                                val ok = saveToDownloads(context, fileNm, mime, finalBytes)
                                if (ok) {
                                    android.widget.Toast.makeText(context, "Exported Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Export failed: unable to write document.", android.widget.Toast.LENGTH_LONG).show()
                                }
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download icons",
                                tint = ElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = PremiumCyan, fontWeight = FontWeight.Bold, fontFamily = DMMonoFontFamily, fontSize = 11.sp)
            }
        },
        containerColor = DeepMidnight,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.2.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    )
}


// Helper to parse archived JSON — kept for any legacy calls
private data class ParsedArchivedDetails(val introduction: String = "", val depthLayers: List<com.example.data.model.DepthLayerInsight> = emptyList())

private fun parseArchivedJson(jsonContent: String): ParsedArchivedDetails {
    return try {
        val parsed = ResponseParser.parse(jsonContent)
        ParsedArchivedDetails(introduction = parsed.introduction, depthLayers = parsed.depthLayers)
    } catch (e: Exception) {
        ParsedArchivedDetails()
    }
}