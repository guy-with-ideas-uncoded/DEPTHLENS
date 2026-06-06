package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.ResponseParser
import com.example.ui.theme.*
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
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    var showAttachBottomSheet by remember { mutableStateOf(false) }
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..20 -> "Good evening,"
            else -> "Good night,"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight)
    ) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Landing Top Row — hamburger + greeting + new-chat pencil + avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: hamburger + greeting
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hamburger menu to open sidebar drawer
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Surface2)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open sessions",
                            tint = TextSecondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 15.sp,
                        color = TextSecondaryColor
                    )
                }

                // Right: new-chat pencil + avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✏️ Stylish new-chat pencil button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        ElectricViolet.copy(alpha = 0.18f),
                                        PremiumCyan.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(ElectricViolet, PremiumCyan.copy(alpha = 0.7f))
                                ),
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable { onCreateNewSession() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New chat",
                            tint = ElectricViolet,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Premium Gradient Avatar "A"
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricViolet, PremiumCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Landing Hero — DepthLens Logo instead of headline text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_depthlens_logo),
                    contentDescription = "DepthLens",
                    modifier = Modifier
                        .height(52.dp)
                        .padding(bottom = 6.dp)
                )

                Text(
                    text = "Choose a mode or start typing — DepthLens will reveal what lies beneath.",
                    fontFamily = InstrumentSansFontFamily,
                    fontSize = 11.sp,
                    color = Color.White,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                                fontSize = 8.5.sp,
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
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor,
                                            fontFamily = InstrumentSansFontFamily,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 8.5.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            color = TextMutedColor,
                                            lineHeight = 10.sp
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

                Text(
                    text = "ANALYSIS",
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp,
                    fontFamily = DMMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeMessages.forEach { message ->
                        if (message.role == "user") {
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
                                        fontSize = 12.sp,
                                        color = TextPrimaryColor,
                                        fontFamily = InstrumentSansFontFamily
                                    )
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
                                            if (parsedResponse.introduction.isNotEmpty()) {
                                                Text(
                                                    text = parsedResponse.introduction,
                                                    fontSize = 12.sp,
                                                    color = TextSecondaryColor,
                                                    lineHeight = 17.sp,
                                                    fontFamily = InstrumentSansFontFamily,
                                                    modifier = Modifier.padding(bottom = 10.dp)
                                                )
                                            }

                                            if (parsedResponse.depthLayers.isNotEmpty()) {
                                                Text(
                                                    text = "DEPTH LAYERS",
                                                    fontSize = 7.5.sp,
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
                                                                    fontSize = 11.sp,
                                                                    color = TextSecondaryColor,
                                                                    lineHeight = 15.sp,
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
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryColor,
                                                    lineHeight = 15.sp,
                                                    fontFamily = InstrumentSansFontFamily
                                                )
                                            }

                                            // ── Suggested Questions (after analysis) ──────
                                            if (parsedResponse.suggestedQuestions.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "DIG DEEPER",
                                                    fontSize = 7.5.sp,
                                                    letterSpacing = 1.sp,
                                                    fontFamily = DMMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PremiumCyan,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                                                                fontSize = 10.5.sp,
                                                                color = TextSecondaryColor,
                                                                lineHeight = 14.sp,
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
                                                    fontSize = 7.5.sp,
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
                                                            Text("✓", fontSize = 10.sp, color = ElectricViolet)
                                                            Spacer(modifier = Modifier.width(7.dp))
                                                            Text(
                                                                text = path,
                                                                fontSize = 10.sp,
                                                                color = TextSecondaryColor,
                                                                fontFamily = InstrumentSansFontFamily,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // ── Copy Button ────────────────────────────────
                                            val clipboardManager = LocalClipboardManager.current
                                            var copied by remember { mutableStateOf(false) }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(if (copied) ElectricViolet.copy(alpha = 0.18f) else Surface3)
                                                        .border(1.dp, if (copied) ElectricViolet.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(7.dp))
                                                        .clickable {
                                                            clipboardManager.setText(AnnotatedString(message.text))
                                                            copied = true
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy analysis",
                                                            tint = if (copied) ElectricViolet else TextMutedColor,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = if (copied) "Copied!" else "Copy",
                                                            fontSize = 8.5.sp,
                                                            fontFamily = DMMonoFontFamily,
                                                            color = if (copied) ElectricViolet else TextMutedColor
                                                        )
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
                                            fontSize = 12.sp,
                                            color = TextSecondaryColor,
                                            lineHeight = 17.sp,
                                            fontFamily = InstrumentSansFontFamily,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isLoading && activeMessages.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141420), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(ElectricViolet, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analysing…", fontSize = 11.sp, color = TextMutedColor, fontFamily = DMMonoFontFamily)
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
                        .clickable { showAttachBottomSheet = true },
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

                BasicTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = InstrumentSansFontFamily,
                        fontSize = 11.5.sp,
                        color = TextPrimaryColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    decorationBox = { innerTextField ->
                        if (rawText.isEmpty()) {
                            Text(
                                text = "Describe any situation, decision, or pattern…",
                                fontFamily = InstrumentSansFontFamily,
                                fontSize = 11.5.sp,
                                color = TextMutedColor,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                val speechCtx = LocalContext.current
                var isListening by remember { mutableStateOf(false) }
                val speechRecognizer = remember {
                    try { SpeechRecognizer.createSpeechRecognizer(speechCtx) } catch (e: Exception) { null }
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
                LaunchedEffect(speechRecognizer) { speechRecognizer?.setRecognitionListener(recognitionListener) }
                DisposableEffect(Unit) { onDispose { speechRecognizer?.destroy() } }
                val audioPermLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted && speechRecognizer != null) {
                        isListening = true
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        speechRecognizer.startListening(intent)
                    }
                }

                IconButton(
                    onClick = {
                        if (isListening) {
                            speechRecognizer?.stopListening(); isListening = false
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(speechCtx, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                isListening = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                speechRecognizer?.startListening(intent)
                            } else {
                                audioPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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
    ) { uri -> uri?.let { onAddAttachment(it.toString()) } }

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
