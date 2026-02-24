package com.bhan796.anagramarena.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

@Composable
fun CosmeticName(
    displayName: String,
    equippedCosmetic: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current
) {
    val transition = rememberInfiniteTransition(label = "cosmeticName")
    val pulseAlpha = transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    ).value
    val waveColor = transition.animateColor(
        initialValue = Color.Cyan,
        targetValue = Color(0xFF4488FF),
        animationSpec = infiniteRepeatable(animation = tween(4000), repeatMode = RepeatMode.Reverse),
        label = "waveColor"
    ).value
    val hue = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = FastOutLinearInEasing)),
        label = "hue"
    ).value

    when (equippedCosmetic) {
        null, "" -> Text(displayName, modifier = modifier, style = style)
        "c_silver" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFFC0C0C0))
        "c_mint" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFFAAFFDD))
        "c_crimson" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFFFF4444))
        "c_amber" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFFFFB300))
        "u_royal" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFF4488FF))
        "u_violet" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFF9933FF))
        "u_forest" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFF39FF14))
        "r_electric" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFF0088FF))
        "e_magenta" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFFFF00CC))
        "l_obsidian" -> Text(displayName, modifier = modifier, style = style, color = Color(0xFF220033))
        "c_pulse", "r_neon_green" -> Text(displayName, modifier = modifier.alpha(pulseAlpha), style = style, color = if (equippedCosmetic == "r_neon_green") Color(0xFF39FF14) else Color.Cyan)
        "u_glow" -> Text(displayName, modifier = modifier, style = style.copy(shadow = Shadow(Color.White, blurRadius = 12f)), color = Color.White)
        "u_wave" -> Text(displayName, modifier = modifier, style = style, color = waveColor)
        "r_spectrum", "l_holo", "m_prism" -> Text(displayName, modifier = modifier, style = style, color = Color.hsv(hue, 1f, 1f))
        "r_dual" -> Text(displayName, modifier = modifier, style = style.copy(brush = Brush.linearGradient(listOf(Color.Cyan, Color(0xFFFFD700)))))
        "l_crown" -> Text("CROWN $displayName", modifier = modifier, style = style, color = Color(0xFFFFD700))
        "m_void" -> Text(displayName, modifier = modifier, style = style.copy(shadow = Shadow(Color(0xFF9900FF), blurRadius = 18f)), color = Color(0xFFCC66FF))
        "m_matrix" -> Text(displayName, modifier = modifier, style = style, color = if (pulseAlpha > 0.85f) Color(0xFF39FF14) else Color(0xFF005500))
        "m_phoenix", "l_solar" -> Text(displayName, modifier = modifier, style = style, color = Color.hsv((hue % 40f), 1f, 1f))
        "l_aurora", "m_aurora_ex", "l_ice", "e_gold_rush", "e_glitch", "e_plasma", "r_flicker" ->
            Text(displayName, modifier = modifier, style = style, color = Color.hsv(hue, 0.8f, 1f))
        else -> Text(displayName, modifier = modifier, style = style)
    }
}
