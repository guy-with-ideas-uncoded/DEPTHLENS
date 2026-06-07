package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Context

// DepthLens Theme System Controller
object ThemeManager {
    private var isInitialized = false
    var isDarkTheme by mutableStateOf(true)
    var themeName by mutableStateOf("Deep Sea")

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        themeName = prefs.getString("theme_name", "Deep Sea") ?: "Deep Sea"
        isDarkTheme = themeName != "Polar Dawn"
        isInitialized = true
    }

    fun setTheme(context: Context, newTheme: String) {
        themeName = newTheme
        isDarkTheme = newTheme != "Polar Dawn"
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_name", newTheme).putBoolean("is_dark_theme", isDarkTheme).apply()
    }
}

// DepthLens Typography Scale Controller
object TypographyManager {
    private var isInitialized = false
    var currentFontSizeKey by mutableStateOf("Medium")

    val currentScale: Float
        get() = when (currentFontSizeKey) {
            "Extra Small" -> 0.85f
            "Small" -> 0.95f
            "Medium" -> 1.00f
            "Large" -> 1.15f
            "Extra Large" -> 1.30f
            else -> 1.00f
        }

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        currentFontSizeKey = prefs.getString("font_size_key", "Medium") ?: "Medium"
        isInitialized = true
    }

    fun setFontSize(context: Context, sizeKey: String) {
        currentFontSizeKey = sizeKey
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("font_size_key", sizeKey).apply()
    }
}

// DepthLens Dynamic Premium Visual Palette supporting Void, Deep Sea, Polar Dawn, and Ember themes
val DeepMidnight: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF000000)
    "Deep Sea" -> Color(0xFF060609)
    "Polar Dawn" -> Color(0xFFFFFFFF)
    "Ember" -> Color(0xFF0B0606)
    else -> Color(0xFF060609)
}

val RichNavy: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF050505)
    "Deep Sea" -> Color(0xFF0D0D14)
    "Polar Dawn" -> Color(0xFFF8FAFC)
    "Ember" -> Color(0xFF140B0B)
    else -> Color(0xFF0D0D14)
}

val SurfaceCardColor: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF0B0B0C)
    "Deep Sea" -> Color(0xFF141420)
    "Polar Dawn" -> Color(0xFFFFFFFF)
    "Ember" -> Color(0xFF1F1212)
    else -> Color(0xFF141420)
}

val ElectricViolet: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFFE2E8F0) // Silver/Platinum outline
    "Deep Sea" -> Color(0xFF7E65FF) // Electric Violet
    "Polar Dawn" -> Color(0xFF581C87) // Deep Indigo Purple
    "Ember" -> Color(0xFFFF5E1A) // Fiery Orange
    else -> Color(0xFF7E65FF)
}

val PremiumCyan: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFFCBD5E1) // Silver-blue details
    "Deep Sea" -> Color(0xFF00D4FF) // Vibrant Cyan
    "Polar Dawn" -> Color(0xFF0369A1) // Rich Deep Blue
    "Ember" -> Color(0xFFFFAA40) // Glowing Amber
    else -> Color(0xFF00D4FF)
}

val HighlightGlow: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFFF8FAFC)
    "Deep Sea" -> Color(0xFF7E65FF)
    "Polar Dawn" -> Color(0xFF581C87)
    "Ember" -> Color(0xFFFF5E1A)
    else -> Color(0xFF7E65FF)
}

val TextPrimaryColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF0F172A) // Deep Charcoal
    "Void" -> Color(0xFFF8FAFC)
    "Ember" -> Color(0xFFFFF3F0)
    else -> Color(0xFFF0EEFF) // Off-white for Dark Sea
}

val TextSecondaryColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF334155) // Dark Slate
    "Void" -> Color(0xFFCBD5E1) // Silver grey
    "Ember" -> Color(0xFFD3C2BD)
    else -> Color(0x99F0EEFF)
}

val SuccessColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF047857) // Solid Emerald
    "Void" -> Color(0xFF34D399)
    "Ember" -> Color(0xFF10B981)
    else -> Color(0xFF2EE8A0)
}

val WarningColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFFB45309) // Rich Amber
    "Void" -> Color(0xFFFBBF24)
    "Ember" -> Color(0xFFF59E0B)
    else -> Color(0xFFFFAA40)
}

val ErrorColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFFB91C1C) // Pure Crimson
    "Void" -> Color(0xFFF87171)
    "Ember" -> Color(0xFFEF4444)
    else -> Color(0xFFFF5E8A)
}

val TextMutedColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF64748B)
    "Void" -> Color(0xFF666666)
    "Ember" -> Color(0xFF8E7C77)
    else -> Color(0x4DF0EEFF)
}

val SectionLabelColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF475569)
    "Void" -> Color(0xFF888888)
    "Ember" -> Color(0xFFA68F8A)
    else -> Color(0x99F0EEFF)
}

val PlaceholderColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn" -> Color(0xFF94A3B8)
    "Void" -> Color(0xFF444444)
    "Ember" -> Color(0xFF60524F)
    else -> Color(0x4DF0EEFF)
}

val ThemeNameColor: Color get() = PremiumCyan
val HeroSubtitleColor: Color get() = PremiumCyan

val Surface1: Color get() = RichNavy
val Surface2: Color get() = SurfaceCardColor

val Surface3: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF131315)
    "Deep Sea" -> Color(0xFF1C1C2E)
    "Polar Dawn" -> Color(0xFFF1F5F9)
    "Ember" -> Color(0xFF271818)
    else -> Color(0xFF1C1C2E)
}

val Surface4: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF1C1C1F)
    "Deep Sea" -> Color(0xFF22223A)
    "Polar Dawn" -> Color(0xFFE2E8F0)
    "Ember" -> Color(0xFF332020)
    else -> Color(0xFF22223A)
}

val BorderSubtle: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF1C1C1F)
    "Deep Sea" -> Color(0x0FFFFFFF)
    "Polar Dawn" -> Color(0xFFE2E8F0)
    "Ember" -> Color(0xFF332020)
    else -> Color(0x0FFFFFFF)
}

val BorderActive: Color get() = ElectricViolet

val CardBorderColor: Color get() = when (ThemeManager.themeName) {
    "Void" -> Color(0xFF222225)
    "Deep Sea" -> BorderSubtle
    "Polar Dawn" -> Color(0xFFCBD5E1)
    "Ember" -> Color(0xFF3A2323)
    else -> BorderSubtle
}

// Legacy mapping support to prevent compiling or refactoring issues
val Purple80: Color get() = ElectricViolet
val PurpleGrey80: Color get() = PremiumCyan
val Pink80: Color get() = HighlightGlow

val Purple40: Color get() = ElectricViolet
val PurpleGrey40: Color get() = PremiumCyan
val Pink40: Color get() = HighlightGlow

val ObsidianBackground: Color get() = DeepMidnight
val DeepSurface: Color get() = RichNavy
val TextPrimary: Color get() = TextPrimaryColor
val TextSecondary: Color get() = TextSecondaryColor
val SuccessGreen: Color get() = SuccessColor
val AccentOrange: Color get() = WarningColor
val ErrorRed: Color get() = ErrorColor

val ToggleOnColor: Color get() = PremiumCyan
val SidebarIconColor: Color get() = ElectricViolet
val SidebarTextColor: Color get() = TextPrimaryColor

// Helper function to return high contrast, premium colors for all 10 reality layers
fun getLayerColor(layerNumber: Int): Color = when (ThemeManager.themeName) {
    "Polar Dawn" -> when (layerNumber) {
        1 -> Color(0xFF0369A1) // Deep Royal Blue
        2 -> Color(0xFF047857) // Dark Emerald Green
        3 -> Color(0xFF581C87) // Deep Indigo Purple
        4 -> Color(0xFFB91C1C) // Crimson Red
        5 -> Color(0xFFB45309) // Deep Amber
        6 -> Color(0xFFC2410C) // Rust/Copper
        7 -> Color(0xFF701A75) // Rich Magenta
        8 -> Color(0xFF1E3A8A) // True Navy Blue
        9 -> Color(0xFF9D174D) // Dark Rose Pink
        else -> Color(0xFF475569) // Charcoal/Slate
    }
    else -> when (layerNumber) {
        1 -> Color(0xFF00D4FF) // Electric Cyan
        2 -> Color(0xFF2EE8A0) // Mint Emerald
        3 -> Color(0xFF7E65FF) // Electric Violet
        4 -> Color(0xFFFF5E8A) // Vibrant Pink
        5 -> Color(0xFFFFAA40) // Glowing Amber
        6 -> Color(0xFFFF7A5C) // Coral Orange
        7 -> Color(0xFFA855F7) // Radiant Purple
        8 -> Color(0xFF60A5FA) // Sky Blue
        9 -> Color(0xFFF472B6) // Soft Rose
        else -> Color(0xFFE2E8F0) // Platinum Grey
    }
}

val Layer1: Color get() = getLayerColor(1)
val Layer2: Color get() = getLayerColor(2)
val Layer3: Color get() = getLayerColor(3)
val Layer4: Color get() = getLayerColor(4)
val Layer5: Color get() = getLayerColor(5)
val Layer6: Color get() = getLayerColor(6)
val Layer7: Color get() = getLayerColor(7)
val Layer8: Color get() = getLayerColor(8)
val Layer9: Color get() = getLayerColor(9)
val Layer10: Color get() = getLayerColor(10)
