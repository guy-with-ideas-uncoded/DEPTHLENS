package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF7B5CF5),     // Electric Violet
    secondary = Color(0xFF00E5FF),   // Premium Cyan
    tertiary = Color(0xFF9D7FFF),     // Glowing Highlight
    background = Color(0xFF04030F),   // Primary Deep Midnight
    surface = Color(0xFF080618),      // Secondary Background
    onPrimary = Color.White,
    onSecondary = Color(0xFF0D0C22),
    onBackground = Color(0xFFF0EEFF), // Text Primary
    onSurface = Color(0xFFF0EEFF),    // Text Surface Primary
    outline = Color(0xFF0D0C22)       // Card Border/Surface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF7B5CF5),     // Electric Violet
    secondary = Color(0xFF0891B2),   // Accent: #0891B2
    tertiary = Color(0xFF0F766E),     // Accent Dark
    background = Color(0xFFFFFFFF),   // Primary Polar Dawn background
    surface = Color(0xFFF8FAFC),      // Surface background
    onPrimary = Color.White,
    onSecondary = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A), // Primary Text Dark: #0F172A
    onSurface = Color(0xFF0F172A),    // Primary Text Dark: #0F172A
    outline = Color(0xFFCBD5E1)       // Border: #CBD5E1
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = ThemeManager.isDarkTheme,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography.scaled(TypographyManager.currentScale),
    content = content
  )
}
