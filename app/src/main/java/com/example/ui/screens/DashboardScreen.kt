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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.viewmodel.IntelligenceViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: IntelligenceViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val memoryInsights by viewModel.memoryInsights.collectAsState()
    
    val isMemoryEnabled by viewModel.isMemoryEnabled.collectAsState()
    val isCollectiveOptIn by viewModel.isCollectiveIntelligenceOptIn.collectAsState()
    val isSystemControlsExpanded by viewModel.isSystemControlsExpanded.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    val continuityBrief by viewModel.continuityBrief.collectAsState()
    val continuityBriefStatus by viewModel.continuityBriefStatus.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

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

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showThemeSelectorDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReportBugDialog by remember { mutableStateOf(false) }
    
    var reportBugMessage by remember { mutableStateOf("") }
    var reportBugSubmitted by remember { mutableStateOf(false) }
    var reportBugUserConsented by remember { mutableStateOf(false) }
    
    var feedbackCategory by remember { mutableStateOf("Suggestion") }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackEmail by remember { mutableStateOf("") }
    var feedbackSubmitted by remember { mutableStateOf(false) }

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

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setAttachment(uri.toString())
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
    var sidebarSearchQuery by remember { mutableStateOf("") }

    // DEPTHLENS NAVIGATION & BACK BUTTON STANDARD™
    BackHandler(enabled = true) {
        when {
            // Priority 1: Sidebar Drawer Open -> Close Sidebar
            drawerState.isOpen -> {
                coroutineScope.launch { drawerState.close() }
            }
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
            listState.animateScrollToItem(activeMessages.size - 1)
        }
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
        val curVerStr = packageInfo.versionName ?: "1.0"
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
                            text = "✦ Leave DepthLens?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Your current analysis will remain saved.",
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
                        // EXIT (Primary Linear Gradient Button)
                        Button(
                            onClick = {
                                showExitConfirm = false
                                val activity = (context as? android.app.Activity)
                                activity?.finish()
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
                                "Exit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // STAY (Minimal Transparent Text Button)
                        TextButton(
                            onClick = { showExitConfirm = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Transparent)
                        ) {
                            Text(
                                "Stay",
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

    if (showAboutDialog) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: java.lang.Exception) {
            null
        }
        val appVersion = packageInfo?.versionName ?: "2.0"
        
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(ElectricViolet.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_depthlens_logo),
                            contentDescription = "DepthLens Logo",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
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
                        text = "Most people see the surface.\n\nDepthLens reveals what lies beneath reality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
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
                            Triple(true, "Deep Midnight", "Dark • Focused • Cinematic"),
                            Triple(false, "Polar Dawn", "Clean • Bright • Productive")
                        )

                        themesList.forEach { (isDark, name, desc) ->
                            val isSelected = ThemeManager.isDarkTheme == isDark
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        ThemeManager.setTheme(context, isDark)
                                        android.widget.Toast.makeText(context, "Theme Applied: $name", android.widget.Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            showThemeSelectorDialog = false
                                        }
                                    }
                                    .background(
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF1E1E2E) else Color(0xFFF1F5F9)
                                        } else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            if (isDark) PremiumCyan.copy(alpha = 0.8f) else Color(0xFFE2E8F0)
                                        } else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, if (isSelected) PremiumCyan else if (ThemeManager.isDarkTheme) Color(0xFF475569) else Color(0xFF94A3B8), CircleShape)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(PremiumCyan, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (ThemeManager.isDarkTheme) Color.White else Color(0xFF0F172A)
                                        } else TextSecondaryColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (ThemeManager.isDarkTheme) Color(0xFF8E8D9F) else Color(0xFF64748B)
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
                if (!feedbackSubmitted) {
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
                                .height(100.dp),
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
                            text = "Thank You!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your feedback has been successfully securely registered. We review every submission manually.",
                            fontSize = 12.sp,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (!feedbackSubmitted) {
                    Button(
                        onClick = {
                            if (feedbackMessage.trim().isNotBlank()) {
                                feedbackSubmitted = true
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
                            showFeedbackDialog = false
                            feedbackSubmitted = false
                            feedbackMessage = ""
                            feedbackEmail = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text("Done", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                if (!feedbackSubmitted) {
                    TextButton(onClick = { showFeedbackDialog = false }) {
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
        val appVerStr = packageInfoReport?.versionName ?: "2.0"
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
                                .height(100.dp),
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
                                reportBugSubmitted = true
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
                        fontStyle = FontStyle.Italic
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

    // Beautiful Premium Modal Sidebar Redesign
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = RichNavy,
                drawerContentColor = TextPrimaryColor,
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())

                // Redesigned Brand Header with Neon Violet Accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(ElectricViolet.copy(alpha = 0.4f), Color.Transparent)
                                        )
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                                    contentDescription = "DepthLens Logo",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DEPTHLENS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryColor,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "See Beyond Surface",
                                    fontSize = 11.sp,
                                    color = PremiumCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceCardColor, modifier = Modifier.padding(horizontal = 20.dp))

                // Primary Quick Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.createSession("")
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Thread", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Scenario", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset State", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp, color = ErrorColor)
                    }
                }

                // Highly structured navigation links partition
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // SEC 1: PINNED SCENARIOS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = WarningColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PINNED FOCUS SCENARIOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryColor,
                            letterSpacing = 1.sp
                        )
                    }

                    val pinnedTemplates = listOf(
                        "Macro Systems Feedback Loop" to "Systems Feedback: Outline circular delays.",
                        "Psychological Incentive Audit" to "Incentives: Map social delays & resource blockades."
                    )
                    pinnedTemplates.forEach { (title, queryText) ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    color = SidebarTextColor.copy(alpha = 0.9f)
                                )
                            },
                            selected = false,
                            onClick = {
                                viewModel.createSession(title)
                                viewModel.sendQuery(queryText)
                                coroutineScope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PremiumCyan.copy(alpha = 0.7f), modifier = Modifier.size(12.dp)) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Conversations Box per User Intent
                    androidx.compose.foundation.text.BasicTextField(
                        value = sidebarSearchQuery,
                        onValueChange = { sidebarSearchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = SidebarTextColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PremiumCyan),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DeepMidnight)
                                    .border(1.dp, SurfaceCardColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = PremiumCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (sidebarSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search conversations...",
                                            fontSize = 13.sp,
                                            color = PlaceholderColor
                                        )
                                    }
                                    innerTextField()
                                }
                                if (sidebarSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { sidebarSearchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextSecondaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // SEC 2: RECENT CONVERSATIONS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECENT CONVERSATIONS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryColor,
                            letterSpacing = 1.sp
                        )
                    }

                    // Filtering Logic
                    val filteredSessions = remember(sessions, sidebarSearchQuery) {
                        if (sidebarSearchQuery.isBlank()) {
                            sessions
                        } else {
                            sessions.filter { session ->
                                session.title.contains(sidebarSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredSessions.isEmpty()) {
                        Text(
                            text = if (sidebarSearchQuery.isEmpty()) "No conversation history." else "No matches found.",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            modifier = Modifier.padding(horizontal = 25.dp, vertical = 8.dp),
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        filteredSessions.forEach { session ->
                            val isSelected = session.id == activeSessionId
                            val relativeTime = getRelativeTimeString(session.lastUpdatedAt)
                            
                            androidx.compose.material3.Surface(
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SurfaceCardColor else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Star Pin / Unpin button toggles isPinned locally with aesthetic response
                                    IconButton(
                                        onClick = { viewModel.togglePinSession(session.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = if (session.isPinned) "Unpin" else "Pin",
                                            tint = if (session.isPinned) WarningColor else Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            maxLines = 1,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextSecondaryColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = relativeTime,
                                            fontSize = 10.sp,
                                            color = TextSecondaryColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    // Close / Delete Session button
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = ErrorColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SEC 3: SYSTEM WORKSPACES
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE ENTERPRISE WORKSPACES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryColor,
                            letterSpacing = 1.sp
                        )
                    }

                    val workspaceNodes = listOf("Cognitive Forensics Lab", "Strategic Alignment Workspace")
                    workspaceNodes.forEach { ws ->
                        NavigationDrawerItem(
                            label = { Text(ws, fontSize = 12.sp, color = TextSecondaryColor) },
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "$ws activated locally.", Toast.LENGTH_SHORT).show()
                            },
                            icon = { Box(modifier = Modifier.size(6.dp).background(ElectricViolet, CircleShape)) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp)
                        )
                    }
                }

                // SEC 4: Bottom alignment for Privacy & Memory configuration trigger (Stops visual memory clutter!)
                HorizontalDivider(color = SurfaceCardColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepMidnight)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSystemControlsExpanded(!isSystemControlsExpanded) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("💡 SYSTEM CONTROLS", fontSize = 11.sp, color = TextPrimaryColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Icon(
                                imageVector = if (isSystemControlsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isSystemControlsExpanded) "Collapse" else "Expand",
                                tint = PremiumCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isSystemControlsExpanded,
                            enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                // Collective Learning toggle row
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Collective Learning",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryColor
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isCollectiveOptIn) "ON" else "OFF",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCollectiveOptIn) SuccessColor else TextSecondaryColor
                                            )
                                            Switch(
                                                checked = isCollectiveOptIn,
                                                onCheckedChange = { viewModel.setCollectiveIntelligenceOptIn(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = SuccessColor,
                                                    checkedTrackColor = SuccessColor.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = TextSecondaryColor,
                                                    uncheckedTrackColor = SurfaceCardColor.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.scale(0.75f)
                                            )
                                        }
                                    }
                                }

                                // Notifications toggle row
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Notifications",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryColor
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (notificationsEnabled) "ON" else "OFF",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (notificationsEnabled) SuccessColor else TextSecondaryColor
                                            )
                                            Switch(
                                                checked = notificationsEnabled,
                                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = SuccessColor,
                                                    checkedTrackColor = SuccessColor.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = TextSecondaryColor,
                                                    uncheckedTrackColor = SurfaceCardColor.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.scale(0.75f)
                                            )
                                        }
                                    }
                                }

                                // Appearance row
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clickable {
                                            coroutineScope.launch { drawerState.close() }
                                            showThemeSelectorDialog = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Appearance",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SidebarTextColor
                                            )
                                        }
                                        Text(
                                            text = if (com.example.ui.theme.ThemeManager.isDarkTheme) "Deep Midnight" else "Polar Dawn",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeNameColor,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                }

                                // Feedback button
                                Button(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showFeedbackDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentPadding = PaddingValues(10.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Feedback",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SidebarTextColor
                                    )
                                }

                                // Report Bug button
                                Button(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showReportBugDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentPadding = PaddingValues(10.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Report Bug",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SidebarTextColor
                                    )
                                }

                                // Manage Memory & Privacy button
                                Button(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showMemoryDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentPadding = PaddingValues(10.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Manage Memory & Privacy",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SidebarTextColor
                                    )
                                }

                                val updateStatusText = remember(latestRelease) {
                                    val curInst = GithubUpdateManager.getInstalledVersion(context)
                                    val rel = latestRelease
                                    if (rel != null && GithubUpdateManager.isNewerVersion(rel.tagName, curInst)) {
                                        "Version $curInst\nUpdate available → ${rel.tagName}"
                                    } else {
                                        "Version $curInst\nLatest version installed"
                                    }
                                }

                                // Check For Updates button
                                Button(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showUpdatesDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 40.dp),
                                    contentPadding = PaddingValues(10.dp, 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = updateStatusText,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SidebarTextColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // About DepthLens button
                                Button(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showAboutDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentPadding = PaddingValues(10.dp, 4.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = SidebarIconColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "About DepthLens",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SidebarTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DEPTHLENS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = TextPrimaryColor,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "See Beyond Surface",
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumCyan,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Sidebar Menu", tint = ElectricViolet)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createSession("") }) {
                            Icon(Icons.Default.Create, contentDescription = "New Chat", tint = PremiumCyan)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DeepMidnight,
                        titleContentColor = TextPrimaryColor,
                        navigationIconContentColor = ElectricViolet,
                        actionIconContentColor = PremiumCyan
                    )
                )
            },
            containerColor = DeepMidnight
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
                                attachedImageUri = attachedImageUri,
                                isLoading = isLoading
                            )
                        } else {
                            // Comfortably padded scrolling Chat Feed
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 90.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(activeMessages) { message ->
                                        if (message.role == "user") {
                                            UserMessageBubble(message)
                                        } else {
                                            if (message.text.startsWith("Error:") || message.text.contains("Error invoking DepthLens")) {
                                                AnalysisFailureErrorCard(
                                                    errorMessage = message.text,
                                                    onRetry = { viewModel.retryLastAnalysis(message.id) },
                                                    onReportBug = { showReportBugDialog = true },
                                                    onCancel = { viewModel.deleteMessage(message.id) }
                                                )
                                            } else {
                                                val parsed = remember(message.text) { ResponseParser.parse(message.text) }
                                                DepthLensDiagnosticCard(
                                                    parsed = parsed,
                                                    onPromptSelected = { query -> viewModel.sendQuery(query) },
                                                    messageId = message.id,
                                                    onRegenerate = { viewModel.regenerateLastAnalysis(message.id) }
                                                )
                                            }
                                        }
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
                            attachedImageUri = attachedImageUri,
                            isLoading = isLoading,
                            onAddAttachment = { showAttachmentSelector = true },
                            onRemoveAttachment = { viewModel.clearAttachment() },
                            onSubmit = { text -> viewModel.sendQuery(text) }
                        )
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
        // Glowing Iris Core Logo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricViolet.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RichNavy)
                    .border(2.dp, PremiumCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                    contentDescription = "DepthLens Iris Core",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

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
                // If there's an active attached preview in center
                attachedImageUri?.let { uri ->
                    AttachmentPreviewItem(
                        uri = uri,
                        onRemove = onRemoveAttachment,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
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
fun UserMessageBubble(
    message: MessageEntity,
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
                if (!message.imageUri.isNullOrEmpty()) {
                    val context = LocalContext.current
                    val mimeType = remember(message.imageUri) { getUriMimeType(context, message.imageUri) }
                    
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
                                    model = message.imageUri,
                                    contentDescription = "Source thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        mimeType.startsWith("audio/") -> {
                            AudioPlayBubble(uriString = message.imageUri)
                        }
                        else -> {
                            FileDocumentBubble(uriString = message.imageUri, mimeType = mimeType)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ElectricViolet),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        lineHeight = 18.sp
                    )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Conversation overview context
        if (parsed.introduction.isNotEmpty()) {
            Text(
                text = parsed.introduction,
                fontSize = 13.sp,
                color = TextPrimaryColor,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // 1. Executive Summary Panel
        parsed.executiveSummary?.let { summary ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardColor),
                border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(PremiumCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "EXECUTIVE SUMMARY BRIEFING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumCyan,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        parsed.confidence?.let { lvl ->
                            val color = when (lvl.lowercase()) {
                                "high" -> SuccessColor
                                "medium" -> WarningColor
                                else -> ErrorColor
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "$lvl confidence".uppercase(),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        color = TextPrimaryColor,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 2. Key Insights - Active Cognitive Layers Panel
        if (parsed.depthLayers.isNotEmpty()) {
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
                        "KEY INSIGHTS COGNITIVE LAYERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    parsed.depthLayers.forEachIndexed { idx, layer ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Surface(
                            color = if (isExpanded) RichNavy else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LAYER ${layer.layerNumber} - ${layer.layerName.uppercase()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumCyan
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand Layer",
                                        tint = TextSecondaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Text(
                                        text = layer.description,
                                        fontSize = 11.sp,
                                        color = TextSecondaryColor,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp)
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
            val shareableText = remember(parsed) { parsed.toShareableText() }
            
            // 📋 Copy Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("DepthLens Analysis", shareableText)
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📋",
                    fontSize = 15.sp
                )
            }
            
            // ↗ Share Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareableText)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "DepthLens Analysis")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share DepthLens Report"))
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "↗",
                    fontSize = 15.sp,
                    color = PremiumCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // 🔄 Regenerate Button (AI response only)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onRegenerate()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔄",
                    fontSize = 15.sp
                )
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
    attachedImageUri: String?,
    isLoading: Boolean,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepMidnight)
            .border(BorderStroke(1.dp, SurfaceCardColor))
            .navigationBarsPadding()
            .padding(10.dp)
    ) {
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
                    .background(RichNavy, CircleShape)
                    .border(1.dp, SurfaceCardColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach resource",
                    tint = PremiumCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = {
                    Text(
                        text = "Reply, request deep loop, or query...",
                        fontSize = 12.sp,
                        color = TextSecondaryColor
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SurfaceCardColor, RoundedCornerShape(20.dp))
                    .heightIn(min = 40.dp, max = 110.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedContainerColor = RichNavy,
                    unfocusedContainerColor = RichNavy,
                    disabledContainerColor = RichNavy,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (rawText.trim().isNotEmpty() || attachedImageUri != null) {
                        onSubmit(rawText)
                        rawText = ""
                    }
                },
                enabled = !isLoading && (rawText.trim().isNotBlank() || attachedImageUri != null),
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isLoading) RichNavy else ElectricViolet, CircleShape)
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PremiumCyan,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Synthesizing cognitive continuity tracking...",
                            fontSize = 11.sp,
                            color = PremiumCyan,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic)
                        )
                    }
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
                            fontStyle = FontStyle.Italic
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = PremiumCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "RUNNING RECONSTRUCTION...",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deconstructing inputs across progressive layers of reality...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

fun ParsedResponse.toShareableText(): String {
    val builder = java.lang.StringBuilder()
    builder.append("=== DEPTHLENS STRATEGIC RECONSTRUCTION REPORT ===\n\n")
    if (introduction.isNotBlank()) {
        builder.append("INTRODUCTION\n")
        builder.append(introduction).append("\n\n")
    }
    
    val summary = executiveSummary
    if (!summary.isNullOrBlank()) {
        builder.append("EXECUTIVE SUMMARY\n")
        builder.append(summary).append("\n\n")
    }
    
    if (depthLayers.isNotEmpty()) {
        builder.append("DEPTH LAYERS (DECONSTRUCTING REALITY)\n")
        depthLayers.forEach { layer ->
            builder.append("• LAYER ").append(layer.layerNumber).append(" (").append(layer.layerName.uppercase()).append("): ")
                .append(layer.description).append("\n")
        }
        builder.append("\n")
    }
    
    val rcr = rootCauseReport
    if (rcr != null) {
        builder.append("ROOT CAUSE REPORT (THE 'WHY')\n")
        builder.append("Symptom: ").append(rcr.symptom).append("\n")
        builder.append("Immediate Cause: ").append(rcr.immediateCause).append("\n")
        builder.append("Underlying Cause: ").append(rcr.underlyingCause).append("\n")
        builder.append("Deeper Cause: ").append(rcr.deeperCause).append("\n")
        builder.append("Root Cause Estimate: ").append(rcr.rootCauseEstimate).append("\n")
        builder.append("Supporting Evidence: ").append(rcr.supportingEvidence).append("\n")
        if (rcr.alternativeExplanation.isNotBlank()) {
            builder.append("Alternative Explanation: ").append(rcr.alternativeExplanation).append("\n")
        }
        builder.append("\n")
    }
    
    val hd = humanDrivers
    if (hd != null) {
        builder.append("HUMAN DRIVERS (PSYCHOMOTIVE ANATOMY)\n")
        builder.append("Surface Intention: ").append(hd.surfaceIntention).append("\n")
        builder.append("Emotional Driver: ").append(hd.emotionalDriver).append("\n")
        builder.append("Core Need: ").append(hd.needDriver).append("\n")
        builder.append("Core Fear: ").append(hd.fearDriver).append("\n")
        builder.append("Incentives: ").append(hd.incentiveDriver).append("\n")
        builder.append("Identity Alignment: ").append(hd.identityDriver).append("\n")
        builder.append("Hidden Motives: ").append(hd.hiddenMotives).append("\n")
        builder.append("\n")
    }
    
    if (futureScenarios.isNotEmpty()) {
        builder.append("FUTURE SCENARIOS & PROBABILITIES\n")
        futureScenarios.forEach { scenario ->
            builder.append("- ").append(scenario.codeName.uppercase()).append(" - ").append(scenario.displayName).append(" (Prob: ").append(scenario.probability).append("%)\n")
            builder.append("  Outcome: ").append(scenario.impactText).append("\n")
            if (scenario.earlyWarningSigns.isNotEmpty()) {
                builder.append("  Early Warning Signs:\n")
                scenario.earlyWarningSigns.forEach { sign ->
                    builder.append("    * ").append(sign).append("\n")
                }
            }
        }
        builder.append("\n")
    }
    
    val conf = confidence
    if (!conf.isNullOrBlank()) {
        builder.append("Confidence Level: ").append(conf).append("\n")
    }
    builder.append("=================================================")
    return builder.toString()
}

