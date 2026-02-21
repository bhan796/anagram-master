package com.bhan796.anagramarena.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme()
private val DarkScheme = darkColorScheme()

@Composable
fun AnagramArenaTheme(content: @Composable () -> Unit) {
    val colors = LightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}