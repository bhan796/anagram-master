package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonDivider
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.RankBadge
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        NeonTitle(state.stats?.displayName ?: "PLAYER")

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        if (state.errorMessage != null) {
            Text("Error: ${state.errorMessage}", style = MaterialTheme.typography.bodyMedium)
            ArcadeButton(
                text = "RETRY",
                onClick = viewModel::refresh,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val stats = state.stats
        if (stats != null) {
            StatRow("Matches Played", stats.matchesPlayed.toString())
            NeonDivider()
            StatRow("Wins", stats.wins.toString())
            NeonDivider()
            StatRow("Losses", stats.losses.toString())
            NeonDivider()
            StatRow("Draws", stats.draws.toString())
            NeonDivider()
            StatRow("Total Score", stats.totalScore.toString())
            NeonDivider()
            StatRow("Rating", stats.rating.toString())
            NeonDivider()
            StatRow("Peak Rating", stats.peakRating.toString())
            NeonDivider()
            StatRow("Ranked W-L-D", "${stats.rankedWins}-${stats.rankedLosses}-${stats.rankedDraws}")
            RankBadge(tier = stats.rankTier)
        }

        val history = state.history
        if (history != null) {
            Text("RECENT MATCHES (${history.count})", style = MaterialTheme.typography.headlineSmall)
            if (history.count == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No matches played yet", style = MaterialTheme.typography.bodyMedium)
                        Text("Play an online match to see history and stats.", style = MaterialTheme.typography.bodySmall, color = ColorDimText)
                    }
                }
            } else {
                history.matches.take(5).forEach { match ->
                    val myScore = match.players.firstOrNull { it.playerId == state.playerId }?.score ?: 0
                    val opp = match.players.firstOrNull { it.playerId != state.playerId }
                    val oppScore = opp?.score ?: 0
                    val outcomeLabel = when {
                        match.winnerPlayerId == null -> "DRAW"
                        match.winnerPlayerId == state.playerId -> "WIN"
                        else -> "LOSS"
                    }
                    val outcomeColor = when (outcomeLabel) {
                        "WIN" -> ColorCyan
                        "LOSS" -> com.bhan796.anagramarena.ui.theme.ColorRed
                        else -> ColorDimText
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(match.matchId.take(8), style = MaterialTheme.typography.labelMedium, color = ColorDimText)
                                Text(outcomeLabel, style = MaterialTheme.typography.labelMedium, color = outcomeColor)
                            }
                            Text("You $myScore - Opponent $oppScore", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ColorDimText)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = ColorCyan)
    }
}
