package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.example.R

// Define Google Fonts Provider
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// 1. DM Serif Display (standard upright style)
val DMSerifDisplayFont = GoogleFont("DM Serif Display")
val DMSerifDisplayFontFamily = FontFamily(
    Font(googleFont = DMSerifDisplayFont, fontProvider = fontProvider, weight = FontWeight.Normal)
)

// 2. DM Mono for monospace labels, badges, metadata
val DMMonoFont = GoogleFont("DM Mono")
val DMMonoFontFamily = FontFamily(
    Font(googleFont = DMMonoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = DMMonoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = DMMonoFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// 3. Instrument Sans for body text
val InstrumentSansFont = GoogleFont("Instrument Sans")
val InstrumentSansFontFamily = FontFamily(
    Font(googleFont = InstrumentSansFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = InstrumentSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = InstrumentSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = InstrumentSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InstrumentSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// Create the Typography object mapped to display, body, and label text configurations:
// - displayLarge/displayMedium/displaySmall -> DM Serif Display (Normal)
// - labelLarge/labelMedium/labelSmall -> DM Mono
// - bodyLarge/bodyMedium/bodySmall -> Instrument Sans
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DMSerifDisplayFontFamily,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DMSerifDisplayFontFamily,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DMSerifDisplayFontFamily,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InstrumentSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InstrumentSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InstrumentSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DMMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.4.sp // ~0.1em letter spacing
    ),
    labelMedium = TextStyle(
        fontFamily = DMMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DMMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.0.sp
    )
)

fun TextStyle.scaled(scale: Float): TextStyle {
    if (scale == 1.0f) return this
    val scaledFontSize = if (this.fontSize.isSp) (this.fontSize.value * scale).sp else this.fontSize
    val scaledLineHeight = if (this.lineHeight.isSp) (this.lineHeight.value * scale).sp else this.lineHeight
    return this.copy(fontSize = scaledFontSize, lineHeight = scaledLineHeight)
}

fun Typography.scaled(scale: Float): Typography {
    if (scale == 1.0f) return this
    return Typography(
        displayLarge = this.displayLarge.scaled(scale),
        displayMedium = this.displayMedium.scaled(scale),
        displaySmall = this.displaySmall.scaled(scale),
        headlineLarge = this.headlineLarge.scaled(scale),
        headlineMedium = this.headlineMedium.scaled(scale),
        headlineSmall = this.headlineSmall.scaled(scale),
        titleLarge = this.titleLarge.scaled(scale),
        titleMedium = this.titleMedium.scaled(scale),
        titleSmall = this.titleSmall.scaled(scale),
        bodyLarge = this.bodyLarge.scaled(scale),
        bodyMedium = this.bodyMedium.scaled(scale),
        bodySmall = this.bodySmall.scaled(scale),
        labelLarge = this.labelLarge.scaled(scale),
        labelMedium = this.labelMedium.scaled(scale),
        labelSmall = this.labelSmall.scaled(scale)
    )
}