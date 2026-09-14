package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HigherSelfProfile
import com.example.data.model.SelfReflectionComparison
import com.example.ui.theme.ThemeManager

@Composable
fun HigherSelfView(
    profile: HigherSelfProfile,
    reflection: SelfReflectionComparison?,
    isReflecting: Boolean,
    onReflectRequested: (String) -> Unit,
    onDiscussInChat: (String) -> Unit,
    onAddTrait: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dilemmaInput by remember { mutableStateOf("") }

    val quickQuestions = remember {
        listOf(
            "Mujhe kya karna chahiye?",
            "Mere blind spots aur recurring patterns kya hain?",
            "Kya mera current decision mere core values se align karta hai?",
            "I've changed: let me update who I am becoming"
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Higher Self Architectural Identity Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            ThemeManager.accentColor.copy(alpha = 0.5f),
                            Color(0xFFA855F7).copy(alpha = 0.3f)
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ThemeManager.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = ThemeManager.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Higher Self / Inner Guide",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Reflective synthesis of your long-term identity",
                                    fontSize = 11.sp,
                                    color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ThemeManager.accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${profile.totalEvidenceNodes} traits",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeManager.accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Cognitive Pipeline Badge: Memory → Understanding → Self-Awareness → Reflection → Guidance
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (ThemeManager.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "COGNITIVE SYNTHESIS PIPELINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeManager.accentColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Memory → Understanding → Self-Awareness → Reflection → Guidance",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Agency Guardrail Note
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Preserves user agency: Guides, never commands. The final decision is always yours.",
                            fontSize = 11.sp,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // 2. Reflective Synthesis Statement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF162036) else Color(0xFFF1F5F9)
                ),
                border = BorderStroke(
                    1.dp,
                    if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "WHO YOU ARE STRIVING TO BECOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = profile.reflectiveSynthesis.ifBlank {
                            "Continuously learning from your thoughts, goals, and values across conversations. Share a dilemma below to begin self-reflection."
                        },
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp,
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                    )
                }
            }
        }

        // 3. Core Anchors & Trajectory Highlights
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reflective Anchors & Pillars",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                )

                // Values & Non-Negotiables
                PillarSection(
                    title = "Core Values & Non-Negotiables",
                    items = profile.coreValues,
                    emptyPlaceholder = "Values will populate as you discuss decisions and priorities.",
                    chipColor = Color(0xFF00E5FF),
                    icon = Icons.Default.Favorite
                )

                // Long-Term Aspirations
                PillarSection(
                    title = "Long-Term Trajectory & Aspirations",
                    items = profile.longTermAspirations,
                    emptyPlaceholder = "Share your long-term ambitions with DepthLens to anchor them here.",
                    chipColor = Color(0xFFA855F7),
                    icon = Icons.Default.TrackChanges
                )

                // Thinking & Reasoning Patterns
                PillarSection(
                    title = "Recurring Thinking Patterns",
                    items = profile.recurringPatterns,
                    emptyPlaceholder = "Observed cognitive styles and problem-solving patterns will appear here.",
                    chipColor = Color(0xFFF59E0B),
                    icon = Icons.Default.Lightbulb
                )

                // Constructive Mirror: Potential Blind Spots
                PillarSection(
                    title = "Constructive Mirrors & Potential Blind Spots",
                    items = profile.potentialBlindSpots,
                    emptyPlaceholder = "DepthLens will respectfully highlight recurring friction points to guard against impulsive choices.",
                    chipColor = Color(0xFFEF4444),
                    icon = Icons.Default.Visibility
                )

                // Evolving Beliefs
                if (profile.evolvingBeliefs.isNotEmpty()) {
                    PillarSection(
                        title = "Evolving Beliefs (Identity Shifts)",
                        items = profile.evolvingBeliefs,
                        emptyPlaceholder = "",
                        chipColor = Color(0xFF10B981),
                        icon = Icons.Default.Update
                    )
                }
            }
        }

        // 4. Interactive Self-Reflection Dilemma Mirror
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = ThemeManager.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Self-Reflection Mirror",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                            Text(
                                text = "Current Self vs. Higher Self comparison on any choice or confusion",
                                fontSize = 11.sp,
                                color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dilemmaInput,
                        onValueChange = { dilemmaInput = it },
                        placeholder = {
                            Text(
                                "Enter a decision, dilemma, or question (e.g., 'Kya mujhe job switch karni chahiye?')",
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeManager.accentColor,
                            unfocusedBorderColor = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // Quick prompt chips
                    Text(
                        text = "Quick Inquiries:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickQuestions) { q ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ThemeManager.isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { dilemmaInput = q }
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 10.sp,
                                    color = ThemeManager.accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { onReflectRequested(dilemmaInput) },
                        enabled = dilemmaInput.isNotBlank() && !isReflecting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.accentColor)
                    ) {
                        if (isReflecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Synthesizing Higher Self Reflection...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Examine from Higher Self Perspective", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Active Reflection Comparison Result Card
        if (reflection != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ThemeManager.isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, ThemeManager.accentColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Dilemma Analysis",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeManager.accentColor
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ThemeManager.accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Perspective Mirror",
                                    fontSize = 10.sp,
                                    color = ThemeManager.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "\"${reflection.dilemma}\"",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF334155),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Current Self Perspective
                        ComparisonPerspectiveBox(
                            title = "Current Self (Immediate Emotion & Impulse)",
                            description = reflection.currentSelfPerspective,
                            borderColor = Color(0xFFF59E0B),
                            icon = Icons.Default.HourglassEmpty
                        )

                        Spacer(Modifier.height(10.dp))

                        // Higher Self Perspective
                        ComparisonPerspectiveBox(
                            title = "Higher Self (Core Values & Long-Term Trajectory)",
                            description = reflection.higherSelfPerspective,
                            borderColor = Color(0xFF10B981),
                            icon = Icons.Default.Psychology
                        )

                        Spacer(Modifier.height(10.dp))

                        // Tension & Blind Spot
                        ComparisonPerspectiveBox(
                            title = "Constructive Tension & Blind Spot",
                            description = reflection.tensionOrBlindSpot,
                            borderColor = Color(0xFFEF4444),
                            icon = Icons.Default.WarningAmber
                        )

                        Spacer(Modifier.height(14.dp))

                        // Reflective Questions
                        if (reflection.reflectiveQuestions.isNotEmpty()) {
                            Text(
                                text = "Introspective Questions to Ask Yourself:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                            Spacer(Modifier.height(6.dp))
                            reflection.reflectiveQuestions.forEachIndexed { idx, q ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = "${idx + 1}. ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeManager.accentColor
                                    )
                                    Text(
                                        text = q,
                                        fontSize = 12.sp,
                                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Practical Guidance Step
                        if (reflection.practicalGuidance.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ThemeManager.accentColor.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = ThemeManager.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Practical Grounding Step:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeManager.accentColor
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = reflection.practicalGuidance,
                                            fontSize = 12.sp,
                                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                onDiscussInChat("Let's look at this dilemma from my Higher Self's perspective: \"${reflection.dilemma}\"")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ThemeManager.accentColor)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = ThemeManager.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Discuss with DepthLens in Chat",
                                color = ThemeManager.accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarSection(
    title: String,
    items: List<String>,
    emptyPlaceholder: String,
    chipColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeManager.isDarkTheme) Color(0xFF131B2E) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeManager.isDarkTheme) Color.White else Color(0xFF1E293B)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                if (emptyPlaceholder.isNotBlank()) {
                    Text(
                        text = emptyPlaceholder,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                }
            } else {
                FlowRowLayout(items = items, chipColor = chipColor)
            }
        }
    }
}

@Composable
private fun FlowRowLayout(
    items: List<String>,
    chipColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { itemText ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = itemText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonPerspectiveBox(
    title: String,
    description: String,
    borderColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = borderColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B)
            )
        }
    }
}
