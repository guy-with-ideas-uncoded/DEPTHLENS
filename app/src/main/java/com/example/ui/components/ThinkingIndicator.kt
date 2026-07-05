package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.PremiumCyan
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
// DEPTHLENS · THINKING INDICATOR  v6.0
//
// Neural wave design — animated eye icon + 5 sine-wave dots
// matching JSX v6 ThinkingIndicator design exactly.
// ─────────────────────────────────────────────────────────────

@Composable
fun ThreeDotThinkingIndicator(
    modifier: Modifier = Modifier,
    text: String = "Analyzing..."
) {
    // Tick counter for sine wave animation (60fps equivalent)
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val interval = 16L // ~60fps
        while (true) {
            kotlinx.coroutines.delay(interval)
            tick++
        }
    }

    Row(
        modifier = modifier
            .wrapContentWidth()
            .depthGlass(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp),
                borderWidth = 1.dp
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // ── Animated eye icon ─────────────────────────
        val eyePulseAlpha = (0.7f + 0.3f * sin(tick * 0.12f).toFloat()).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.size(width = 16.dp, height = 10.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Eye outline path
            val eyePath = Path().apply {
                moveTo(0f, cy)
                quadraticBezierTo(cx, 0f, w, cy)
                quadraticBezierTo(cx, h, 0f, cy)
                close()
            }
            drawPath(
                path = eyePath,
                color = ElectricViolet,
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Iris dot — pulses
            drawCircle(
                color = PremiumCyan.copy(alpha = eyePulseAlpha),
                radius = w * 0.13f,
                center = Offset(cx, cy)
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── Neural wave dots (5 nodes, sine wave propagation) ─
        val nodes = 5
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until nodes) {
                val phase = (tick * 0.15f) - (i * 0.7f)
                val y = sin(phase.toDouble()).toFloat()
                val sizeDp = (3.5f + y * 1.5f).coerceIn(1.5f, 6f).dp
                val opacity = (0.4f + (y + 1f) * 0.3f).coerceIn(0.2f, 1f)
                val color = if (i % 2 == 0) ElectricViolet else PremiumCyan
                val offsetY = (y * -2f).dp

                Box(
                    modifier = Modifier
                        .offset(y = offsetY)
                        .size(sizeDp)
                        .background(
                            color = color.copy(alpha = opacity),
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}
