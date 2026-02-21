package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun MatchmakingScreen(
    contentPadding: PaddingValues,
    onlineState: OnlineUiState,
    onBack: () -> Unit,
    onJoinQueue: () -> Unit,
    onCancelQueue: () -> Unit,
    onRetryConnection: () -> Unit,
    onMatchReady: () -> Unit
) {
    var searchStarted by remember { mutableStateOf(false) }
    val dotsAnim = rememberInfiniteTransition(label = "searchDots")
    val dotsProgress by dotsAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900)),
        label = "dotsProgress"
    )

    val searchingLabel = "SEARCHING" + ".".repeat((dotsProgress * 3).toInt() + 1)
    val hasExistingMatch = onlineState.matchState != null
    val hasMatchAfterSearch = searchStarted && hasExistingMatch
    val primaryButtonText = when {
        hasMatchAfterSearch -> "MATCH FOUND!"
        searchStarted && onlineState.isInMatchmaking -> searchingLabel
        hasExistingMatch -> "ALREADY IN MATCH"
        else -> "FIND OPPONENT"
    }
    val primaryButtonEnabled =
        !searchStarted &&
            !onlineState.isInMatchmaking &&
            !hasExistingMatch &&
            onlineState.connectionState is SocketConnectionState.Connected

    LaunchedEffect(searchStarted, onlineState.matchState?.matchId) {
        if (searchStarted && onlineState.matchState != null) {
            kotlinx.coroutines.delay(1000)
            onMatchReady()
            searchStarted = false
        }
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("Search for opponents...")

        Text("Guest Alias")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.55f), RoundedCornerShape(sdp(6.dp)))
                .padding(horizontal = sdp(14.dp), vertical = sdp(12.dp))
        ) {
            Text(onlineState.displayName.orEmpty())
        }

        ArcadeButton(
            text = primaryButtonText,
            onClick = {
                searchStarted = true
                onJoinQueue()
            },
            enabled = primaryButtonEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        if (searchStarted && onlineState.isInMatchmaking && onlineState.matchState == null) {
            ArcadeButton(
                text = "CANCEL SEARCH",
                onClick = {
                    searchStarted = false
                    onCancelQueue()
                },
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
