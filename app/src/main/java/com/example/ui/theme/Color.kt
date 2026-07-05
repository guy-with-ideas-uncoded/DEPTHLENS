package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.content.Context

// ─────────────────────────────────────────────
// DEPTHLENS  ·  VISUAL SYSTEM  v5.0
// Design language: Spatial Glass / Depth Field
// Inspired by iOS 27 liquid-glass + Android 17
// adaptive-surface + DEPTHLENS' eye metaphor.
// ─────────────────────────────────────────────

object ThemeManager {
    private var isInitialized = false
    var isDarkTheme by mutableStateOf(true)
    var themeName by mutableStateOf("Deep Sea")
    var accentColor by mutableStateOf(Color(0xFF7C5CFF))
    var glassStyle by mutableStateOf("Liquid Crystal")
    var blurStrength by mutableStateOf(24f)
    var reduceMotion by mutableStateOf(false)
    var hapticFeedback by mutableStateOf(true)
    var appIconIndex by mutableStateOf(0)

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        themeName = prefs.getString("theme_name", "Deep Sea") ?: "Deep Sea"
        val defaultVioletHex = when (themeName) {
            "Void" -> "#8E7CFF"
            "Future" -> "#3D7CFF"
            "Ember" -> "#FF6B4A"
            "Purple" -> "#C46BFF"
            else -> "#7C5CFF"
        }
        val storedAccent = prefs.getString("accent_color", defaultVioletHex) ?: defaultVioletHex
        accentColor = Color(android.graphics.Color.parseColor(storedAccent))
        glassStyle = prefs.getString("glass_style", "Liquid Crystal") ?: "Liquid Crystal"
        blurStrength = prefs.getFloat("blur_strength", 24f)
        reduceMotion = prefs.getBoolean("reduce_motion", false)
        hapticFeedback = prefs.getBoolean("haptic_feedback", true)
        appIconIndex = prefs.getInt("app_icon_index", 0)
        isDarkTheme = themeName != "Dawn" && themeName != "Polar Dawn"
        isInitialized = true
    }

    fun setTheme(context: Context, newTheme: String) {
        themeName = newTheme
        isDarkTheme = newTheme != "Dawn" && newTheme != "Polar Dawn"
        val defaultVioletHex = when (newTheme) {
            "Void" -> "#8E7CFF"
            "Future" -> "#3D7CFF"
            "Ember" -> "#FF6B4A"
            "Purple" -> "#C46BFF"
            else -> "#7C5CFF"
        }
        accentColor = Color(android.graphics.Color.parseColor(defaultVioletHex))
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_name", newTheme)
            .putBoolean("is_dark_theme", isDarkTheme)
            .putString("accent_color", defaultVioletHex)
            .apply()
    }
    
    fun setAccent(context: Context, hexColor: String) {
        accentColor = Color(android.graphics.Color.parseColor(hexColor))
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("accent_color", hexColor).apply()
    }
    
    fun setGlass(context: Context, style: String) {
        glassStyle = style
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("glass_style", style).apply()
    }
    
    fun setBlur(context: Context, strength: Float) {
        blurStrength = strength
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("blur_strength", strength).apply()
    }
    
    fun setMotion(context: Context, reduce: Boolean) {
        reduceMotion = reduce
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("reduce_motion", reduce).apply()
    }
    
    fun setHaptics(context: Context, enabled: Boolean) {
        hapticFeedback = enabled
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
    }

    fun setAppIcon(context: Context, index: Int) {
        appIconIndex = index
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("app_icon_index", index).apply()
    }
}

object TypographyManager {
    private var isInitialized = false
    var currentFontSizeKey by mutableStateOf("Medium")

    val currentScale: Float
        get() = when (currentFontSizeKey) {
            "Small"  -> 0.90f
            "Medium" -> 1.05f
            "Large"  -> 1.20f
            else     -> 1.05f
        }

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        val storedKey = prefs.getString("font_size_key", "Medium") ?: "Medium"
        // Migrate any legacy "Extra Small" / "Extra Large" values to the nearest standard size
        currentFontSizeKey = when (storedKey) {
            "Extra Small" -> "Small"
            "Extra Large" -> "Large"
            "Small", "Medium", "Large" -> storedKey
            else -> "Medium"
        }
        isInitialized = true
    }

    fun setFontSize(context: Context, sizeKey: String) {
        currentFontSizeKey = sizeKey
        val prefs = context.applicationContext.getSharedPreferences("depthlens_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("font_size_key", sizeKey).apply()
    }
}

// ─── BACKGROUND DEPTHS ──────────────────────
val DeepMidnight: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF070608)
    "Deep Sea"           -> Color(0xFF0C0B1C)
    "Polar Dawn", "Dawn" -> Color(0xFFDDE3F1)
    "Ember"              -> Color(0xFF120706)
    "Future"             -> Color(0xFF04081A)
    "Purple"             -> Color(0xFF0E0420)
    else                 -> Color(0xFF0C0B1C)
}

val RichNavy: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF141416)
    "Deep Sea"           -> Color(0xFF161334)
    "Polar Dawn", "Dawn" -> Color(0xFFEEF1F8)
    "Ember"              -> Color(0xFF26100F)
    "Future"             -> Color(0xFF0A1330)
    "Purple"             -> Color(0xFF1D0D3A)
    else                 -> Color(0xFF161334)
}

val SurfaceCardColor: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF141318)
    "Deep Sea"           -> Color(0xFF15132C)
    "Polar Dawn", "Dawn" -> Color(0xFFFFFFFF)
    "Ember"              -> Color(0xFF1A0E0E)
    "Future"             -> Color(0xFF080E1F)
    "Purple"             -> Color(0xFF130D29)
    else                 -> Color(0xFF15132C)
}

// ─── GLASS SURFACE TOKENS (new in v5) ────────
// Used for frosted-glass cards, bottom sheets, nav bars.
val GlassDark: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xC7FFFFFF)   // rgba(255,255,255,.78) light dawn
    "Void"               -> Color(0x9E141318)   // Frost material over card color
    "Future"     -> Color(0x9E080E1F)
    "Ember"      -> Color(0x9E1A0E0E)
    "Purple"     -> Color(0x9E130D29)
    "Deep Sea"   -> Color(0x9E16132E)   // Frost material (62% of #16132E)
    else         -> Color(0x9E16132E)
}

val DynamicGlassFill: Color get() {
    val isLiquid = ThemeManager.glassStyle == "Liquid Crystal"
    val isDawn = ThemeManager.themeName == "Polar Dawn" || ThemeManager.themeName == "Dawn"
    return if (isLiquid) {
        if (isDawn) Color(0x73FFFFFF) else Color(0x12FFFFFF) // rgba(255,255,255,0.45) or rgba(255,255,255,0.07)
    } else {
        GlassDark
    }
}

val GlassBorder: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xB3FFFFFF)   // rgba(255,255,255,0.7)
    else                 -> Color(0x33FFFFFF)   // rgba(255,255,255,0.20)
}

val GlassShimmer: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x2B7E65FF)
    else                 -> Color(0x14FFFFFF)
}

// ─── ACCENT PRIMARIES ────────────────────────
val ElectricViolet: Color get() = ThemeManager.accentColor

val PremiumCyan: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF38E1D8)
    "Deep Sea"           -> Color(0xFF38E1D8)
    "Polar Dawn", "Dawn" -> Color(0xFF38E1D8)
    "Ember"              -> Color(0xFFFFB347)
    "Future"             -> Color(0xFF39E6FF)
    "Purple"             -> Color(0xFFFF79D4)
    else                 -> Color(0xFF38E1D8)
}

val HighlightGlow: Color get() = ElectricViolet

// ─── SCRIM & OVERLAY TOKENS ──────────────────
val ScrimColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x66000000)
    else                 -> Color(0x99000000)
}

// ─── NAVIGATION BAR ──────────────────────────
val NavBarSurface: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xF0FFFFFF)   // near-opaque white
    else                 -> Color(0xE6090818)   // near-opaque deep navy
}

val NavBarIndicator: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x1A5B21B6)
    else                 -> Color(0x338B6FFF)
}

// ─── TEXT ────────────────────────────────────
val TextPrimaryColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFF1A1830)
    else                 -> Color(0xFFEFEDFF)
}

val TextSecondaryColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFF5B5878)
    else                 -> Color(0xFF9D98C9)
}

val TextMutedColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFF5B5878)
    else                 -> Color(0xFF9D98C9)
}

val SectionLabelColor: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFFA78BFA)
    "Deep Sea"           -> Color(0xFFA78BFA)
    "Polar Dawn", "Dawn" -> Color(0xFF5B5878)
    "Ember"              -> Color(0xFFFF8A6E)
    "Future"             -> Color(0xFF5B82FF)
    "Purple"             -> Color(0xFFD98AFF)
    else                 -> Color(0xFFA78BFA)
}

val PlaceholderColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFF9080C0)
    "Void"               -> Color(0xFF404858)
    "Ember"              -> Color(0xFF584844)
    "Future"             -> Color(0x44DFF4FF)
    else                 -> Color(0x44EEEBFF)
}

// ─── SEMANTIC ────────────────────────────────
val SuccessColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFF057A55)
    "Void"               -> Color(0xFF2ECC8E)
    "Ember"              -> Color(0xFF12B87A)
    "Future"             -> Color(0xFF00FF9E)
    else                 -> Color(0xFF1EE89A)
}

val WarningColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFFAA5500)
    "Void"               -> Color(0xFFFABB20)
    "Ember"              -> Color(0xFFF5A000)
    "Future"             -> Color(0xFFFFB800)
    else                 -> Color(0xFFFFAA38)
}

val ErrorColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xFFBB1A1A)
    "Void"               -> Color(0xFFF87070)
    "Ember"              -> Color(0xFFEF4040)
    "Future"             -> Color(0xFFFF2255)
    else                 -> Color(0xFFFF5577)
}

// ─── SURFACES ────────────────────────────────
val Surface1: Color get() = RichNavy
val Surface2: Color get() = SurfaceCardColor

val Surface3: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF141416)
    "Deep Sea"           -> Color(0xFF1A1930)
    "Polar Dawn", "Dawn" -> Color(0xFFEEF1F6)
    "Ember"              -> Color(0xFF221414)
    "Future"             -> Color(0xFF10162A)
    else                 -> Color(0xFF1A1930)
}

val Surface4: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF1E1E22)
    "Deep Sea"           -> Color(0xFF20203C)
    "Polar Dawn", "Dawn" -> Color(0xFFE0E6EE)
    "Ember"              -> Color(0xFF2E1A1A)
    "Future"             -> Color(0xFF182040)
    else                 -> Color(0xFF20203C)
}

// ─── BORDERS ─────────────────────────────────
val BorderSubtle: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF1A1A1E)
    "Deep Sea"           -> Color(0x12FFFFFF)
    "Polar Dawn", "Dawn" -> Color(0xFFDDE2EA)
    "Ember"              -> Color(0xFF2A1818)
    "Future"             -> Color(0x10FFFFFF)
    else                 -> Color(0x12FFFFFF)
}

val BorderActive: Color get() = ElectricViolet

val CardBorderColor: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF222228)
    "Deep Sea"           -> BorderSubtle
    "Polar Dawn", "Dawn" -> Color(0xFFCDD4DF)
    "Ember"              -> Color(0xFF361E1E)
    "Future"             -> Color(0x1800FF7A)
    else                 -> BorderSubtle
}

// ─── INTERACTIVE PRESS STATE ─────────────────
val PressedOverlay: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x14000000)
    else                 -> Color(0x14FFFFFF)
}

// ─── BOTTOM INPUT BAR ────────────────────────
val InputBarSurface: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0xF8FFFFFF)
    else                 -> Color(0xF00C0B1E)
}

val InputBarBorder: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x228B6FFF)
    else                 -> Color(0x338B6FFF)
}

// ─── RIPPLE / CLICK FEEDBACK ─────────────────
val RippleColor: Color get() = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> Color(0x225B21B6)
    else                 -> Color(0x228B6FFF)
}

// ─── GRADIENTS (convenience stops) ───────────
val GradientStart: Color get() = ElectricViolet
val GradientEnd: Color get() = when (ThemeManager.themeName) {
    "Void"               -> Color(0xFF5B3FD6)
    "Deep Sea"           -> Color(0xFF5B3FD6)
    "Polar Dawn", "Dawn" -> Color(0xFF5B3FD6)
    "Ember"              -> Color(0xFFD63B1F)
    "Future"             -> Color(0xFF1F4FD0)
    "Purple"             -> Color(0xFF8B2FD6)
    else                 -> Color(0xFF5B3FD6)
}

// Hero ambient gradient for splash / backgrounds
val AmbientGradientTop: Color get() = RichNavy
val AmbientGradientBottom: Color get() = DeepMidnight

// ─── LAYER COLOURS (reality depth rings) ─────
fun getLayerColor(layerNumber: Int): Color = when (ThemeManager.themeName) {
    "Polar Dawn", "Dawn" -> when (layerNumber) {
        1  -> Color(0xFF0369A1)
        2  -> Color(0xFF047857)
        3  -> Color(0xFF581C87)
        4  -> Color(0xFFB91C1C)
        5  -> Color(0xFFB45309)
        6  -> Color(0xFFC2410C)
        7  -> Color(0xFF701A75)
        8  -> Color(0xFF1E3A8A)
        9  -> Color(0xFF9D174D)
        else -> Color(0xFF475569)
    }
    else -> when (layerNumber) {
        1  -> Color(0xFF00E0FF)
        2  -> Color(0xFF1EE89A)
        3  -> Color(0xFF8B6FFF)
        4  -> Color(0xFFFF5577)
        5  -> Color(0xFFFFAA38)
        6  -> Color(0xFFFF7A5C)
        7  -> Color(0xFFA855F7)
        8  -> Color(0xFF60A5FA)
        9  -> Color(0xFFF472B6)
        else -> Color(0xFFDDE4EE)
    }
}

val Layer1:  Color get() = getLayerColor(1)
val Layer2:  Color get() = getLayerColor(2)
val Layer3:  Color get() = getLayerColor(3)
val Layer4:  Color get() = getLayerColor(4)
val Layer5:  Color get() = getLayerColor(5)
val Layer6:  Color get() = getLayerColor(6)
val Layer7:  Color get() = getLayerColor(7)
val Layer8:  Color get() = getLayerColor(8)
val Layer9:  Color get() = getLayerColor(9)
val Layer10: Color get() = getLayerColor(10)

// ─── LEGACY ALIASES (keep compiling) ─────────
val Purple80:              Color get() = ElectricViolet
val PurpleGrey80:          Color get() = PremiumCyan
val Pink80:                Color get() = HighlightGlow
val Purple40:              Color get() = ElectricViolet
val PurpleGrey40:          Color get() = PremiumCyan
val Pink40:                Color get() = HighlightGlow
val ObsidianBackground:    Color get() = DeepMidnight
val DeepSurface:           Color get() = RichNavy
val TextPrimary:           Color get() = TextPrimaryColor
val TextSecondary:         Color get() = TextSecondaryColor
val SuccessGreen:          Color get() = SuccessColor
val AccentOrange:          Color get() = WarningColor
val ErrorRed:              Color get() = ErrorColor
val ToggleOnColor:         Color get() = PremiumCyan
val SidebarIconColor:      Color get() = ElectricViolet
val SidebarTextColor:      Color get() = TextPrimaryColor
val ThemeNameColor:        Color get() = PremiumCyan
val HeroSubtitleColor:     Color get() = PremiumCyan