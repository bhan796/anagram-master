package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText

@Composable
fun MatchmakingScreen(
    contentPadding: PaddingValues,
    onlineState: OnlineUiState,
    onBack: () -> Unit,
    onJoinQueue: (String?) -> Unit,
    onCancelQueue: () -> Unit,
    onRetryConnection: () -> Unit
) {
    var displayName by remember { mutableStateOf(onlineState.displayName.orEmpty()) }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("SEARCHING...")

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (optional)") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        PulsingDots(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

        Text(
            text = "Connection: ${connectionText(onlineState.connectionState)}",
            style = MaterialTheme.typography.labelMedium,
            color = ColorDimText
        )
        Text(
            text = "Status: ${onlineState.statusMessage.ifBlank { "Idle" }}",
            style = MaterialTheme.typography.labelMedium,
            color = ColorDimText
        )
        Text(
            text = "Queue size: ${onlineState.queueSize}",
            style = MaterialTheme.typography.labelMedium,
            color = ColorDimText
        )

        if (!onlineState.isInMatchmaking) {
            ArcadeButton(
                text = "FIND OPPONENT",
                onClick = { onJoinQueue(displayName.ifBlank { null }) },
                enabled = onlineState.connectionState is SocketConnectionState.Connected,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ArcadeButton(
                text = "CANCEL SEARCH",
                onClick = onCancelQueue,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (onlineState.connectionState is SocketConnectionState.Failed) {
            ArcadeButton(
                text = "RETRY CONNECTION",
                onClick = onRetryConnection,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            val alpha by infinite.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(ColorCyan)
            )

            if (index < 2) {
                Box(modifier = Modifier.size(8.dp))
            }
        }
    }
}

private fun connectionText(state: SocketConnectionState): String = when (state) {
    SocketConnectionState.Connected -> "Connected"
    SocketConnectionState.Connecting -> "Connecting"
    SocketConnectionState.Disconnected -> "Disconnected"
    is SocketConnectionState.Failed -> "Failed: ${state.reason}"
    is SocketConnectionState.Reconnecting -> "Reconnecting (${state.attempt})"
}
