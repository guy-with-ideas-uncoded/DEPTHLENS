package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.composed
import android.os.Build
import com.example.ui.theme.*

// ─────────────────────────────────────────────────────────────
// DEPTHLENS · GLASS COMPONENT LIBRARY  v5.0
//
// All interactive surfaces use the "squish" press model:
//   scale 1.0 → 0.96 (instant spring) → 1.0 (release spring)
// Tap targets minimum 48×48dp per accessibility guidelines.
// ─────────────────────────────────────────────────────────────

// ── SQUISH BUTTON ────────────────────────────────────────────
// Drop-in replacement for any clickable surface that needs
// iOS-27-style haptic-feeling scale feedback.
@Composable
fun SquishSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = if (isPressed) Motion.PressSpring else Motion.CardSpring,
        label = "squish_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = RippleColor, bounded = true),
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}

// ── GLASS CARD ───────────────────────────────────────────────
// Frosted-glass card. Provide elevation= for tonal lift.
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.lg,
    borderAlpha: Float = 1f,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .depthGlass(cornerRadius = cornerRadius, borderAlpha = borderAlpha)
            .padding(Spacing.base),
        content = content
    )
}

// ── TINTED SURFACE CARD ──────────────────────────────────────
// Standard card using Surface2 + CardBorderColor. Upgraded with
// premium translucent next-gen glass layer, light reflection & shimmers.
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.lg,
    padding: Dp = Spacing.base,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val translucentSurface = when(ThemeManager.themeName) {
        "Polar Dawn" -> SurfaceCardColor.copy(alpha = 0.94f)
        else -> SurfaceCardColor.copy(alpha = 0.90f)
    }
    
    val baseModifier = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .drawBehind {
            // Main translucent next-gen glass layer - 85% to 95% opacity
            val backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    translucentSurface.copy(alpha = 0.94f),
                    translucentSurface.copy(alpha = 0.88f)
                )
            )
            drawRoundRect(
                brush = backgroundBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )
            
            // Soft light reflection flow
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.00f),
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.04f else 0.08f),
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.12f else 0.18f),
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.04f else 0.08f),
                    Color.White.copy(alpha = 0.00f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            drawRoundRect(
                brush = shimmerBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
            )
        }
        .border(
            width = 0.8.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.20f else 0.35f),
                    GlassBorder.copy(alpha = 0.9f),
                    GlassShimmer.copy(alpha = 0.45f),
                    GlassBorder.copy(alpha = 0.25f)
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1.0f,
            animationSpec = Motion.PressSpring,
            label = "card_scale"
        )
        Column(
            modifier = baseModifier
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = RippleColor, bounded = true),
                    onClick = onClick
                )
                .padding(padding),
            content = content
        )
    } else {
        Column(
            modifier = baseModifier.padding(padding),
            content = content
        )
    }
}

// ── GRADIENT BUTTON ──────────────────────────────────────────
// Primary CTA — upgraded with premium liquid glass highlights & reflections.
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    cornerRadius: Dp = Radius.md
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = Motion.PressSpring,
        label = "btn_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.48f,
        animationSpec = Motion.fadeTween(Motion.FAST),
        label = "btn_alpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GradientStart.copy(alpha = if (ThemeManager.isDarkTheme) 0.88f else 1f),
                        GradientEnd.copy(alpha = if (ThemeManager.isDarkTheme) 0.88f else 1f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                // Diagonal glossy specular lighting flow
                val reflectionBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawRoundRect(
                    brush = reflectionBrush,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.2f), bounded = true),
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontFamily = DMMonoFontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── GHOST BUTTON ────────────────────────────────────────────
// Secondary action — upgraded with accent glass coating & specular reflections.
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = ElectricViolet,
    cornerRadius: Dp = Radius.md
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = Motion.PressSpring,
        label = "ghost_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .background(
                color = accentColor.copy(alpha = if (isPressed) 0.16f else 0.08f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .drawBehind {
                val glassReflection = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.02f),
                        Color.White.copy(alpha = 0.07f),
                        Color.White.copy(alpha = 0.02f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawRoundRect(
                    brush = glassReflection,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.65f),
                        accentColor.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor, bounded = true),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accentColor,
            fontFamily = DMMonoFontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// ── ICON BUTTON (square) ─────────────────────────────────────
// 38×38dp tap target upgraded to fully glassmorphic surface.
@Composable
fun IconSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.md,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = Motion.PressSpring,
        label = "icon_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(Surface2.copy(alpha = 0.52f), RoundedCornerShape(cornerRadius))
            .drawBehind {
                val glassRef = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                )
                drawRoundRect(
                    brush = glassRef,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassBorder.copy(alpha = 0.7f),
                        GlassShimmer.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = RippleColor, bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ── PILL CHIP ────────────────────────────────────────────────
// Pill chip upgraded with premium glass texture and dynamic selections.
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.fadeTween(Motion.FAST),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextMutedColor,
        animationSpec = tween(durationMillis = Motion.FAST, easing = Motion.EaseOutSmooth),
        label = "chip_text"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = Motion.PressSpring,
        label = "chip_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .background(
                brush = if (selected) Brush.linearGradient(
                    listOf(
                        GradientStart.copy(alpha = if (ThemeManager.isDarkTheme) 0.82f else 1f),
                        GradientEnd.copy(alpha = if (ThemeManager.isDarkTheme) 0.62f else 0.82f)
                    )
                ) else Brush.linearGradient(
                    listOf(
                        Surface3.copy(alpha = 0.45f),
                        Surface3.copy(alpha = 0.45f)
                    )
                ),
                shape = Radius.PillShape
            )
            .drawBehind {
                val glassRef = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawRoundRect(
                    brush = glassRef,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
            .border(
                width = 0.8.dp,
                brush = if (selected) Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ) else Brush.linearGradient(
                    listOf(
                        GlassBorder.copy(alpha = 0.5f),
                        GlassShimmer.copy(alpha = 0.25f)
                    )
                ),
                shape = Radius.PillShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = RippleColor, bounded = true),
                onClick = onClick
            )
            .padding(horizontal = Spacing.base, vertical = Spacing.xs + 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = DMMonoFontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// ── ANIMATED SECTION REVEAL ──────────────────────────────────
// Wraps any content with a staggered fade+slide-up entrance.
@Composable
fun RevealSection(
    visible: Boolean,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.MEDIUM,
            delayMillis = delayMillis,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "reveal_alpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = tween(
            durationMillis = Motion.MEDIUM,
            delayMillis = delayMillis,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "reveal_slide"
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .offset(y = offsetY),
        content = { content() }
    )
}

// ── LOADING SKELETON ─────────────────────────────────────────
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.sm
) {
    val shimmerProgress by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = Motion.Shimmer,
            label = "shimmer_progress"
        )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        Surface3,
                        Surface4,
                        Surface3
                    ),
                    start = Offset(size.width * (shimmerProgress - 0.3f), 0f),
                    end = Offset(size.width * (shimmerProgress + 0.3f), size.height)
                )
                drawRect(gradient)
            }
    )
}

// ── Formatted response text ─────────────────────────────────────
// Renders DepthLens reply text by parsing it into semantic blocks:
//   - SECTION: Dynamic styled headings
//   - [EMPHASIS:HIGH] Starred high-contrast violet panels
//   - [EMPHASIS:MEDIUM] Info cyan panels
//   - [EMPHASIS:LOW] Dotted subtle context panels
//   - Standard paragraphs and lists with inline style mappings
sealed class BlockElement {
    data class SectionHeader(val title: String) : BlockElement()
    data class Callout(val text: String, val level: String) : BlockElement()
    data class Paragraph(val text: String) : BlockElement()
}

private val blocksCache = java.util.concurrent.ConcurrentHashMap<String, List<BlockElement>>()

// Parses response text into sequence of block elements
fun parseResponseToBlocks(rawText: String): List<BlockElement> {
    return blocksCache.getOrPut(rawText) {
        val blocks = mutableListOf<BlockElement>()
        val cleanedText = collapseBlankLines(rawText)
        
        val tagPattern = Regex("""\[EMPHASIS:(HIGH|MEDIUM|LOW)\]([\s\S]*?)\[/EMPHASIS\]""", RegexOption.IGNORE_CASE)
        var lastIndex = 0
        val matches = tagPattern.findAll(cleanedText).toList()
        
        if (matches.isEmpty()) {
            parseTextAndSections(cleanedText, blocks)
        } else {
            for (match in matches) {
                val startIndex = match.range.first
                val endIndex = match.range.last + 1
                
                if (startIndex > lastIndex) {
                    val preText = cleanedText.substring(lastIndex, startIndex)
                    parseTextAndSections(preText, blocks)
                }
                
                val level = match.groupValues[1].uppercase()
                val content = match.groupValues[2].trim()
                if (content.isNotEmpty()) {
                    blocks.add(BlockElement.Callout(content, level))
                }
                lastIndex = endIndex
            }
            
            if (lastIndex < cleanedText.length) {
                val postText = cleanedText.substring(lastIndex)
                parseTextAndSections(postText, blocks)
            }
        }
        
        blocks.filter {
            when (it) {
                is BlockElement.Paragraph -> it.text.isNotBlank()
                is BlockElement.SectionHeader -> it.title.isNotBlank()
                is BlockElement.Callout -> it.text.isNotBlank()
            }
        }
    }
}

private fun parseTextAndSections(text: String, blocks: MutableList<BlockElement>) {
    val lines = text.split("\n")
    var currentParagraphLines = mutableListOf<String>()
    
    for (line in lines) {
        val trimmedLine = line.trim()
        val metadataRegex = Regex("^\\[?\\**(importance|emphasis|priority|confidence|severity|level)\\**(?:\\s*:\\s*|\\s+)(high|medium|low|critical)\\**\\]?\\.?\\s*$", RegexOption.IGNORE_CASE)
        val standaloneRegex = Regex("^\\[?\\**(high|medium|low|critical)\\**\\]?\\.?\\s*$", RegexOption.IGNORE_CASE)
        if (metadataRegex.matches(trimmedLine) || standaloneRegex.matches(trimmedLine)) {
            continue
        }
        if (trimmedLine.startsWith("SECTION:", ignoreCase = true)) {
            if (currentParagraphLines.isNotEmpty()) {
                blocks.add(BlockElement.Paragraph(currentParagraphLines.joinToString("\n")))
                currentParagraphLines.clear()
            }
            val title = trimmedLine.substring("SECTION:".length).trim()
            blocks.add(BlockElement.SectionHeader(title))
        } else {
            currentParagraphLines.add(line)
        }
    }
    
    if (currentParagraphLines.isNotEmpty()) {
        blocks.add(BlockElement.Paragraph(currentParagraphLines.joinToString("\n")))
    }
}

// Renders inline formatted and markdown-stripped elements safely
private data class InlineFormattingKey(
    val rawText: String,
    val accentColor: androidx.compose.ui.graphics.Color,
    val highlightColor: androidx.compose.ui.graphics.Color
)

private val inlineFormattingCache = java.util.concurrent.ConcurrentHashMap<InlineFormattingKey, androidx.compose.ui.text.AnnotatedString>()

fun parseInlineFormatting(
    rawText: String,
    accentColor: androidx.compose.ui.graphics.Color = ElectricViolet,
    highlightColor: androidx.compose.ui.graphics.Color = PremiumCyan
): androidx.compose.ui.text.AnnotatedString {
    val key = InlineFormattingKey(rawText, accentColor, highlightColor)
    return inlineFormattingCache.getOrPut(key) {
        buildAnnotatedString {
            var text = rawText
                .replace("[EMPHASIS:HIGH]", "")
                .replace("[EMPHASIS:MEDIUM]", "")
                .replace("[EMPHASIS:LOW]", "")
                .replace("[/EMPHASIS]", "")
                .replace("[emphasis:high]", "")
                .replace("[emphasis:medium]", "")
                .replace("[emphasis:low]", "")
                .replace("[/emphasis]", "")

            text = text.replace(Regex("""^#+\s+"""), "")

            // Support standard markdown tags gracefully so they are NEVER exposed in copy text or raw visuals
            val pattern = Regex("""(\*\*[^*]+\*\*|__[^_]+__|`[^`]+`)""")
            var lastIndex = 0
            
            for (match in pattern.findAll(text)) {
                if (match.range.first > lastIndex) {
                    val segment = text.substring(lastIndex, match.range.first)
                    appendInlineLabels(segment, accentColor)
                }
                val token = match.value
                when {
                    token.startsWith("**") -> {
                        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(token.removePrefix("**").removeSuffix("**"))
                        }
                    }
                    token.startsWith("__") -> {
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = highlightColor,
                                background = highlightColor.copy(alpha = 0.14f)
                            )
                        ) {
                            append(token.removePrefix("__").removeSuffix("__"))
                        }
                    }
                    token.startsWith("`") -> {
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(
                                fontFamily = DMMonoFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = PremiumCyan,
                                background = Color(0xFF1E1E2C)
                            )
                        ) {
                            append(token.removePrefix("`").removeSuffix("`"))
                        }
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) {
                val segment = text.substring(lastIndex)
                appendInlineLabels(segment, accentColor)
            }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineLabels(text: String, accentColor: Color) {
    val lines = text.split("\n")
    lines.forEachIndexed { i, line ->
        val labelMatch = Regex("""^\s*([A-Za-z][A-Za-z0-9 /'-]{1,40}:)(\s*)""").find(line)
        var rest = line
        if (labelMatch != null) {
            val label = labelMatch.groupValues[1]
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor)) {
                append(label)
            }
            rest = line.substring(labelMatch.value.length)
            append(" ")
        }
        append(rest)
        if (i != lines.lastIndex) append("\n")
    }
}

@Composable
fun FormattedResponseText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 20.sp,
    color: Color = TextPrimaryColor
) {
    val accent = ElectricViolet
    val highlight = PremiumCyan
    val blocks = remember(text) { parseResponseToBlocks(text) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is BlockElement.SectionHeader -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (if (index == 0) 4 else 14).dp, bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(13.dp)
                                    .background(ElectricViolet, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = block.title.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DMMonoFontFamily,
                                color = ElectricViolet,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorderColor.copy(alpha = 0.5f))
                        )
                    }
                }
                is BlockElement.Callout -> {
                    val isHigh = block.level == "HIGH"
                    val isMedium = block.level == "MEDIUM"
                    val cardColor = when {
                        isHigh -> ElectricViolet.copy(alpha = 0.08f)
                        isMedium -> PremiumCyan.copy(alpha = 0.08f)
                        else -> Surface2.copy(alpha = 0.3f)
                    }
                    val borderColor = when {
                        isHigh -> ElectricViolet.copy(alpha = 0.4f)
                        isMedium -> PremiumCyan.copy(alpha = 0.4f)
                        else -> BorderSubtle.copy(alpha = 0.3f)
                    }
                    val icon = when {
                        isHigh -> Icons.Filled.AutoAwesome
                        isMedium -> Icons.Filled.Info
                        else -> Icons.Filled.Lightbulb
                    }
                    val tintColor = when {
                        isHigh -> ElectricViolet
                        isMedium -> PremiumCyan
                        else -> TextMutedColor
                    }
                    val titleText = when {
                        isHigh -> "CRITICAL OUTCOME / DYNAMIC"
                        isMedium -> "SYSTEMIC INSIGHT"
                        else -> "CONTEXT / PARAMETERS"
                    }

                    val inlineAnnotatedText = remember(block.text) {
                        parseInlineFormatting(block.text, accent, highlight)
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = block.level,
                                tint = tintColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = titleText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DMMonoFontFamily,
                                    color = tintColor,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                                Text(
                                    text = inlineAnnotatedText,
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    color = color,
                                    fontFamily = InstrumentSansFontFamily
                                )
                            }
                        }
                    }
                }
                is BlockElement.Paragraph -> {
                    val inlineAnnotatedText = remember(block.text) {
                        parseInlineFormatting(block.text, accent, highlight)
                    }
                    Text(
                        text = inlineAnnotatedText,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = color,
                        fontFamily = InstrumentSansFontFamily,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// Collapses 3+ consecutive newlines (i.e. 2+ blank lines) down to a single blank
// line, and trims leading/trailing blank lines, so AI responses don't show large
// empty gaps between paragraphs.
fun collapseBlankLines(text: String): String {
    return text
        .replace(Regex("[ \t]+\n"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

// ─── UNIFIED GLASS MATERIAL SYSTEM MODIFIERS ───

fun Modifier.glassScreenBackground(): Modifier = this.composed {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_drift")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_drift_angle"
    )

    val isDawn = ThemeManager.themeName == "Polar Dawn" || ThemeManager.themeName == "Dawn"

    // IMPORTANT: never apply a RenderEffect blur here. This modifier sits on the ROOT
    // screen node, so a renderEffect blurs the ENTIRE UI — every text/button/icon — and
    // stays blurred even at slider 0 (the old code clamped to min 8px). That was the
    // "whole screen is blurry" bug. The ambient blobs below are already soft radial
    // gradients; the frosted look comes from the translucent glass surfaces (depthGlass),
    // not from blurring the content.
    this.then(
        Modifier.drawBehind {
            val topColor = AmbientGradientTop
            val bottomColor = AmbientGradientBottom
            
            // Draw the theme-tinted background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )

            // Animated coordinates for drifting blobs
            val driftX1 = Math.cos(animValue.toDouble()).toFloat() * 25.dp.toPx()
            val driftY1 = Math.sin(animValue.toDouble()).toFloat() * 25.dp.toPx()
            val driftX2 = Math.sin(animValue.toDouble()).toFloat() * 20.dp.toPx()
            val driftY2 = Math.cos(animValue.toDouble()).toFloat() * 20.dp.toPx()
            val driftX3 = Math.cos(animValue.toDouble() + 1.2).toFloat() * 30.dp.toPx()
            val driftY3 = Math.sin(animValue.toDouble() + 1.2).toFloat() * 30.dp.toPx()

            val alphaScale = if (isDawn) 0.5f else 1.0f
            // Blur slider (8..40) softens/spreads the ambient aurora — a safe, visible
            // effect that never touches the foreground content.
            val blurFactor = 0.8f + (ThemeManager.blurStrength.coerceIn(8f, 40f) / 40f) * 0.5f

            // 1. Cyan blob at 80% width, 8% height, radius = 70% width
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PremiumCyan.copy(alpha = 0.32f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.80f + driftX1, size.height * 0.08f + driftY1),
                    radius = size.width * 0.70f * blurFactor
                ),
                center = Offset(size.width * 0.80f + driftX1, size.height * 0.08f + driftY1),
                radius = size.width * 0.70f * blurFactor
            )

            // 2. Violet blob at 12% width, 26% height, radius = 75% width
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.48f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f + driftX2, size.height * 0.26f + driftY2),
                    radius = size.width * 0.75f * blurFactor
                ),
                center = Offset(size.width * 0.12f + driftX2, size.height * 0.26f + driftY2),
                radius = size.width * 0.75f * blurFactor
            )

            // 3. Violet2 blob at 70% width, 92% height, radius = 80% width
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SectionLabelColor.copy(alpha = 0.38f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.70f + driftX3, size.height * 0.92f + driftY3),
                    radius = size.width * 0.80f * blurFactor
                ),
                center = Offset(size.width * 0.70f + driftX3, size.height * 0.92f + driftY3),
                radius = size.width * 0.80f * blurFactor
            )

            // ── Distinct drifting "balls" (preview .blob b1/b2/b3) ──────────────
            // Smaller & brighter than the ambient wash above, floating vertically so
            // they read as moving orbs. glassScreenBackground() is on the app root, so
            // these appear on every screen just like the preview.
            val floatA = Math.sin(animValue.toDouble()).toFloat() * 34.dp.toPx()
            val floatB = Math.sin(animValue.toDouble() + 2.1).toFloat() * 34.dp.toPx()
            val floatC = Math.sin(animValue.toDouble() + 4.2).toFloat() * 34.dp.toPx()
            // Bright, solid core → transparent edge so the orb reads as a distinct
            // floating "ball" (like the preview .blob), not a faint wash. Independent of
            // the blur slider so it stays visible at any blur level.
            val ballAlpha = if (isDawn) 0.42f else 0.78f

            // b1 — violet, left edge upper
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to ElectricViolet.copy(alpha = ballAlpha),
                        0.55f to ElectricViolet.copy(alpha = ballAlpha * 0.5f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.06f, size.height * 0.20f + floatA),
                    radius = size.width * 0.24f
                ),
                center = Offset(size.width * 0.06f, size.height * 0.20f + floatA),
                radius = size.width * 0.24f
            )
            // b2 — cyan, right edge mid
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to PremiumCyan.copy(alpha = ballAlpha),
                        0.55f to PremiumCyan.copy(alpha = ballAlpha * 0.5f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.94f, size.height * 0.47f + floatB),
                    radius = size.width * 0.21f
                ),
                center = Offset(size.width * 0.94f, size.height * 0.47f + floatB),
                radius = size.width * 0.21f
            )
            // b3 — violet2, lower left
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to SectionLabelColor.copy(alpha = ballAlpha * 0.9f),
                        0.55f to SectionLabelColor.copy(alpha = ballAlpha * 0.45f),
                        1.0f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.24f, size.height * 0.82f + floatC),
                    radius = size.width * 0.26f
                ),
                center = Offset(size.width * 0.24f, size.height * 0.82f + floatC),
                radius = size.width * 0.26f
            )

            // Dynamic deep glossy glass reflection overlay
            val glossBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.05f else 0.12f),
                    Color.White.copy(alpha = 0.0f)
                ),
                center = Offset(size.width * 0.25f, size.height * 0.15f),
                radius = size.width * 1.5f
            )
            drawRect(brush = glossBrush)
            
            // Refractive physical boundary highlight
            val edgeBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (ThemeManager.isDarkTheme) 0.03f else 0.07f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height * 0.2f)
            )
            drawRect(brush = edgeBrush)
        }
    )
}

fun Modifier.depthGlass(
    shape: androidx.compose.ui.graphics.Shape? = null,
    cornerRadius: Dp = Radius.lg,
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 1f,
    customTint: Color? = null,
    showSpecularHighlight: Boolean = true,
    elevation: Dp = 10.dp
): Modifier = this.composed {
    val isLiquid = ThemeManager.glassStyle == "Liquid Crystal"
    val isDawn = ThemeManager.themeName == "Polar Dawn" || ThemeManager.themeName == "Dawn"

    val shapeToUse = shape ?: if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RoundedCornerShape(0.dp)

    // Base Fill:
    // "Liquid Crystal" translucency: rgba(255,255,255,0.07) on dark themes, rgba(255,255,255,0.45) on Dawn.
    // "Frost Aurora" opaque: rgba(22,19,46,0.62) (which is GlassDark) on dark, rgba(255,255,255,0.78) on Dawn.
    val themeBaseColor = customTint ?: if (isLiquid) {
        if (isDawn) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.07f)
    } else {
        if (isDawn) Color.White.copy(alpha = 0.78f) else GlassDark
    }

    // Keep the glass film at the exact preview opacity (Liquid 0.07 / Dawn 0.45,
    // Frost 0.62 / Dawn 0.78). Do NOT tie it to the blur slider — that made the glass
    // nearly invisible at low blur and off-spec vs the preview.
    val themeBaseColorAdjusted = themeBaseColor

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            themeBaseColorAdjusted,
            themeBaseColorAdjusted.copy(alpha = themeBaseColorAdjusted.alpha * 0.85f)
        )
    )

    // Softer border so glass surfaces read as glass, not hard outlined boxes/"patti"
    // (the bright 0.20 white edge was very visible on opaque Frost Aurora).
    val borderAlphaVal = (if (isDawn) 0.30f else 0.09f) * borderAlpha
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlphaVal),
            GlassBorder.copy(alpha = borderAlphaVal * 0.5f),
            GlassShimmer.copy(alpha = borderAlphaVal * 0.3f),
            GlassBorder.copy(alpha = borderAlphaVal * 0.1f)
        )
    )

    val shadowModifier = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shapeToUse,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (isLiquid) 0.04f else 0.08f),
            spotColor = Color.Black.copy(alpha = if (isLiquid) 0.06f else 0.12f)
        )
    } else {
        Modifier
    }

    this
        .then(shadowModifier)
        .clip(shapeToUse)
        .drawBehind {
            val outline = shapeToUse.createOutline(size, layoutDirection, this)

            // Draw the translucent film / opaque background color on top of the backdrop copy
            drawOutline(
                outline = outline,
                brush = backgroundBrush
            )

            // NOTE: removed the bright top highlight line — on opaque (Frost Aurora)
            // surfaces it read as a hard white "patti"/box outline across the top.

            if (showSpecularHighlight) {
                // Diagonal specular sheen = linear-gradient(135deg, rgba(255,255,255,0.20), transparent 46%) at ~0.65 opacity
                val sheenBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.13f), // 0.20f * 0.65f
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.46f, size.height * 0.46f)
                )
                drawOutline(
                    outline = outline,
                    brush = sheenBrush
                )
            }
        }
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shapeToUse
        )
}

fun Modifier.premiumGlassBg(
    cornerRadius: Dp = Radius.lg,
    borderAlpha: Float = 1f,
    customTint: Color? = null,
    borderWidth: Dp = 1.dp,
    showSpecularHighlight: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape? = null,
    elevation: Dp = 10.dp
): Modifier = this.depthGlass(
    shape = shape,
    cornerRadius = cornerRadius,
    borderWidth = borderWidth,
    borderAlpha = borderAlpha,
    customTint = customTint,
    showSpecularHighlight = showSpecularHighlight,
    elevation = elevation
)