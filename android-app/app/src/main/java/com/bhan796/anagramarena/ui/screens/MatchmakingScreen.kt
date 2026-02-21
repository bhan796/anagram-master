package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle

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
    val dotsAnim = rememberInfiniteTransition(label = "searchDots")
    val dotsProgress by dotsAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900)),
        label = "dotsProgress"
    )

    val searchingLabel = "SEARCHING" + ".".repeat((dotsProgress * 3).toInt() + 1)
    val primaryButtonText = when {
        onlineState.matchState != null -> "MATCH FOUND!"
        onlineState.isInMatchmaking -> searchingLabel
        else -> "FIND OPPONENT"
    }
    val primaryButtonEnabled =
        !onlineState.isInMatchmaking &&
            onlineState.matchState == null &&
            onlineState.connectionState is SocketConnectionState.Connected

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("Search for opponents...")

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (optional)") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        ArcadeButton(
            text = primaryButtonText,
            onClick = { onJoinQueue(displayName.ifBlank { null }) },
            enabled = primaryButtonEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        if (onlineState.isInMatchmaking && onlineState.matchState == null) {
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
