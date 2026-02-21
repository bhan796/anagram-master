package com.bhan796.anagramarena.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val LocalUiScale = staticCompositionLocalOf { 1f }

@Composable
fun rememberUiScale(): Float {
    val config = LocalConfiguration.current
    val widthScale = config.screenWidthDp / 411f
    val heightScale = config.screenHeightDp / 891f
    return minOf(widthScale, heightScale).coerceIn(0.82f, 1.20f)
}

@Composable
fun sdp(value: Dp): Dp = value * LocalUiScale.current

@Composable
fun ssp(value: TextUnit): TextUnit = if (value.isSp) (value.value * LocalUiScale.current).sp else value
