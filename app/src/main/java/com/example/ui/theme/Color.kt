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

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("is_dark_theme", true)
        isInitialized = true
    }

    fun setTheme(context: Context, dark: Boolean) {
        isDarkTheme = dark
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_dark_theme", dark).apply()
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

// DepthLens Dynamic Premium Visual Palette from bash.html design tokens
val DeepMidnight: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF060609) else Color(0xFFFFFFFF)
val RichNavy: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF0D0D14) else Color(0xFFF8FAFC)
val SurfaceCardColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF141420) else Color(0xFFFFFFFF)
val ElectricViolet: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF7E65FF) else Color(0xFF7E65FF)
val PremiumCyan: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF00D4FF) else Color(0xFF0891B2)
val HighlightGlow: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF7E65FF) else Color(0xFF7E65FF)
val TextPrimaryColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFF0EEFF) else Color(0xFF0F172A)
val TextSecondaryColor: Color get() = if (ThemeManager.isDarkTheme) Color(0x99F0EEFF) else Color(0xFF334155)
val SuccessColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF2EE8A0) else Color(0xFF10B981)
val WarningColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFFFAA40) else Color(0xFFF59E0B)
val ErrorColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFFF5E8A) else Color(0xFFEF4444)

val TextMutedColor: Color get() = if (ThemeManager.isDarkTheme) Color(0x4DF0EEFF) else Color(0xFF64748B)
val SectionLabelColor: Color get() = if (ThemeManager.isDarkTheme) Color(0x99F0EEFF) else Color(0xFF475569)
val PlaceholderColor: Color get() = if (ThemeManager.isDarkTheme) Color(0x4DF0EEFF) else Color(0xFF94A3B8)
val ThemeNameColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0F766E)
val HeroSubtitleColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0F766E)

// New surface design tokens from bash.html
val Surface1: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF0D0D14) else Color(0xFFF8FAFC)
val Surface2: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF141420) else Color(0xFFFFFFFF)
val Surface3: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF1C1C2E) else Color(0xFFF1F5F9)
val Surface4: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF22223A) else Color(0xFFE2E8F0)

// Custom border tokens from bash.html
val BorderSubtle: Color get() = if (ThemeManager.isDarkTheme) Color(0x0FFFFFFF) else Color(0x0D000000)
val BorderActive: Color get() = if (ThemeManager.isDarkTheme) Color(0x667E65FF) else Color(0x997E65FF)

val CardBorderColor: Color get() = if (ThemeManager.isDarkTheme) BorderSubtle else Color(0xFFCBD5E1)

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

val ToggleOnColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0891B2)
val SidebarIconColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0891B2)
val SidebarTextColor: Color get() = if (ThemeManager.isDarkTheme) TextPrimaryColor else Color(0xFF1E293B)

// 10 specific reality layer colors requested
val Layer1 = Color(0xFF00D4FF)
val Layer2 = Color(0xFF2EE8A0)
val Layer3 = Color(0xFF7E65FF)
val Layer4 = Color(0xFFFF5E8A)
val Layer5 = Color(0xFFFFAA40)
val Layer6 = Color(0xFFFF7A5C)
val Layer7 = Color(0xFFA855F7)
val Layer8 = Color(0xFF60A5FA)
val Layer9 = Color(0xFFF472B6)
val Layer10 = Color(0xFFE2E8F0)
