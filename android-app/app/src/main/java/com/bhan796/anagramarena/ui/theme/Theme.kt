package com.bhan796.anagramarena.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// --- Palette ---
val ColorBackground      = Color(0xFF0A0A18)   // near-black dark navy
val ColorSurface         = Color(0xFF12122A)   // slightly lighter navy for cards
val ColorSurfaceVariant  = Color(0xFF1C1C3A)   // tile/card backgrounds
val ColorCyan            = Color(0xFF00F5FF)   // primary electric cyan
val ColorGold            = Color(0xFFFFD700)   // secondary gold/yellow
val ColorMagenta         = Color(0xFFFF00CC)   // tertiary accent
val ColorGreen           = Color(0xFF39FF14)   // success / valid
val ColorRed             = Color(0xFFFF3A3A)   // error / invalid
val ColorWhite           = Color(0xFFFFFFFF)
val ColorDimText         = Color(0xFF8888AA)

private val ArcadeColorScheme = darkColorScheme(
    primary          = ColorCyan,
    onPrimary        = ColorBackground,
    primaryContainer = Color(0xFF003040),
    secondary        = ColorGold,
    onSecondary      = ColorBackground,
    tertiary         = ColorMagenta,
    background       = ColorBackground,
    onBackground     = ColorWhite,
    surface          = ColorSurface,
    onSurface        = ColorWhite,
    surfaceVariant   = ColorSurfaceVariant,
    onSurfaceVariant = ColorDimText,
    error            = ColorRed,
    onError          = ColorWhite,
    outline          = ColorCyan.copy(alpha = 0.4f),
)

@Composable
fun AnagramArenaTheme(content: @Composable () -> Unit) {
    val uiScale = rememberUiScale()
    CompositionLocalProvider(LocalUiScale provides uiScale) {
        MaterialTheme(
            colorScheme = ArcadeColorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
