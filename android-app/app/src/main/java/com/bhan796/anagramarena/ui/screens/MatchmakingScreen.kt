package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.OnlineUiState

@Composable
fun MatchmakingScreen(
    contentPadding: PaddingValues,
    onlineState: OnlineUiState,
    onJoinQueue: (String?) -> Unit,
    onCancelQueue: () -> Unit,
    onRetryConnection: () -> Unit
) {
    var displayName by remember { mutableStateOf(onlineState.displayName.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Online Matchmaking")

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Connection: ${connectionText(onlineState.connectionState)}")
        Text("Status: ${onlineState.statusMessage.ifBlank { "Idle" }}")
        Text("Queue size: ${onlineState.queueSize}")

        if (!onlineState.isInMatchmaking) {
            Button(
                onClick = { onJoinQueue(displayName.ifBlank { null }) },
                enabled = onlineState.connectionState is SocketConnectionState.Connected
            ) {
                Text("Find Opponent")
            }
        } else {
            CircularProgressIndicator()
            Button(onClick = onCancelQueue) {
                Text("Cancel Search")
            }
        }

        if (onlineState.connectionState is SocketConnectionState.Failed) {
            Button(onClick = onRetryConnection) {
                Text("Retry Connection")
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
