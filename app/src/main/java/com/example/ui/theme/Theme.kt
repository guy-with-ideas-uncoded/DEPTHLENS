package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// ─────────────────────────────────────────────────────────────
// DEPTHLENS · THEME  v5.0
//
// Material 3 color schemes aligned with the v5 palette.
// NavTransitions: Android 17 predictive-back feel —
//   enter = slide up 40dp + fade, exit = scale down + fade.
// ─────────────────────────────────────────────────────────────

private val DarkColorScheme: ColorScheme
    @Composable
    get() = darkColorScheme(
        primary            = ElectricViolet,
        onPrimary          = Color.White,
        primaryContainer   = ElectricViolet.copy(alpha = 0.2f),
        onPrimaryContainer = TextPrimaryColor,
        secondary          = PremiumCyan,
        onSecondary        = Color.Black,
        secondaryContainer = PremiumCyan.copy(alpha = 0.2f),
        onSecondaryContainer = TextPrimaryColor,
        tertiary           = SuccessColor,
        onTertiary         = Color.Black,
        background         = DeepMidnight,
        onBackground       = TextPrimaryColor,
        surface            = DeepMidnight,
        onSurface          = TextPrimaryColor,
        surfaceVariant     = SurfaceCardColor,
        onSurfaceVariant   = TextSecondaryColor,
        outline            = BorderSubtle,
        outlineVariant     = GlassBorder,
        error              = ErrorColor,
        onError            = Color.White,
        scrim              = ScrimColor
    )

private val LightColorScheme: ColorScheme
    @Composable
    get() = lightColorScheme(
        primary            = ElectricViolet,
        onPrimary          = Color.White,
        primaryContainer   = ElectricViolet.copy(alpha = 0.2f),
        onPrimaryContainer = TextPrimaryColor,
        secondary          = PremiumCyan,
        onSecondary        = Color.White,
        secondaryContainer = PremiumCyan.copy(alpha = 0.2f),
        onSecondaryContainer = TextPrimaryColor,
        tertiary           = SuccessColor,
        onTertiary         = Color.White,
        background         = DeepMidnight,
        onBackground       = TextPrimaryColor,
        surface            = DeepMidnight,
        onSurface          = TextPrimaryColor,
        surfaceVariant     = SurfaceCardColor,
        onSurfaceVariant   = TextSecondaryColor,
        outline            = BorderSubtle,
        outlineVariant     = GlassBorder,
        error              = ErrorColor,
        onError            = Color.White,
        scrim              = ScrimColor
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, TypographyManager.currentScale) {
        Density(currentDensity.density, currentDensity.fontScale * TypographyManager.currentScale)
    }

    // Custom text selection colors matching ThemeManager.accentColor
    val selectionColors = remember(ThemeManager.accentColor) {
        androidx.compose.foundation.text.selection.TextSelectionColors(
            handleColor = ThemeManager.accentColor,
            backgroundColor = ThemeManager.accentColor.copy(alpha = 0.35f)
        )
    }

    CompositionLocalProvider(
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography.scaled(TypographyManager.currentScale)
        ) {
            CompositionLocalProvider(
                androidx.compose.foundation.text.selection.LocalTextSelectionColors provides selectionColors,
                content = content
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Navigation transitions — use with NavHost contentTransition.
// Matches Android 17 predictive back gesture feel:
//   Forward: new screen slides up from below + fades in
//   Back: current screen slides down + fades out (returns)
// ─────────────────────────────────────────────────────────────
object NavTransitions {

    val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { it / 12 },
            animationSpec  = tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)
        ) + fadeIn(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate))
    }

    val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it / 16 },
            animationSpec = tween(Motion.FAST, easing = Motion.EmphasizedAccelerate)
        ) + fadeOut(tween(Motion.FAST, easing = Motion.EmphasizedAccelerate))
    }

    val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { -it / 12 },
            animationSpec  = tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)
        ) + fadeIn(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate))
    }

    val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { it / 12 },
            animationSpec = tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)
        ) + fadeOut(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate))
    }
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleOnPress: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleOnPress else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounce_click_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}
