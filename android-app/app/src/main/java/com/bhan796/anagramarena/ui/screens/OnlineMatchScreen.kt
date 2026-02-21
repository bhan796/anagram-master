package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.OnlineUiState
import com.bhan796.anagramarena.online.RoundType

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (match == null) {
            Text("Waiting for match state...")
            return@Column
        }

        Text("Online Match")
        Text("Round ${match.roundNumber} - ${match.roundType.name.lowercase()}")
        Text("Time: ${state.secondsRemaining}s")

        val me = state.myPlayer
        val opponent = state.opponentPlayer
        if (me != null && opponent != null) {
            Text("You (${me.displayName}): ${me.score}  |  Opponent (${opponent.displayName}): ${opponent.score}")
        }

        if (state.lastError != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Error: ${state.lastError.message}")
                    Button(onClick = onDismissError) {
                        Text("Dismiss")
                    }
                }
            }
        }

        when (match.phase) {
            MatchPhase.AWAITING_LETTERS_PICK -> {
                Text(if (state.isMyTurnToPick) "Your turn to pick letters" else "Opponent is picking letters")
                LetterSlots(match.letters)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPickVowel, enabled = state.isMyTurnToPick) {
                        Text("Vowel")
                    }
                    Button(onClick = onPickConsonant, enabled = state.isMyTurnToPick) {
                        Text("Consonant")
                    }
                }
            }

            MatchPhase.LETTERS_SOLVING -> {
                Text("Build your longest valid word")
                LetterSlots(match.letters)
                OutlinedTextField(
                    value = state.wordInput,
                    onValueChange = onWordChange,
                    label = { Text("Word") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.hasSubmittedWord
                )
                Button(onClick = onSubmitWord, enabled = !state.hasSubmittedWord) {
                    Text(if (state.hasSubmittedWord) "Submitted" else "Submit Word")
                }
                if (state.hasSubmittedWord) {
                    Text("Waiting for opponent or timeout...")
                }
            }

            MatchPhase.CONUNDRUM_SOLVING -> {
                Text("Conundrum")
                Text(match.scrambled?.uppercase().orEmpty())
                OutlinedTextField(
                    value = state.conundrumGuessInput,
                    onValueChange = onConundrumGuessChange,
                    label = { Text("Guess") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSubmitConundrumGuess) {
                    Text("Submit Guess")
                }
                Text("Multiple guesses allowed. Respect rate limit.")
            }

            MatchPhase.ROUND_RESULT -> {
                Text("Round Result")
                val result = match.roundResults.lastOrNull()
                if (result != null) {
                    Text("Round ${result.roundNumber} (${result.type.name.lowercase()})")
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
                Text("Next round starting soon...")
            }

            MatchPhase.FINISHED -> {
                Text("Final Result")
                Text(
                    when {
                        match.winnerPlayerId == null -> "Draw"
                        match.winnerPlayerId == state.playerId -> "You Win"
                        else -> "You Lose"
                    }
                )
                for (result in match.roundResults) {
                    Text("R${result.roundNumber} ${result.type.name.lowercase()} -> ${result.awardedScores}")
                }
                Button(onClick = onBackToHome) {
                    Text("Back Home")
                }
            }

            MatchPhase.UNKNOWN -> {
                Text("Unknown match phase")
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            Card {
                Text(
                    text = if (index < letters.size) letters[index] else "_",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                )
            }
        }
    }
}
