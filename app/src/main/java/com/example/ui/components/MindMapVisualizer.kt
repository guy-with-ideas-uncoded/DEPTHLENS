package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpistemicClassification
import com.example.data.model.IdentityCategories
import com.example.data.model.IdentityNodeEntity
import com.example.data.model.MindMapCategoryGroup
import com.example.ui.theme.*
import kotlin.math.*

data class VisualNode(
    val id: String,
    val title: String,
    val category: String,
    val classification: String,
    val confidence: Int,
    val color: Color,
    val isCategory: Boolean,
    val count: Int = 0,
    var x: Float,
    var y: Float,
    val radius: Float,
    val entity: IdentityNodeEntity? = null
)

@Composable
fun MindMapVisualizer(
    groups: List<MindMapCategoryGroup>,
    userName: String = "YOU",
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    onTraitSelected: (IdentityNodeEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Breathing pulse animation for central YOU node
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    // Orbital rotation for ambient motion
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (ThemeManager.isDarkTheme) Color(0xFF090D16).copy(alpha = 0.85f)
                else Color(0xFFF4F6FC).copy(alpha = 0.9f)
            )
            .border(
                1.dp,
                if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.08f)
                else Color.Black.copy(alpha = 0.06f),
                RoundedCornerShape(20.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffsetX += dragAmount.x
                    panOffsetY += dragAmount.y
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f + panOffsetX
        val centerY = height / 2f + panOffsetY

        // Calculate visual layout for categories
        val displayedCategories = remember(groups) {
            val activeNames = groups.map { it.category }.toSet()
            // Always show active categories first, followed by key canonical categories
            val list = groups.map { it.category }.toMutableList()
            IdentityCategories.ALL.forEach { cat ->
                if (!list.contains(cat) && list.size < 12) {
                    list.add(cat)
                }
            }
            list
        }

        val categoryNodes = remember(displayedCategories, groups, centerX, centerY, orbitAngle) {
            val count = displayedCategories.size
            val radiusOrbit = min(width, height) * 0.38f
            displayedCategories.mapIndexed { index, catName ->
                val angleDeg = (360f / count) * index + (orbitAngle * 0.1f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val x = centerX + (radiusOrbit * cos(angleRad)).toFloat()
                val y = centerY + (radiusOrbit * sin(angleRad)).toFloat()
                val group = groups.firstOrNull { it.category == catName }
                val nodeCount = group?.nodes?.size ?: 0
                val colorHex = IdentityCategories.getCategoryColorHex(catName)

                VisualNode(
                    id = "cat_$catName",
                    title = catName,
                    category = catName,
                    classification = "CATEGORY",
                    confidence = group?.averageConfidence ?: 85,
                    color = Color(colorHex),
                    isCategory = true,
                    count = nodeCount,
                    x = x,
                    y = y,
                    radius = if (catName == selectedCategory) 42f else 32f
                )
            }
        }

        // Child trait nodes when a category is selected
        val activeTraitNodes = remember(selectedCategory, groups, centerX, centerY) {
            if (selectedCategory == null) emptyList()
            else {
                val group = groups.firstOrNull { it.category == selectedCategory }
                val traits = group?.nodes ?: emptyList()
                val catVisual = categoryNodes.firstOrNull { it.category == selectedCategory }
                val originX = catVisual?.x ?: centerX
                val originY = catVisual?.y ?: centerY
                val traitOrbit = 140f

                traits.take(8).mapIndexed { idx, trait ->
                    val angleDeg = (360f / min(traits.size, 8)) * idx
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val x = originX + (traitOrbit * cos(angleRad)).toFloat()
                    val y = originY + (traitOrbit * sin(angleRad)).toFloat()

                    val color = when (trait.getEpistemic()) {
                        EpistemicClassification.EXPLICIT_FACT -> Color(0xFF00E5FF)
                        EpistemicClassification.STRONG_PATTERN -> Color(0xFFA855F7)
                        EpistemicClassification.INFERENCE -> Color(0xFFF59E0B)
                        EpistemicClassification.TEMPORARY_STATE -> Color(0xFF94A3B8)
                    }

                    VisualNode(
                        id = trait.id,
                        title = trait.title,
                        category = trait.category,
                        classification = trait.classification,
                        confidence = trait.confidence,
                        color = color,
                        isCategory = false,
                        x = x,
                        y = y,
                        radius = 24f,
                        entity = trait
                    )
                }
            }
        }

        // Draw Canvas Links & Orbit Rings
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(categoryNodes, activeTraitNodes) {
                    detectTapGestures { tapOffset ->
                        // Check if trait clicked
                        val clickedTrait = activeTraitNodes.firstOrNull {
                            val dist = sqrt((it.x - tapOffset.x).pow(2) + (it.y - tapOffset.y).pow(2))
                            dist <= it.radius * 1.5f
                        }
                        if (clickedTrait?.entity != null) {
                            onTraitSelected(clickedTrait.entity)
                            return@detectTapGestures
                        }

                        // Check if category clicked
                        val clickedCategory = categoryNodes.firstOrNull {
                            val dist = sqrt((it.x - tapOffset.x).pow(2) + (it.y - tapOffset.y).pow(2))
                            dist <= it.radius * 1.5f
                        }
                        if (clickedCategory != null) {
                            if (selectedCategory == clickedCategory.category) {
                                onCategorySelected(null) // deselect
                            } else {
                                onCategorySelected(clickedCategory.category)
                            }
                            return@detectTapGestures
                        }

                        // Tap empty space resets
                        onCategorySelected(null)
                    }
                }
        ) {
            val centerOffset = Offset(centerX, centerY)

            // Orbital background guides
            drawCircle(
                color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                radius = min(width, height) * 0.38f,
                center = centerOffset,
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
            )
            drawCircle(
                color = if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.015f),
                radius = min(width, height) * 0.22f,
                center = centerOffset,
                style = Stroke(width = 1f)
            )

            // Dynamic links from center to categories
            categoryNodes.forEach { catNode ->
                val isSelected = catNode.category == selectedCategory
                val lineColor = if (isSelected) catNode.color else catNode.color.copy(alpha = if (catNode.count > 0) 0.35f else 0.12f)
                val lineWidth = if (isSelected) 3f else if (catNode.count > 0) 1.5f else 0.8f

                drawLine(
                    color = lineColor,
                    start = centerOffset,
                    end = Offset(catNode.x, catNode.y),
                    strokeWidth = lineWidth
                )
            }

            // Links from selected category to its child traits
            val catVisual = categoryNodes.firstOrNull { it.category == selectedCategory }
            if (catVisual != null) {
                activeTraitNodes.forEach { traitNode ->
                    drawLine(
                        color = traitNode.color.copy(alpha = 0.65f),
                        start = Offset(catVisual.x, catVisual.y),
                        end = Offset(traitNode.x, traitNode.y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }
            }

            // Draw Central Node Rings
            val centerRadius = 50f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ThemeManager.accentColor.copy(alpha = ringAlpha),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = centerRadius * 2.2f
                ),
                radius = centerRadius * 2.2f,
                center = centerOffset
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(ThemeManager.accentColor, Color(0xFF00E5FF)),
                    start = Offset(centerX - centerRadius, centerY - centerRadius),
                    end = Offset(centerX + centerRadius, centerY + centerRadius)
                ),
                radius = centerRadius,
                center = centerOffset
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = centerRadius,
                center = centerOffset,
                style = Stroke(width = 1.5f)
            )

            // Draw Category Nodes
            categoryNodes.forEach { node ->
                val isSelected = node.category == selectedCategory
                val nodeOffset = Offset(node.x, node.y)

                if (isSelected) {
                    drawCircle(
                        color = node.color.copy(alpha = 0.25f),
                        radius = node.radius * 1.8f,
                        center = nodeOffset
                    )
                }

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(node.color, node.color.copy(alpha = 0.7f)),
                        start = Offset(node.x - node.radius, node.y - node.radius),
                        end = Offset(node.x + node.radius, node.y + node.radius)
                    ),
                    radius = node.radius,
                    center = nodeOffset
                )
                drawCircle(
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                    radius = node.radius,
                    center = nodeOffset,
                    style = Stroke(width = if (isSelected) 2.5f else 1.2f)
                )
            }

            // Draw Trait Child Nodes
            activeTraitNodes.forEach { trait ->
                val traitOffset = Offset(trait.x, trait.y)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(trait.color, trait.color.copy(alpha = 0.6f)),
                        start = Offset(trait.x - trait.radius, trait.y - trait.radius),
                        end = Offset(trait.x + trait.radius, trait.y + trait.radius)
                    ),
                    radius = trait.radius,
                    center = traitOffset
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = trait.radius,
                    center = traitOffset,
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // Center Label overlay ("YOU")
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { (centerX - 50f * pulseScale).toDp() },
                    y = with(density) { (centerY - 50f * pulseScale).toDp() }
                )
                .size(with(density) { (100f * pulseScale).toDp() }),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userName.take(8).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MIND MAP",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 7.sp
                )
            }
        }

        // Category Text Labels overlay
        categoryNodes.forEach { catNode ->
            val isSelected = catNode.category == selectedCategory
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (catNode.x - 55f).toDp() },
                        y = with(density) { (catNode.y - catNode.radius - 22f).toDp() }
                    )
                    .width(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = catNode.title,
                        color = if (isSelected) Color.White else if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = if (isSelected) 10.sp else 8.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (catNode.count > 0) {
                        Surface(
                            color = catNode.color.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Text(
                                text = "${catNode.count} traits",
                                color = catNode.color,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Trait Labels overlay
        activeTraitNodes.forEach { traitNode ->
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (traitNode.x - 45f).toDp() },
                        y = with(density) { (traitNode.y + traitNode.radius + 2f).toDp() }
                    )
                    .width(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = if (ThemeManager.isDarkTheme) Color(0xFF111827).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, traitNode.color.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = traitNode.title,
                        color = if (ThemeManager.isDarkTheme) Color.White else Color.Black,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Floating Quick Reset / Recenter Button
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    onCategorySelected(null)
                },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (ThemeManager.isDarkTheme) Color.White.copy(alpha = 0.1f)
                        else Color.Black.copy(alpha = 0.06f)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = "Recenter",
                    tint = if (ThemeManager.isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
