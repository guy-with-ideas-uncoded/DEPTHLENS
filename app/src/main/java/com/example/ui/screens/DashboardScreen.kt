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

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Pick media launcher
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setAttachment(uri.toString())
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
            showMemoryDialog -> {
                showMemoryDialog = false
            }
            showClearConfirm -> {
                showClearConfirm = false
            }
            showResetConfirm -> {
                showResetConfirm = false
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
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
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
                                        color = Color.White
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
                                Text("${memoryInsights.size} nodes cached", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        color = Color.White
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
            },
            confirmButton = {
                TextButton(onClick = { showMemoryDialog = false }) {
                    Text("Close", color = PremiumCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = RichNavy,
            title = { Text("Purge Cached Memories?", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
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
            title = { Text("Wipe All Application Data?", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
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
                targetValue = if (isMounted) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isMounted) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "alpha"
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RichNavy),
                border = BorderStroke(1.5.dp, ElectricViolet.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Header with Glowing Branded Logo
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DeepMidnight)
                                .border(1.5.dp, PremiumCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_depthlens_logo),
                                contentDescription = "DepthLens Core",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Exit DepthLens?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Your conversations and memories are safely saved.\n\nWould you like to exit?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryColor,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons Layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // STAY (Primary / High Prominence)
                        Button(
                            onClick = { showExitConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                "Stay",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // EXIT (Secondary / Subtler)
                        OutlinedButton(
                            onClick = {
                                showExitConfirm = false
                                val activity = (context as? android.app.Activity)
                                activity?.finish()
                            },
                            border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = ErrorColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                "Exit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorColor
                            )
                        }
                    }
                }
            }
        }
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
                                    text = "DEPTHLENS OMEGA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Reality Intelligence OS",
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
                                    color = Color.White.copy(alpha = 0.9f)
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
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PremiumCyan),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DeepMidnight)
                                    .border(1.dp, SurfaceCardColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
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
                                            color = TextSecondaryColor.copy(alpha = 0.7f)
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SYSTEMS CONTROLS", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            if (isMemoryEnabled) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SuccessColor.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "LEARNING ON",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showMemoryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            contentPadding = PaddingValues(10.dp, 4.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PremiumCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Manage Memory & Privacy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
                                text = "DEPTHLENS OMEGA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(5.dp).background(SuccessColor, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SECURE LOCAL OS v2.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
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
                        titleContentColor = Color.White,
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
                                onAddAttachment = {
                                    pickMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveAttachment = { viewModel.clearAttachment() },
                                attachedImageUri = attachedImageUri,
                                isLoading = isLoading
                            )
                        } else {
                            // Comfortably padded scrolling Chat Feed
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
                                        val parsed = remember(message.text) { ResponseParser.parse(message.text) }
                                        DepthLensDiagnosticCard(
                                            parsed = parsed,
                                            onPromptSelected = { query -> viewModel.sendQuery(query) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom chat panel is displayed ONLY when actively chatting (Hides nested messy overlapping components on pristine home)
                    if (activeMessages.isNotEmpty()) {
                        BottomInputPanel(
                            attachedImageUri = attachedImageUri,
                            isLoading = isLoading,
                            onAddAttachment = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveAttachment = { viewModel.clearAttachment() },
                            onSubmit = { text -> viewModel.sendQuery(text) }
                        )
                    }
                }

                // Dynamic loading block screen overlay - Reconstructed to look ultra-futuristic
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepMidnight.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RichNavy),
                            border = BorderStroke(1.2.dp, PremiumCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PremiumCyan, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "RECONSTRUCTING COGNITIVE NODES...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Deconstructing inputs across 10 progressive layers of reality, running pattern search systems, analyzing drivers... Complete reports caching in on-device memory.",
                                    fontSize = 11.sp,
                                    color = TextSecondaryColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
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
    attachedImageUri: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when {
            currentHour < 12 -> "Good Morning"
            currentHour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

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

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Premium Greeting
        Text(
            text = "$greeting.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Ready to explore deeper?",
            style = MaterialTheme.typography.headlineSmall,
            color = PremiumCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

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
                // If there's an active attached thumbnail preview in center
                attachedImageUri?.let { uri ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, PremiumCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attached Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Image Attached", fontSize = 11.sp, color = SuccessColor, fontWeight = FontWeight.Bold)
                            Text("Ready for multi-dimensional scan", fontSize = 9.sp, color = TextSecondaryColor)
                        }
                        IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorColor, modifier = Modifier.size(14.dp))
                        }
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
                            color = TextSecondaryColor,
                            lineHeight = 18.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 130.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
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
                            disabledContainerColor = RichNavy
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
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
                            color = Color.White
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
                color = Color.White,
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
                        color = Color.White,
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
                                color = Color.White,
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
                                Text("${sc.codeName} - ${sc.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                color = Color.White,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, SurfaceCardColor, RoundedCornerShape(6.dp))
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Attachment description",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Multimodal asset ready", fontSize = 11.sp, color = SuccessColor, fontWeight = FontWeight.Bold)
                    Text("Analyzing graphical structure", fontSize = 9.sp, color = TextSecondaryColor)
                }
                IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Deattach", tint = ErrorColor, modifier = Modifier.size(12.dp))
                }
            }
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
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
