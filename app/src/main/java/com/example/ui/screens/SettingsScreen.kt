package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.*

@Composable
fun SettingsScreen(
    isMemoryEnabled: Boolean,
    onMemoryEnabledChanged: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    voiceOutputEnabled: Boolean = true,
    onVoiceOutputEnabledChanged: (Boolean) -> Unit = {},
    wakeWordEnabled: Boolean = false,
    onWakeWordEnabledChanged: (Boolean) -> Unit = {},
    isCollectiveOptIn: Boolean,
    onCollectiveOptInChanged: (Boolean) -> Unit,
    isPrivacyModeEnabled: Boolean = false,
    onPrivacyModeEnabledChanged: (Boolean) -> Unit = {},
    activeThemeName: String,
    onThemeSelected: (String) -> Unit,
    onShowMemoryDetails: () -> Unit = {},
    onShowUpdateDetails: () -> Unit = {},
    onWipeAllUserData: () -> Unit,
    onShowAbout: () -> Unit = {},
    onReportBug: () -> Unit = {},
    isLoggedIn: Boolean = false,
    isGuest: Boolean = false,
    userName: String = "",
    userEmail: String = "",
    userPhotoUrl: String = "",
    onNavigateToEditProfile: () -> Unit = {},
    githubToken: String = "",
    repoOwnerAndName: String = "",
    onSaveGithubSettings: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onLoginWithGoogle: (String, String) -> Unit = { _, _ -> },
    onLoginAsGuest: (String) -> Unit = {},
    diagnostics: com.example.ui.viewmodel.EngineDiagnostics = com.example.ui.viewmodel.EngineDiagnostics(),
    onRefreshDiagnostics: () -> Unit = {},
    selectedModel: String = com.example.data.repository.IntelligenceRepository.DEFAULT_MODEL,
    onModelSelected: (String) -> Unit = {},
    selectedVoiceAccent: String = "en_US",
    onVoiceAccentSelected: (String) -> Unit = {},
    syncStatus: String = "Offline",
    lastSyncedTime: String? = null,
    chatsSyncedCount: Int = 0,
    pendingUploadsCount: Int = 0,
    onSaveProfileName: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSubscreen by remember { mutableStateOf<String?>(null) }

    // System back retraces the settings drill-down (subscreen → back to its parent)
    // instead of jumping out to chat. Disabled at the root so the parent handles it.
    androidx.activity.compose.BackHandler(enabled = currentSubscreen != null) {
        currentSubscreen = when (currentSubscreen) {
            "privacy_policy", "terms_of_service" -> "about"
            else -> null
        }
    }

    // Navigation and sub-screen routing animation
    AnimatedContent(
        targetState = currentSubscreen,
        transitionSpec = {
            if (targetState != null) {
                // Animate slide-in from right
                slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(240)) { -it / 2 } + fadeOut()
            } else {
                // Animate slide-out to right
                slideInHorizontally(animationSpec = tween(280)) { -it / 2 } + fadeIn() togetherWith
                        slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut()
            }
        },
        label = "subscreen_transition"
    ) { subscreen ->
        when (subscreen) {
            "appearance" -> {
                AppearanceSubscreen(
                    onThemeSelected = onThemeSelected,
                    onBack = { currentSubscreen = null }
                )
            }
            "voicelang" -> {
                VoiceLangSubscreen(
                    wakeWordEnabled = wakeWordEnabled,
                    onWakeWordEnabledChanged = onWakeWordEnabledChanged,
                    selectedVoiceAccent = selectedVoiceAccent,
                    onVoiceAccentSelected = onVoiceAccentSelected,
                    onBack = { currentSubscreen = null }
                )
            }
            "ai" -> {
                AiIntelligenceSubscreen(
                    selectedModel = selectedModel,
                    onModelSelected = onModelSelected,
                    isMemoryEnabled = isMemoryEnabled,
                    onMemoryEnabledChanged = onMemoryEnabledChanged,
                    onBack = { currentSubscreen = null }
                )
            }
            "notif" -> {
                NotificationsSubscreen(
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsEnabledChanged = onNotificationsEnabledChanged,
                    onBack = { currentSubscreen = null }
                )
            }
            "privacy" -> {
                PrivacySubscreen(
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    onPrivacyModeEnabledChanged = onPrivacyModeEnabledChanged,
                    onWipeAllUserData = onWipeAllUserData,
                    onBack = { currentSubscreen = null }
                )
            }
            "update" -> {
                UpdateSubscreen(
                    onBack = { currentSubscreen = null }
                )
            }
            "report" -> {
                ReportBugSubscreen(
                    userName = userName,
                    userEmail = userEmail,
                    onBack = { currentSubscreen = null }
                )
            }
            "about" -> {
                AboutSubscreen(
                    onNavigateToPrivacyPolicy = { currentSubscreen = "privacy_policy" },
                    onNavigateToTerms = { currentSubscreen = "terms_of_service" },
                    onBack = { currentSubscreen = null }
                )
            }
            "privacy_policy" -> {
                PrivacyPolicySubscreen(
                    onBack = { currentSubscreen = "about" }
                )
            }
            "terms_of_service" -> {
                TermsOfServiceSubscreen(
                    onBack = { currentSubscreen = "about" }
                )
            }
            else -> {
                // Main Settings view
                SettingsMainView(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    isLoggedIn = isLoggedIn,
                    isGuest = isGuest,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToSub = { currentSubscreen = it },
                    onSignOut = onSignOut,
                    onBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
fun SettingsMainView(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    isLoggedIn: Boolean,
    isGuest: Boolean,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSub: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Tokens
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val accentGrad = Brush.linearGradient(listOf(ElectricViolet, GradientEnd))

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Glass Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                    Text("‹", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "Settings",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(38.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PROFILE HEADER CARD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleOnPress = 0.96f) { onNavigateToEditProfile() }
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val initial = if (userName.isNotEmpty()) userName.first().uppercase() else "A"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentGrad),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "User Name" },
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InstrumentSansFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail.ifBlank { "user@example.com" },
                            color = textMuted,
                            fontSize = 11.sp,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(accentGrad)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                }

                // PREFERENCES
                Text(
                    text = "PREFERENCES",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    SettingsRow(
                        icon = Icons.Default.Tune,
                        title = "Appearance",
                        desc = "Theme, glass material, accent, icon",
                        onClick = { onNavigateToSub("appearance") }
                    )
                    SettingsRow(
                        icon = Icons.Default.Public,
                        title = "Voice & Language",
                        desc = if (com.example.ui.viewmodel.ENABLE_WAKE_WORD) "Hey Lens, accent, speed" else "Voice accent, speaking speed",
                        onClick = { onNavigateToSub("voicelang") }
                    )
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI & Intelligence",
                        desc = "Model, depth, personality",
                        onClick = { onNavigateToSub("ai") }
                    )
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        desc = "Alerts, Dynamic Island",
                        onClick = { onNavigateToSub("notif") },
                        isLast = true
                    )
                }

                // DATA & SUPPORT
                Text(
                    text = "DATA & SUPPORT",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    SettingsRow(
                        icon = Icons.Default.Shield,
                        title = "Privacy & Data",
                        desc = "Cleanup, incognito, lock",
                        onClick = { onNavigateToSub("privacy") }
                    )
                    val context = LocalContext.current
                    val latestReleaseState by com.example.ui.screens.GithubUpdateManager.latestRelease.collectAsState()
                    val hasUpdate = latestReleaseState?.let {
                        com.example.ui.screens.GithubUpdateManager.isNewerVersion(it.tagName, com.example.ui.screens.GithubUpdateManager.getInstalledVersion(context))
                    } ?: false

                    SettingsRow(
                        icon = Icons.Default.SystemUpdate,
                        title = "Update App",
                        desc = if (hasUpdate) "Show Update Available" else "Check for updates",
                        onClick = { onNavigateToSub("update") }
                    )
                    SettingsRow(
                        icon = Icons.Default.BugReport,
                        title = "Report a Bug",
                        desc = null,
                        onClick = { onNavigateToSub("report") }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About DepthLens",
                        desc = null,
                        onClick = { onNavigateToSub("about") },
                        isLast = true
                    )
                }

                // Sign out button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .bounceClick(scaleOnPress = 0.96f) { onSignOut() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFFF6B8A).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign out",
                        color = Color(0xFFFF6B8A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InstrumentSansFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(60.dp)) // Nav bar clearance
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String?,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleOnPress = 0.96f) { onClick() }
                .padding(vertical = 13.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelViolet,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontFamily = InstrumentSansFontFamily
                )
                if (desc != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        color = textMuted,
                        fontSize = 10.sp,
                        fontFamily = InstrumentSansFontFamily
                    )
                }
            }

            Text(
                text = "›",
                color = textMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )
        }
        if (!isLast) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SUBSCREENS
// ─────────────────────────────────────────────────────────────

@Composable
fun SubscreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
            Text("‹", color = TextPrimaryColor, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimaryColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InstrumentSansFontFamily
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSubscreen(
    onThemeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Tokens
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val accentGrad = Brush.linearGradient(listOf(ElectricViolet, GradientEnd))

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.96f) { onBack() }
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.offset(y = (-2).dp))
                }
                Text(
                    text = "Appearance",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(38.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // THEME SECTION
                Text(
                    text = "THEME",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(16.dp)
                ) {
                    val themes = listOf(
                        "Deep Sea" to listOf(Color(0xFF7C5CFF), Color(0xFF0E0D1F)),
                        "Void" to listOf(Color(0xFF8E7CFF), Color(0xFF070608)),
                        "Future" to listOf(Color(0xFF3D7CFF), Color(0xFF04081A)),
                        "Ember" to listOf(Color(0xFFFF6B4A), Color(0xFF120706)),
                        "Purple" to listOf(Color(0xFFC46BFF), Color(0xFF0E0420)),
                        "Polar Dawn" to listOf(Color(0xFFCDD6FF), Color(0xFFEEF1F8))
                    )

                    // Preview parity: a single flex-wrap of swatches (gap 10), not two
                    // fixed rows with a padding hack.
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        themes.forEach { (name, colors) ->
                            val isSelected = ThemeManager.themeName == name
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(colors))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .bounceClick(scaleOnPress = 0.96f) {
                                        ThemeManager.setTheme(context, name)
                                        onThemeSelected(name)
                                    }
                            )
                        }
                    }
                }

                // ACCENT COLOR SECTION
                Text(
                    text = "ACCENT COLOR",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(16.dp)
                ) {
                    val accentColors = listOf(
                        "#7C5CFF", "#38E1D8", "#FF6B4A", "#C46BFF", "#3D7CFF", "#33D17A", "#FFB000"
                    )
                    
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        accentColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = ThemeManager.accentColor == color
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .bounceClick(scaleOnPress = 0.96f) {
                                        ThemeManager.setAccent(context, hex)
                                    }
                            )
                        }
                    }
                }

                // APP MATERIAL SECTION
                Text(
                    text = "APP MATERIAL (Glass Style)",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isLiquid = ThemeManager.glassStyle == "Liquid Crystal"

                    // Liquid Crystal Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isLiquid) ThemeManager.accentColor else glassBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .bounceClick(scaleOnPress = 0.96f) { ThemeManager.setGlass(context, "Liquid Crystal") }
                            .padding(14.dp)
                    ) {
                        Text("💧 Liquid Crystal", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InstrumentSansFontFamily)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Real backdrop blur — peeche ka content sach mein blur. iOS-26 vibe. (Android 12+)", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily, lineHeight = 14.sp)
                        Spacer(modifier = Modifier.weight(1f).heightIn(min = 10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                        )
                    }

                    // Frost Aurora Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (!isLiquid) ThemeManager.accentColor else glassBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .bounceClick(scaleOnPress = 0.96f) { ThemeManager.setGlass(context, "Frost Aurora") }
                            .padding(14.dp)
                    ) {
                        Text("❄ Frost Aurora", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InstrumentSansFontFamily)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Translucent tint + sheen, no heavy blur. Har device par smooth & fast.", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily, lineHeight = 14.sp)
                        Spacer(modifier = Modifier.weight(1f).heightIn(min = 10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .premiumGlassBg(cornerRadius = 10.dp)
                        )
                    }
                }

                // Blur Slider Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Blur strength", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Text("Liquid Crystal ke liye", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Slider(
                                value = ThemeManager.blurStrength,
                                onValueChange = { ThemeManager.setBlur(context, it) },
                                valueRange = 8f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ThemeManager.accentColor,
                                    activeTrackColor = ThemeManager.accentColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }

                // APP ICON SECTION
                Text(
                    text = "APP ICON",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(16.dp)
                ) {
                    var selectedIconIndex by remember { mutableIntStateOf(ThemeManager.appIconIndex) }
                    // Preview parity: real DepthLens eye-mark on the icon tiles.
                    val icons = listOf(
                        Brush.linearGradient(listOf(Color(0xFF7C5CFF), Color(0xFF5B3FD6))),
                        Brush.linearGradient(listOf(Color(0xFF38E1D8), Color(0xFF0E0D1F))),
                        Brush.linearGradient(listOf(Color(0xFFFF6B4A), Color(0xFF120706))),
                        Brush.linearGradient(listOf(Color(0xFFC46BFF), Color(0xFF0E0420))),
                        Brush.linearGradient(listOf(Color(0xFF0B0A17), Color(0xFF0B0A17)))
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        icons.forEachIndexed { index, brush ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(brush)
                                    .border(
                                        width = if (selectedIconIndex == index) 2.dp else if (index == 4) 1.dp else 0.dp,
                                        color = if (selectedIconIndex == index) Color.White else if (index == 4) Color(0xFF333333) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .bounceClick(scaleOnPress = 0.96f) {
                                        selectedIconIndex = index
                                        ThemeManager.setAppIcon(context, index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                                    contentDescription = "App icon $index",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // MOTION & FEEL SECTION
                Text(
                    text = "MOTION & FEEL",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) { ThemeManager.setMotion(context, !ThemeManager.reduceMotion) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduce motion", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Kam animations", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Switch(
                            checked = ThemeManager.reduceMotion,
                            onCheckedChange = { ThemeManager.setMotion(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) { ThemeManager.setHaptics(context, !ThemeManager.hapticFeedback) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Haptic feedback", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily, modifier = Modifier.weight(1f))
                        Switch(
                            checked = ThemeManager.hapticFeedback,
                            onCheckedChange = { ThemeManager.setHaptics(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun VoiceLangSubscreen(
    wakeWordEnabled: Boolean,
    onWakeWordEnabledChanged: (Boolean) -> Unit,
    selectedVoiceAccent: String,
    onVoiceAccentSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Using speech_prefs as SpeechManager also uses it or we can pass it down.
    // SpeechManager uses "speech_prefs" for voice_accent and speed.
    val prefs = remember { context.getSharedPreferences("speech_prefs", Context.MODE_PRIVATE) }
    
    var autoLanguage by remember { mutableStateOf(prefs.getBoolean("auto_language", true)) }
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("speed", 1.0f) * 100f) }
    var interruptSensitivity by remember { mutableFloatStateOf(prefs.getFloat("interrupt_sensitivity", 6f)) }

    // Theme Tokens
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val accentGrad = Brush.linearGradient(listOf(ElectricViolet, GradientEnd))

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.96f) { onBack() }
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.offset(y = (-2).dp))
                }
                Text(
                    text = "Voice & Language",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(38.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // WAKE WORD SECTION
                if (com.example.ui.viewmodel.ENABLE_WAKE_WORD) {
                    Text(
                        text = "WAKE WORD",
                        color = labelViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontFamily = InstrumentSansFontFamily,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumGlassBg(cornerRadius = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleOnPress = 0.98f) { onWakeWordEnabledChanged(!wakeWordEnabled) }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"Hey Lens\" hands-free", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Bolke voice chat kholo", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = onWakeWordEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // SPEECH SECTION
                Text(
                    text = "SPEECH",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    // Auto Language Match
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) {
                                autoLanguage = !autoLanguage
                                prefs.edit().putBoolean("auto_language", autoLanguage).apply()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto language match", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("User ki language mein reply", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Switch(
                            checked = autoLanguage,
                            onCheckedChange = {
                                autoLanguage = it
                                prefs.edit().putBoolean("auto_language", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                    // Voice Accent
                    var accentExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) { accentExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice accent", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                        
                        val accentLabels = mapOf(
                            "hi_IN" to "Indian (hi-IN)",
                            "en_IN" to "Indian English (en-IN)",
                            "gu_IN" to "Gujarati (gu-IN)",
                            "en_US" to "US English",
                            "en_GB" to "UK English"
                        )
                        
                        Box {
                            Text(
                                text = accentLabels[selectedVoiceAccent] ?: "US English",
                                color = labelViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InstrumentSansFontFamily
                            )
                            DropdownMenu(
                                expanded = accentExpanded,
                                onDismissRequest = { accentExpanded = false },
                                modifier = Modifier.background(bgTop).border(1.dp, glassBorder, RoundedCornerShape(8.dp))
                            ) {
                                accentLabels.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = textPrimary, fontFamily = InstrumentSansFontFamily) },
                                        onClick = {
                                            onVoiceAccentSelected(code)
                                            prefs.edit().putString("voice_accent", code).apply()
                                            accentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                    // Speaking Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speaking speed", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily, modifier = Modifier.weight(1f))
                        Slider(
                            value = voiceSpeed,
                            onValueChange = {
                                voiceSpeed = it
                                prefs.edit().putFloat("speed", it / 100f).apply()
                            },
                            valueRange = 50f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeManager.accentColor,
                                activeTrackColor = ThemeManager.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                    // Interrupt Sensitivity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interrupt sensitivity", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Beech mein roko kitni aasaani se", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Slider(
                            value = interruptSensitivity,
                            onValueChange = {
                                interruptSensitivity = it
                                prefs.edit().putFloat("interrupt_sensitivity", it).apply()
                            },
                            valueRange = 1f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeManager.accentColor,
                                activeTrackColor = ThemeManager.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiIntelligenceSubscreen(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    isMemoryEnabled: Boolean,
    onMemoryEnabledChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE) }
    
    var autoModel by remember { mutableStateOf(prefs.getBoolean("auto_layer_selection", true)) }
    var responseDepth by remember { mutableStateOf(prefs.getString("response_depth", "Balanced") ?: "Balanced") }
    var personalityTone by remember { mutableStateOf(prefs.getString("ai_personality", "Clinical") ?: "Clinical") }
    var deepThought by remember { mutableStateOf(prefs.getBoolean("is_deep_thought_enabled", false)) }
    var creativitySlider by remember { mutableFloatStateOf(prefs.getFloat("creativity_level", 40f)) }

    // Theme Tokens
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 1.dp, showSpecularHighlight = false)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.96f) { onBack() }
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.offset(y = (-2).dp))
                }
                Text(
                    text = "AI & Intelligence",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(38.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // MODEL SECTION
                Text(
                    text = "MODEL",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .bounceClick(scaleOnPress = 0.98f) {
                            autoModel = !autoModel
                            prefs.edit().putBoolean("auto_layer_selection", autoModel).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Automatic model selection", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("DepthLens switches the model for best results (Flash ⇄ Pro ⇄ Lite)", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily, lineHeight = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        if (autoModel) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(ThemeManager.accentColor)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("AUTO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("MANUAL", color = textMuted, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                // RESPONSE DEPTH SECTION
                Text(
                    text = "RESPONSE DEPTH",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val depths = listOf("Concise", "Balanced", "Deep")
                        depths.forEach { depth ->
                            val isSelected = responseDepth == depth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ThemeManager.accentColor else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (!isSelected) glassBorder else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .bounceClick(scaleOnPress = 0.96f) {
                                        responseDepth = depth
                                        prefs.edit().putString("response_depth", depth).apply()
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = depth,
                                    color = if (isSelected) Color.White else textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }
                    }
                }

                // AI PERSONALITY SECTION
                Text(
                    text = "AI PERSONALITY",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(16.dp)
                ) {
                    val tones = listOf("Clinical", "Warm", "Strategic", "Mentor", "Spiritual")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tones.forEach { tone ->
                            val isSelected = personalityTone == tone
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isSelected) ThemeManager.accentColor else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (!isSelected) glassBorder else Color.Transparent,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .bounceClick(scaleOnPress = 0.96f) {
                                        personalityTone = tone
                                        prefs.edit().putString("ai_personality", tone).apply()
                                    }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tone,
                                    color = if (isSelected) Color.White else textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }
                    }
                }

                // BEHAVIOUR SECTION
                Text(
                    text = "BEHAVIOUR",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    // Deep Thought mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) {
                                deepThought = !deepThought
                                prefs.edit().putBoolean("is_deep_thought_enabled", deepThought).apply()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deep Thought mode", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Slower, deeper reasoning", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Switch(
                            checked = deepThought,
                            onCheckedChange = {
                                deepThought = it
                                prefs.edit().putBoolean("is_deep_thought_enabled", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                    // Persistent memory
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(scaleOnPress = 0.98f) { onMemoryEnabledChanged(!isMemoryEnabled) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Persistent memory", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Remembers patterns", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Switch(
                            checked = isMemoryEnabled,
                            onCheckedChange = onMemoryEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                    // Creativity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Creativity", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily, modifier = Modifier.weight(1f))
                        Slider(
                            value = creativitySlider,
                            onValueChange = {
                                creativitySlider = it
                                prefs.edit().putFloat("creativity_level", it).apply()
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeManager.accentColor,
                                activeTrackColor = ThemeManager.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun NotificationsSubscreen(
    notificationsEnabled: Boolean,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
    
    var liveSession by remember { mutableStateOf(prefs.getBoolean("live_session_enabled", true)) }
    var screenActive by remember { mutableStateOf(prefs.getBoolean("screen_share_notification", true)) }
    var insightReminder by remember { mutableStateOf(prefs.getBoolean("daily_insight_reminder", false)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("notification_sound", true)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("notification_vibration", true)) }

    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Notifications",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ALERTS SECTION
                Text(
                    text = "ALERTS",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Response ready", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("only when app minimized", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = onNotificationsEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live session (Dynamic Island)", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = liveSession,
                                onCheckedChange = { 
                                    liveSession = it 
                                    prefs.edit().putBoolean("live_session_enabled", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Screen-share active", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = screenActive,
                                onCheckedChange = { 
                                    screenActive = it 
                                    prefs.edit().putBoolean("screen_share_notification", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily insight reminder", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = insightReminder,
                                onCheckedChange = { 
                                    insightReminder = it 
                                    prefs.edit().putBoolean("daily_insight_reminder", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                    }
                }

                // SOUND SECTION
                Text(
                    text = "SOUND",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notification sound", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { 
                                    soundEnabled = it 
                                    prefs.edit().putBoolean("notification_sound", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vibration", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { 
                                    vibrationEnabled = it 
                                    prefs.edit().putBoolean("notification_vibration", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PrivacySubscreen(
    isPrivacyModeEnabled: Boolean,
    onPrivacyModeEnabledChanged: (Boolean) -> Unit,
    onWipeAllUserData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
    val exportScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    val runExport: () -> Unit = {
        if (!isExporting) {
            isExporting = true
            exportScope.launch {
                val displayPath = withContext(Dispatchers.IO) { exportAllDepthLensData(context) }
                isExporting = false
                Toast.makeText(
                    context,
                    if (displayPath != null) "Export successful\nSaved to:\n$displayPath"
                    else "Export failed. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val storagePermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) runExport()
        else Toast.makeText(context, "Storage permission needed to export.", Toast.LENGTH_SHORT).show()
    }

    var autoCleanup by remember { mutableStateOf(prefs.getBoolean("auto_cleanup", true)) }
    var appLock by remember { mutableStateOf(prefs.getBoolean("biometric_lock_enabled", false)) }

    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Privacy & Data",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PRIVACY",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Incognito session", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("don't save", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = isPrivacyModeEnabled,
                                onCheckedChange = onPrivacyModeEnabledChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto privacy cleanup", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("auto-delete files", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = autoCleanup,
                                onCheckedChange = { 
                                    autoCleanup = it 
                                    prefs.edit().putBoolean("auto_cleanup", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric app lock", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("fingerprint / face", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Switch(
                                checked = appLock,
                                onCheckedChange = { 
                                    appLock = it 
                                    prefs.edit().putBoolean("biometric_lock_enabled", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "YOUR DATA",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isExporting) {
                                    // API 29+ writes to public Downloads via MediaStore (no permission).
                                    // Pre-Q needs WRITE_EXTERNAL_STORAGE — request it first.
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ||
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        runExport()
                                    } else {
                                        storagePermLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Export",
                                        tint = textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Export all data", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Export",
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWipeAllUserData() }
                                .padding(horizontal = 8.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear",
                                        tint = Color(0xFFFF6B8A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Clear local data", color = Color(0xFFFF6B8A), fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Clear",
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun UpdateSubscreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val latestReleaseState by com.example.ui.screens.GithubUpdateManager.latestRelease.collectAsState()
    val initialHasUpdate = latestReleaseState?.let {
        com.example.ui.screens.GithubUpdateManager.isNewerVersion(it.tagName, com.example.ui.screens.GithubUpdateManager.getInstalledVersion(context))
    } ?: false

    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf<com.example.ui.screens.GitHubRelease?>(if (initialHasUpdate) latestReleaseState else null) }
    var updateStatusMessage by remember { mutableStateOf(if (initialHasUpdate) "Show Update Available" else "You are up to date") }
    val prefs = context.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
    var autoWifi by remember { mutableStateOf(prefs.getBoolean("auto_update_wifi", true)) }

    // Get real version info
    val packageInfo = remember { 
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode ?: 0
    }

    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder
    val logoBlue = Color(0xFF38E1D8)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Update App",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official DepthLens logo with breathing animation
                DepthLensLogo(
                    size = 72.dp,
                    showGlow = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DepthLens v$versionName",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = InstrumentSansFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updateStatusMessage,
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = InstrumentSansFontFamily
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(ThemeManager.accentColor, Color(0xFF5B3FD6))))
                        .clickable {
                            if (updateAvailable != null) {
                                GithubUpdateManager.downloadAndUpdate(context, updateAvailable!!)
                            } else {
                                checking = true
                                GithubUpdateManager.checkForUpdates(context, force = true) { hasUpdate, release ->
                                    checking = false
                                    if (hasUpdate && release != null) {
                                        updateAvailable = release
                                        updateStatusMessage = "Show Update Available"
                                    } else {
                                        updateAvailable = null
                                        updateStatusMessage = "You are up to date"
                                        Toast.makeText(context, "No updates available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (checking) "Checking..." else if (updateAvailable != null) "Install Update" else "Check for updates",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WHAT'S NEW",
                        color = labelViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontFamily = InstrumentSansFontFamily,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumGlassBg(cornerRadius = 20.dp)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (updateAvailable != null) updateAvailable!!.body else {
                                if (com.example.ui.viewmodel.ENABLE_WAKE_WORD) {
                                    "• Liquid Crystal & Frost Aurora glass materials\n• Smarter auto language matching (Hinglish/Gujarati)\n• Fixed voice echo & \"Hey Lens\" stability\n• Redesigned Settings + new Dashboard"
                                } else {
                                    "• Liquid Crystal & Frost Aurora glass materials\n• Smarter auto language matching (Hinglish/Gujarati)\n• Fixed voice echo & audio stability\n• Redesigned Settings + new Dashboard"
                                }
                            },
                            color = textMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumGlassBg(cornerRadius = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto-update over Wi-Fi", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Switch(
                                checked = autoWifi,
                                onCheckedChange = { 
                                    autoWifi = it 
                                    prefs.edit().putBoolean("auto_update_wifi", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = glassBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ReportBugSubscreen(
    userName: String,
    userEmail: String,
    onBack: () -> Unit
) {
    val options = listOf("Voice / Video issue", "Screen-share issue", "Wrong language reply", "App crash / freeze", "UI / design glitch", "Other")
    var expanded by remember { mutableStateOf(false) }
    var problemType by remember { mutableStateOf(options[0]) }
    var description by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var hasScreenshot by remember { mutableStateOf(false) }

    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Report a Bug",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "What went wrong?",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 0.dp, showSpecularHighlight = false)
                        .clickable { expanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(problemType, color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(bgTop).border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily) },
                                onClick = {
                                    problemType = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "Describe it",
                    color = labelViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontFamily = InstrumentSansFontFamily,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 0.dp, showSpecularHighlight = false)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontFamily = InstrumentSansFontFamily
                        ),
                        decorationBox = { innerTextField ->
                            if (description.isEmpty()) {
                                Text("Steps to reproduce, what happened...", color = textMuted, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 0.dp, showSpecularHighlight = false)
                        .clickable {
                            hasScreenshot = true
                            Toast.makeText(context, "Screenshot attached", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(if (hasScreenshot) "📎 Screenshot attached" else "📎 Attach screenshot", color = textPrimary, fontSize = 10.5.sp, fontFamily = InstrumentSansFontFamily)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include device logs", color = textPrimary, fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("send diagnostics", color = textMuted, fontSize = 10.sp, fontFamily = InstrumentSansFontFamily)
                        }
                        Switch(
                            checked = includeLogs,
                            onCheckedChange = { includeLogs = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ThemeManager.accentColor,
                                uncheckedThumbColor = textMuted,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = glassBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(ThemeManager.accentColor, Color(0xFF5B3FD6))))
                        .clickable {
                            if (description.isBlank()) {
                                Toast.makeText(context, "Please describe the problem.", Toast.LENGTH_SHORT).show()
                            } else {
                                // Simulate sending report
                                Toast.makeText(context, "\uD83D\uDC1E Bug report sent — thank you!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Send report",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AboutSubscreen(
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val packageInfo = remember { 
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode ?: 0
    }

    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val logoBlue = Color(0xFF38E1D8)
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "About DepthLens",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real DepthLens eye-mark logo using the single shared component.
                DepthLensLogo(
                    modifier = Modifier.padding(top = 8.dp),
                    size = 88.dp,
                    showGlow = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "DepthLens",
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = InstrumentSansFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "REALITY INTELLIGENCE PLATFORM",
                    color = logoBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = InstrumentSansFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version $versionName (Build $versionCode)",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontFamily = InstrumentSansFontFamily
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Link Rows List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Column {
                        AboutLinkRow(label = "Privacy Policy") {
                            onNavigateToPrivacyPolicy()
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        AboutLinkRow(label = "Terms of Service") {
                            onNavigateToTerms()
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        AboutLinkRow(label = "Website") {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://guy-with-ideas-uncoded.github.io/DEPTHLENS/")))
                        }
                        HorizontalDivider(color = glassBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        AboutLinkRow(label = "Share with friends") {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "DepthLens — see beneath the surface. An AI that reveals hidden patterns, root causes and probable futures. Try it: shorturl.at/TDOZi")
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share DepthLens via"))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawCircle(
                            color = textMuted,
                            radius = size.width * 0.45f,
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                        )
                        drawOval(
                            color = textMuted,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.35f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.4f)
                        )
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.3f, size.height * 0.4f)
                                lineTo(size.width * 0.22f, size.height * 0.22f)
                                lineTo(size.width * 0.42f, size.height * 0.36f)
                                close()
                            },
                            color = textMuted
                        )
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.7f, size.height * 0.4f)
                                lineTo(size.width * 0.78f, size.height * 0.22f)
                                lineTo(size.width * 0.58f, size.height * 0.36f)
                                close()
                            },
                            color = textMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = " © 2026 ",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontFamily = InstrumentSansFontFamily
                    )
                    Text(
                        text = "DepthLens",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/guy-with-ideas-uncoded/DEPTHLENS")))
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .premiumGlassBg(cornerRadius = 0.dp, borderWidth = 0.dp, showSpecularHighlight = false)
                        .clickable {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.instagram.com/depth_lens_")))
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val strokeWidth = 1.2.dp.toPx()
                            drawRoundRect(
                                color = textPrimary,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.15f),
                                size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.7f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                            drawCircle(
                                color = textPrimary,
                                radius = size.width * 0.18f,
                                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                            drawCircle(
                                color = textPrimary,
                                radius = size.width * 0.04f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.32f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Follow on Instagram",
                            color = textPrimary,
                            fontSize = 11.sp,
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PrivacyPolicySubscreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Privacy Policy",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "DepthLens Privacy Policy (last updated 2026). DepthLens (\"we\", \"the app\") helps you analyze thoughts, patterns and decisions. This policy explains what we collect and how we use it.",
                            color = textPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = InstrumentSansFontFamily
                        )

                        PolicySection(
                            title = "Account data",
                            body = "When you sign in with email or Google, we store your name, email and profile details via Firebase Authentication to identify your account."
                        )

                        PolicySection(
                            title = "Conversations & insights",
                            body = "Your chats, sessions and generated insights are stored securely (locally on your device and, if sync is enabled, in your private Firebase/Firestore space) so your history and memory insights work across sessions. They are tied to your account and not shared with other users."
                        )

                        PolicySection(
                            title = "AI processing",
                            body = "To answer you, your messages (and any attachments, screen or voice you share during a session) are sent to the AI model provider (Google Gemini) solely to generate a response. We do not sell your data."
                        )

                        PolicySection(
                            title = "Voice, camera, screen",
                            body = "Microphone, camera and screen-share are used only during an active session you start, to provide the feature. Nothing is captured in the background."
                        )

                        PolicySection(
                            title = "Local data & privacy controls",
                            body = "You can use Incognito sessions (not saved), enable auto privacy cleanup, export your data, or clear local data anytime from Privacy & Data settings."
                        )

                        PolicySection(
                            title = "Security",
                            body = "Data in transit is encrypted (HTTPS). We take reasonable measures to protect your information."
                        )

                        PolicySection(
                            title = "Your rights",
                            body = "You can edit your profile, delete your data, or sign out at any time. Deleting the app or clearing data removes local content."
                        )

                        PolicySection(
                            title = "Contact",
                            body = "Reach us via the GitHub page linked in the app."
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TermsOfServiceSubscreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val bgTop = RichNavy
    val bgBottom = DeepMidnight
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    val glassFill = DynamicGlassFill
    val glassBorder = GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .bounceClick(scaleOnPress = 0.88f) { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Terms of Service",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassBg(cornerRadius = 20.dp)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "DepthLens Terms of Service (2026). By using DepthLens you agree to these terms.",
                            color = textPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = InstrumentSansFontFamily
                        )

                        PolicySection(
                            title = "Purpose",
                            body = "DepthLens provides AI-assisted analysis, insights and reflection tools. It is for personal informational use and is not a substitute for professional medical, legal, financial or psychological advice."
                        )

                        PolicySection(
                            title = "Accounts",
                            body = "You are responsible for your account and for keeping your login secure. Provide accurate information."
                        )

                        PolicySection(
                            title = "Acceptable use",
                            body = "Do not use the app for unlawful, harmful or abusive purposes, or to violate others' rights."
                        )

                        PolicySection(
                            title = "AI output",
                            body = "Responses are generated by an AI model and may be inaccurate or incomplete. Use your own judgement; you are responsible for decisions you make."
                        )

                        PolicySection(
                            title = "Content",
                            body = "You retain ownership of what you enter. You grant the app permission to process your content only to provide the service."
                        )

                        PolicySection(
                            title = "Availability",
                            body = "The service is provided \"as is\" without warranties. We may update, change or discontinue features. We are not liable for indirect or consequential damages to the extent permitted by law."
                        )

                        PolicySection(
                            title = "Changes",
                            body = "We may update these terms; continued use means acceptance."
                        )

                        PolicySection(
                            title = "Contact",
                            body = "Via the GitHub page linked in the app."
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PolicySection(title: String, body: String) {
    val textPrimary = TextPrimaryColor
    val textMuted = TextMutedColor
    val labelViolet = SectionLabelColor
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = labelViolet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = InstrumentSansFontFamily
        )
        Text(
            text = body,
            color = textMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = InstrumentSansFontFamily
        )
    }
}

@Composable
fun AboutLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFEFEDFF), fontSize = 13.sp, fontFamily = InstrumentSansFontFamily)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = Color(0xFF9D98C9),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Export ALL local DepthLens data → public Downloads/DepthLens/ as JSON.
// Writes the file BEFORE returning; returns the human-readable path or null.
// ─────────────────────────────────────────────────────────────────────────────
suspend fun exportAllDepthLensData(context: android.content.Context): String? {
    return try {
        val db = com.example.data.database.DepthDatabase.getDatabase(context)
        val sessions = db.sessionDao().getAllSessions()
        val attByMsg = db.attachmentDao().getAllAttachments().groupBy { it.messageId }

        val tsReadable = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)

        val root = org.json.JSONObject()
        root.put("app", "DepthLens")
        root.put("exportVersion", 1)
        root.put("exportedAt", tsReadable.format(java.util.Date()))

        val conversations = org.json.JSONArray()
        for (s in sessions) {
            val sObj = org.json.JSONObject()
            sObj.put("id", s.id)
            sObj.put("title", s.title)
            sObj.put("createdAt", s.createdAt)
            sObj.put("createdAtReadable", tsReadable.format(java.util.Date(s.createdAt)))
            sObj.put("lastUpdatedAt", s.lastUpdatedAt)
            sObj.put("isPinned", s.isPinned)

            val messages = db.messageDao().getMessagesForSession(s.id)
            val msgArr = org.json.JSONArray()
            for (m in messages) {
                val mObj = org.json.JSONObject()
                mObj.put("id", m.id)
                mObj.put("role", m.role)
                mObj.put("text", m.text)
                mObj.put("timestamp", m.timestamp)
                mObj.put("timestampReadable", tsReadable.format(java.util.Date(m.timestamp)))
                mObj.put("imageUri", m.imageUri ?: org.json.JSONObject.NULL)
                mObj.put("replyToMessageId", m.replyToMessageId ?: org.json.JSONObject.NULL)
                mObj.put("selectedText", m.selectedText ?: org.json.JSONObject.NULL)

                val attArr = org.json.JSONArray()
                for (a in attByMsg[m.id].orEmpty()) {
                    val aObj = org.json.JSONObject()
                    aObj.put("attachmentId", a.attachmentId)
                    aObj.put("mimeType", a.mimeType)
                    aObj.put("fileName", a.fileName)
                    aObj.put("localUri", a.localUri)
                    aObj.put("remoteUrl", a.remoteUrl ?: org.json.JSONObject.NULL)
                    aObj.put("thumbnailUrl", a.thumbnailUrl ?: org.json.JSONObject.NULL)
                    attArr.put(aObj)
                }
                mObj.put("attachments", attArr)
                msgArr.put(mObj)
            }
            sObj.put("messageCount", messages.size)
            sObj.put("messages", msgArr)
            conversations.put(sObj)
        }
        root.put("conversationCount", sessions.size)
        root.put("conversations", conversations)

        // Memory insights (locally stored user data)
        val insightsArr = org.json.JSONArray()
        runCatching {
            for (ins in db.memoryInsightDao().getAllInsightsFlow().first()) {
                insightsArr.put(org.json.JSONObject().apply {
                    put("id", ins.id)
                    put("category", ins.category)
                    put("content", ins.content)
                    put("timestamp", ins.timestamp)
                })
            }
        }
        root.put("memoryInsights", insightsArr)

        val archivedArr = org.json.JSONArray()
        runCatching {
            for (ar in db.archivedInsightDao().getAllArchivedInsightsFlow().first()) {
                archivedArr.put(org.json.JSONObject().apply {
                    put("id", ar.id)
                    put("sessionId", ar.sessionId)
                    put("query", ar.query)
                    put("introTitle", ar.introTitle)
                    put("jsonContent", ar.jsonContent)
                    put("timestamp", ar.timestamp)
                })
            }
        }
        root.put("archivedInsights", archivedArr)

        // Settings / preferences
        val settingsObj = org.json.JSONObject()
        runCatching {
            val prefs = context.getSharedPreferences("depthlens_prefs", android.content.Context.MODE_PRIVATE)
            for ((k, v) in prefs.all) {
                settingsObj.put(k, when (v) {
                    null -> org.json.JSONObject.NULL
                    is Set<*> -> org.json.JSONArray(v.toList())
                    else -> v
                })
            }
        }
        root.put("settings", settingsObj)

        // Write the JSON to public Downloads/DepthLens/ BEFORE reporting success.
        val fileName = "DepthLens_Export_" +
            java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US).format(java.util.Date()) +
            ".json"
        val bytes = root.toString(2).toByteArray(Charsets.UTF_8)
        val subDir = "DepthLens"

        val wrote = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(
                    android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS + "/" + subDir
                )
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            )
            if (uri == null) false
            else context.contentResolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false
        } else {
            val dir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                subDir
            )
            if (!dir.exists()) dir.mkdirs()
            java.io.File(dir, fileName).writeBytes(bytes)
            true
        }

        if (wrote) "Downloads/$subDir/$fileName" else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
