package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// DEPTHLENS · MOTION TOKENS  v5.0
//
// Design intent:
//   – Every interactive element uses physics-based springs,
//     not fixed-duration tweens, so the UI feels "alive."
//   – Tap/press: immediate 96 % scale-down with a fast spring
//     back (iOS 27 "squish").
//   – Screen transitions: shared-Z cross-fade + vertical slide,
//     matching Android 17 predictive-back feel.
//   – Ambient elements: slow, sinusoidal infiniteRepeatable
//     with RepeatMode.Reverse — no jarring loops.
//   – All durations use a 4-step scale: instant (0), fast (150),
//     medium (320), slow (520), cinematic (900).
// ─────────────────────────────────────────────────────────────

object Motion {

    // ── DURATIONS (ms) ───────────────────────
    const val INSTANT    = 0
    const val FAST       = 150
    const val MEDIUM     = 300
    const val SLOW       = 500
    const val CINEMATIC  = 900

    // ── STANDARD EASINGS ─────────────────────
    // Android 17 "emphasized" easing (replaces FastOutSlowIn)
    val EmphasizedEasing      = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    // Android 17 "emphasized decelerate" (for elements entering the screen)
    val EmphasizedDecelerate  = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    // Android 17 "emphasized accelerate" (for elements leaving the screen)
    val EmphasizedAccelerate  = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    // iOS 27 "spring feel" cubic (matches UISpringTimingParameters style but robust via closed-form easeOutBack)
    val iOSSpringEasing        = Easing { t ->
        val tm1 = t - 1.0f
        1.0f + 2.70158f * tm1 * tm1 * tm1 + 1.70158f * tm1 * tm1
    }
    // Smooth out — used for fades and subtle scale
    val EaseOutSmooth          = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // ── SPRING SPECS ─────────────────────────
    // Tap/press "squish": fast, slightly bouncy
    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessHigh
    )

    // Card appear / expand: medium bounce
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    // Drawer / bottom-sheet slide: no bounce, very natural
    val SlideSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    // Size/layout spring (for Dp values)
    val LayoutSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    // ── TWEEN SPECS ──────────────────────────
    // Screen entrance (enter transition)
    fun enterTween() = tween<Float>(
        durationMillis = MEDIUM,
        easing         = EmphasizedDecelerate
    )

    // Screen exit (exit transition)
    fun exitTween() = tween<Float>(
        durationMillis = FAST,
        easing         = EmphasizedAccelerate
    )

    // Generic fade
    fun fadeTween(duration: Int = MEDIUM) = tween<Float>(
        durationMillis = duration,
        easing         = EaseOutSmooth
    )

    // Stagger helper: returns a delayMillis for index i with base step
    fun staggerDelay(index: Int, step: Int = 40) = index * step

    // ── AMBIENT INFINITE SPECS ───────────────
    // Slow float — for orbs, halos, breathing backgrounds
    val AmbientFloat = infiniteRepeatable<Float>(
        animation  = tween(4000, easing = EaseInOutQuad),
        repeatMode = RepeatMode.Reverse
    )

    // Pulse — for glow rings, scan halos
    val PulseGlow = infiniteRepeatable<Float>(
        animation  = tween(2400, easing = EaseInOutQuad),
        repeatMode = RepeatMode.Reverse
    )

    // Shimmer sweep — for loading skeletons
    val Shimmer = infiniteRepeatable<Float>(
        animation  = tween(1600, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )

    // ── TRANSITION OFFSET ────────────────────
    // Vertical slide distance for screen transitions
    val ScreenSlideOffset = 40.dp
}
