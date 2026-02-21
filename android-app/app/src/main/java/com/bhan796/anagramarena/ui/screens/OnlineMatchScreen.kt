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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.online.RoundType
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.LetterTile
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.ScoreBadge
import com.bhan796.anagramarena.ui.components.TimerBar
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorRed
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant

@Composable
fun OnlineMatchScreen(
    contentPadding: PaddingValues,
    state: OnlineUiState,
    onPickVowel: () -> Unit,
    onPickConsonant: () -> Unit,
    onWordChange: (String) -> Unit,
    onSubmitWord: () -> Unit,
    onConundrumGuessChange: (String) -> Unit,
    onSubmitConundrumGuess: () -> Unit,
    onDismissError: () -> Unit,
    onBackToHome: () -> Unit
) {
    val match = state.matchState

    ArcadeScaffold(contentPadding = contentPadding) {
        if (match == null) {
            NeonTitle("MATCH")
            Text("Waiting for match state...", style = MaterialTheme.typography.bodyMedium)
            return@ArcadeScaffold
        }

        NeonTitle("ROUND ${match.roundNumber}")
        NeonTitle(match.phase.name.replace('_', ' '))
        TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = 30)
        Text(state.statusMessage, style = MaterialTheme.typography.labelMedium, color = ColorDimText)

        val me = state.myPlayer
        val opponent = state.opponentPlayer
        if (me != null && opponent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreBadge(label = me.displayName, score = me.score, color = ColorCyan)
                ScoreBadge(label = opponent.displayName, score = opponent.score, color = ColorGold)
            }
        }

        if (state.lastError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, ColorRed, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Error: ${state.lastError.message}", style = MaterialTheme.typography.bodyMedium)
                    ArcadeButton(
                        text = "DISMISS",
                        onClick = onDismissError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.localValidationMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Text(text = state.localValidationMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }

        when (match.phase) {
            MatchPhase.AWAITING_LETTERS_PICK -> {
                Text(
                    if (state.isMyTurnToPick) "YOUR TURN TO PICK" else "OPPONENT IS PICKING",
                    style = MaterialTheme.typography.headlineSmall
                )
                LetterSlots(match.letters)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcadeButton(
                        text = "VOWEL",
                        onClick = onPickVowel,
                        enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                        modifier = Modifier.weight(1f)
                    )
                    ArcadeButton(
                        text = "CONSONANT",
                        onClick = onPickConsonant,
                        enabled = state.isMyTurnToPick && state.connectionState !is SocketConnectionState.Reconnecting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            MatchPhase.LETTERS_SOLVING -> {
                Text("Build your longest valid word", style = MaterialTheme.typography.headlineSmall)
                LetterSlots(match.letters)
                OutlinedTextField(
                    value = state.wordInput,
                    onValueChange = onWordChange,
                    label = { Text("Word") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.hasSubmittedWord
                )
                ArcadeButton(
                    text = if (state.hasSubmittedWord) "SUBMITTED" else "SUBMIT WORD",
                    onClick = onSubmitWord,
                    enabled = !state.hasSubmittedWord,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.hasSubmittedWord) {
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ColorCyan,
                        trackColor = ColorSurfaceVariant
                    )
                    Text("Waiting for opponent or timeout...", style = MaterialTheme.typography.labelMedium)
                }
            }

            MatchPhase.CONUNDRUM_SOLVING -> {
                NeonTitle("CONUNDRUM")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = match.scrambled?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.displaySmall,
                        color = ColorGold,
                        letterSpacing = 8.sp
                    )
                }
                OutlinedTextField(
                    value = state.conundrumGuessInput,
                    onValueChange = onConundrumGuessChange,
                    label = { Text("Guess") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
                ArcadeButton(
                    text = "SUBMIT GUESS",
                    onClick = onSubmitConundrumGuess,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Multiple guesses allowed. Respect rate limit.", style = MaterialTheme.typography.labelMedium, color = ColorDimText)
            }

            MatchPhase.ROUND_RESULT -> {
                NeonTitle("ROUND RESULT")
                val result = match.roundResults.lastOrNull()
                if (result != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Round ${result.roundNumber} (${result.type.name.lowercase()})", style = MaterialTheme.typography.bodyMedium)
                            if (result.type == RoundType.LETTERS) {
                                Text("Letters: ${result.letters?.joinToString(separator = "").orEmpty()}")
                                val myId = state.playerId
                                val mySubmission = myId?.let { result.submissions?.get(it) }
                                Text("Your word: ${mySubmission?.word ?: "(none)"}")
                                Text("Valid: ${mySubmission?.isValid ?: false}, Score: ${mySubmission?.score ?: 0}")
                            } else {
                                Text("Scramble: ${result.scrambled.orEmpty()}")
                                Text("Answer: ${result.answer.orEmpty()}")
                                Text("First solver: ${result.firstCorrectPlayerId ?: "none"}")
                            }
                        }
                    }
                }
                Text("Next round starting soon...", style = MaterialTheme.typography.labelMedium)
            }

            MatchPhase.FINISHED -> {
                NeonTitle("FINAL RESULT")
                Text(
                    text = when {
                        match.winnerPlayerId == null -> "Draw"
                        match.winnerPlayerId == state.playerId -> "You Win"
                        else -> "You Lose"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                match.roundResults.forEach { result ->
                    Text("R${result.roundNumber} ${result.type.name.lowercase()} -> ${result.awardedScores}")
                }
                ArcadeButton(
                    text = "BACK HOME",
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MatchPhase.UNKNOWN -> {
                Text("Unknown match phase", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            val letter = if (index < letters.size) letters[index] else "_"
            LetterTile(letter = letter, revealed = index < letters.size, index = index)
        }
    }
}
