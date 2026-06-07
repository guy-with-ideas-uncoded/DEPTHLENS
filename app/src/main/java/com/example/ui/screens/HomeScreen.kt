package com.example.ui.screens


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    sessions: List<SessionEntity>,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .statusBarsPadding()
            .imePadding()
    ) {
        val scrollState = rememberScrollState()
        val isKeyboardVisible = WindowInsets.isImeVisible

        LaunchedEffect(activeMessages.size, isKeyboardVisible, isLoading) {
            if (activeMessages.isNotEmpty()) {
                kotlinx.coroutines.delay(100)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {

            // Symmetrical Sized Header Row (v4.1.4 Sleek Design)
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

                // Center aligned: Sleek Symmetrical DepthLens Logo (Compact & aligned v4.1.4)
                Image(
                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                    contentDescription = "DepthLens",
                    modifier = Modifier
                        .height(34.dp)
                        .padding(vertical = 2.dp),
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
                    Text(
                        text = greeting,
                        style = TextStyle(
                            fontFamily = DMMonoFontFamily,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = Color.White,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = ElectricViolet.copy(alpha = 0.45f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 12f
                            )
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
                    color = TextMutedColor
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
                    Triple("Psychology", "🧠", "Unpack hidden motives & patterns"),
                    Triple("Systems", "🌐", "Feedback loops & incentives"),
                    Triple("Probability", "📈", "Map timeline outcomes forward"),
                    Triple("Business", "💼", "Corporate models, strategies & goals"),
                    Triple("Relationships", "⚓", "Interpersonal dynamics & attachments"),
                    Triple("Spiritual", "✨", "Higher principles, purpose & growth"),
                    Triple("Decision Making", "🎯", "Heuristics, risks & choices")
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
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                                        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            SelectionContainer {
                                                Column {
                                                    if (parsedResponse.introduction.isNotEmpty()) {
                                                Text(
                                                    text = parsedResponse.introduction,
                                                    fontSize = 18.sp,
                                                    color = TextSecondaryColor,
                                                    lineHeight = 25.sp,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    modifier = Modifier.padding(bottom = 10.dp)
                                                )
                                            }

                                            if (parsedResponse.depthLayers.isNotEmpty()) {
                                                Text(
                                                    text = "DEPTH LAYERS",
                                                    fontSize = 11.sp,
                                                    letterSpacing = 1.sp,
                                                    fontFamily = DMMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMutedColor,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    parsedResponse.depthLayers.forEach { layer ->
                                                        val layerColor = when (layer.layerNumber) {
                                                            1 -> Layer1; 2 -> Layer2; 3 -> Layer3
                                                            4 -> Layer4; 5 -> Layer5; 6 -> Layer6
                                                            7 -> Layer7; 8 -> Layer8; 9 -> Layer9
                                                            else -> Layer10
                                                        }
                                                        var layerExpanded by remember { mutableStateOf(false) }
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Surface3, RoundedCornerShape(6.dp))
                                                                .border(1.dp, if (layerExpanded) layerColor.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(6.dp))
                                                                .clickable { layerExpanded = !layerExpanded }
                                                                .padding(8.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Box(modifier = Modifier.size(5.dp).background(layerColor, CircleShape))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = "L${layer.layerNumber} · ${layer.layerName}",
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = TextPrimaryColor,
                                                                        fontFamily = DMMonoFontFamily
                                                                    )
                                                                }
                                                                Icon(
                                                                    imageVector = if (layerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                    contentDescription = null,
                                                                    tint = TextMutedColor,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                            if (layerExpanded) {
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    text = layer.description,
                                                                    fontSize = 16.sp,
                                                                    color = TextSecondaryColor,
                                                                    lineHeight = 22.sp,
                                                                    fontFamily = InstrumentSansFontFamily
                                                                )
                                                                // Multi-level: sub-insights if available
                                                                if (layer.description.length > 100) {
                                                                    Spacer(modifier = Modifier.height(6.dp))
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .background(layerColor.copy(alpha = 0.06f), RoundedCornerShape(5.dp))
                                                                            .padding(6.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Box(modifier = Modifier.size(3.dp).background(layerColor, CircleShape))
                                                                        Spacer(modifier = Modifier.width(5.dp))
                                                                        Text(
                                                                            text = "Tap suggested questions below to explore this layer further",
                                                                            fontSize = 8.sp,
                                                                            color = layerColor.copy(alpha = 0.8f),
                                                                            fontFamily = DMMonoFontFamily
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if ((parsedResponse.executiveSummary ?: "").isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = parsedResponse.executiveSummary!!,
                                                    fontSize = 16.sp,
                                                    color = TextSecondaryColor,
                                                    lineHeight = 22.sp,
                                                    fontFamily = InstrumentSansFontFamily
                                                )
                                            }

                                            // ── Suggested Questions (after an                                             // ── Dig Deeper Section ────────────────────────
                                             val associatedUserQuery = remember(message.id, activeMessages) {
                                                 activeMessages
                                                     .subList(0, activeMessages.indexOfFirst { it.id == message.id }.coerceAtLeast(0))
                                                     .findLast { it.role == "user" }?.text ?: ""
                                             }

                                             // ── Action Row (Copy, Deeper, Share, Export) ──
                                             val clipboardManager = LocalClipboardManager.current
                                             var copied by remember { mutableStateOf(false) }

                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .padding(vertical = 12.dp),
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 // Copy Button with glass style and violet borders
                                                 Box(
                                                     modifier = Modifier
                                                         .height(34.dp)
                                                         .weight(1f)
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(Surface2)
                                                         .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                         .clickable {
                                                             val cleanText = ResponseParser.getCopyableText(message.text)
                                                             clipboardManager.setText(AnnotatedString(cleanText))
                                                             copied = true
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.ContentCopy,
                                                             contentDescription = "Copy text",
                                                             tint = if (copied) ElectricViolet else TextPrimaryColor,
                                                             modifier = Modifier.size(13.dp)
                                                         )
                                                         Text(
                                                             text = if (copied) "Copied" else "Copy",
                                                             fontFamily = InstrumentSansFontFamily,
                                                             fontWeight = FontWeight.Medium,
                                                             fontSize = 11.sp,
                                                             color = if (copied) ElectricViolet else TextPrimaryColor
                                                         )
                                                     }
                                                 }

                                                 // Go Deeper Button with interactive behavior
                                                 Box(
                                                     modifier = Modifier
                                                         .height(34.dp)
                                                         .weight(1.1f)
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(Surface2)
                                                         .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                         .clickable {
                                                             val deeperPrompt = "Go Deeper on the previous analysis of '" + associatedUserQuery + "'. Reveal: assumptions, unconscious patterns, systemic forces, hidden incentives, and long-term trajectories. Provide genuinely new insights."
                                                             onSubmitQuery(if (associatedUserQuery.isNotEmpty()) deeperPrompt else "Go Deeper on previous situation")
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.TrendingUp,
                                                             contentDescription = "Go Deeper",
                                                             tint = TextPrimaryColor,
                                                             modifier = Modifier.size(13.dp)
                                                         )
                                                         Text(
                                                             text = "Deeper",
                                                             fontFamily = InstrumentSansFontFamily,
                                                             fontWeight = FontWeight.Medium,
                                                             fontSize = 11.sp,
                                                             color = TextPrimaryColor
                                                         )
                                                     }
                                                 }

                                                 // Share Button
                                                 Box(
                                                     modifier = Modifier
                                                         .height(34.dp)
                                                         .weight(1f)
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(Surface2)
                                                         .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                         .clickable {
                                                             val cleanText = ResponseParser.getCopyableText(message.text)
                                                             val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                 type = "text/plain"
                                                                 putExtra(Intent.EXTRA_TEXT, cleanText)
                                                             }
                                                             context.startActivity(Intent.createChooser(shareIntent, "Share Analysis"))
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Share,
                                                             contentDescription = "Share",
                                                             tint = TextPrimaryColor,
                                                             modifier = Modifier.size(13.dp)
                                                         )
                                                         Text(
                                                             text = "Share",
                                                             fontFamily = InstrumentSansFontFamily,
                                                             fontWeight = FontWeight.Medium,
                                                             fontSize = 11.sp,
                                                             color = TextPrimaryColor
                                                         )
                                                     }
                                                 }

                                                 // Export File Button
                                                 Box(
                                                     modifier = Modifier
                                                         .height(34.dp)
                                                         .weight(1f)
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(Surface2)
                                                         .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                         .clickable {
                                                             try {
                                                                 val cleanText = ResponseParser.getCopyableText(message.text)
                                                                 val cacheFile = java.io.File(context.cacheDir, "depthlens_analysis_report.txt")
                                                                 cacheFile.writeText(cleanText)
                                                                 val contentUri = FileProvider.getUriForFile(
                                                                     context,
                                                                     "com.aistudio.depthlens.omg.fileprovider",
                                                                     cacheFile
                                                                 )
                                                                 val exportIntent = Intent(Intent.ACTION_SEND).apply {
                                                                     type = "text/plain"
                                                                     putExtra(Intent.EXTRA_STREAM, contentUri)
                                                                     addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                 }
                                                                 context.startActivity(Intent.createChooser(exportIntent, "Export Analysis Report"))
                                                             } catch (e: Exception) {
                                                                 android.widget.Toast.makeText(context, "Export Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                                             }
                                                         },
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Download,
                                                             contentDescription = "Export Report",
                                                             tint = TextPrimaryColor,
                                                             modifier = Modifier.size(13.dp)
                                                         )
                                                         Text(
                                                             text = "Export",
                                                             fontFamily = InstrumentSansFontFamily,
                                                             fontWeight = FontWeight.Medium,
                                                             fontSize = 11.sp,
                                                             color = TextPrimaryColor
                                                         )
                                                     }
                                                 }
                                             }

                                              Spacer(modifier = Modifier.height(6.dp))
                                              Text(
                                                  text = "DIG DEEPER",
                                                  fontSize = 11.sp,
                                                  letterSpacing = 1.sp,
                                                  fontFamily = DMMonoFontFamily,
                                                  fontWeight = FontWeight.Bold,
                                                  color = PremiumCyan,
                                                  modifier = Modifier.padding(bottom = 6.dp)
                                              )

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

                                             // ── Exploration paths ──────────────────────────
                                             if (parsedResponse.explorationPaths.isNotEmpty()) {
                                                 Spacer(modifier = Modifier.height(10.dp))
                                                 Text(
                                                     text = "EXPLORE FURTHER",
                                                     fontSize = 11.sp,
                                                     letterSpacing = 1.sp,
                                                     fontFamily = DMMonoFontFamily,
                                                     fontWeight = FontWeight.Bold,
                                                     color = ElectricViolet,
                                                     modifier = Modifier.padding(bottom = 5.dp)
                                                 )
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
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                                        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
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
                                .background(Color(0xFF141420), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
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
                            .background(Color(0xFF141420), RoundedCornerShape(8.dp))
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
    }
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