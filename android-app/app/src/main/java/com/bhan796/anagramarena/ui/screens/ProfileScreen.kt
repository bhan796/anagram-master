package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagramarena.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile & Stats")

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        if (state.errorMessage != null) {
            Text("Error: ${state.errorMessage}")
            Button(onClick = viewModel::refresh) {
                Text("Retry")
            }
        }

        val stats = state.stats
        if (stats != null) {
            Text("Player: ${stats.displayName}")
            Text("Player ID: ${stats.playerId}")
            Text("Matches: ${stats.matchesPlayed}")
            Text("Wins: ${stats.wins}  Losses: ${stats.losses}  Draws: ${stats.draws}")
            Text("Total Score: ${stats.totalScore}")
            Text("Average Score: ${stats.averageScore}")
        }

        val history = state.history
        if (history != null) {
            Text("Recent Matches (${history.count})")
            history.matches.take(5).forEach { match ->
                val myScore = match.players.firstOrNull { it.playerId == state.playerId }?.score ?: 0
                val opp = match.players.firstOrNull { it.playerId != state.playerId }
                val oppScore = opp?.score ?: 0
                Text("${match.matchId.take(8)}: You $myScore - Opponent $oppScore")
            }
        }
    }
}
