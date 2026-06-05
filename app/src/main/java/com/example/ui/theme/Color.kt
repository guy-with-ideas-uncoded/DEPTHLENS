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

// DepthLens Dynamic Premium Visual Palette
val DeepMidnight: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF04030F) else Color(0xFFFFFFFF)
val RichNavy: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF080618) else Color(0xFFF8FAFC)
val SurfaceCardColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF0D0C22) else Color(0xFFFFFFFF)
val ElectricViolet: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF7B5CF5) else Color(0xFF7B5CF5)
val PremiumCyan: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF00E5FF) else Color(0xFF0891B2)
val HighlightGlow: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF9D7FFF) else Color(0xFF7B5CF5)
val TextPrimaryColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFF0EEFF) else Color(0xFF0F172A)
val TextSecondaryColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF8E8D9F) else Color(0xFF334155)
val SuccessColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF00E676) else Color(0xFF10B981)
val WarningColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFFFC857) else Color(0xFFF59E0B)
val ErrorColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFFFF5A5F) else Color(0xFFEF4444)

val TextMutedColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF64637A) else Color(0xFF64748B)
val SectionLabelColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF8E8D9F) else Color(0xFF475569)
val PlaceholderColor: Color get() = if (ThemeManager.isDarkTheme) Color(0xFF64637A) else Color(0xFF94A3B8)
val ThemeNameColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0F766E)
val HeroSubtitleColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0F766E)

// Legacy mapping support to prevent compiling or refactoring issues
val Purple80: Color get() = ElectricViolet
val PurpleGrey80: Color get() = PremiumCyan
val Pink80: Color get() = HighlightGlow

val Purple40: Color get() = ElectricViolet
val PurpleGrey40: Color get() = PremiumCyan
val Pink40: Color get() = HighlightGlow

val ObsidianBackground: Color get() = DeepMidnight
val DeepSurface: Color get() = RichNavy
val CardBorderColor: Color get() = if (ThemeManager.isDarkTheme) SurfaceCardColor else Color(0xFFCBD5E1) // Rich border visibility in Polar Dawn
val TextPrimary: Color get() = TextPrimaryColor
val TextSecondary: Color get() = TextSecondaryColor
val SuccessGreen: Color get() = SuccessColor
val AccentOrange: Color get() = WarningColor
val ErrorRed: Color get() = ErrorColor

val ToggleOnColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0891B2)
val SidebarIconColor: Color get() = if (ThemeManager.isDarkTheme) PremiumCyan else Color(0xFF0891B2)
val SidebarTextColor: Color get() = if (ThemeManager.isDarkTheme) TextPrimaryColor else Color(0xFF1E293B)
