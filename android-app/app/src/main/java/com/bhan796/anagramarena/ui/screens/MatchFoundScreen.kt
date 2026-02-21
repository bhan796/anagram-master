package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.RankBadge
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun MatchFoundScreen(contentPadding: PaddingValues, state: OnlineUiState, onDone: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "matchFoundPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        onDone()
    }

    val me = state.myPlayer
    val opp = state.opponentPlayer

    ArcadeScaffold(contentPadding = contentPadding) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(8.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.5f), RoundedCornerShape(sdp(8.dp)))
                .padding(sdp(16.dp)),
            verticalArrangement = Arrangement.spacedBy(sdp(12.dp))
        ) {
            NeonTitle("MATCH FOUND")
            Text("${me?.displayName ?: "You"} VS ${opp?.displayName ?: "Opponent"}", style = MaterialTheme.typography.headlineSmall)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                    Text("YOU", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                    Text((me?.rating ?: 1000).toString(), style = MaterialTheme.typography.headlineMedium, color = ColorCyan)
                    RankBadge(tier = me?.rankTier ?: "silver")
                }
                Column(verticalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                    Text("OPP", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                    Text((opp?.rating ?: 1000).toString(), style = MaterialTheme.typography.headlineMedium, color = ColorGold)
                    RankBadge(tier = opp?.rankTier ?: "silver")
                }
            }

            Text("Entering arena in 10 seconds...", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
        }
    }
}
