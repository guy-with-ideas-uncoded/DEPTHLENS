package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionEntity
import com.example.ui.theme.*
import com.example.ui.components.depthGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.viewmodel.SessionSearchResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun SessionsScreen(
    searchResults: List<SessionSearchResult>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    activeSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onCreateNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onTogglePinSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    val context = LocalContext.current
    var renamingSession by remember { mutableStateOf<SessionEntity?>(null) }
    var renamingTitleText by remember { mutableStateOf("") }

    val pinnedResults = remember(searchResults) { searchResults.filter { it.session.isPinned } }
    val unpinnedResults = remember(searchResults) { searchResults.filter { !it.session.isPinned } }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Topbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button ‹
                IconButton(
                    onClick = onNavigateToChat,
                    modifier = Modifier.size(38.dp)
                ) {
                    Text(
                        text = "‹",
                        color = TextPrimaryColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // Title "History"
                Text(
                    text = "History",
                    color = TextPrimaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )

                // Create new session ✎
                IconButton(
                    onClick = {
                        onCreateNewSession()
                        onNavigateToChat()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Text(
                        text = "✎",
                        color = TextPrimaryColor,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sessions List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item(key = "search_header") {
                    // Search Sessions Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .depthGlass(cornerRadius = 14.dp, borderWidth = 1.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⌕",
                            color = ElectricViolet,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search sessions…",
                                    color = TextMutedColor,
                                    fontSize = 13.sp,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    color = TextPrimaryColor,
                                    fontSize = 13.sp,
                                    fontFamily = InstrumentSansFontFamily
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (searchResults.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No historic sessions found.",
                                color = TextMutedColor,
                                fontSize = 12.sp,
                                fontFamily = InstrumentSansFontFamily
                            )
                        }
                    }
                } else {
                    // 1. PINNED SECTION
                    if (pinnedResults.isNotEmpty()) {
                        item(key = "pinned_section_label") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = "📌 Pinned",
                                    color = TextPrimaryColor.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }

                        items(pinnedResults, key = { "pinned_${it.session.id}" }) { result ->
                            SessionCard(
                                result = result,
                                activeSessionId = activeSessionId,
                                onSessionSelected = onSessionSelected,
                                onNavigateToChat = onNavigateToChat,
                                onTogglePinSession = onTogglePinSession,
                                onStartRename = { session ->
                                    renamingSession = session
                                    renamingTitleText = session.title
                                },
                                onDeleteSession = onDeleteSession,
                                modifier = Modifier.animateItemPlacement(animationSpec = tween(durationMillis = 250))
                            )
                        }

                        item(key = "pinned_section_divider") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    PremiumCyan.copy(alpha = 0.4f),
                                                    ElectricViolet.copy(alpha = 0.4f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All Conversations",
                                    color = TextPrimaryColor.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InstrumentSansFontFamily,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. UNPINNED SECTION
                    items(unpinnedResults, key = { "unpinned_${it.session.id}" }) { result ->
                        SessionCard(
                            result = result,
                            activeSessionId = activeSessionId,
                            onSessionSelected = onSessionSelected,
                            onNavigateToChat = onNavigateToChat,
                            onTogglePinSession = onTogglePinSession,
                            onStartRename = { session ->
                                    renamingSession = session
                                    renamingTitleText = session.title
                            },
                            onDeleteSession = onDeleteSession,
                            modifier = Modifier.animateItemPlacement(animationSpec = tween(durationMillis = 250))
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (renamingSession != null) {
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            containerColor = DeepMidnight,
            title = {
                Text(
                    text = "Rename Conversation",
                    color = TextPrimaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InstrumentSansFontFamily
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = renamingTitleText,
                        onValueChange = { newValue ->
                            if (newValue.length <= 80) {
                                renamingTitleText = newValue
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimaryColor,
                            fontSize = 13.sp,
                            fontFamily = InstrumentSansFontFamily
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = ElectricViolet
                        ),
                        placeholder = {
                            Text(
                                text = "Enter title...",
                                color = TextMutedColor,
                                fontSize = 13.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val clean = renamingTitleText.trim()
                                if (clean.isNotEmpty()) {
                                    onRenameSession(renamingSession!!.id, clean)
                                    renamingSession = null
                                }
                            }
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = renamingTitleText.trim()
                        if (clean.isNotEmpty()) {
                            onRenameSession(renamingSession!!.id, clean)
                            renamingSession = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = renamingTitleText.trim().isNotEmpty()
                ) {
                    Text(
                        text = "Save",
                        fontSize = 12.sp,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { renamingSession = null }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextMutedColor,
                        fontSize = 12.sp,
                        fontFamily = InstrumentSansFontFamily
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SessionCard(
    result: SessionSearchResult,
    activeSessionId: String?,
    onSessionSelected: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onTogglePinSession: (String) -> Unit,
    onStartRename: (SessionEntity) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val session = result.session
    val isSelected = session.id == activeSessionId
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateStr = remember(session.lastUpdatedAt) {
        try {
            sdf.format(Date(session.lastUpdatedAt))
        } catch (e: Exception) {
            "Yesterday"
        }
    }

    // Toggle Pin Animation configuration
    val pinRotation by animateFloatAsState(
        targetValue = if (session.isPinned) 0f else -45f,
        animationSpec = tween(durationMillis = 250)
    )
    val pinScale by animateFloatAsState(
        targetValue = if (session.isPinned) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 250)
    )
    val pinColor by animateColorAsState(
        targetValue = if (session.isPinned) PremiumCyan else TextMutedColor.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 250)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .depthGlass(
                cornerRadius = 16.dp,
                borderWidth = 1.dp,
                customTint = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else null
            )
            .let { m ->
                if (isSelected) {
                    m.border(1.2.dp, ElectricViolet, RoundedCornerShape(16.dp))
                } else m
            }
            .bounceClick {
                onSessionSelected(session.id)
                onNavigateToChat()
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Pin Icon
        IconButton(
            onClick = { onTogglePinSession(session.id) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (session.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Toggle pin",
                tint = pinColor,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer(
                        rotationZ = pinRotation,
                        scaleX = pinScale,
                        scaleY = pinScale
                    )
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Middle Content (Title + Date/Snippet)
        Column(modifier = Modifier.weight(1f)) {
            // Title crossfade instead of instantly changing
            AnimatedContent(
                targetState = session.title.ifBlank { "Unidentified Analysis" },
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "titleCrossfade"
            ) { targetTitle ->
                Text(
                    text = targetTitle,
                    color = TextPrimaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InstrumentSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!result.matchingSnippet.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = result.matchingSnippet,
                    color = PremiumCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InstrumentSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateStr,
                color = TextMutedColor,
                fontSize = 10.sp,
                fontFamily = InstrumentSansFontFamily
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Right actions: Rename (Pencil) + Delete (Trash)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Rename pencil
            IconButton(
                onClick = { onStartRename(session) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename session",
                    tint = TextMutedColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Delete Session trash
            IconButton(
                onClick = { onDeleteSession(session.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete session",
                    tint = Color(0xFFFF6B8A).copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
