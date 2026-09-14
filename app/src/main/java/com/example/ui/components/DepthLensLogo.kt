package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun DepthLensLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    showGlow: Boolean = true,
    isGenerating: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "depthlens_logo_breathe")
    
    // Smooth scale animation: faster pulse when generating (1.00 -> 1.12), calm breathing when idle (1.00 -> 1.05)
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isGenerating) 0.98f else 1.00f,
        targetValue = if (isGenerating) 1.12f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isGenerating) 650 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    
    // Soft glow intensity synchronized with breathing (brighter aura when generating)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isGenerating) 0.35f else 0.15f,
        targetValue = if (isGenerating) 0.75f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isGenerating) 650 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_glow_alpha"
    )

    val logoViolet = Color(0xFF8B5CF6)
    val logoCyan = Color(0xFF00D9FF)

    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                logoViolet.copy(alpha = glowAlpha),
                                logoCyan.copy(alpha = glowAlpha * (if (isGenerating) 0.7f else 0.4f)),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        Image(
            painter = painterResource(id = R.drawable.ic_depthlens_logo),
            contentDescription = "DepthLens Logo",
            modifier = Modifier.size(size * 0.85f)
        )
    }
}
