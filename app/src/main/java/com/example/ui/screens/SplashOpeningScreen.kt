package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmbientGradientBottom
import com.example.ui.theme.AmbientGradientTop
import com.example.ui.theme.InstrumentSansFontFamily
import com.example.ui.theme.PremiumCyan
import com.example.ui.components.DepthLensLogo
import kotlinx.coroutines.delay

@Composable
fun SplashOpeningScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Navigate after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000)
        onAnimationComplete()
    }

    // Infinite breathing scale for the logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_breath")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // Wave heights for the 5 bars below the title
    val waveAnimation = rememberInfiniteTransition(label = "wave_heights")
    
    val h1 by waveAnimation.animateFloat(
        initialValue = 8f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by waveAnimation.animateFloat(
        initialValue = 12f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by waveAnimation.animateFloat(
        initialValue = 16f, targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by waveAnimation.animateFloat(
        initialValue = 10f, targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by waveAnimation.animateFloat(
        initialValue = 6f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(850, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "h5"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AmbientGradientTop, AmbientGradientBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Official DepthLens logo with breathing animation
            DepthLensLogo(
                size = 108.dp,
                showGlow = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "DepthLens",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = InstrumentSansFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "REALITY INTELLIGENCE PLATFORM",
                color = PremiumCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = InstrumentSansFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Dynamic Wave Indicator (5 vertical bars) matching CSS .wave
            Row(
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(h1, h2, h3, h4, h5).forEach { currentHeight ->
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(currentHeight.dp)
                            .background(
                                color = Color(0xFF8B5CF6),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}
