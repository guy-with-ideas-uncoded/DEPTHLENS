package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// DEPTHLENS · SPATIAL TOKENS  v5.0
//
// Geometry philosophy:
//   – "Depth Field" aesthetic: large radii for macro surfaces
//     (cards, modals), tight radii for micro elements (chips,
//     badges), zero radius for fullscreen layers.
//   – Spacing uses an 8dp base grid (4, 8, 12, 16, 20, 24, 32,
//     48, 64). Half-steps (6, 10, 14) only for fine-tuned gaps.
//   – Elevation uses tonal surfaces (Android 17) rather than
//     drop-shadows for dark-mode. Drop-shadows only on light mode.
// ─────────────────────────────────────────────────────────────

object Spacing {
    val xs   =  4.dp
    val sm   =  8.dp
    val md   = 12.dp
    val base = 16.dp
    val lg   = 20.dp
    val xl   = 24.dp
    val xxl  = 32.dp
    val huge = 48.dp
    val epic = 64.dp
}

object Radius {
    val none    =  0.dp
    val xs      =  4.dp
    val sm      =  8.dp
    val md      = 12.dp
    val lg      = 16.dp
    val xl      = 20.dp
    val xxl     = 24.dp
    val pill    = 100.dp  // fully rounded

    // Pre-built shapes
    val SmShape  = RoundedCornerShape(sm)
    val MdShape  = RoundedCornerShape(md)
    val LgShape  = RoundedCornerShape(lg)
    val XlShape  = RoundedCornerShape(xl)
    val XxlShape = RoundedCornerShape(xxl)
    val PillShape= RoundedCornerShape(pill)
}

object Elevation {
    // Tonal elevation offsets for Android 17 surface tinting
    val none   =  0.dp
    val low    =  1.dp
    val medium =  3.dp
    val high   =  6.dp
    val modal  = 12.dp
}

object IconSize {
    val xs  = 14.dp
    val sm  = 16.dp
    val md  = 20.dp
    val lg  = 24.dp
    val xl  = 28.dp
    val xxl = 36.dp
}
