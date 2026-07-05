package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.components.ThreeDotThinkingIndicator
import com.example.ui.components.glassScreenBackground
import com.example.ui.components.IntelligenceOSVisualizer
import com.example.ui.components.DeepSynthesisPanel
import com.example.ui.components.SummaryOfInquiryPanel
import com.example.ui.components.FormattedResponseText
import com.example.ui.components.DepthLensLogo
import com.example.ui.viewmodel.IntelligenceViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.BasicTextField
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: IntelligenceViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val currentSession = remember(sessions, activeSessionId) {
        sessions.find { it.id == activeSessionId }
    }
    val isLoading by viewModel.isLoading.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val memoryInsights by viewModel.memoryInsights.collectAsState()
    
    val isMemoryEnabled by viewModel.isMemoryEnabled.collectAsState()
    val isCollectiveOptIn by viewModel.isCollectiveIntelligenceOptIn.collectAsState()
    val isPrivacyModeEnabled by viewModel.isPrivacyModeEnabled.collectAsState()
    val isDeepThoughtEnabled by viewModel.isDeepThoughtEnabled.collectAsState()
    val isSystemControlsExpanded by viewModel.isSystemControlsExpanded.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val voiceOutputEnabled by viewModel.voiceOutputEnabled.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()
    val autoLayerSelection by viewModel.autoLayerSelection.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val githubToken by viewModel.githubToken.collectAsState()
    val repoOwnerAndName by viewModel.repoOwnerAndName.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val archivedInsights by viewModel.archivedInsights.collectAsState()
    val probabilityForecast by viewModel.probabilityForecast.collectAsState()
    val deepDiveInsights by viewModel.deepDiveInsights.collectAsState()
    val isDeepDiveLoading by viewModel.isDeepDiveLoading.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val voiceAccent by viewModel.voiceAccent.collectAsState()

    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
    val chatsSyncedCount by viewModel.chatsSyncedCount.collectAsState()
    val pendingUploadsCount by viewModel.pendingUploadsCount.collectAsState()

    val continuityBrief by viewModel.continuityBrief.collectAsState()
    val continuityBriefStatus by viewModel.continuityBriefStatus.collectAsState()

    val selectedMessageId by viewModel.selectedMessageId.collectAsState()
    val selectedText by viewModel.selectedText.collectAsState()
    val replyMessageId by viewModel.replyMessageId.collectAsState()
    val replySelectedText by viewModel.replySelectedText.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val sessionsListState = rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(activeSessionId) {
        if (activeSessionId != null) {
            viewModel.reconnectConversationContext()
        }
    }

    androidx.compose.runtime.LaunchedEffect(continuityBriefStatus) {
        if (continuityBriefStatus == "Done") {
            android.widget.Toast.makeText(context, "✓ Previous conversation restored", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearContinuityBrief()
        }
    }

    // Pick media launcher
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setAttachment(uri.toString())
        }
    }

    var currentTab by rememberSaveable { mutableStateOf("home") }
    val startVoiceMode by viewModel.startVoiceMode.collectAsState()
    LaunchedEffect(startVoiceMode) {
        if (startVoiceMode) {
            currentTab = "voice_conversation"
            viewModel.triggerVoiceMode(false)
        }
    }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showThemeSelectorDialog by remember { mutableStateOf(false) }
    var activeThemeName by remember { mutableStateOf(ThemeManager.themeName) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReportBugDialog by remember { mutableStateOf(false) }
    val selectedMode by viewModel.selectedMode.collectAsState()
    var selectedDepth by remember { mutableStateOf("Standard Analysis") }
    
    var reportBugMessage by remember { mutableStateOf("") }
    var reportBugSubmitted by remember { mutableStateOf(false) }
    var reportBugUserConsented by remember { mutableStateOf(false) }
    
    var feedbackCategory by remember { mutableStateOf("Suggestion") }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackEmail by remember { mutableStateOf("") }
    var feedbackSubmitted by remember { mutableStateOf(false) }
    var isSendingFeedback by remember { mutableStateOf(false) }
    var feedbackSendResult by remember { mutableStateOf<Boolean?>(null) }
    var wasLoading by remember { mutableStateOf(false) }

    // Media permission states & helper functions
    var permissionToRequest by remember { mutableStateOf<String?>(null) }
    var pendingPickerToLaunch by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPermissionExplanationDialog by remember { mutableStateOf<String?>(null) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingPickerToLaunch?.invoke()
            pendingPickerToLaunch = null
        } else {
            showPermissionExplanationDialog = "Without permission, DepthLens cannot access files to perform intelligence analysis."
            pendingPickerToLaunch = null
        }
    }

    val requestMediaPermissionAndLaunch = { permission: String, onGranted: () -> Unit ->
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            onGranted()
        } else {
            pendingPickerToLaunch = onGranted
            permissionToRequest = permission
            mediaPermissionLauncher.launch(permission)
        }
    }

    fun getStoragePermissionForType(mimeGroup: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            when (mimeGroup) {
                "image" -> android.Manifest.permission.READ_MEDIA_IMAGES
                "video" -> android.Manifest.permission.READ_MEDIA_VIDEO
                "audio" -> android.Manifest.permission.READ_MEDIA_AUDIO
                else -> android.Manifest.permission.READ_MEDIA_IMAGES
            }
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var showAttachmentSelector by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    val voiceRecorder = remember { VoiceRecorder(context) }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingDuration = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000)
                recordingDuration++
            }
        }
    }

    // Manage background WakeWordService reactively (avoiding mic access conflicts)
    LaunchedEffect(wakeWordEnabled, currentTab) {
        val serviceIntent = android.content.Intent(context, com.example.WakeWordService::class.java)
        if (wakeWordEnabled && currentTab != "voice_conversation") {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("WakeWordService", "Error starting foreground service", e)
            }
        } else {
            try {
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                android.util.Log.e("WakeWordService", "Error stopping foreground service", e)
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecordingAudio = true
            voiceRecorder.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission denied. Live audio simulation active.", Toast.LENGTH_SHORT).show()
            isRecordingAudio = true
        }
    }

    val toggleRecording = {
        if (!isRecordingAudio) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasPermission) {
                isRecordingAudio = true
                voiceRecorder.startRecording()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        } else {
            isRecordingAudio = false
            val path = voiceRecorder.stopRecording()
            if (path != null) {
                viewModel.setAttachment(Uri.fromFile(java.io.File(path)).toString())
            } else {
                viewModel.setAttachment("content://simulated_voice_input.m4a")
            }
        }
    }


    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setAttachment(uri.toString())
        }
    }



    // DEPTHLENS GITHUB UPDATE SYSTEM STATES & FLOW COLLECTORS
    val latestRelease by GithubUpdateManager.latestRelease.collectAsState()
    val isCheckingForUpdates by GithubUpdateManager.isChecking.collectAsState()
    val isDownloadingUpdate by GithubUpdateManager.isDownloading.collectAsState()
    val updateDownloadProgress by GithubUpdateManager.downloadProgress.collectAsState()
    val updateDownloadedBytes by GithubUpdateManager.downloadedBytes.collectAsState()
    val updateServerTotalBytes by GithubUpdateManager.totalBytes.collectAsState()
    val updateLastCheckedTimestamp by GithubUpdateManager.lastChecked.collectAsState()
    val updateAutoCheckEnabled by GithubUpdateManager.autoCheckEnabled.collectAsState()
    val updateHistoryList by GithubUpdateManager.updateHistory.collectAsState()
    val updateErrorMessage by GithubUpdateManager.updateError.collectAsState()

    var showUpdatesDialog by remember { mutableStateOf(false) }
    var showUpdateAvailableDialog by remember { mutableStateOf(false) }

    // Silently prompt dialog if update found and not dismissed
    LaunchedEffect(latestRelease) {
        val rel = latestRelease
        if (rel != null) {
            val curVer = GithubUpdateManager.getInstalledVersion(context)
            if (GithubUpdateManager.isNewerVersion(rel.tagName, curVer)) {
                val dismissed = GithubUpdateManager.getDismissedVersion(context)
                if (dismissed != rel.tagName) {
                    showUpdateAvailableDialog = true
                }
            }
        }
    }

    // Modal dialog controls for local Memory and Privacy Settings
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var sidebarSearchQuery by remember { mutableStateOf("") }
    var sidebarSearchOpen by remember { mutableStateOf(false) }
    var sidebarSearchFocused by remember { mutableStateOf(false) }

    // ── Conversation search + long-press context menu state ────────────────
    val filteredSessions by viewModel.filteredSessions.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val sessionSearchQuery by viewModel.sessionSearchQuery.collectAsState()
    var contextMenuSession by remember { mutableStateOf<SessionEntity?>(null) }
    var renameTargetSession by remember { mutableStateOf<SessionEntity?>(null) }
    var renameTextFieldValue by remember { mutableStateOf("") }
    var deleteTargetSession by remember { mutableStateOf<SessionEntity?>(null) }

    LaunchedEffect(sidebarSearchQuery) {
        viewModel.setSessionSearchQuery(sidebarSearchQuery)
    }


    // DEPTHLENS NAVIGATION & BACK BUTTON STANDARD™
    BackHandler(enabled = true) {
        when {
            // Priority 2: Dialogs/Modals Open -> Close Modal
            showUpdatesDialog -> {
                showUpdatesDialog = false
            }
            showUpdateAvailableDialog -> {
                showUpdateAvailableDialog = false
            }
            showMemoryDialog -> {
                showMemoryDialog = false
            }
            showAboutDialog -> {
                showAboutDialog = false
            }
            showClearConfirm -> {
                showClearConfirm = false
            }
            showResetConfirm -> {
                showResetConfirm = false
            }
            showFeedbackDialog -> {
                showFeedbackDialog = false
            }
            showThemeSelectorDialog -> {
                showThemeSelectorDialog = false
            }
            showReportBugDialog -> {
                showReportBugDialog = false
            }
            showExitConfirm -> {
                showExitConfirm = false
            }
            showSignOutConfirm -> {
                showSignOutConfirm = false
            }
            contextMenuSession != null -> {
                contextMenuSession = null
            }
            renameTargetSession != null -> {
                renameTargetSession = null
            }
            deleteTargetSession != null -> {
                deleteTargetSession = null
            }
            // Priority 3: Sub-tabs and Sub-views
            currentTab == "edit_profile" -> {
                currentTab = "settings"
            }
            currentTab == "voice_conversation" -> {
                // Voice overlay active — let it handle its own back via onDismiss callback
                // (don't force-exit here; that closes it instantly before it can render)
            }
            currentTab != "home" -> {
                if (currentTab == "sessions" && viewModel.getChatDeletedFlag()) {
                    viewModel.selectSession(null)
                    viewModel.clearChatDeletedFlag()
                }
                currentTab = "home"
            }
            // Priority 4: Results Feed active -> Return to Landing Screen
            activeMessages.isNotEmpty() -> {
                viewModel.selectSession(null)
            }
            // Priority 6: Home Screen is clean -> Show Exit Confirmation Dialog
            else -> {
                showExitConfirm = true
            }
        }
    }

    // Scroll to bottom when a new analytic report lands
    LaunchedEffect(activeMessages.size, isLoading) {
        if (activeMessages.isNotEmpty()) {
            if (isLoading) {
                listState.animateScrollToItem(activeMessages.size - 1)
            } else if (wasLoading) {
                listState.animateScrollToItem(activeMessages.size - 1, scrollOffset = 0)
            }
        }
        wasLoading = isLoading
    }

    // Interactive onboarding overlay disabled
    if (false) {
        var onboardingName by remember { mutableStateOf("") }
        var onboardingEmail by remember { mutableStateOf("") }
        val isEmailValid = onboardingEmail.contains("@") && onboardingEmail.length > 5

        AlertDialog(
            onDismissRequest = {}, // Disallow dismissal without completing onboarding
            containerColor = DeepMidnight,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DEPTHLENS RECONSTRUCTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.3.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Welcome Explorer",
                        fontSize = 20.sp,
                        fontFamily = DMSerifDisplayFontFamily,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                        color = TextPrimaryColor
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter your name to personalize your DepthLens experience.",
                        fontSize = 12.sp,
                        color = TextSecondaryColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = onboardingName,
                        onValueChange = { onboardingName = it },
                        label = { Text("Explorer Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Neo", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            viewModel.loginAsGuest(onboardingName.ifBlank { "Explorer" })
                            viewModel.setOnboardingCompleted(true)
                        },
                        enabled = onboardingName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, disabledContainerColor = RichNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Exploring", fontSize = 11.sp, color = Color.White)
                    }
                }
            },
            modifier = Modifier.border(1.2.dp, ElectricViolet, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Interactive confirm loops for memory actions
    if (showMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            containerColor = RichNavy,
            textContentColor = TextPrimaryColor,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PremiumCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memory Storage & Privacy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "DepthLens compiles persistent cognitive insights locally on your device to dynamically adapt future reasoning flows.",
                        fontSize = 12.sp,
                        color = TextSecondaryColor,
                        lineHeight = 16.sp
                    )

                    // 1. Memory Switch card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                        border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Memory Engine Enabled",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                    Text(
                                        text = if (isMemoryEnabled) "Silent tracking active" else "Context learning suspended",
                                        fontSize = 10.sp,
                                        color = TextSecondaryColor
                                    )
                                }
                                Switch(
                                    checked = isMemoryEnabled,
                                    onCheckedChange = { viewModel.setMemoryEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PremiumCyan,
                                        checkedTrackColor = PremiumCyan.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextSecondaryColor,
                                        uncheckedTrackColor = SurfaceCardColor
                                    )
                                )
                            }

                            HorizontalDivider(color = RichNavy.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Memories Compiled:", fontSize = 11.sp, color = TextSecondaryColor)
                                Text("${memoryInsights.size} nodes cached", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Decryption Cache Size:", fontSize = 11.sp, color = TextSecondaryColor)
                                val storageEst = String.format("%.2f KB", memoryInsights.size * 0.16f + 1.22f)
                                Text(storageEst, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                            }
                        }
                    }

                    // 2. Collective Intel switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Opt-In collective learning",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                    Text(
                                        text = "Share anonymous structural patterns with peers.",
                                        fontSize = 9.sp,
                                        color = TextSecondaryColor,
                                        lineHeight = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isCollectiveOptIn,
                                    onCheckedChange = { viewModel.setCollectiveIntelligenceOptIn(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricViolet,
                                        checkedTrackColor = ElectricViolet.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextSecondaryColor,
                                        uncheckedTrackColor = SurfaceCardColor
                                    )
                                )
                            }
                        }
                    }

                    // Data protection banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stored On Device • Encrypted • User Controlled", fontSize = 9.sp, color = SuccessColor, fontWeight = FontWeight.Medium)
                    }

                    // Action controllers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Export complete! Packaged ${memoryInsights.size} insights to local workspace.", Toast.LENGTH_LONG).show()
                            },
                            border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Logs", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryDialog = false }) {
                    Text("Close", color = PremiumCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showUpdateAvailableDialog && latestRelease != null) {
        DepthLensUpdateAvailableDialog(
            release = latestRelease!!,
            onDismiss = {
                showUpdateAvailableDialog = false
                latestRelease?.let {
                    GithubUpdateManager.dismissVersion(context, it.tagName)
                }
            },
            onUpdateNow = {
                showUpdateAvailableDialog = false
                latestRelease?.let {
                    GithubUpdateManager.downloadAndUpdate(context, it)
                }
            }
        )
    }

    if (showUpdatesDialog) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val curVerStr = packageInfo.versionName ?: "4.0.0"
        SoftwareUpdatesDialog(
            onDismissRequest = { showUpdatesDialog = false },
            onManualCheck = {
                GithubUpdateManager.checkForUpdates(context, force = true) { isNew, rel ->
                    if (isNew && rel != null) {
                        showUpdateAvailableDialog = true
                    } else {
                        Toast.makeText(context, "DepthLens is completely up to date!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            isChecking = isCheckingForUpdates,
            autoCheckEnabled = updateAutoCheckEnabled,
            onAutoCheckToggle = { enabled ->
                GithubUpdateManager.setAutoCheckEnabled(context, enabled)
            },
            lastChecked = updateLastCheckedTimestamp,
            latestRelease = latestRelease,
            history = updateHistoryList,
            currentVersion = curVerStr
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = RichNavy,
            title = { Text("Purge Cached Memories?", color = TextPrimaryColor, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
            text = { Text("This will permanently discard all ${memoryInsights.size} compiled memory nodes. Future insights won't be contextually adapted. Proceed?", color = TextSecondaryColor, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMemoryInsights()
                        showClearConfirm = false
                        Toast.makeText(context, "Memory nodes completely purged.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Purge Memory", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = PremiumCyan)
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = RichNavy,
            title = { Text("Wipe All Application Data?", color = TextPrimaryColor, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
            text = { Text("This completely wipes all thread archives, uploaded links, and compiled behaviors. DepthLens will re-initialize to pristine clean states.", color = TextSecondaryColor, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showResetConfirm = false
                        Toast.makeText(context, "Application state re-initialized.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Deconstruct State", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = PremiumCyan)
                }
            }
        )
    }

    if (showExitConfirm) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showExitConfirm = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            var isMounted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isMounted = true
            }
            val scale by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "alpha"
            )

            // Premium glass exit dialog — matches the app's liquid-glass language.
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceCardColor.copy(alpha = 0.98f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brand mark inside a soft accent glow
                DepthLensLogo(
                    size = 60.dp,
                    showGlow = true
                )

                Text(
                    text = "Leave DepthLens?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryColor,
                    fontFamily = InstrumentSansFontFamily
                )
                Text(
                    text = "Your current analysis stays saved — pick up right where you left off.",
                    fontSize = 12.sp,
                    color = TextSecondaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    fontFamily = InstrumentSansFontFamily
                )

                Spacer(modifier = Modifier.height(2.dp))

                // EXIT — accent gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush = Brush.linearGradient(listOf(ElectricViolet, GradientEnd)))
                        .clickable {
                            showExitConfirm = false
                            (context as? android.app.Activity)?.finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Exit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InstrumentSansFontFamily)
                }

                // STAY — glass
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.06f else 0.10f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { showExitConfirm = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Stay", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimaryColor, fontFamily = InstrumentSansFontFamily)
                }
            }
        }
    }

    if (showSignOutConfirm) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSignOutConfirm = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            var isMounted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isMounted = true
            }
            val scale by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "alpha"
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xEC14141E) else Color(0xECFFFFFF)
                ),
                border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Modern compact header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✦ Sign Out?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Are you sure you want to sign out of your DepthLens account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons Layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // SIGN OUT (Primary Linear Gradient Button)
                        Button(
                            onClick = {
                                showSignOutConfirm = false
                                viewModel.signOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF7C3AED), Color(0xFF06B6D4))
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                "Sign Out",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // CANCEL (Minimal Transparent Text Button)
                        TextButton(
                            onClick = { showSignOutConfirm = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Transparent)
                        ) {
                            Text(
                                "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (ThemeManager.isDarkTheme) Color(0xFF8E8D9F) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── v7: Conversation long-press context menu (Pin / Rename / Delete) ──
    contextMenuSession?.let { session ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { contextMenuSession = null },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF14141E) else Color.White
                ),
                border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(0.52f)
            ) {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    // Header: conversation title preview
                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                        Text(
                            text = session.title.ifEmpty { "Untitled" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            fontFamily = InstrumentSansFontFamily,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Updated " + getRelativeTimeString(session.lastUpdatedAt).lowercase(),
                            fontSize = 8.sp,
                            color = TextMutedColor,
                            fontFamily = DMMonoFontFamily
                        )
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                    Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)) {
                        // Pin / Unpin
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    viewModel.togglePinSession(session.id)
                                    contextMenuSession = null
                                }
                                .padding(horizontal = 5.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(PremiumCyan.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PremiumCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = if (session.isPinned) "Unpin" else "Pin conversation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }

                        // Rename
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    renameTextFieldValue = session.title
                                    renameTargetSession = session
                                    contextMenuSession = null
                                }
                                .padding(horizontal = 5.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(ElectricViolet.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = "Rename",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }

                        // Delete
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    deleteTargetSession = session
                                    contextMenuSession = null
                                }
                                .padding(horizontal = 5.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(ErrorColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ErrorColor,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = "Delete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ErrorColor,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }
                }
            }
        }
    }

    // ── v7: Rename conversation dialog ─────────────────────────────────
    renameTargetSession?.let { session ->
        AlertDialog(
            onDismissRequest = { renameTargetSession = null },
            containerColor = RichNavy,
            title = {
                Text(
                    text = "Rename Conversation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                OutlinedTextField(
                    value = renameTextFieldValue,
                    onValueChange = { renameTextFieldValue = it },
                    singleLine = true,
                    placeholder = { Text("Conversation title", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = ElectricViolet
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameSession(session.id, renameTextFieldValue)
                        renameTargetSession = null
                    },
                    enabled = renameTextFieldValue.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        disabledContainerColor = SurfaceCardColor
                    )
                ) {
                    Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetSession = null }) {
                    Text("Cancel", color = TextSecondaryColor, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )
    }

    // ── v7: Delete conversation confirmation ───────────────────────────
    deleteTargetSession?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTargetSession = null },
            containerColor = RichNavy,
            title = {
                Text(
                    text = "Delete Conversation?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "\"${session.title.ifEmpty { "Untitled" }}\" and all of its messages will be permanently removed. This cannot be undone.",
                    fontSize = 12.sp,
                    color = TextSecondaryColor,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        if (activeSessionId == session.id) {
                            currentTab = "home"
                        }
                        deleteTargetSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetSession = null }) {
                    Text("Cancel", color = PremiumCyan, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ErrorColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )
    }

    if (showAboutDialog) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: java.lang.Exception) {
            null
        }
        val appVersion = packageInfo?.versionName ?: "4.0.0"
        
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "ABOUT DEPTHLENS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DepthLensLogo(
                        size = 64.dp,
                        showGlow = true
                    )
                    
                    Text(
                        text = "DEPTHLENS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryColor,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Text(
                        text = "See Beyond Surface",
                        style = MaterialTheme.typography.titleSmall,
                        color = PremiumCyan,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "Most people see events.\nDepthLens reveals patterns.\n\nMost people see actions.\nDepthLens reveals motives.\n\nMost people see outcomes.\nDepthLens reveals causes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
Text(
    text = "WEBSITE",
    color = PremiumCyan,
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.5.sp,
    textAlign = TextAlign.Center,
    modifier = Modifier
        .fillMaxWidth()
        .clickable {
            uriHandler.openUri("https://guy-with-ideas-uncoded.github.io/DEPTHLENS/")
        }
        .padding(vertical = 4.dp)
)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("CLOSE", color = PremiumCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepMidnight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ElectricViolet, RoundedCornerShape(16.dp))
        )
    }

    if (showThemeSelectorDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showThemeSelectorDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            var isMounted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isMounted = true
            }
            val scale by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "alpha"
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xEC14141E) else Color(0xECFFFFFF)
                ),
                border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val themesList = listOf(
                            Triple("Void", "Void", "Pure Black • Silver • Minimalist"),
                            Triple("Deep Sea", "Deep Sea", "Indigo • Neon Cyan • Cinematic"),
                            Triple("Polar Dawn", "Polar Dawn", "Clean • Bright • Productive"),
                            Triple("Ember", "Ember", "Fiery • Warm • Glowing Charcoal"),
                            Triple("Future", "Future", "Holographic • Neon Laser • 2126 OS")
                        )

                        themesList.forEach { (themeKey, name, desc) ->
                            val isSelected = activeThemeName == themeKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeThemeName = themeKey
                                        android.widget.Toast.makeText(context, "Theme Applied: $name", android.widget.Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            showThemeSelectorDialog = false
                                        }
                                    }
                                    .background(
                                        color = if (isSelected) Surface3 else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ElectricViolet.copy(alpha = 0.8f) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, if (isSelected) ElectricViolet else BorderSubtle, CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(ElectricViolet, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextPrimaryColor else TextSecondaryColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextMutedColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showThemeSelectorDialog = false },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Done",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumCyan
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = {
                showFeedbackDialog = false
                feedbackSubmitted = false
                feedbackSendResult = null
                isSendingFeedback = false
                feedbackMessage = ""
                feedbackEmail = ""
            },
            title = {
                Text(
                    text = "FEEDBACK CHOSEN PATH",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                if (isSendingFeedback) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = ElectricViolet, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Routing payload to secure cloud server...",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (feedbackSendResult == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Help us evolve DepthLens. Share suggestions, feature requests, or performance notes directly.",
                            fontSize = 12.sp,
                            color = TextPrimaryColor,
                            lineHeight = 16.sp
                        )

                        Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                        val categories = listOf("Suggestion", "Feature Request", "UI Feedback", "Performance", "General")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                val isSelected = feedbackCategory == cat
                                Card(
                                    onClick = { feedbackCategory = cat },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ElectricViolet else RichNavy
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) PremiumCyan else SurfaceCardColor),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Text("Message", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                        TextField(
                            value = feedbackMessage,
                            onValueChange = { feedbackMessage = it },
                            placeholder = { Text("What can we improve?", fontSize = 12.sp, color = TextSecondaryColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor,
                                focusedContainerColor = RichNavy,
                                unfocusedContainerColor = RichNavy,
                                cursorColor = PremiumCyan,
                                focusedIndicatorColor = ElectricViolet,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Text("Optional Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                        TextField(
                            value = feedbackEmail,
                            onValueChange = { feedbackEmail = it },
                            placeholder = { Text("your@email.com", fontSize = 12.sp, color = TextSecondaryColor) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor,
                                focusedContainerColor = RichNavy,
                                unfocusedContainerColor = RichNavy,
                                cursorColor = PremiumCyan,
                                focusedIndicatorColor = ElectricViolet,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                } else if (feedbackSendResult == true) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Thank you for your feedback.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your insights have been securely registered to ensure they are never lost. We manually review every query.",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = ErrorColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Unable to send feedback. Please try again.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Server network communication was interrupted, but your submission has been saved to the secure database history.",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (isSendingFeedback) {
                    // Hidden during loading
                } else if (feedbackSendResult == null) {
                    Button(
                        onClick = {
                            if (feedbackMessage.trim().isNotBlank()) {
                                isSendingFeedback = true
                                viewModel.submitFeedback(
                                    category = feedbackCategory,
                                    message = feedbackMessage,
                                    email = feedbackEmail
                                ) { success ->
                                    isSendingFeedback = false
                                    feedbackSendResult = success
                                    feedbackSubmitted = true
                                }
                            }
                        },
                        enabled = feedbackMessage.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            disabledContainerColor = RichNavy
                        )
                    ) {
                        Text("Submit Feedback", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            if (feedbackSendResult == false) {
                                // Allow retry
                                feedbackSendResult = null
                                feedbackSubmitted = false
                                isSendingFeedback = false
                            } else {
                                showFeedbackDialog = false
                                feedbackSubmitted = false
                                feedbackSendResult = null
                                feedbackMessage = ""
                                feedbackEmail = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text(
                            text = if (feedbackSendResult == false) "Retry" else "Done",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            dismissButton = {
                if (isSendingFeedback) {
                    // No cancel while sending
                } else if (feedbackSendResult == null) {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text("Cancel", color = ErrorColor, fontSize = 12.sp)
                    }
                } else if (feedbackSendResult == false) {
                    TextButton(
                        onClick = {
                            showFeedbackDialog = false
                            feedbackSubmitted = false
                            feedbackSendResult = null
                            feedbackMessage = ""
                            feedbackEmail = ""
                        }
                    ) {
                        Text("Cancel", color = ErrorColor, fontSize = 12.sp)
                    }
                }
            },
            containerColor = DeepMidnight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ElectricViolet, RoundedCornerShape(16.dp))
        )
    }

    if (showReportBugDialog) {
        val packageInfoReport = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
        val appVerStr = packageInfoReport?.versionName ?: "4.0.0"
        val deviceModel = android.os.Build.MODEL ?: "Unknown Device"
        val androidVer = android.os.Build.VERSION.RELEASE ?: "Unknown Android"
        val reportTimestamp = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()) }

        AlertDialog(
            onDismissRequest = {
                showReportBugDialog = false
                reportBugSubmitted = false
                reportBugMessage = ""
            },
            title = {
                Text(
                    text = "DIAGNOSTIC SYSTEM FAULT REPORT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorColor,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                if (!reportBugSubmitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "DepthLens automatically registers local systems diagnostic data to trace core issues. No conversation context is shared.",
                            fontSize = 12.sp,
                            color = TextPrimaryColor,
                            lineHeight = 16.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = RichNavy),
                            border = BorderStroke(1.dp, SurfaceCardColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SYSTEM TELEMETRY CACHE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PremiumCyan, fontFamily = FontFamily.Monospace)
                                Text("• App Version: $appVerStr", fontSize = 10.sp, color = TextSecondaryColor, fontFamily = FontFamily.Monospace)
                                Text("• Device Model: $deviceModel", fontSize = 10.sp, color = TextSecondaryColor, fontFamily = FontFamily.Monospace)
                                Text("• Android Core: v$androidVer", fontSize = 10.sp, color = TextSecondaryColor, fontFamily = FontFamily.Monospace)
                                Text("• Log Timestamp: $reportTimestamp", fontSize = 10.sp, color = TextSecondaryColor, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Text("Describe What Happened", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                        TextField(
                            value = reportBugMessage,
                            onValueChange = { reportBugMessage = it },
                            placeholder = { Text("Describe sequence which triggered unexpected system behaviour...", fontSize = 12.sp, color = TextSecondaryColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor,
                                focusedContainerColor = RichNavy,
                                unfocusedContainerColor = RichNavy,
                                cursorColor = PremiumCyan,
                                focusedIndicatorColor = ElectricViolet,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                  checked = reportBugUserConsented,
                                  onCheckedChange = { reportBugUserConsented = it },
                                  colors = androidx.compose.material3.CheckboxDefaults.colors(
                                      checkedColor = ElectricViolet,
                                      uncheckedColor = Color.Gray
                                  )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "I consent to submit technical logs for diagnostic repairs.",
                                fontSize = 11.sp,
                                color = TextSecondaryColor
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Thank You",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The systems team has received your telemetry and report. Your file-access data remains protected.",
                            fontSize = 12.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (!reportBugSubmitted) {
                    Button(
                        onClick = {
                            if (reportBugMessage.trim().isNotBlank()) {
                                viewModel.submitBugReport(
                                    message = reportBugMessage
                                ) { _ ->
                                    reportBugSubmitted = true
                                }
                            }
                        },
                        enabled = reportBugMessage.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorColor,
                            disabledContainerColor = RichNavy
                        )
                    ) {
                        Text("Send Report", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            showReportBugDialog = false
                            reportBugSubmitted = false
                            reportBugMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text("Done", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                if (!reportBugSubmitted) {
                    TextButton(onClick = { showReportBugDialog = false }) {
                        Text("Cancel", color = TextSecondaryColor, fontSize = 12.sp)
                    }
                }
            },
            containerColor = DeepMidnight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ErrorColor, RoundedCornerShape(16.dp))
        )
    }

    if (showPermissionExplanationDialog != null) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = null },
            title = {
                Text(
                    "MEDIA ACCESS SAFEGUARD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        showPermissionExplanationDialog!!,
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "DepthLens only accesses files you explicitly choose to analyze.",
                        color = TextSecondaryColor,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Normal
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanationDialog = null
                        permissionToRequest?.let { perm ->
                            mediaPermissionLauncher.launch(perm)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Allow Access", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanationDialog = null }) {
                    Text("Don't Allow", color = ErrorColor, fontSize = 12.sp)
                }
            },
            containerColor = DeepMidnight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.2.dp, ElectricViolet, RoundedCornerShape(16.dp))
        )
    }

    if (!isLoggedIn && !isGuest) {
        LoginScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Old navigation drawer / sidebar completely removed
    // Themed background spans the WHOLE screen (incl. behind the top bar & bottom nav),
    // so the header/nav float over the gradient like the preview — no opaque dark bands.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .glassScreenBackground()
    ) {
        // Active visual state controllers

        // Apply the theme ONLY when the user actively changes it — not on first
        // composition. setTheme() resets the accent to the theme default, so running
        // it on every launch was wiping the user's saved accent → theme/color appeared
        // to "revert to default" after reopening the app.
        var themeApplyReady by remember { mutableStateOf(false) }
        LaunchedEffect(activeThemeName) {
            if (themeApplyReady) {
                ThemeManager.setTheme(context, activeThemeName)
            } else {
                themeApplyReady = true
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentTab != "voice_conversation" && !WindowInsets.isImeVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 0.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(78.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple("home", "Chat", "home"),
                                Triple("insights", "Insights", "insights"),
                                Triple("settings", "Settings", "settings")
                            ).forEach { (tabId, label, _) ->
                                val isActive = currentTab == tabId
                                BottomTabItem(
                                    tabId = tabId,
                                    label = label,
                                    isActive = isActive,
                                    onClick = { 
                                        currentTab = tabId 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val bottomPadding = if (WindowInsets.isImeVisible) {
                0.dp
            } else {
                (innerPadding.calculateBottomPadding() - 10.dp).coerceAtLeast(0.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = 0.dp,
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = bottomPadding
                    )
            ) {
                // Multi-screen routing with smooth fade + subtle slide-up transition
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val premiumEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                        (fadeIn(animationSpec = tween(300, easing = premiumEasing)) + slideInVertically(
                            animationSpec = tween(300, easing = premiumEasing),
                            initialOffsetY = { (it * 0.05f).toInt() }
                        )).togetherWith(fadeOut(animationSpec = tween(250, easing = premiumEasing)) + slideOutVertically(
                            animationSpec = tween(250, easing = premiumEasing),
                            targetOffsetY = { (it * -0.02f).toInt() }
                        ))
                    },
                    label = "screen_routing_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        "home" -> {
                            HomeScreen(
                                sessions = sessions,
                                selectedMode = selectedMode,
                                onModeSelected = { viewModel.setSelectedMode(it) },
                                selectedDepth = selectedDepth,
                                onDepthSelected = { selectedDepth = it; viewModel.setSelectedDepth(it) },
                                isDeepThoughtEnabled = isDeepThoughtEnabled,
                                onDeepThoughtToggle = { viewModel.setDeepThoughtEnabled(it) },
                                onSessionSelected = { sessionId -> viewModel.selectSession(sessionId) },
                                onSubmitQuery = { text -> viewModel.sendQuery(text) },
                                onNavigateToChat = { /* no-op: results shown on home */ },
                                onNavigateToAnalysis = { /* no-op: removed */ },
                                onAddAttachment = { uri -> viewModel.setAttachment(uri) },
                                onRemoveAttachment = { viewModel.clearAttachment() },
                                onRemoveAttachmentUri = { uri -> viewModel.removeAttachmentUri(uri) },
                                attachedImageUri = attachedImageUri,
                                archivedInsights = archivedInsights,
                                onDeleteArchivedInsight = { id -> viewModel.deleteArchivedInsight(id) },
                                activeMessages = activeMessages,
                                isLoading = isLoading,
                                onStopGeneration = { viewModel.stopActiveGeneration() },
                                onRetryLastAnalysis = { msgId -> viewModel.retryLastAnalysis(msgId) },
                                onRegenerateLastAnalysis = { msgId -> viewModel.regenerateLastAnalysis(msgId) },
                                onOpenDrawer = { currentTab = "sessions" },
                                onCreateNewSession = { viewModel.createSession("") },
                                onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                                isPrivacyModeEnabled = isPrivacyModeEnabled,
                                onGetAttachmentsFlow = { msgId, uriField -> viewModel.getAttachmentsForMessageFlow(msgId, uriField) },
                                speechManager = viewModel.speechManager,
                                onNavigateToVoiceMode = { currentTab = "voice_conversation" },
                                onDigDeeper = { originalPrompt, responseText -> viewModel.digDeeper(originalPrompt, responseText) },
                                selectedMessageId = selectedMessageId,
                                selectedText = selectedText,
                                replyMessageId = replyMessageId,
                                replySelectedText = replySelectedText,
                                onEnterSelectionMode = { msgId, txt -> viewModel.enterSelectionMode(msgId, txt) },
                                onClearSelectionMode = { viewModel.clearSelectionMode() },
                                onSetReplyState = { msgId, txt -> viewModel.setReplyState(msgId, txt) },
                                onClearReplyState = { viewModel.clearReplyState() }
                            )
                        }
                        "sessions" -> {
                            SessionsScreen(
                                searchResults = searchResults,
                                searchQuery = sessionSearchQuery,
                                onSearchQueryChange = { viewModel.setSessionSearchQuery(it) },
                                activeSessionId = activeSessionId,
                                onSessionSelected = { sessionId -> viewModel.selectSession(sessionId) },
                                onCreateNewSession = { viewModel.createSession("") },
                                onDeleteSession = { sessionId -> viewModel.deleteSession(sessionId) },
                                onNavigateToChat = {
                                    if (viewModel.getChatDeletedFlag()) {
                                        viewModel.selectSession(null)
                                        viewModel.clearChatDeletedFlag()
                                    }
                                    currentTab = "home"
                                },
                                onTogglePinSession = { sessionId -> viewModel.togglePinSession(sessionId) },
                                onRenameSession = { sessionId, newTitle -> viewModel.renameSession(sessionId, newTitle) },
                                listState = sessionsListState
                            )
                        }
                        "insights" -> {
                            AnalysisScreen(
                                activeMessages = activeMessages,
                                selectedMode = selectedMode,
                                onBackToHome = { currentTab = "home" },
                                onSubmitQuery = { text -> viewModel.sendQuery(text) },
                                deepDiveInsights = deepDiveInsights,
                                isDeepDiveLoading = isDeepDiveLoading,
                                onGenerateDeepDive = { originalPrompt, responseText -> viewModel.digDeeper(originalPrompt, responseText) },
                                sessions = sessions,
                                memoryInsights = memoryInsights,
                                archivedInsights = archivedInsights,
                                probabilityForecast = probabilityForecast
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                isMemoryEnabled = isMemoryEnabled,
                                onMemoryEnabledChanged = { viewModel.setMemoryEnabled(it) },
                                notificationsEnabled = notificationsEnabled,
                                onNotificationsEnabledChanged = { viewModel.setNotificationsEnabled(it) },
                                voiceOutputEnabled = voiceOutputEnabled,
                                onVoiceOutputEnabledChanged = { viewModel.setVoiceOutputEnabled(it) },
                                wakeWordEnabled = wakeWordEnabled,
                                onWakeWordEnabledChanged = { viewModel.setWakeWordEnabled(it) },
                                isCollectiveOptIn = isCollectiveOptIn,
                                onCollectiveOptInChanged = { viewModel.setCollectiveIntelligenceOptIn(it) },
                                isPrivacyModeEnabled = isPrivacyModeEnabled,
                                onPrivacyModeEnabledChanged = { viewModel.setPrivacyModeEnabled(it) },
                                activeThemeName = activeThemeName,
                                onThemeSelected = { activeThemeName = it },
                                onShowMemoryDetails = { showMemoryDialog = true },
                                onShowUpdateDetails = { showUpdatesDialog = true },
                                onWipeAllUserData = { showResetConfirm = true },
                                onShowAbout = { showAboutDialog = true },
                                onReportBug = { showReportBugDialog = true },
                                isLoggedIn = isLoggedIn,
                                isGuest = isGuest,
                                userName = userName,
                                userEmail = userEmail,
                                userPhotoUrl = userPhotoUrl,
                                onNavigateToEditProfile = { currentTab = "edit_profile" },
                                githubToken = githubToken,
                                repoOwnerAndName = repoOwnerAndName,
                                onSaveGithubSettings = { token, repo -> viewModel.saveGithubSettings(token, repo) },
                                onSignOut = { showSignOutConfirm = true },
                                onLoginWithGoogle = { email, name -> viewModel.loginWithGoogle(email, name) },
                                onLoginAsGuest = { name -> viewModel.loginAsGuest(name) },
                                diagnostics = diagnostics,
                                onRefreshDiagnostics = { viewModel.refreshDiagnostics() },
                                selectedModel = selectedModel,
                                onModelSelected = { viewModel.setSelectedModel(it) },
                                selectedVoiceAccent = voiceAccent,
                                onVoiceAccentSelected = { viewModel.setVoiceAccent(it) },
                                syncStatus = syncStatus,
                                lastSyncedTime = lastSyncedTime,
                                chatsSyncedCount = chatsSyncedCount,
                                pendingUploadsCount = pendingUploadsCount,
                                onNavigateBack = { currentTab = "home" }
                            )
                        }
                        "edit_profile" -> {
                            EditProfileScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentTab = "settings" }
                            )
                        }
                    }
                }

                VoiceConversationOverlay(
                    visible = currentTab == "voice_conversation",
                    onDismiss = {
                        viewModel.speechManager.stop()
                        currentTab = "home"
                    },
                    onSubmitQuery = { text, attachedUri -> 
                        if (attachedUri != null) {
                            viewModel.setAttachment(attachedUri)
                        }
                        viewModel.sendQuery(text)
                    },
                    onUserInterrupt = { viewModel.stopActiveGeneration() },
                    isLoading = isLoading,
                    speechManager = viewModel.speechManager,
                    activeMessages = activeMessages,
                    voiceOutputEnabled = voiceOutputEnabled
                )

                if (isDownloadingUpdate) {
                    SoftwareDownloadProgressBarCard(
                        progress = updateDownloadProgress,
                        downloadedBytes = updateDownloadedBytes,
                        totalBytes = updateServerTotalBytes,
                        onCancel = {
                            GithubUpdateManager.cancelDownload()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        Box(modifier = Modifier.size(0.dp)) {
            val legacyContentToIgnore = @Composable {
                Scaffold(
            topBar = {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF0D0D14))
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                color = Color(0x0FFFFFFF), // Border subtle
                                start = androidx.compose.ui.geometry.Offset(0f, size.height - strokeWidth/2),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height - strokeWidth/2),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Hamburger menu removed as per bottom-nav constraint

                        // App icon area: theme accent tint background, border
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(ThemeManager.accentColor.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, ThemeManager.accentColor.copy(alpha = 0.45f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val w = size.width
                                val h = size.height
                                val outerPath = Path().apply {
                                    moveTo(w * 0.15f, h * 0.5f)
                                    cubicTo(w * 0.35f, h * 0.25f, w * 0.65f, h * 0.25f, w * 0.85f, h * 0.5f)
                                    cubicTo(w * 0.65f, h * 0.75f, w * 0.35f, h * 0.75f, w * 0.15f, h * 0.5f)
                                    close()
                                }
                                drawPath(path = outerPath, color = ThemeManager.accentColor, style = Stroke(width = 1.2.dp.toPx()))
                                drawCircle(color = Color(0xFF00D4FF), radius = w * 0.18f, style = Stroke(width = 1.dp.toPx()))
                            }
                        }

                        Text(
                            text = "DEPTHLENS",
                            fontSize = 11.sp,
                            letterSpacing = 1.3.sp,
                            color = Color(0x99F0EEFF), // ink-2
                            fontFamily = DMMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Mode badge: theme accent text, theme accent-dim background, rounded pill shape
                        Box(
                            modifier = Modifier
                                .background(ThemeManager.accentColor.copy(alpha = 0.18f), shape = RoundedCornerShape(20.dp))
                                .border(1.dp, ThemeManager.accentColor.copy(alpha = 0.45f), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedMode.uppercase(),
                                fontSize = 9.sp,
                                color = ThemeManager.accentColor,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { viewModel.createSession("") }) {
                            Icon(Icons.Default.Create, contentDescription = "New Session", tint = Color(0xFF00D4FF))
                        }
                    }
                }
            },
            containerColor = Color(0xFF0D0D14) // Overall screen background: Color(0xFF0D0D14)
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background radial glows for Apple / Linear depth atmospheric styling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ElectricViolet.copy(alpha = 0.08f), Color.Transparent),
                                radius = 2200f
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeMessages.isEmpty()) {
                            // Centered spacious Homepage / Landing Screen redesign
                            LandingScreen(
                                onQuerySelected = { query -> viewModel.sendQuery(query) },
                                onAddAttachment = { showAttachmentSelector = true },
                                onRemoveAttachment = { viewModel.clearAttachment() },
                                onRemoveAttachmentUri = { uri -> viewModel.removeAttachmentUri(uri) },
                                attachedImageUri = attachedImageUri,
                                isLoading = isLoading
                            )
                        } else {
                            // Comfortably padded scrolling Chat Feed
                            val onGetAttachmentsFlowCached = remember(viewModel) {
                                { msgId: String, uriField: String -> viewModel.getAttachmentsForMessageFlow(msgId, uriField) }
                            }
                            val onPromptSelectedCached = remember(viewModel) {
                                { query: String -> viewModel.sendQuery(query) }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(activeMessages, key = { it.id }) { message ->
                                    if (message.role == "user") {
                                        UserMessageBubble(message, onGetAttachmentsFlowCached)
                                    } else {
                                        if (message.text.startsWith("Error:") || message.text.contains("Error invoking DepthLens")) {
                                            val onRetryCached = remember(message.id, viewModel) {
                                                { viewModel.retryLastAnalysis(message.id) }
                                            }
                                            val onCancelCached = remember(message.id, viewModel) {
                                                { viewModel.deleteMessage(message.id) }
                                            }
                                            val onReportBugCached = remember {
                                                { showReportBugDialog = true }
                                            }
                                            AnalysisFailureErrorCard(
                                                errorMessage = message.text,
                                                onRetry = onRetryCached,
                                                onReportBug = onReportBugCached,
                                                onCancel = onCancelCached
                                            )
                                        } else {
                                            val associatedUserQuery = remember(message.id) {
                                                val idx = activeMessages.indexOfFirst { it.id == message.id }
                                                if (idx > 0) {
                                                     activeMessages.subList(0, idx).findLast { it.role == "user" }?.text ?: ""
                                                } else {
                                                     ""
                                                }
                                            }
                                            val onRegenerateCached = remember(message.id, viewModel) {
                                                { viewModel.regenerateLastAnalysis(message.id) }
                                            }
                                            AssistantMessageBubble(
                                                message = message,
                                                associatedUserQuery = associatedUserQuery,
                                                onPromptSelected = onPromptSelectedCached,
                                                onRegenerate = onRegenerateCached
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    // Dynamic non-blocking Inline Loading State
                    if (isLoading && activeMessages.isNotEmpty()) {
                        InlineLoadingIndicator()
                    }

                    // Bottom chat panel handles voice recording overrides & triggers
                    if (isRecordingAudio) {
                        RecordingVoiceHud(
                            durationSeconds = recordingDuration,
                            onSave = { toggleRecording() },
                            onCancel = {
                                isRecordingAudio = false
                                voiceRecorder.stopRecording()
                            }
                        )
                    } else if (activeMessages.isNotEmpty()) {
                        BottomInputPanel(
                            selectedMode = selectedMode,
                            attachedImageUri = attachedImageUri,
                            isLoading = isLoading,
                            onAddAttachment = { showAttachmentSelector = true },
                            onRemoveAttachment = { viewModel.clearAttachment() },
                            onSubmit = { text -> viewModel.sendQuery(text) },
                            onModeChanged = { viewModel.setSelectedMode(it) },
                            onInputFocusChanged = { inputFocused = it }
                        )
                    }
                }

                // Floating Navigation Buttons — hidden when keyboard is open
                if (activeMessages.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = !inputFocused,
                        enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.BottomEnd)
                            .padding(bottom = 100.dp, end = 16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Scroll to Top Button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Surface2.copy(alpha = 0.85f), CircleShape)
                                    .border(1.0.dp, ElectricViolet.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Scroll to Top",
                                    tint = TextPrimaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Scroll to Bottom Button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(activeMessages.size - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Surface2.copy(alpha = 0.85f), CircleShape)
                                    .border(1.0.dp, ElectricViolet.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll to Bottom",
                                    tint = TextPrimaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                }

                if (showAttachmentSelector) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showAttachmentSelector = false }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepMidnight),
                            border = BorderStroke(1.2.dp, ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "UNIVERSAL INPUT SELECTION™",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumCyan,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Button(
                                    onClick = {
                                        showAttachmentSelector = false
                                        requestMediaPermissionAndLaunch(getStoragePermissionForType("image")) {
                                            pickMediaLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RichNavy),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Photo / Image Asset", color = TextPrimaryColor, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        showAttachmentSelector = false
                                        requestMediaPermissionAndLaunch(getStoragePermissionForType("video")) {
                                            pickDocumentLauncher.launch("*/*")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RichNavy),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PDF, Video or Data Document", color = TextPrimaryColor, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        showAttachmentSelector = false
                                        toggleRecording()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RichNavy),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Live Voice Recording Input", color = TextPrimaryColor, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Fullscreen loading overlay removed. Interactive inline loading active.

                if (isDownloadingUpdate) {
                    SoftwareDownloadProgressBarCard(
                        progress = updateDownloadProgress,
                        downloadedBytes = updateDownloadedBytes,
                        totalBytes = updateServerTotalBytes,
                        onCancel = {
                            GithubUpdateManager.cancelDownload()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
    }
    }
}

// Custom TopBar colors mapping rule
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarAppBarColors() = TopAppBarDefaults.centerAlignedTopAppBarColors(
    containerColor = DeepMidnight,
    titleContentColor = Color.White
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(
    onQuerySelected: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onRemoveAttachmentUri: (String) -> Unit = {},
    attachedImageUri: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good Morning."
            in 12..16 -> "Good Afternoon."
            in 17..20 -> "Good Evening."
            else -> "Good Night."
        }
    }
    val subtitleText = "Are you ready to dive deeper and reveal the absolute reality?"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Iris Core Logo using the single shared component
        DepthLensLogo(
            size = 96.dp,
            showGlow = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitleText,
            style = MaterialTheme.typography.titleMedium,
            color = HeroSubtitleColor,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Center conversational Input Panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
            border = BorderStroke(1.2.dp, ElectricViolet.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If there's active attached previews in center
                attachedImageUri?.let { uriString ->
                    val uris = uriString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    uris.forEach { singleUri ->
                        AttachmentPreviewItem(
                            uri = singleUri,
                            onRemove = {
                                if (uris.size <= 1) {
                                    onRemoveAttachment()
                                } else {
                                    onRemoveAttachmentUri(singleUri)
                                }
                            },
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }

                // Spacious multiline text input area
                TextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                                                Text(
                                                    text = "Describe a situation, relationship tension, or system risk you want to dissect...",
                                                    fontSize = 13.sp,
                                                    color = PlaceholderColor,
                                                    lineHeight = 18.sp
                                                )
                                            },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 130.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onAddAttachment,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(36.dp)
                            .background(RichNavy, CircleShape)
                            .border(1.dp, PremiumCyan.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach image",
                            tint = PremiumCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (rawText.trim().isNotBlank() || attachedImageUri != null) {
                                onQuerySelected(rawText)
                                rawText = ""
                            }
                        },
                        enabled = !isLoading && (rawText.trim().isNotBlank() || attachedImageUri != null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = Color.White,
                            disabledContainerColor = RichNavy,
                            disabledContentColor = TextSecondaryColor
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Smart inquiry nodes (Beautiful high-fidelity recommendation grid)
        Text(
            text = "CHOOSE SYSTEM DIAGNOSTIC PATH",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PremiumCyan,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        val promptMappings = listOf(
            "Analyze a Decision" to "Analyze a Decision: I'm choosing between expansion and pivoting. Deconstruct hidden risks.",
            "Reveal Hidden Motives" to "Hidden Motives: Explain status positioning behind passive-aggressive resource blocks.",
            "Root Cause Analysis" to "Root Cause Analysis: Why do I experience intense fatigue when starting high-focus work?",
            "Relationship Insights" to "Relationship Insights: What drives our circular arguments and emotional stonewalling?",
            "Future Probability Analysis" to "Future Probability Analysis: Map the current trajectory persistent loops.",
            "Business Strategy" to "Business Strategy Audit: Map team incentives vs executive operational rules.",
            "Challenge Assumptions" to "Challenge Assumptions: Dissect the cognitive filters in our business expansion vision."
        )

        promptMappings.forEach { (label, query) ->
            Card(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SurfaceCardColor),
                colors = CardDefaults.cardColors(containerColor = RichNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onQuerySelected(query) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(ElectricViolet.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = query,
                            fontSize = 10.sp,
                            color = TextSecondaryColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: MessageEntity,
    associatedUserQuery: String,
    onPromptSelected: (String) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = remember(message.text) { ResponseParser.parse(message.text) }
    Column(modifier = modifier.fillMaxWidth()) {
        // AI label: theme accent color, DM Mono font, uppercase, 8sp, with a 6dp theme accent dot prefix
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ThemeManager.accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "DEPTHLENS" + (if (parsed.depthLayers.isNotEmpty()) " · ${parsed.depthLayers.size} LAYERS" else ""),
                fontSize = 8.sp,
                color = ThemeManager.accentColor,
                fontFamily = DMMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // AI bubble background: Color(0xFF141420), border: Color(0x0FFFFFFF), corner radius 14dp (top-left 3dp)
        Card(
            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                DepthLensDiagnosticCard(
                    parsed = parsed,
                    userQuery = associatedUserQuery,
                    onPromptSelected = onPromptSelected,
                    messageId = message.id,
                    onRegenerate = onRegenerate
                )
            }
        }
    }
}

@Composable
fun UserMessageBubble(
    message: MessageEntity,
    onGetAttachmentsFlow: (String, String) -> kotlinx.coroutines.flow.Flow<List<com.example.data.model.AttachmentEntity>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                val attachmentsState = remember(message.id, message.imageUri) {
                    onGetAttachmentsFlow(message.id, message.imageUri ?: "")
                }.collectAsState(initial = emptyList())
                val attachments = attachmentsState.value

                if (attachments.isNotEmpty()) {
                    attachments.forEach { attachment ->
                        val context = LocalContext.current
                        val mimeType = attachment.mimeType
                        val modelToLoad = if (!attachment.remoteUrl.isNullOrBlank()) attachment.remoteUrl else attachment.localUri
                        
                        when {
                            mimeType.startsWith("image/") -> {
                                Card(
                                    border = BorderStroke(1.dp, SurfaceCardColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .size(130.dp)
                                ) {
                                    AsyncImage(
                                        model = modelToLoad,
                                        contentDescription = "Source thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            mimeType.startsWith("audio/") -> {
                                AudioPlayBubble(uriString = modelToLoad)
                            }
                            else -> {
                                FileDocumentBubble(uriString = modelToLoad, mimeType = mimeType)
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 3.dp, bottomStart = 14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemeManager.accentColor.copy(alpha = if (ThemeManager.isDarkTheme) 0.90f else 0.92f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = ThemeManager.accentColor.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = message.text,
                            fontSize = 11.5.sp,
                            color = Color(0xFFF0EEFF),
                            fontFamily = InstrumentSansFontFamily,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "User Input",
                    fontSize = 8.sp,
                    color = TextSecondaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                )
            }
        }
    }
}

// Redesigned high-contrast beautiful structured Intelligence Briefing cards
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepthLensDiagnosticCard(
    parsed: ParsedResponse,
    onPromptSelected: (String) -> Unit,
    messageId: String,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
    userQuery: String = ""
) {
    if (parsed.isFollowUp) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                FormattedResponseText(
                    text = parsed.introduction,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // New interactive v4.1.6 future visualizer
        IntelligenceOSVisualizer(
            parsed = parsed,
            rawText = parsed.introduction,
            messageId = messageId,
            onSubmitQuery = onPromptSelected
        )
        Spacer(modifier = Modifier.height(16.dp))

        val cleanIntroDisplay = remember(parsed.introduction, parsed.executiveSummary) {
            val summary = parsed.executiveSummary ?: ""
            val rawIntro = parsed.introduction
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

        // Conversation overview context
        if (cleanIntroDisplay.isNotEmpty()) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                FormattedResponseText(
                    text = cleanIntroDisplay,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        SummaryOfInquiryPanel(
            userQuery = userQuery,
            introduction = parsed.introduction,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!parsed.deepSynthesis.isNullOrBlank()) {
            DeepSynthesisPanel(
                synthesisText = parsed.deepSynthesis,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // v4.1.3 Probability Assessment Card
        parsed.probabilityAssessment?.let { pa ->
            val color = when {
                pa.likelihood >= 70 -> SuccessColor
                pa.likelihood >= 40 -> WarningColor
                else -> ErrorColor
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "PROBABILITY ASSESSMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${pa.confidence} CONFIDENCE".uppercase(),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Overall Likelihood", fontSize = 11.5.sp, color = TextSecondaryColor)
                        Text("${pa.likelihood}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { pa.likelihood / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = RichNavy
                    )

                    if (pa.reasoningFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reasoning Factors", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = PremiumCyan, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        pa.reasoningFactors.forEach { factor ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("•", color = color, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(factor, fontSize = 11.sp, color = TextPrimaryColor, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Probability Matrix Engine Card layout
        if (parsed.probabilityMetrics != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, RichNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "PROBABILITY MATRIX ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val metrics = parsed.probabilityMetrics!!
                    val list = listOf(
                        Triple("Confidence Level", metrics.confidence, ElectricViolet),
                        Triple("Likelihood Index", metrics.likelihood, PremiumCyan),
                        Triple("Risk Vector", metrics.risk, Color(0xFFFF5555)),
                        Triple("Opportunity Delta", metrics.opportunity, Color(0xFF50E3C2))
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.forEach { (label, value, tint) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondaryColor
                                    )
                                    Text(
                                        text = "$value%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tint
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Surface3)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(value / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(tint, tint.copy(alpha = 0.5f))
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        // 3. Most Likely Explanation - Root Cause report
        parsed.rootCauseReport?.let { rc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, WarningColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MOST LIKELY EXPLANATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningColor,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticIndicatorBlock(label = "Visible Symptom", content = rc.symptom)
                    DiagnosticIndicatorBlock(label = "Immediate Cause", content = rc.immediateCause)
                    DiagnosticIndicatorBlock(label = "Underlying Cause/Incentive", content = rc.underlyingCause)
                    DiagnosticIndicatorBlock(label = "Deeper Defense Matrix", content = rc.deeperCause)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(RichNavy, RoundedCornerShape(6.dp))
                            .border(1.dp, WarningColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("PROBABILISTIC ROOT ESTIMATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PremiumCyan)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rc.rootCauseEstimate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // v4.1.3 Future Pathways Card
        if (parsed.futurePathways.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, RichNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "PROBABILITY FUTURE PATHWAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    parsed.futurePathways.forEach { pathway ->
                        val barColor = when {
                            pathway.probability >= 70 -> SuccessColor
                            pathway.probability >= 40 -> WarningColor
                            else -> ErrorColor
                        }

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pathway.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                                Text("${pathway.probability}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = barColor)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { pathway.probability / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = barColor,
                                trackColor = RichNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (pathway.description.isNotEmpty()) {
                                Text(pathway.description, fontSize = 11.sp, color = TextSecondaryColor, lineHeight = 15.sp)
                            }
                            if (pathway.drivers.isNotEmpty() || pathway.risks.isNotEmpty() || pathway.opportunities.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (pathway.drivers.isNotEmpty()) {
                                        Text("Drivers: ${pathway.drivers}", fontSize = 9.5.sp, color = PremiumCyan, modifier = Modifier.weight(1f))
                                    }
                                    if (pathway.risks.isNotEmpty()) {
                                        Text("Risks: ${pathway.risks}", fontSize = 9.5.sp, color = ErrorColor.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                                    }
                                    if (pathway.opportunities.isNotEmpty()) {
                                        Text("Opps: ${pathway.opportunities}", fontSize = 9.5.sp, color = SuccessColor.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // v4.1.3 Timeline Forecast Card
        parsed.timelineForecast?.let { tf ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "TIMELINE FORECAST HORIZONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val horizons = listOf(
                        Triple("Short Term (1-30 Days)", tf.shortTermProb, tf.shortTermDesc),
                        Triple("Mid Term (1-6 Months)", tf.midTermProb, tf.midTermDesc),
                        Triple("Long Term (6-24 Months)", tf.longTermProb, tf.longTermDesc)
                    )

                    horizons.forEach { (label, prob, desc) ->
                        val col = when {
                            prob >= 70 -> SuccessColor
                            prob >= 40 -> WarningColor
                            else -> ErrorColor
                        }
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                                Text("$prob%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = col)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { prob / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = col,
                                trackColor = RichNavy
                            )
                            if (desc.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(desc, fontSize = 10.sp, color = TextSecondaryColor, lineHeight = 14.sp)
                            }
                        }
                    }

                    if (tf.explanation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RichNavy, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("PROBABILITY DECAY DRIVER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(tf.explanation, fontSize = 10.sp, color = TextPrimaryColor, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // v4.1.3 Decision Impact Card
        parsed.decisionImpact?.let { di ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, RichNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "DECISION IMPACT ANALYSIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = RichNavy),
                            border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Status Quo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                    Text("${di.statusQuoProb}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ErrorColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(di.statusQuoDesc, fontSize = 10.sp, color = TextPrimaryColor, lineHeight = 13.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = RichNavy),
                            border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Proactive Action", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessColor)
                                    Text("${di.actionProb}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SuccessColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(di.actionDesc, fontSize = 10.sp, color = TextPrimaryColor, lineHeight = 13.sp)
                            }
                        }
                    }

                    if (di.comparison.isNotEmpty() || di.risks.isNotEmpty() || di.benefits.isNotEmpty() || di.tradeoffs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (di.comparison.isNotEmpty()) {
                                Text("Comparison: ${di.comparison}", fontSize = 10.sp, color = TextSecondaryColor, lineHeight = 13.sp)
                            }
                            if (di.risks.isNotEmpty()) {
                                Text("Risks: ${di.risks}", fontSize = 10.sp, color = ErrorColor.copy(alpha = 0.85f), lineHeight = 13.sp)
                            }
                            if (di.benefits.isNotEmpty()) {
                                Text("Benefits: ${di.benefits}", fontSize = 10.sp, color = SuccessColor.copy(alpha = 0.85f), lineHeight = 13.sp)
                            }
                            if (di.tradeoffs.isNotEmpty()) {
                                Text("Tradeoffs: ${di.tradeoffs}", fontSize = 10.sp, color = WarningColor, lineHeight = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // v4.1.3 Forecast Summary Card
        parsed.forecastSummary?.let { fs ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "FORECAST INTELLIGENCE SUMMARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val items = listOf(
                            Triple("Outcome", fs.mostLikelyOutcome, ElectricViolet),
                            Triple("Key Risk", fs.keyRisk, Color(0xFFFF5555)),
                            Triple("Opportunity", fs.opportunityWindow, Color(0xFF50E3C2))
                        )

                        items.forEach { (label, value, tint) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = RichNavy),
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$value%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = tint)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prediction Confidence Rating", fontSize = 11.sp, color = TextMutedColor)
                        Text(fs.predictionConfidence.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessColor)
                    }
                }
            }
        }

        // 4. Human incentives deceptions
        parsed.humanDrivers?.let { hd ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, SurfaceCardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "HUMAN INCENTIVES & DECEPTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticIndicatorBlock(label = "Surface Cover-up Intent", content = hd.surfaceIntention)
                    DiagnosticIndicatorBlock(label = "Emotional Pressure", content = hd.emotionalDriver)
                    DiagnosticIndicatorBlock(label = "Protected Need Focus", content = hd.needDriver)
                    DiagnosticIndicatorBlock(label = "Avoided Vulnerable Fear", content = hd.fearDriver)
                    DiagnosticIndicatorBlock(label = "Identity Protection Loop", content = hd.identityDriver)
                    DiagnosticIndicatorBlock(label = "Hidden Tactical Motives", content = hd.hiddenMotives)
                }
            }
        }

        // 5. Future probabilities scenario matrix
        if (parsed.futureScenarios.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, SurfaceCardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "PROBABILISTIC FUTURE ANALYSIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    parsed.futureScenarios.forEach { sc ->
                        val barColor = when {
                            sc.probability >= 50 -> SuccessColor
                            sc.probability >= 20 -> WarningColor
                            else -> ElectricViolet
                        }

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${sc.codeName} - ${sc.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                                Text("${sc.probability}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { sc.probability / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = barColor,
                                trackColor = RichNavy
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(sc.impactText, fontSize = 10.sp, color = TextSecondaryColor, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }

        // 6. Recommended Actions (Exploration pathways)
        if (parsed.explorationPaths.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "EXPLORE FURTHER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumCyan,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parsed.explorationPaths.forEach { path ->
                    OutlinedButton(
                        onClick = { onPromptSelected("$path path specifically in reference to this scenario.") },
                        border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCardColor,
                            contentColor = PremiumCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = PremiumCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(path, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 7. Next Questions Vertically Stacked Cards (Zero truncation, premium look!)
        if (parsed.suggestedQuestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "NEXT QUESTIONS TO ASK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SuccessColor,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parsed.suggestedQuestions.forEach { q ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RichNavy),
                        border = BorderStroke(1.dp, SurfaceCardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPromptSelected(q) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Bullet Point / Play Arrow with Neon Glow Accent
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(SuccessColor.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = SuccessColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = q,
                                fontSize = 12.sp,
                                color = TextPrimaryColor,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
            val shareableText = remember(parsed) { parsed.toShareableText() }
            
            // Copy Button - styled consistent with app design
            var dashCopied by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (dashCopied) ElectricViolet.copy(alpha = 0.18f) else Surface3)
                    .border(1.dp, if (dashCopied) ElectricViolet.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(7.dp))
                    .clickable {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("DepthLens Analysis", shareableText)
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(context, "Copied Successfully", Toast.LENGTH_SHORT).show()
                        dashCopied = true
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy analysis",
                        tint = if (dashCopied) ElectricViolet else TextMutedColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (dashCopied) "Copied!" else "Copy",
                        fontSize = 8.5.sp,
                        fontFamily = DMMonoFontFamily,
                        color = if (dashCopied) ElectricViolet else TextMutedColor
                    )
                }
            }

            // PDF Export Button - styled consistent with Copy Button
            var isPdfExporting by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isPdfExporting) SuccessColor.copy(alpha = 0.18f) else Surface3)
                    .border(1.dp, if (isPdfExporting) SuccessColor.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(7.dp))
                    .clickable {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        isPdfExporting = true
                        val fileName = "depthlens_analysis_${System.currentTimeMillis()}.pdf"
                        val pdfBytes = generatePdfReport("DepthLens Analysis Result", shareableText)
                        val ok = saveToDownloads(context, fileName, "application/pdf", pdfBytes)
                        if (ok) {
                            Toast.makeText(context, "Exported Successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Export failed: unable to write PDF file.", Toast.LENGTH_SHORT).show()
                        }
                        isPdfExporting = false
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Export to PDF",
                        tint = if (isPdfExporting) SuccessColor else PremiumCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isPdfExporting) "Saving..." else "Export PDF",
                        fontSize = 8.5.sp,
                        fontFamily = DMMonoFontFamily,
                        color = if (isPdfExporting) SuccessColor else PremiumCyan
                    )
                }
            }
            
            // Share Button - styled consistent with Copy and Export buttons
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Surface3)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                    .clickable {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        try {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareableText)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "DepthLens Analysis")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share DepthLens Report"))
                            Toast.makeText(context, "Shared Successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share analysis",
                        tint = PremiumCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Share",
                        fontSize = 8.5.sp,
                        fontFamily = DMMonoFontFamily,
                        color = PremiumCyan
                    )
                }
            }

            // Regenerate Button - styled consistent with app design
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Surface3)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                    .clickable {
                        try {
                            onRegenerate()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Regeneration failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate analysis",
                        tint = WarningColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Regen",
                        fontSize = 8.5.sp,
                        fontFamily = DMMonoFontFamily,
                        color = WarningColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "◈ Reality diagnostic reports verified. Pattern matching complete.",
            fontSize = 8.sp,
            color = SuccessColor.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

@Composable
fun DiagnosticIndicatorBlock(label: String, content: String) {
    if (content.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 3.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryColor,
                letterSpacing = 0.5.sp
            )
            Text(
                text = content,
                fontSize = 11.sp,
                color = TextPrimaryColor,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun BottomInputPanel(
    selectedMode: String,
    attachedImageUri: String?,
    isLoading: Boolean,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSubmit: (String) -> Unit,
    onModeChanged: (String) -> Unit = {},
    onInputFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    val modes = listOf("Multi-Layer", "Deep Synthesis", "Psychology", "Systems", "Probability", "Root Cause")

    var isListeningForSpeech by remember { mutableStateOf(false) }
    val speechContext = LocalContext.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val recognitionListener = remember {
        object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListeningForSpeech = false
            }
            override fun onError(error: Int) {
                isListeningForSpeech = false
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    rawText = if (rawText.isEmpty()) spokenText else "$rawText $spokenText"
                }
                isListeningForSpeech = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.setListener(null, "DASHBOARD_SCREEN_MIC")
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.releaseOwnership("DASHBOARD_SCREEN_MIC")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListeningForSpeech = true
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            com.example.ui.viewmodel.SharedSpeechRecognizerManager.startListening(speechContext, intent, recognitionListener, "DASHBOARD_SCREEN_MIC")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D14)) // Surface 1
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = Color(0x0FFFFFFF), // Border subtle top line
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
            .navigationBarsPadding()
            .padding(12.dp)
    ) {
        // Mode chips row: inactive chip has border Color(0x0FFFFFFF), text Color(0x4DF0EEFF). Active mode chip: theme accent border, theme accent text, theme accent-dim background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { mode ->
                val isActive = mode == selectedMode
                Box(
                    modifier = Modifier
                        .clickable { 
                            onModeChanged(mode)
                        }
                        .background(
                            color = if (isActive) ThemeManager.accentColor.copy(alpha = 0.18f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) ThemeManager.accentColor else Color(0x0FFFFFFF),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mode,
                        fontSize = 8.5.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) ThemeManager.accentColor else Color(0x4DF0EEFF)
                    )
                }
            }
        }

        attachedImageUri?.let { uri ->
            AttachmentPreviewItem(
                uri = uri,
                onRemove = onRemoveAttachment,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAddAttachment,
                enabled = !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .background(Surface2, CircleShape) // Surface 2
                    .border(1.dp, BorderSubtle, CircleShape) // Border subtle
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach resource",
                    tint = PremiumCyan, // PremiumCyan
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = {
                    Text(
                        text = "Ask anything deeper...",
                        fontSize = 12.sp,
                        color = TextSecondaryColor.copy(alpha = 0.5f), // ink-3 dynamic
                        fontFamily = InstrumentSansFontFamily
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface2) // Surface 2
                    .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp)) // Border subtle
                    .heightIn(min = 40.dp, max = 110.dp)
                    .onFocusChanged { onInputFocusChanged(it.isFocused) },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryColor, // ink-1 dynamic
                    unfocusedTextColor = TextPrimaryColor,
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2,
                    disabledContainerColor = Surface2,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (isListeningForSpeech) {
                        com.example.ui.viewmodel.SharedSpeechRecognizerManager.stopListening("DASHBOARD_SCREEN_MIC")
                        com.example.ui.viewmodel.SharedSpeechRecognizerManager.releaseOwnership("DASHBOARD_SCREEN_MIC")
                        isListeningForSpeech = false
                    } else {
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            speechContext,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            isListeningForSpeech = true
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            com.example.ui.viewmodel.SharedSpeechRecognizerManager.startListening(speechContext, intent, recognitionListener, "DASHBOARD_SCREEN_MIC")
                        } else {
                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isListeningForSpeech) Color(0xFFFF3B30).copy(alpha = 0.2f) else Surface2,
                        CircleShape
                    )
                    .border(1.dp, if (isListeningForSpeech) Color(0xFFFF3B30) else BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = if (isListeningForSpeech) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListeningForSpeech) Color(0xFFFF3B30) else PremiumCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (rawText.trim().isNotEmpty() || attachedImageUri != null) {
                        val promptWithMode = if (selectedMode != "Root Cause") {
                            "[$selectedMode Mode] ${rawText.trim()}"
                        } else {
                            rawText.trim()
                        }
                        onSubmit(promptWithMode)
                        rawText = ""
                    }
                },
                enabled = !isLoading && (rawText.trim().isNotBlank() || attachedImageUri != null),
                modifier = Modifier
                    .size(40.dp)
                    .background(ThemeManager.accentColor, CircleShape) // Theme-dependent Send Button
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Transmit",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun getRelativeTimeString(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return if (minutes == 1L) "1 min ago" else "$minutes mins ago"
    val hours = minutes / 60
    if (hours < 24) return if (hours == 1L) "1 hour ago" else "$hours hours ago"
    val days = hours / 24
    if (days < 7) return if (days == 1L) "Yesterday" else "$days days ago"
    val weeks = days / 7
    return if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
}

@Composable
fun AudioPlayBubble(uriString: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var progress by remember { mutableStateOf(0.0f) }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.toFloat()
                        val duration = player.duration.toFloat()
                        if (duration > 0) {
                            progress = current / duration
                        }
                    } else {
                        isPlaying = false
                        progress = 1.0f
                    }
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RichNavy)
            .border(1.dp, SurfaceCardColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            mediaPlayer = android.media.MediaPlayer.create(context, Uri.parse(uriString))
                        }
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    isPlaying = !isPlaying
                }
            },
            modifier = Modifier
                .size(32.dp)
                .background(PremiumCyan.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = "Play voice note",
                tint = PremiumCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text("Voice Input Diagnostic", fontSize = 11.sp, color = SuccessColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = PremiumCyan,
                trackColor = SurfaceCardColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun FileDocumentBubble(uriString: String, mimeType: String) {
    val context = LocalContext.current
    val (typeName, color) = remember(mimeType) {
        when {
            mimeType == "application/pdf" -> "PDF Source Document" to ErrorColor
            mimeType.startsWith("video/") -> "Video Evidence File" to ElectricViolet
            else -> "Source Intellectual File" to WarningColor
        }
    }
    
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { openAttachment(context, uriString, mimeType) }
            .background(RichNavy)
            .border(1.dp, SurfaceCardColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mimeType.startsWith("video/")) Icons.Default.PlayArrow else Icons.Default.List,
                contentDescription = typeName,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(typeName, fontSize = 11.sp, color = TextPrimaryColor, fontWeight = FontWeight.Bold)
            Text("First-class semantic token active", fontSize = 9.sp, color = TextSecondaryColor)
        }
    }
}

@Composable
fun AttachmentPreviewItem(
    uri: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mimeType = remember(uri) { getUriMimeType(context, uri) }
    
    val (typeName, colorAccent) = remember(mimeType) {
        when {
            mimeType.startsWith("image/") -> "Image Asset" to PremiumCyan
            mimeType.startsWith("audio/") -> "Audio Recording" to SuccessColor
            mimeType.startsWith("video/") -> "Video Source" to ElectricViolet
            mimeType == "application/pdf" -> "PDF Document" to ErrorColor
            else -> "Document Resource" to WarningColor
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RichNavy)
            .border(1.dp, SurfaceCardColor, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colorAccent.copy(alpha = 0.15f))
                .border(1.dp, colorAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mimeType.startsWith("image/")) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Preview Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = when {
                        mimeType.startsWith("audio/") -> Icons.Default.Place
                        mimeType.startsWith("video/") -> Icons.Default.PlayArrow
                        mimeType == "application/pdf" -> Icons.Default.List
                        else -> Icons.Default.Edit
                    },
                    contentDescription = typeName,
                    tint = colorAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = typeName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryColor
            )
            Text(
                text = "First-class source: Ready for contextual reasoning",
                fontSize = 9.sp,
                color = TextSecondaryColor
            )
        }
        
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Deattach",
                tint = ErrorColor.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun RecordingVoiceHud(
    durationSeconds: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepMidnight)
            .border(BorderStroke(1.dp, ErrorColor))
            .navigationBarsPadding()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var dotAlpha by remember { mutableStateOf(1f) }
        LaunchedEffect(Unit) {
            while (true) {
                dotAlpha = if (dotAlpha == 1f) 0.2f else 1f
                kotlinx.coroutines.delay(600)
            }
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(ErrorColor.copy(alpha = dotAlpha))
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Text(
            text = "LIVE AUDIO SPECTRA SCAN ACTIVE",
            color = ErrorColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60),
            color = TextPrimaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        IconButton(
            onClick = onSave,
            modifier = Modifier
                .size(34.dp)
                .background(SuccessColor, CircleShape)
        ) {
            Icon(Icons.Default.Send, contentDescription = "Accept Audio", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        
        Spacer(modifier = Modifier.width(6.dp))
        
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(34.dp)
                .background(RichNavy, CircleShape)
                .border(1.dp, SurfaceCardColor, CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel Recording", tint = ErrorColor, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun ConversationContinuityDashboard(
    brief: String?,
    status: String,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (status == "Syncing") PremiumCyan else ElectricViolet.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = RichNavy.copy(alpha = 0.85f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Continuity Engine",
                        tint = PremiumCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONVERSATION CONTINUITY ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        letterSpacing = 1.sp
                    )
                }
                
                if (brief != null) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Expand",
                            tint = TextSecondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tracks discussion vectors across sessions.",
                fontSize = 9.sp,
                color = TextSecondaryColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (status) {
                "Idle" -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Context re-connection point available.",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onSync,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Reconnect Previous Discussion", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                "Syncing" -> {
                    ThreeDotThinkingIndicator(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        text = "Synthesizing cognitive continuity tracking..."
                    )
                }
                "Done" -> {
                    if (brief != null && expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepMidnight)
                                .border(1.dp, SurfaceCardColor, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = brief,
                                fontSize = 11.sp,
                                color = TextSecondaryColor,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (brief != null && !expanded) {
                        Text(
                            text = "Continuity summary minimized. Tap arrow to expand.",
                            fontSize = 10.sp,
                            color = PremiumCyan,
                            fontStyle = FontStyle.Normal
                        )
                    }
                }
                "Error" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reconnection failed.",
                            fontSize = 11.sp,
                            color = ErrorColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Sync",
                            fontSize = 11.sp,
                            color = PremiumCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onSync() }
                        )
                    }
                }
            }
        }
    }
}

fun getUriMimeType(context: android.content.Context, uriString: String): String {
    val uri = Uri.parse(uriString)
    if (uri.scheme == "content" || uri.scheme == "android.resource") {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }
    val ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uriString)
    if (!ext.isNullOrEmpty()) {
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }
    return when {
        uriString.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        uriString.endsWith(".mp3", ignoreCase = true) || uriString.endsWith(".m4a", ignoreCase = true) || uriString.endsWith(".aac", ignoreCase = true) || uriString.endsWith(".wav", ignoreCase = true) -> "audio/mpeg"
        uriString.endsWith(".mp4", ignoreCase = true) || uriString.endsWith(".mov", ignoreCase = true) -> "video/mp4"
        uriString.endsWith(".png", ignoreCase = true) || uriString.endsWith(".jpg", ignoreCase = true) || uriString.endsWith(".jpeg", ignoreCase = true) || uriString.endsWith(".webp", ignoreCase = true) -> "image/png"
        else -> "application/octet-stream"
    }
}

class VoiceRecorder(private val context: android.content.Context) {
    private var recorder: android.media.MediaRecorder? = null
    private var outputFile: java.io.File? = null

    fun startRecording(): String? {
        return try {
            val file = java.io.File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.m4a")
            outputFile = file
            
            val rec = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            rec.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecording(): String? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        return outputFile?.absolutePath
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthLensUpdateAvailableDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit,
    onUpdateNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RichNavy,
        textContentColor = TextPrimaryColor,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PremiumCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "New Version Available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A new version of DepthLens is available with improvements and new features.",
                    fontSize = 13.sp,
                    color = TextPrimaryColor,
                    lineHeight = 18.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Your conversations, memories, and settings will be preserved during this update.",
                            fontSize = 12.sp,
                            color = SuccessColor,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepMidnight),
                    border = BorderStroke(1.dp, SurfaceCardColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VERSION ${release.tagName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = release.publishedAt,
                                fontSize = 11.sp,
                                color = TextSecondaryColor
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = SurfaceCardColor
                        )
                        
                        val bodyText = release.body
                        bodyText.split("\n").forEach { line ->
                            val trimmedLine = line.trim()
                            if (trimmedLine.startsWith("###")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = trimmedLine.replace("###", "").trim().uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            } else if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                                Row(
                                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", color = PremiumCyan, modifier = Modifier.padding(end = 6.dp))
                                    Text(
                                        text = trimmedLine.substring(1).trim(),
                                        fontSize = 12.sp,
                                        color = TextPrimaryColor,
                                        lineHeight = 16.sp
                                    )
                                }
                            } else if (trimmedLine.isNotEmpty()) {
                                Text(
                                    text = trimmedLine,
                                    fontSize = 12.sp,
                                    color = TextSecondaryColor,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Download", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondaryColor)
            ) {
                Text("Later")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareUpdatesDialog(
    onDismissRequest: () -> Unit,
    onManualCheck: () -> Unit,
    isChecking: Boolean,
    autoCheckEnabled: Boolean,
    onAutoCheckToggle: (Boolean) -> Unit,
    lastChecked: Long,
    latestRelease: GitHubRelease?,
    history: List<String>,
    currentVersion: String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = RichNavy,
        textContentColor = TextPrimaryColor,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = PremiumCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Software Updates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepMidnight),
                    border = BorderStroke(1.dp, SurfaceCardColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Version", fontSize = 11.sp, color = TextSecondaryColor)
                            Text(
                                "v$currentVersion",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Safety Check", fontSize = 11.sp, color = TextSecondaryColor)
                            val checkStr = if (lastChecked == 0L) "Never" else {
                                val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                                sdf.format(Date(lastChecked))
                            }
                            Text(
                                checkStr,
                                fontSize = 11.sp,
                                color = TextPrimaryColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepMidnight)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto Check for Updates",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                        Text(
                            text = "Verify security release updates silently on startup (every 24h).",
                            fontSize = 10.sp,
                            color = TextSecondaryColor,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = onAutoCheckToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PremiumCyan,
                            uncheckedThumbColor = TextSecondaryColor,
                            uncheckedTrackColor = SurfaceCardColor
                        )
                    )
                }

                Button(
                    onClick = onManualCheck,
                    enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = PremiumCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting to server...", fontSize = 12.sp, color = PremiumCyan)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates Now", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (latestRelease != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(SuccessColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Latest Release Cached",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tag: ${latestRelease.tagName} • ${latestRelease.name}",
                                fontSize = 11.sp,
                                color = TextPrimaryColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "UPDATE PIPELINE HISTORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    
                    if (history.isEmpty()) {
                        Text(
                            text = "No recorded updates yet.",
                            fontSize = 11.sp,
                            color = TextSecondaryColor
                        )
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepMidnight),
                            border = BorderStroke(1.dp, SurfaceCardColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                history.forEach { h ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("✓", color = SuccessColor, modifier = Modifier.padding(end = 6.dp), fontSize = 11.sp)
                                        Text(
                                            text = h,
                                            fontSize = 11.sp,
                                            color = TextPrimaryColor,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(contentColor = PremiumCyan)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SoftwareDownloadProgressBarCard(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepMidnight.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(PremiumCyan.copy(alpha = 0.15f), CircleShape)
            ) {
                CircularProgressIndicator(
                    progress = { if (progress >= 0f) progress else 0f },
                    color = PremiumCyan,
                    trackColor = SurfaceCardColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UPDATING SYSTEM DEPTHLENS...",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val percentageStr = if (progress >= 0f) "${(progress * 100).toInt()}%" else "Downloading..."
                val progressMbStr = if (totalBytes > 0) {
                    val downloadedMb = downloadedBytes.toFloat() / (1024 * 1024)
                    val totalMb = totalBytes.toFloat() / (1024 * 1024)
                    String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
                } else {
                    val downloadedKb = downloadedBytes / 1024
                    "$downloadedKb KB downloaded"
                }

                Text(
                    text = "$percentageStr ($progressMbStr)",
                    fontSize = 11.sp,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { if (progress >= 0f) progress else 0f },
                    color = PremiumCyan,
                    trackColor = SurfaceCardColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .background(SurfaceCardColor, CircleShape)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel update download",
                    tint = ErrorColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun AnalysisFailureErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onReportBug: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
        border = BorderStroke(1.2.dp, ErrorColor.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analysis Couldn't Be Completed",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Something went wrong while processing your request. Please check your connectivity and confirm configuration settings.",
                fontSize = 12.sp,
                color = TextSecondaryColor,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepMidnight, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = errorMessage.removePrefix("Error invoking DepthLens engine:").trim(),
                    fontSize = 11.sp,
                    color = ErrorColor.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    lineHeight = 15.sp
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Retry Analysis", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onReportBug,
                    colors = ButtonDefaults.buttonColors(containerColor = RichNavy),
                    border = BorderStroke(1.dp, SurfaceCardColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Report Bug", color = PremiumCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("Cancel", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun InlineLoadingIndicator() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = RichNavy.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ThreeDotThinkingIndicator(
                text = "Thinking..."
            )
        }
    }
}

fun ParsedResponse.toShareableText(): String {
    return this.exportText()
}

@Composable
fun BottomTabItem(
    tabId: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val transitionDuration = 280
    val cubicBezierEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    // Press scale shrinks button down slightly (active press: scale(.95))
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "press_scale"
    )

    // popIn Animatable for scale and opacity matching HTML @keyframes popIn exactly
    val popInScale = remember { Animatable(if (isActive) 1f else 1f) }
    val popInOpacity = remember { Animatable(if (isActive) 1f else 1f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            popInOpacity.snapTo(0.75f)
            popInScale.snapTo(0.88f)
            // Parallel execution of opacity and scale
            launch {
                popInOpacity.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 340, easing = cubicBezierEasing)
                )
            }
            launch {
                popInScale.animateTo(
                    targetValue = 1.06f,
                    animationSpec = tween(durationMillis = 204, easing = cubicBezierEasing)
                )
                popInScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 136, easing = cubicBezierEasing)
                )
            }
        } else {
            popInScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = cubicBezierEasing)
            )
            popInOpacity.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = cubicBezierEasing)
            )
        }
    }

    val finalScale = pressScale * popInScale.value

    // Icon scale (scales icon up slightly when active, matching HTML's popIn scale on svg)
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = transitionDuration, easing = cubicBezierEasing),
        label = "icon_scale"
    )

    // Tint for icon/label
    val tint by animateColorAsState(
        targetValue = when {
            isActive -> Color.White
            else -> TextMutedColor
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "content_tint"
    )

    // Glow alpha transition for the Neon Halo outer glow
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = transitionDuration, easing = cubicBezierEasing),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        // Double-layer Neon Halo Glow behind the active button
        if (glowAlpha > 0.01f) {
            // Layer 1: Tight halo (20px glow, 75% intensity of Theme accent color)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .background(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to ElectricViolet.copy(alpha = glowAlpha * 0.75f),
                                0.4f to ElectricViolet.copy(alpha = glowAlpha * 0.35f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )

            // Layer 2: Wide bloom (44px halo, 38% intensity of Theme accent color)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(2.2f)
                    .background(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to ElectricViolet.copy(alpha = glowAlpha * 0.38f),
                                0.5f to ElectricViolet.copy(alpha = glowAlpha * 0.12f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )
        }

        // Content layer - active tab is a contained rounded-corner glossy 3D button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(finalScale)
                .then(
                    if (isActive) Modifier.clip(RoundedCornerShape(17.dp))
                    else Modifier
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .graphicsLayer {
                    alpha = if (isActive) popInOpacity.value else 1f
                }
                .drawBehind {
                    if (isActive) {
                        val cornerRadiusPx = 17.dp.toPx()
                        val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

                        // 1. Glossy gradient background
                        // linear-gradient(160deg, color-mix(in srgb, var(--accent) 90%, white 14%), var(--accent) 45%, var(--accent3))
                        val glossyBrush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to lerp(ElectricViolet, Color.White, 0.14f),
                                0.45f to ElectricViolet,
                                1.0f to GradientEnd
                            ),
                            start = Offset(size.width * 0.2f, 0f),
                            end = Offset(size.width * 0.8f, size.height)
                        )
                        drawRoundRect(brush = glossyBrush, cornerRadius = cornerRadius)

                        // 2. Inset top highlight: inset 0 1.5px 0 rgba(255,255,255,.55)
                        val topHighlightBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 1.5.dp.toPx()
                        )
                        drawRoundRect(brush = topHighlightBrush, cornerRadius = cornerRadius)

                        // 3. Inset bottom shading: inset 0 -11px 18px color-mix(in srgb, var(--accent3) 65%, transparent)
                        val bottomShadeBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GradientEnd.copy(alpha = 0.65f)
                            ),
                            startY = (size.height - 24.dp.toPx()).coerceAtLeast(0f),
                            endY = size.height
                        )
                        drawRoundRect(brush = bottomShadeBrush, cornerRadius = cornerRadius)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .scale(iconScale),
                    contentAlignment = Alignment.Center
                ) {
                    when (tabId) {
                        "home", "chat" -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val cx = w / 2f
                                val cy = h / 2f

                                val peakOffset = h * 0.24f
                                val eyePath = Path().apply {
                                    moveTo(w * 0.15f, cy)
                                    cubicTo(w * 0.35f, cy - peakOffset, w * 0.65f, cy - peakOffset, w * 0.85f, cy)
                                    cubicTo(w * 0.65f, cy + peakOffset, w * 0.35f, cy + peakOffset, w * 0.15f, cy)
                                    close()
                                }
                                drawPath(
                                    path = eyePath,
                                    color = tint,
                                    style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
                                )

                                val irisR = w * 0.17f
                                drawArc(
                                    color = if (isActive) PremiumCyan else tint,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(cx - irisR, cy - irisR),
                                    size = Size(irisR * 2, irisR * 2),
                                    style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                        "insights" -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val cx = w / 2f
                                val cy = h / 2f

                                val path = Path().apply {
                                    moveTo(cx, h * 0.15f)
                                    lineTo(w * 0.85f, cy)
                                    lineTo(cx, h * 0.85f)
                                    lineTo(w * 0.15f, cy)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = tint,
                                    style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
                                )

                                drawCircle(
                                    color = if (isActive) PremiumCyan else tint,
                                    radius = 2.2.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                        "settings" -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val cx = w / 2f
                                val cy = h / 2f

                                val outerR = w * 0.32f
                                val innerR = w * 0.16f

                                drawCircle(
                                    color = tint,
                                    radius = outerR,
                                    style = Stroke(width = 1.7.dp.toPx())
                                )
                                drawCircle(
                                    color = if (isActive) PremiumCyan else tint,
                                    radius = innerR,
                                    style = Stroke(width = 1.4.dp.toPx())
                                )

                                val teethCount = 8
                                for (i in 0 until teethCount) {
                                    val angleRad = Math.toRadians((i * 360.0 / teethCount))
                                    val cosV = Math.cos(angleRad).toFloat()
                                    val sinV = Math.sin(angleRad).toFloat()

                                    val startX = cx + outerR * cosV
                                    val startY = cy + outerR * sinV
                                    val endX = cx + (outerR + 2.dp.toPx()) * cosV
                                    val endY = cy + (outerR + 2.dp.toPx()) * sinV

                                    drawLine(
                                        color = tint,
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontFamily = InstrumentSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
            }
        }
    }
}

private fun saveToDownloads(context: android.content.Context, fileName: String, mimeType: String, contentBytes: ByteArray): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
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