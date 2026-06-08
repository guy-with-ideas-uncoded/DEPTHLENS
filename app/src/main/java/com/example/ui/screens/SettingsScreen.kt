package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class SettingsThemeItem(val name: String, val bg: Color, val surf: Color, val c1: Color, val c2: Color)

@Composable
fun SettingsScreen(
    isMemoryEnabled: Boolean,
    onMemoryEnabledChanged: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    isCollectiveOptIn: Boolean,
    onCollectiveOptInChanged: (Boolean) -> Unit,
    isPrivacyModeEnabled: Boolean = false,
    onPrivacyModeEnabledChanged: (Boolean) -> Unit = {},
    activeThemeName: String,
    onThemeSelected: (String) -> Unit,
    onShowMemoryDetails: () -> Unit,
    onShowUpdateDetails: () -> Unit,
    onWipeAllUserData: () -> Unit,
    onShowAbout: () -> Unit,
    onReportBug: () -> Unit,
    isLoggedIn: Boolean = false,
    isGuest: Boolean = false,
    userName: String = "",
    userEmail: String = "",
    githubToken: String = "",
    repoOwnerAndName: String = "",
    onSaveGithubSettings: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onLoginWithGoogle: (String, String) -> Unit = { _, _ -> },
    onLoginAsGuest: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExportToast by remember { mutableStateOf(false) }
    var loginEmail by remember { mutableStateOf("") }
    var loginName by remember { mutableStateOf("") }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Page Header
        Text(
            text = "Settings",
            fontFamily = DMSerifDisplayFontFamily,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 26.sp,
            color = TextPrimaryColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Text(
            text = "Configure your reality exploration preferences and session data",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontFamily = InstrumentSansFontFamily,
            color = TextMutedColor,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // --- SECTION: ACCOUNT ---
        Text(
            text = "ACCOUNT & IDENTITY",
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
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoggedIn) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(SuccessColor, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userName,
                                        fontSize = 13.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryColor
                                    )
                                }
                                Text(
                                    text = userEmail,
                                    fontSize = 10.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = TextMutedColor
                                )
                            }
                            
                            Button(
                                onClick = onSignOut,
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Sign Out", fontSize = 10.sp, color = ErrorColor, fontWeight = FontWeight.Bold, fontFamily = InstrumentSansFontFamily)
                            }
                        }

                        Divider(color = BorderSubtle, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cloud Synchronization",
                                fontSize = 10.5.sp,
                                fontFamily = InstrumentSansFontFamily,
                                color = TextSecondaryColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(SuccessColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isGuest) "Offline Guest Mode active" else "Configure Identity to back up conversations and analyses securely.",
                        fontSize = 11.sp,
                        color = TextSecondaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        lineHeight = 15.sp
                    )
                    
                    if (!isGuest) {
                        OutlinedTextField(
                            value = loginName,
                            onValueChange = { loginName = it },
                            label = { Text("Your Explorer Name", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor,
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Button(
                            onClick = {
                                onLoginAsGuest(loginName.ifBlank { "Explorer" })
                            },
                            enabled = loginName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Exploring", fontSize = 11.sp, color = Color.White)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Guest: $userName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryColor,
                                    fontFamily = InstrumentSansFontFamily
                                )
                                Text(
                                    text = "Unsynchronized",
                                    fontSize = 9.sp,
                                    color = TextMutedColor,
                                    fontFamily = DMMonoFontFamily
                                )
                            }
                            Button(
                                onClick = onSignOut,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Upgrade to Cloud Sync", fontSize = 10.sp, color = Color.White, fontFamily = InstrumentSansFontFamily)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: APPEARANCE ---
        Text(
            text = "APPEARANCE",
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
            val themes = listOf(
                SettingsThemeItem("Deep Sea", Color(0xFF0A1628), Color(0xFF0D1F3C), Color(0xFF3B82F6), PremiumCyan),
                SettingsThemeItem("Polar Dawn", Color(0xFFF5F0FF), Color(0xFFEDE8FB), ElectricViolet, Color(0xFFA855F7))
            )

            themes.forEach { theme ->
                val isActive = activeThemeName == theme.name

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Surface2, shape = RoundedCornerShape(8.dp))
                        .border(
                            width = 1.5.dp,
                            color = if (isActive) ElectricViolet else BorderSubtle,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThemeSelected(theme.name) }
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(theme.bg, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(theme.surf)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.c1))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(theme.c2))
                        }

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .background(ElectricViolet, shape = RoundedCornerShape(50))
                                    .border(1.dp, Color.White, shape = RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = theme.name,
                        fontSize = 8.sp,
                        fontFamily = DMMonoFontFamily,
                        color = if (isActive) TextPrimaryColor else TextMutedColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- SECTION: TYPOGRAPHY ---
        Text(
            text = "TYPOGRAPHY",
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
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .clickable { showFontSizeDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Font Size",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InstrumentSansFontFamily,
                        color = TextPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Adjust text size throughout the app.",
                        fontSize = 11.sp,
                        fontFamily = InstrumentSansFontFamily,
                        color = TextMutedColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = TypographyManager.currentFontSizeKey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMMonoFontFamily,
                        color = PremiumCyan
                    )
                    Text(
                        text = "→",
                        fontSize = 14.sp,
                        color = TextMutedColor
                    )
                }
            }
        }

        if (showFontSizeDialog) {
            val context = LocalContext.current
            val fontSizes = listOf("Extra Small", "Small", "Medium", "Large", "Extra Large")

            AlertDialog(
                onDismissRequest = { showFontSizeDialog = false },
                title = {
                    Text(
                        text = "Choose Font Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryColor,
                        fontFamily = DMSerifDisplayFontFamily,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Preview box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface2, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PREVIEW",
                                fontSize = 9.sp,
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "The deeper you look, the more you see.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        fontSizes.forEach { sizeKey ->
                            val isSelected = TypographyManager.currentFontSizeKey == sizeKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Surface3 else Color.Transparent)
                                    .clickable {
                                        TypographyManager.setFontSize(context, sizeKey)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sizeKey,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PremiumCyan else TextPrimaryColor
                                )

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        TypographyManager.setFontSize(context, sizeKey)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = PremiumCyan,
                                        unselectedColor = TextMutedColor
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showFontSizeDialog = false }
                    ) {
                        Text(
                            text = "Done",
                            color = PremiumCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InstrumentSansFontFamily
                        )
                    }
                },
                containerColor = DeepMidnight,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- SECTION: PREFERENCES ---
        Text(
            text = "PREFERENCES",
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsRow(
                icon = "🔔",
                title = "Analysis Notifications",
                subtitle = "Notify when deep reality diagnostics complete",
                iconBg = Color(0x1A7E65FF),
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsEnabledChanged
            )

            SettingsRow(
                icon = "🔒",
                title = "Offline Memory Lock",
                subtitle = "Local sandbox isolation mode",
                iconBg = PremiumCyan.copy(alpha = 0.15f),
                checked = isMemoryEnabled,
                onCheckedChange = onMemoryEnabledChanged
            )

            SettingsRow(
                icon = "⚡",
                title = "Auto Layer Selection",
                subtitle = "Intelligent alignment of Reality Lenses",
                iconBg = WarningColor.copy(alpha = 0.15f),
                checked = isCollectiveOptIn,
                onCheckedChange = onCollectiveOptInChanged
            )

            SettingsRow(
                icon = "🛡️",
                title = "Advanced Privacy Mode",
                subtitle = "Auto-deletes files/recordings and retains final answers",
                iconBg = Color(0x20FF3B30),
                checked = isPrivacyModeEnabled,
                onCheckedChange = onPrivacyModeEnabledChanged
            )
        }

        // --- SECTION: DATA ---
        Text(
            text = "DATA",
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InteractiveSettingsCard(
                icon = "📂",
                title = "Export Chats",
                subtitle = "Download conversational logs and timeline models to JSON",
                onClick = { showExportToast = true }
            )

            InteractiveSettingsCard(
                icon = "🧠",
                title = "Memory Manager",
                subtitle = "Manage semantic anchors, localized vectors and system memories",
                onClick = onShowMemoryDetails
            )
        }

        if (showExportToast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color(0x337E65FF), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, ElectricViolet, shape = RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data successfully exports to local storage!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = InstrumentSansFontFamily
                    )
                    Text(
                        text = "✕",
                        color = ElectricViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showExportToast = false }
                    )
                }
            }
        }

        // --- SECTION: HELP & SUPPORT ---
        Text(
            text = "HELP & SUPPORT",
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InteractiveSettingsCard(
                icon = "✉️",
                title = "Send Feedback",
                subtitle = "Help us refine Reality detection mechanics and features",
                onClick = onReportBug
            )

            InteractiveSettingsCard(
                icon = "🐛",
                title = "Report a Bug",
                subtitle = "File diagnostic details regarding systemic flaws",
                onClick = onReportBug
            )
        }

        // --- SECTION: APP UPDATES ---
        val context = LocalContext.current
        val latestRelease by GithubUpdateManager.latestRelease.collectAsState()
        val isCheckingForUpdates by GithubUpdateManager.isChecking.collectAsState()

        var updateCheckDone by remember { mutableStateOf(false) }

        Text(
            text = "APP UPDATES",
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
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(ElectricViolet.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Check for Updates",
                            fontSize = 16.sp,
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                        Text(
                            text = "Keep DepthLens up to date with the latest improvements and fixes.",
                            fontSize = 14.sp,
                            fontFamily = InstrumentSansFontFamily,
                            color = TextMutedColor,
                            lineHeight = 18.sp
                        )
                    }
                }

                Divider(color = BorderSubtle, thickness = 0.8.dp)

                // Current version row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Version",
                        fontSize = 10.5.sp,
                        fontFamily = InstrumentSansFontFamily,
                        color = TextSecondaryColor
                    )
                    val installedVersion = remember {
                        GithubUpdateManager.getInstalledVersion(context)
                    }
                    Text(
                        text = "v$installedVersion",
                        fontSize = 10.5.sp,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColor
                    )
                }

                // Update status area
                val installedVersionCurrent = remember {
                    GithubUpdateManager.getInstalledVersion(context)
                }
                val release = latestRelease
                val hasUpdate = release != null && GithubUpdateManager.isNewerVersion(release.tagName, installedVersionCurrent)

                if (updateCheckDone || release != null) {
                    if (hasUpdate && release != null) {
                        // Update available
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ElectricViolet.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, ElectricViolet.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(ElectricViolet, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Update Available",
                                        fontSize = 11.sp,
                                        fontFamily = InstrumentSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Version: ${release.tagName.removePrefix("v")}",
                                    fontSize = 10.sp,
                                    fontFamily = DMMonoFontFamily,
                                    color = TextSecondaryColor
                                )
                            }
                            Button(
                                onClick = {
                                    GithubUpdateManager.downloadAndUpdate(context, release)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Update Now",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Up to date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessColor.copy(alpha = 0.06f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, SuccessColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SuccessColor, CircleShape)
                            )
                            Text(
                                text = "You are running the latest version.",
                                fontSize = 11.sp,
                                fontFamily = InstrumentSansFontFamily,
                                color = SuccessColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Check for Updates button
                Button(
                    onClick = {
                        GithubUpdateManager.checkForUpdates(context, force = true) { _, _ ->
                            updateCheckDone = true
                        }
                    },
                    enabled = !isCheckingForUpdates,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckingForUpdates) Surface3 else Surface3,
                        disabledContainerColor = Surface3
                    ),
                    border = BorderStroke(1.dp, if (isCheckingForUpdates) BorderSubtle else ElectricViolet.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isCheckingForUpdates) {
                        CircularProgressIndicator(
                            color = ElectricViolet,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking...",
                            fontSize = 11.sp,
                            color = TextSecondaryColor,
                            fontFamily = InstrumentSansFontFamily
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Check for Updates",
                            fontSize = 11.sp,
                            color = ElectricViolet,
                            fontFamily = InstrumentSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- SECTION: ABOUT ---
        InteractiveSettingsCard(
            icon = "✕",
            title = "About DepthLens",
            subtitle = "View software license details and dynamic credits · v4.1.5",
            onClick = onShowAbout
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- SECTION: OPEN SOURCE ---
        Text(
            text = "OPEN SOURCE",
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
                .background(Surface2, shape = RoundedCornerShape(8.dp))
                .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
                .clickable {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/guy-with-ideas-uncoded/DEPTHLENS")
                    )
                    context.startActivity(intent)
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Surface3, shape = RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_github),
                        contentDescription = "GitHub repository link",
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimaryColor
                    )
                }

                Column {
                    Text(
                        text = "guy-with-ideas-uncoded",
                        fontSize = 16.sp,
                        color = TextPrimaryColor,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DEPTHLENS Open Source Repository",
                        fontSize = 14.sp,
                        color = TextMutedColor,
                        fontFamily = InstrumentSansFontFamily,
                        lineHeight = 18.sp
                    )
                }
            }

            Text(
                text = "↗",
                fontSize = 19.sp,
                color = TextMutedColor,
                fontFamily = InstrumentSansFontFamily
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InteractiveSettingsCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, shape = RoundedCornerShape(8.dp))
            .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Surface3, shape = RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 13.sp)
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = TextPrimaryColor,
                    fontFamily = InstrumentSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextMutedColor,
                    fontFamily = InstrumentSansFontFamily,
                    lineHeight = 18.sp
                )
            }
        }

        Text(
            text = "›",
            fontSize = 19.sp,
            color = TextMutedColor
        )
    }
}

@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String,
    iconBg: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, shape = RoundedCornerShape(8.dp))
            .border(1.dp, BorderSubtle, shape = RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconBg, shape = RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 13.sp)
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = TextPrimaryColor,
                    fontFamily = InstrumentSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextMutedColor,
                    fontFamily = InstrumentSansFontFamily,
                    lineHeight = 18.sp
                )
            }
        }

        val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onCheckedChange(newValue)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricViolet,
                checkedTrackColor = Color(0x2E7E65FF),
                uncheckedThumbColor = TextMutedColor,
                uncheckedTrackColor = Surface4
            )
        )
    }
}
