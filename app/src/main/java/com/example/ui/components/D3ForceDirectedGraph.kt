package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DepthLayerInsight
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

// GRAPH DATA STRUCTURES
data class GraphNode(
    val id: Int,
    val label: String,
    val description: String,
    val color: Color,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 48f
)

data class GraphLink(
    val sourceId: Int,
    val targetId: Int,
    val relationship: String
)

@Composable
fun D3ForceDirectedGraph(
    parsedLayers: List<DepthLayerInsight>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Fallback standard analytical reality levels if no layers are parsed yet
    val nodes = remember(parsedLayers) {
        val initialList = mutableListOf<GraphNode>()
        if (parsedLayers.isEmpty()) {
            val fallbacks = listOf(
                Triple(1, "Observable Reality", "Physical facts visible in plain sight and literal actions."),
                Triple(2, "Behavioral Patterns", "Habitual communication, protocols, and interactive signals."),
                Triple(3, "Emotional Postures", "Underlying anxiety projections, emotional reactions, and blockages."),
                Triple(4, "Strategic Agendas", "Calculated games, competition alignments, and hidden posture."),
                Triple(5, "Systemic Dynamics", "Architectural rules, corporate protocols, and institutional forces."),
                Triple(6, "Root Cause Friction", "The fundamental loop driving downstream issues.")
            )
            val count = fallbacks.size
            fallbacks.forEachIndexed { idx, item ->
                val angle = (2 * Math.PI * idx / count).toFloat()
                initialList.add(
                    GraphNode(
                        id = item.first,
                        label = "L${item.first}",
                        description = "${item.second}: ${item.third}",
                        color = getGraphNodeColor(item.first),
                        x = 350f + 160f * kotlin.math.cos(angle),
                        y = 280f + 160f * kotlin.math.sin(angle)
                    )
                )
            }
        } else {
            val count = parsedLayers.size
            parsedLayers.forEachIndexed { idx, layer ->
                val angle = (2 * Math.PI * idx / count).toFloat()
                initialList.add(
                    GraphNode(
                        id = layer.layerNumber,
                        label = "L${layer.layerNumber}",
                        description = "${layer.layerName}: ${layer.description}",
                        color = getGraphNodeColor(layer.layerNumber),
                        x = 350f + 180f * kotlin.math.cos(angle),
                        y = 280f + 180f * kotlin.math.sin(angle)
                    )
                )
            }
        }
        initialList
    }

    // Build directed feedback loops / connections
    val links = remember(nodes) {
        val list = mutableListOf<GraphLink>()
        if (nodes.size >= 2) {
            for (i in 0 until nodes.size - 1) {
                list.add(GraphLink(nodes[i].id, nodes[i + 1].id, "Downstream Cause"))
            }
            // Add recursive loops to represent system feedback mechanisms!
            list.add(GraphLink(nodes.last().id, nodes.first().id, "Recursive Feedback Loop"))
            if (nodes.size >= 4) {
                list.add(GraphLink(nodes[nodes.size - 1].id, nodes[1].id, "Inter-layer loop"))
            }
        }
        list
    }

    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var draggedNodeId by remember { mutableStateOf<Int?>(null) }
    var physicsTrigger by remember { mutableStateOf(0) }

    // D3 Physics Simulation Engine running in Background Coroutine Context
    LaunchedEffect(nodes, links, draggedNodeId) {
        while (true) {
            // 1. Particle-Particle Charge Repulsion (forces nodes apart so they organize cleanly)
            for (i in nodes.indices) {
                val nodeA = nodes[i]
                for (j in i + 1 until nodes.size) {
                    val nodeB = nodes[j]
                    val dx = nodeB.x - nodeA.x
                    val dy = nodeB.y - nodeA.y
                    val distSq = dx * dx + dy * dy
                    val dist = sqrt(distSq).coerceAtLeast(1f)
                    if (dist < 400f) {
                        val force = (2000f / (distSq + 300f)).coerceAtMost(6f)
                        val fx = force * (dx / dist)
                        val fy = force * (dy / dist)
                        
                        nodeA.vx -= fx
                        nodeA.vy -= fy
                        nodeB.vx += fx
                        nodeB.vy += fy
                    }
                }
            }

            // 2. Link Spring Tension Attraction (pulls mapped interconnected layers together)
            for (link in links) {
                val source = nodes.find { it.id == link.sourceId }
                val target = nodes.find { it.id == link.targetId }
                if (source != null && target != null) {
                    val dx = target.x - source.x
                    val dy = target.y - source.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val targetDist = 160f
                    val force = (dist - targetDist) * 0.02f
                    val fx = force * (dx / dist)
                    val fy = force * (dy / dist)

                    source.vx += fx
                    source.vy += fy
                    target.vx -= fx
                    target.vy -= fy
                }
            }

            // 3. Gravitational Center Gravity Pull (keeps the layout cluster centered inside viewport)
            val centerX = 350f
            val centerY = 280f
            for (node in nodes) {
                val dx = centerX - node.x
                val dy = centerY - node.y
                node.vx += dx * 0.005f
                node.vy += dy * 0.005f
            }

            // 4. Dampen Velocities and Update Positions
            for (node in nodes) {
                if (node.id == draggedNodeId) {
                    node.vx = 0f
                    node.vy = 0f
                } else {
                    node.vx *= 0.8f
                    node.vy *= 0.8f
                    node.x += node.vx
                    node.y += node.vy

                    // Bound constraints to prevent nodes fleeing off Canvas borders
                    node.x = node.x.coerceIn(50f, 650f)
                    node.y = node.y.coerceIn(50f, 510f)
                }
            }

            physicsTrigger = (physicsTrigger + 1) % 100000
            delay(16) // ~60 FPS Simulation Step
        }
    }

    // Loop animation for animated feedback loop pulses traveling along the linkages
    val infiniteTransition = rememberInfiniteTransition(label = "LinkPulses")
    val linkPulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F1A)),
        border = BorderStroke(1.2.dp, Color(0xFF7E65FF).copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Monospace Subtitle Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF2EE8A0), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "D3 COGNITIVE FEEDBACK MECHANISMS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2EE8A0),
                        letterSpacing = 1.sp
                    )
                }
                
                Text(
                    text = "DRAG NODE TO BALANCE PHYSICS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.5.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Graph Canvas Box Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color(0xFF06060B), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                    .pointerInput(nodes) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val hitNode = nodes.find { node ->
                                    val dx = node.x - offset.x
                                    val dy = node.y - offset.y
                                    sqrt(dx * dx + dy * dy) <= node.radius + 20f
                                }
                                if (hitNode != null) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    draggedNodeId = hitNode.id
                                    selectedNode = hitNode
                                }
                            },
                            onDrag = { _, dragAmount ->
                                val id = draggedNodeId
                                if (id != null) {
                                    val active = nodes.find { it.id == id }
                                    if (active != null) {
                                        active.x += dragAmount.x
                                        active.y += dragAmount.y
                                        physicsTrigger = (physicsTrigger + 1) % 100000
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedNodeId = null
                            },
                            onDragCancel = {
                                draggedNodeId = null
                            }
                        )
                    }
                    .clickable {
                        // Unselect node if clicked on background canvas space
                        if (draggedNodeId == null) {
                            selectedNode = null
                        }
                    }
            ) {
                // Physics Trigger recomposes standard Canvas drawing on changes
                val trigger = physicsTrigger
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Draw subtle background circuit alignment grid
                    val gridSpace = 40.dp.toPx()
                    val cols = (size.width / gridSpace).toInt()
                    val rows = (size.height / gridSpace).toInt()
                    for (c in 0..cols) {
                        drawLine(
                            color = Color(0x06FFFFFF),
                            start = Offset(c * gridSpace, 0f),
                            end = Offset(c * gridSpace, size.height),
                            strokeWidth = 0.8f
                        )
                    }
                    for (r in 0..rows) {
                        drawLine(
                            color = Color(0x06FFFFFF),
                            start = Offset(0f, r * gridSpace),
                            end = Offset(size.width, r * gridSpace),
                            strokeWidth = 0.8f
                        )
                    }

                    // 2. Render Links / Connections connecting layers
                    for (link in links) {
                        val src = nodes.find { it.id == link.sourceId }
                        val tgt = nodes.find { it.id == link.targetId }
                        if (src != null && tgt != null) {
                            // Transparent glowing feedback linkage
                            drawLine(
                                color = src.color.copy(alpha = 0.25f),
                                start = Offset(src.x, src.y),
                                end = Offset(tgt.x, tgt.y),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Render animated light pulses flow along linkages
                            val pulseX = src.x + (tgt.x - src.x) * linkPulseProgress
                            val pulseY = src.y + (tgt.y - src.y) * linkPulseProgress
                            drawCircle(
                                color = Color(0xFF00D4FF),
                                radius = 4.dp.toPx(),
                                center = Offset(pulseX, pulseY),
                                alpha = 0.8f
                            )
                        }
                    }

                    // 3. Render Nodes / Layers Circles
                    for (node in nodes) {
                        val isHighlighted = selectedNode?.id == node.id || draggedNodeId == node.id
                        // Draw glowing outline halo under active nodes
                        drawCircle(
                            color = node.color,
                            radius = node.radius + (if (isHighlighted) 12.dp.toPx() else 4.dp.toPx()),
                            center = Offset(node.x, node.y),
                            alpha = if (isHighlighted) 0.3f else 0.12f
                        )

                        // Draw main node button circle
                        drawCircle(
                            color = Color(0xFF080811),
                            radius = node.radius,
                            center = Offset(node.x, node.y)
                        )

                        // Draw layer colored border edge ring
                        drawCircle(
                            color = node.color,
                            radius = node.radius,
                            center = Offset(node.x, node.y),
                            style = Stroke(width = (if (isHighlighted) 2.5.dp.toPx() else 1.5.dp.toPx()))
                        )

                        // Draw textual label elegantly centered in node using Android native Canvas
                        val labelPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 11.dp.toPx()
                            typeface = android.graphics.Typeface.MONOSPACE
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            node.label,
                            node.x,
                            node.y + 4.dp.toPx(),
                            labelPaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Expandable Description Detail View Panel for selected node
            AnimatedVisibility(
                visible = selectedNode != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedNode?.let { node ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141424), RoundedCornerShape(8.dp))
                            .border(1.dp, node.color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ACTIVE RECONSTRUCTION BRIEFING",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = node.color
                                )
                                Text(
                                    text = "LAYER ${node.id}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = node.color
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = node.description,
                                fontSize = 11.5.sp,
                                color = Color(0xFFF0EEFF),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getGraphNodeColor(layerNumber: Int): Color = when (layerNumber) {
    1 -> Color(0xFF00D4FF)
    2 -> Color(0xFF2EE8A0)
    3 -> Color(0xFF7E65FF)
    4 -> Color(0xFFFF5E8A)
    5 -> Color(0xFFFFAA40)
    6 -> Color(0xFFFF7A5C)
    7 -> Color(0xFFA855F7)
    8 -> Color(0xFF60A5FA)
    9 -> Color(0xFFF472B6)
    else -> Color(0xFFE2E8F0)
}
