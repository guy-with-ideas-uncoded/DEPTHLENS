package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
import com.example.ui.components.ThreeDotThinkingIndicator
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    activeMessages: List<MessageEntity>,
    selectedMode: String,
    onModeChanged: (String) -> Unit,
    isLoading: Boolean,
    attachedImageUri: String?,
    isRecordingAudio: Boolean,
    recordingDuration: Int,
    onAddAttachment: (String) -> Unit,
    onRemoveAttachment: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRetryLastAnalysis: (String) -> Unit,
    onRegenerateLastAnalysis: (String) -> Unit,
    onReportBug: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    onCreateNewSession: () -> Unit,
    onGoDeeper: (String) -> Unit = {},
    onArchiveInsight: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputQueryText by remember { mutableStateOf("") }
    var showPlusBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when messages list size change
    LaunchedEffect(activeMessages.size, isLoading) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Chat Toolbar: Minimalist layout matching bash.html instructions (no clutter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .border(1.dp, BorderSubtle)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // High-fidelity active badge icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0x2E7E65FF), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, BorderActive, shape = RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✦",
                        fontSize = 11.sp,
                        color = ElectricViolet,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "DEPTHLENS CONTEXT",
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = TextPrimaryColor,
                    fontFamily = DMMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Glassy selected lens mode pill
                Box(
                    modifier = Modifier
                        .background(Color(0x2E7E65FF), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, BorderActive, shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = selectedMode.uppercase(),
                        fontSize = 8.5.sp,
                        color = ElectricViolet,
                        fontFamily = DMMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // New Analysis launcher trigger
                IconButton(
                    onClick = onCreateNewSession,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "New Thread",
                        tint = PremiumCyan,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Active Messages Feed List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeMessages.isEmpty()) {
                // Interactive empty viewport instructing users to run tests
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0x1A7E65FF), CircleShape)
                            .border(1.2.dp, ElectricViolet.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Active Intelligence Node",
                        fontFamily = DMSerifDisplayFontFamily,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 18.sp,
                        color = TextPrimaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Select a specific mode or start submitting queries in the panel below — DepthLens will unfold multi-dimensional layer reports.",
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 11.sp,
                        color = TextMutedColor,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            } else {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(activeMessages) { message ->
                            if (message.role == "user") {
                                // High fidelity User Bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 270.dp)
                                            .background(
                                                Color(0x2E7E65FF),
                                                shape = RoundedCornerShape(
                                                    topStart = 14.dp,
                                                    topEnd = 14.dp,
                                                    bottomEnd = 3.dp,
                                                    bottomStart = 14.dp
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                BorderActive,
                                                shape = RoundedCornerShape(
                                                    topStart = 14.dp,
                                                    topEnd = 14.dp,
                                                    bottomEnd = 3.dp,
                                                    bottomStart = 14.dp
                                                )
                                            )
                                            .padding(10.dp, 12.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            fontSize = 11.5.sp,
                                            color = TextPrimaryColor,
                                            fontFamily = InstrumentSansFontFamily,
                                            lineHeight = 15.5.sp
                                        )
                                    }
                                }
                            } else {
                                // High fidelity AI Bubble with Embed Insights and Diagnostics
                                if (message.text.startsWith("Error:") || message.text.contains("Error invoking DepthLens")) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        DisplayAIHeader(layersCount = 0)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Surface2, shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                                                .border(1.dp, ErrorColor.copy(alpha = 0.4f), shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Analysis Interrupted",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ErrorColor,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                Text(
                                                    text = message.text,
                                                    fontSize = 10.5.sp,
                                                    color = TextSecondaryColor,
                                                    lineHeight = 14.sp,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onRetryLastAnalysis(message.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Retry Analysis", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }

                                                    OutlinedButton(
                                                        onClick = { onDeleteMessage(message.id) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryColor),
                                                        border = BorderStroke(1.dp, BorderSubtle),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Dismiss", fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val parsed = remember(message.text) { ResponseParser.parse(message.text) }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        DisplayAIHeader(layersCount = parsed.depthLayers.size)

                                        // Comprehensive Premium chat bubble card contents
                                        Card(
                                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                                            colors = CardDefaults.cardColors(containerColor = Surface2),
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Introduction Summary Text
                                                if (parsed.introduction.isNotEmpty()) {
                                                    Text(
                                                        text = parsed.introduction,
                                                        fontSize = 12.sp,
                                                        color = TextPrimaryColor,
                                                        lineHeight = 17.sp,
                                                        fontFamily = InstrumentSansFontFamily,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    )
                                                }

                                                // Executive Summary blockquote
                                                parsed.executiveSummary?.let { summary ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 12.dp)
                                                            .drawBehind {
                                                                val strokeWidth = 2.dp.toPx()
                                                                drawLine(
                                                                    color = ElectricViolet,
                                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                    end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            }
                                                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = summary,
                                                            fontSize = 11.sp,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                            color = TextSecondaryColor,
                                                            lineHeight = 16.sp,
                                                            fontFamily = InstrumentSansFontFamily
                                                        )
                                                    }

                                                    // Chat Inline Probability Assessment
                                                    parsed.probabilityAssessment?.let { pa ->
                                                        val color = when {
                                                            pa.likelihood >= 70 -> SuccessColor
                                                            pa.likelihood >= 40 -> WarningColor
                                                            else -> ErrorColor
                                                        }
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(bottom = 12.dp)
                                                                .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                                .padding(10.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "PROBABILITY LIKELIHOOD",
                                                                    fontSize = 7.5.sp,
                                                                    letterSpacing = 1.sp,
                                                                    fontFamily = DMMonoFontFamily,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = color
                                                                )
                                                                Text(
                                                                    text = "${pa.confidence} CONFIDENCE",
                                                                    fontSize = 7.5.sp,
                                                                    fontFamily = DMMonoFontFamily,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = color
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text("Likelihood Estimate", fontSize = 11.sp, color = TextSecondaryColor, fontFamily = InstrumentSansFontFamily)
                                                                Text("${pa.likelihood}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = color, fontFamily = InstrumentSansFontFamily)
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            LinearProgressIndicator(
                                                                progress = { pa.likelihood / 100f },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(2.dp)),
                                                                color = color,
                                                                trackColor = Surface3
                                                            )
                                                        }
                                                    }
                                                                                            // Inline Reality Layer summary panel
                                                if (parsed.depthLayers.isNotEmpty()) {
                                                    var wasArchived by remember { mutableStateOf(false) }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE REALITY LAYERS",
                                                            fontSize = 7.5.sp,
                                                            letterSpacing = 1.sp,
                                                            fontFamily = DMMonoFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextMutedColor
                                                        )

                                                        Button(
                                                            onClick = {
                                                                val precedingUserMessageIdx = activeMessages.indexOfFirst { it.id == message.id } - 1
                                                                val query = if (precedingUserMessageIdx >= 0 && activeMessages[precedingUserMessageIdx].role == "user") {
                                                                    activeMessages[precedingUserMessageIdx].text
                                                                } else {
                                                                    "Deep-Lens Scanned Exploration"
                                                                }
                                                                onArchiveInsight(query, parsed.introduction, parsed.toJsonString(query))
                                                                wasArchived = true
                                                            },
                                                            enabled = !wasArchived,
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (wasArchived) Color.Transparent else ElectricViolet.copy(alpha = 0.15f),
                                                                contentColor = if (wasArchived) SuccessColor else ElectricViolet,
                                                                disabledContainerColor = Color.Transparent,
                                                                disabledContentColor = SuccessColor
                                                            ),
                                                            border = BorderStroke(1.dp, if (wasArchived) SuccessColor.copy(alpha = 0.4f) else ElectricViolet.copy(alpha = 0.4f)),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text(
                                                                text = if (wasArchived) "✓ ARCHIVED" else "📥 ARCHIVE INSIGHT",
                                                                fontSize = 7.5.sp,
                                                                fontFamily = DMMonoFontFamily,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 12.dp)
                                                    ) {
                                                        parsed.depthLayers.forEach { layer ->
                                                            // Match standard layer colors
                                                            val layerColor = when (layer.layerNumber) {
                                                                1 -> Layer1
                                                                2 -> Layer2
                                                                3 -> Layer3
                                                                4 -> Layer4
                                                                5 -> Layer5
                                                                6 -> Layer6
                                                                7 -> Layer7
                                                                8 -> Layer8
                                                                9 -> Layer9
                                                                else -> Layer10
                                                            }

                                                            var expandedInsightLocal by remember { mutableStateOf(false) }

                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Surface3, shape = RoundedCornerShape(6.dp))
                                                                    .border(
                                                                        1.dp,
                                                                        if (expandedInsightLocal) layerColor.copy(alpha = 0.6f) else BorderSubtle,
                                                                        shape = RoundedCornerShape(6.dp)
                                                                    )
                                                                    .clickable { expandedInsightLocal = !expandedInsightLocal }
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
                                                                        imageVector = if (expandedInsightLocal) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                        contentDescription = null,
                                                                        tint = TextMutedColor,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                }

                                                                if (expandedInsightLocal) {
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        text = layer.description,
                                                                        fontSize = 11.sp,
                                                                        color = TextSecondaryColor,
                                                                        lineHeight = 15.sp,
                                                                        fontFamily = InstrumentSansFontFamily
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Root Cause Report Card
                                                parsed.rootCauseReport?.let { rc ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0x0F7E65FF), shape = RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0x337E65FF), shape = RoundedCornerShape(8.dp))
                                                            .padding(10.dp)
                                                            .padding(bottom = 8.dp)
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "ROOT CAUSE FORMULATION",
                                                                fontSize = 7.5.sp,
                                                                fontFamily = DMMonoFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ElectricViolet,
                                                                modifier = Modifier.padding(bottom = 6.dp)
                                                            )

                                                            Text(
                                                                text = "Symptom: ${rc.symptom}",
                                                                fontSize = 10.sp,
                                                                color = TextPrimaryColor,
                                                                fontFamily = InstrumentSansFontFamily,
                                                                lineHeight = 13.sp
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "Incentive: ${rc.immediateCause}",
                                                                fontSize = 10.sp,
                                                                color = TextSecondaryColor,
                                                                fontFamily = InstrumentSansFontFamily,
                                                                lineHeight = 13.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Regenerate / Action footer row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val precedingUserMessageIdx = activeMessages.indexOfFirst { it.id == message.id } - 1
                                                    val associatedUserQuery = if (precedingUserMessageIdx >= 0 && activeMessages[precedingUserMessageIdx].role == "user") {
                                                        activeMessages[precedingUserMessageIdx].text
                                                    } else {
                                                        ""
                                                    }

                                                    if (associatedUserQuery.isNotBlank()) {
                                                        Button(
                                                            onClick = { onGoDeeper(associatedUserQuery) },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color.Transparent,
                                                                contentColor = PremiumCyan
                                                            ),
                                                            border = BorderStroke(1.dp, PremiumCyan.copy(alpha = 0.5f)),
                                                            shape = RoundedCornerShape(20.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier
                                                                .height(24.dp)
                                                                .padding(end = 8.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .background(PremiumCyan, CircleShape)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "GO DEEPER",
                                                                    fontSize = 7.5.sp,
                                                                    fontFamily = DMMonoFontFamily,
                                                                    fontWeight = FontWeight.Bold,
                                                                    letterSpacing = 0.5.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { onRegenerateLastAnalysis(message.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Regenerate",
                                                            tint = TextMutedColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    // Copy button
                                                    val clipboardManager = LocalClipboardManager.current
                                                    var copied by remember { mutableStateOf(false) }
                                                    Box(
                                                        modifier = androidx.compose.ui.Modifier
                                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                                                            .background(if (copied) ElectricViolet.copy(alpha = 0.18f) else Surface3)
                                                            .border(1.dp, if (copied) ElectricViolet.copy(alpha = 0.6f) else BorderSubtle, androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                                                            .clickable {
                                                                clipboardManager.setText(AnnotatedString(message.text))
                                                                copied = true
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                                    ) {
                                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Copy analysis",
                                                                tint = if (copied) ElectricViolet else TextMutedColor,
                                                                modifier = androidx.compose.ui.Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                text = if (copied) "Copied!" else "Copy",
                                                                fontSize = 8.sp,
                                                                fontFamily = DMMonoFontFamily,
                                                                color = if (copied) ElectricViolet else TextMutedColor
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    IconButton(
                                                        onClick = { onDeleteMessage(message.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = TextMutedColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Streaming state / Typing Loader Animation matching bash.html guidelines
        if (isLoading) {
            ThreeDotThinkingIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                text = "Analyzing deeper…"
            )
        }

        // Voice dictation recording pane or Compose bar at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .border(BorderStroke(1.dp, BorderSubtle))
                .padding(10.dp)
        ) {
            Column {
                // 1. Lens mode chips row
                val inlineModes = listOf("Root Cause", "Psychology", "Systems", "Probability", "Business", "Relationships", "Spiritual", "Decision Making")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    inlineModes.forEach { mode ->
                        val isSelectedChip = mode == selectedMode
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelectedChip) Color(0x2E7E65FF) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelectedChip) ElectricViolet else BorderSubtle,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onModeChanged(mode) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode,
                                fontSize = 8.sp,
                                letterSpacing = 0.08.sp,
                                fontFamily = DMMonoFontFamily,
                                color = if (isSelectedChip) ElectricViolet else TextSecondaryColor
                            )
                        }
                    }
                }

                // 2. Premium file attachment preview above the input field
                attachedImageUri?.let { uri ->
                    val fileNameAndExt = remember(uri) {
                        try {
                            val decoded = java.net.URLDecoder.decode(uri, "UTF-8")
                            val last = decoded.split("/").lastOrNull() ?: "file"
                            val cleanName = if (last.contains("?")) last.substringBefore("?") else last
                            val ext = cleanName.substringAfterLast(".", "").lowercase()
                            Pair(cleanName, ext)
                        } catch (e: Exception) {
                            Pair("attachment.file", "")
                        }
                    }
                    val (fileName, fileExt) = fileNameAndExt

                    val isImage = fileExt in listOf("jpg", "jpeg", "png", "webp", "gif") || uri.contains("image")
                    val isVideo = fileExt in listOf("mp4", "mov", "webm") || uri.contains("video")
                    val isAudio = fileExt in listOf("mp3", "wav", "m4a") || uri.contains("audio")
                    val isPdf = fileExt == "pdf" || uri.contains("pdf")

                    val previewIcon = when {
                        isPdf -> "📄"
                        isVideo -> "🎥"
                        isAudio -> "🎵"
                        else -> "📁"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Surface3, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSubtle, shape = RoundedCornerShape(12.dp))
                            .padding(8.6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isImage) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Image preview",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(ElectricViolet.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                        .border(1.dp, ElectricViolet.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(previewIcon, fontSize = 16.sp)
                                }
                            }

                            Column {
                                Text(
                                    text = fileName.take(24) + (if (fileName.length > 24) "..." else ""),
                                    fontSize = 11.5.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        isImage -> "Image Source Added"
                                        isPdf -> "PDF Document Source Added"
                                        isAudio -> "Audio Recording Source Added"
                                        isVideo -> "Video Media Source Added"
                                        else -> "System File Source Added"
                                    },
                                    fontSize = 8.5.sp,
                                    fontFamily = InstrumentSansFontFamily,
                                    color = TextMutedColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onRemoveAttachment,
                            modifier = Modifier
                                .size(28.dp)
                                .background(ErrorColor.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text("✕", fontSize = 10.sp, color = ErrorColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. New Refactored Input Bar Layout:
                // [ + ] [ Input Field ...................... ] [ Voice-to-Text ] [ Send ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2, shape = RoundedCornerShape(28.dp))
                        .border(1.dp, BorderSubtle, shape = RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // (A) Modern attachment/add button on the left
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        ElectricViolet.copy(alpha = 0.22f),
                                        PremiumCyan.copy(alpha = 0.10f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.2.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(ElectricViolet, PremiumCyan.copy(alpha = 0.7f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showPlusBottomSheet = true },
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

                    // (B) Message Input Field
                    BasicTextField(
                        value = inputQueryText,
                        onValueChange = { inputQueryText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        textStyle = TextStyle(
                            fontFamily = InstrumentSansFontFamily,
                            fontSize = 11.5.sp,
                            color = TextPrimaryColor
                        ),
                        decorationBox = { innerTextField ->
                            if (inputQueryText.isEmpty()) {
                                Text(
                                    text = "Ask anything deeper…",
                                    fontFamily = InstrumentSansFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMutedColor
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // (C) Voice to Text button immediately before Send
                    val speechContext = LocalContext.current
                    var isListeningForSpeech by remember { mutableStateOf(false) }
                    val speechRecognizer = remember {
                        try { SpeechRecognizer.createSpeechRecognizer(speechContext) } catch (e: Exception) { null }
                    }

                    val recognitionListener = remember(speechRecognizer) {
                        object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {
                                isListeningForSpeech = false
                            }
                            override fun onError(error: Int) {
                                isListeningForSpeech = false
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    val spokenText = matches[0]
                                    if (!spokenText.isNullOrBlank()) {
                                        inputQueryText = if (inputQueryText.isEmpty()) spokenText else "$inputQueryText $spokenText"
                                    }
                                }
                                isListeningForSpeech = false
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

                    val audioPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted && speechRecognizer != null) {
                            isListeningForSpeech = true
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechRecognizer.startListening(intent)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isListeningForSpeech) {
                                speechRecognizer?.stopListening()
                                isListeningForSpeech = false
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    speechContext,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    isListeningForSpeech = true
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    }
                                    speechRecognizer?.startListening(intent)
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isListeningForSpeech) ElectricViolet.copy(alpha = 0.25f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isListeningForSpeech) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (isListeningForSpeech) "Stop listening" else "Voice to text",
                            tint = if (isListeningForSpeech) ErrorColor else TextSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // (D) Send button
                    IconButton(
                        onClick = {
                            if (inputQueryText.isNotBlank() || attachedImageUri != null) {
                                val dispatchText = inputQueryText
                                inputQueryText = ""
                                onSubmitQuery(dispatchText)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(ElectricViolet, CircleShape),
                        enabled = inputQueryText.isNotBlank() || attachedImageUri != null,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = "↑",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } // Column ends here

    // (E) Bottom Sheet Design
    androidx.compose.animation.AnimatedVisibility(
        visible = showPlusBottomSheet,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { showPlusBottomSheet = false }
        )
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onAddAttachment(it.toString())
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showPlusBottomSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, BorderSubtle, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMidnight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 18.dp, horizontal = 24.dp)
                ) {
                    // Bottom Sheet Drag Handle
                    Box(
                        modifier = Modifier
                            .size(40.dp, 4.dp)
                            .background(BorderSubtle, CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Add to Conversation",
                        fontSize = 16.sp,
                        fontFamily = InstrumentSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = PremiumCyan,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val selectOptions = listOf(
                        Triple("Images", "🖼️", "image/*"),
                        Triple("Videos", "🎬", "video/*"),
                        Triple("Audio Files", "🎧", "audio/*"),
                        Triple("PDF Documents", "📑", "application/pdf"),
                        Triple("Any File", "🗂️", "*/*")
                    )

                    selectOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface3)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable {
                                    showPlusBottomSheet = false
                                    pickerLauncher.launch(option.third)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(text = option.second, fontSize = 18.sp)
                            Text(
                                text = option.first,
                                fontSize = 13.sp,
                                color = TextPrimaryColor,
                                fontFamily = InstrumentSansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showPlusBottomSheet = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", fontSize = 13.sp, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun DisplayAIHeader(layersCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(ElectricViolet, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "DEPTHLENS" + (if (layersCount > 0) " · $layersCount LAYERS" else " · MODEL"),
            fontSize = 8.sp,
            color = ElectricViolet,
            fontFamily = DMMonoFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
