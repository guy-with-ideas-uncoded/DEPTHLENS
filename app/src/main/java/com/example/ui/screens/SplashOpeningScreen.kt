package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashOpeningScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Animation drivers
    val introProgress = remember { Animatable(0f) }      // For overall scale and logo alpha
    val eyeOpenProgress = remember { Animatable(0f) }    // For drawing eye path opening height
    val irisRotation = remember { Animatable(-90f) }     // For diagonal line alignment
    val splashAlpha = remember { Animatable(1f) }        // For entire screen exit transition

    LaunchedEffect(Unit) {
        // Step 1: Fade-in and scale up the core symbol base
        launch {
            introProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // Custom elastic EaseOutBack
                )
            )
        }
        
        delay(300)
        
        // Step 2: Smooth eye opening animation
        launch {
            eyeOpenProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                )
            )
        }
        
        delay(400)
        
        // Step 3: Spin-lock diagonal line iris
        launch {
            irisRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 900,
                    easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                )
            )
        }
        
        delay(1100)
        // Step 4: Fade out splash stage
        splashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .alpha(splashAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Backlighting dynamic pulse
        val infiniteTransition = rememberInfiniteTransition(label = "backlight")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale * introProgress.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Purple40.copy(alpha = 0.2f), Color.Transparent),
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Drawables Canvas
            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .scale(introProgress.value)
                    .alpha(introProgress.value)
            ) {
                val w = size.width
                val h = size.height
                val centerOffset = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
                
                // 1. Path of Outer Eye outline
                val eyePath = Path().apply {
                    val startX = w * 0.15f
                    val endX = w * 0.85f
                    val centerY = h * 0.5f
                    
                    val peakYOffset = h * 0.22f * eyeOpenProgress.value
                    
                    moveTo(startX, centerY)
                    cubicTo(
                        w * 0.35f, centerY - peakYOffset,
                        w * 0.65f, centerY - peakYOffset,
                        endX, centerY
                    )
                    cubicTo(
                        w * 0.65f, centerY + peakYOffset,
                        w * 0.35f, centerY + peakYOffset,
                        startX, centerY
                    )
                    close()
                }
                
                drawPath(
                    path = eyePath,
                    color = Purple80,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                
                // 2. Center Iris and interior structures (drawn when eye opened)
                if (eyeOpenProgress.value > 0.05f) {
                    val elementAlpha = ((eyeOpenProgress.value - 0.05f) / 0.95f).coerceIn(0f, 1f)
                    val irisRadius = w * 0.16f * eyeOpenProgress.value
                    
                    // Draw outer circular iris stroke - Premium Cyan
                    drawCircle(
                        color = PurpleGrey80.copy(alpha = elementAlpha),
                        radius = irisRadius,
                        center = centerOffset,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    
                    // Math for 45-degree diagonal line rotation
                    val currentRotationAngle = 45f + irisRotation.value
                    val angleRad = Math.toRadians(currentRotationAngle.toDouble())
                    val cosValue = Math.cos(angleRad).toFloat()
                    val sinValue = Math.sin(angleRad).toFloat()
                    
                    // Diagonal line coordinates centered inside iris - Electric Violet
                    val lineHalfLength = irisRadius * 0.9f
                    val startLineOfs = androidx.compose.ui.geometry.Offset(
                        centerOffset.x - lineHalfLength * cosValue,
                        centerOffset.y + lineHalfLength * sinValue
                    )
                    val endLineOfs = androidx.compose.ui.geometry.Offset(
                        centerOffset.x + lineHalfLength * cosValue,
                        centerOffset.y - lineHalfLength * sinValue
                    )
                    
                    drawLine(
                        color = Purple80.copy(alpha = elementAlpha),
                        start = startLineOfs,
                        end = endLineOfs,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // 3. Symmetric node rings centered on either half-line sides
                    val nodeDistance = irisRadius * 0.44f
                    val node1Center = androidx.compose.ui.geometry.Offset(
                        centerOffset.x - nodeDistance * cosValue,
                        centerOffset.y + nodeDistance * sinValue
                    )
                    val node2Center = androidx.compose.ui.geometry.Offset(
                        centerOffset.x + nodeDistance * cosValue,
                        centerOffset.y - nodeDistance * sinValue
                    )
                    
                    val nodeRadius = w * 0.045f * eyeOpenProgress.value
                    
                    // Left-bottom ring node - Premium Cyan outline, Deep Midnight fill
                    drawCircle(
                        color = PurpleGrey80.copy(alpha = elementAlpha),
                        radius = nodeRadius,
                        center = node1Center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = ObsidianBackground.copy(alpha = elementAlpha),
                        radius = (nodeRadius - 1.5.dp.toPx()).coerceAtLeast(0.1f),
                        center = node1Center,
                        style = Fill
                    )
                    
                    // Right-top ring node
                    drawCircle(
                        color = PurpleGrey80.copy(alpha = elementAlpha),
                        radius = nodeRadius,
                        center = node2Center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = ObsidianBackground.copy(alpha = elementAlpha),
                        radius = (nodeRadius - 1.5.dp.toPx()).coerceAtLeast(0.1f),
                        center = node2Center,
                        style = Fill
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "DEPTHLENS",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.alpha(introProgress.value)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "See Beyond Surface",
                style = MaterialTheme.typography.bodyMedium,
                color = PurpleGrey80,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alpha(introProgress.value * 0.85f)
            )
        }
    }
}
