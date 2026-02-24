package com.bhan796.anagramarena.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.LeaderboardEntry
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.CosmeticName
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.ColorWhite
import com.bhan796.anagramarena.ui.theme.sdp

@Composable
fun MatchmakingScreen(
    contentPadding: PaddingValues,
    onlineState: OnlineUiState,
    leaderboard: List<LeaderboardEntry>,
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    onJoinQueue: (String) -> Unit,
    onCancelQueue: () -> Unit,
    onRetryConnection: () -> Unit,
    onMatchReady: () -> Unit,
    onRequireAuth: () -> Unit
) {
    var hasNavigatedToMatch by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("casual") }
    val dotsAnim = rememberInfiniteTransition(label = "searchDots")
    val dotsProgress by dotsAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900)),
        label = "dotsProgress"
    )

    val searchingLabel = "SEARCHING" + ".".repeat((dotsProgress * 3).toInt() + 1)
    val hasExistingMatch = onlineState.matchState != null && onlineState.matchState.phase.name != "FINISHED"
    val isSearching = onlineState.isInMatchmaking || !onlineState.matchId.isNullOrBlank()
    val hasMatchAfterSearch = hasExistingMatch
    val primaryButtonText = when {
        hasMatchAfterSearch -> "MATCH FOUND!"
        isSearching && onlineState.isInMatchmaking -> searchingLabel
        hasExistingMatch -> "ALREADY IN MATCH"
        else -> "FIND OPPONENT"
    }
    val primaryButtonEnabled =
        !onlineState.isInMatchmaking &&
            !hasExistingMatch &&
            onlineState.connectionState is SocketConnectionState.Connected

    LaunchedEffect(hasExistingMatch) {
        if (!hasExistingMatch) {
            hasNavigatedToMatch = false
        }
    }

    LaunchedEffect(hasExistingMatch, onlineState.matchState?.matchId) {
        if (hasExistingMatch && !hasNavigatedToMatch) {
            hasNavigatedToMatch = true
            kotlinx.coroutines.delay(250)
            onMatchReady()
        }
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle("Search for opponents...")

        Text("Guest Alias", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                .border(sdp(1.dp), ColorCyan.copy(alpha = 0.55f), RoundedCornerShape(sdp(6.dp)))
                .padding(horizontal = sdp(14.dp), vertical = sdp(12.dp))
        ) {
            Text(onlineState.displayName.orEmpty(), style = MaterialTheme.typography.labelLarge, color = ColorWhite)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(sdp(10.dp))) {
            ModeOptionButton(
                text = "CASUAL",
                selected = selectedMode == "casual",
                enabled = !isSearching && !onlineState.isInMatchmaking && !hasExistingMatch,
                onClick = { selectedMode = "casual" },
                modifier = Modifier.weight(1f)
            )
            ModeOptionButton(
                text = "RANKED",
                selected = selectedMode == "ranked",
                enabled = !isSearching && !onlineState.isInMatchmaking && !hasExistingMatch && isAuthenticated,
                onClick = {
                    if (!isAuthenticated) {
                        onRequireAuth()
                    } else {
                        selectedMode = "ranked"
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        ArcadeButton(
            text = primaryButtonText,
            onClick = {
                if (selectedMode == "ranked" && !isAuthenticated) {
                    onRequireAuth()
                    return@ArcadeButton
                }
                onJoinQueue(selectedMode)
            },
            enabled = primaryButtonEnabled && !hasExistingMatch,
            modifier = Modifier.fillMaxWidth()
        )

        if (isAuthenticated) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                    .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                    .padding(sdp(12.dp))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(sdp(6.dp))) {
                    Text("LEADERBOARD", style = MaterialTheme.typography.labelLarge, color = ColorWhite)
                    if (leaderboard.isEmpty()) {
                        Text("No ranked matches yet.", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
                    } else {
                        leaderboard.take(8).forEachIndexed { index, entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(sdp(4.dp))) {
                                    Text("#${index + 1}", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
                                    CosmeticName(entry.displayName, entry.equippedCosmetic, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(entry.rating.toString(), style = MaterialTheme.typography.labelLarge, color = com.bhan796.anagramarena.ui.theme.ColorGreen)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(sdp(6.dp)))
                    .border(sdp(1.dp), ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(sdp(6.dp)))
                    .padding(sdp(12.dp))
            ) {
                Text("Sign in to unlock Ranked mode and Leaderboard.", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
            }
        }

        if (isSearching && onlineState.isInMatchmaking && onlineState.matchState == null) {
            ArcadeButton(
                text = "CANCEL SEARCH",
                onClick = {
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

@Composable
private fun ModeOptionButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) ColorCyan else ColorDimText.copy(alpha = 0.45f)
    val textColor = if (selected) ColorWhite else ColorDimText
    val background = if (selected) ColorSurfaceVariant.copy(alpha = 0.95f) else ColorSurfaceVariant.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (selected) sdp(8.dp) else sdp(0.dp),
                shape = RoundedCornerShape(sdp(6.dp)),
                clip = false
            )
            .background(background, RoundedCornerShape(sdp(6.dp)))
            .border(sdp(1.dp), borderColor, RoundedCornerShape(sdp(6.dp)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = sdp(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}
