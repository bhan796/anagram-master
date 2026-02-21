package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagramarena.data.ConundrumProvider
import com.bhan796.anagramarena.viewmodel.ConundrumPracticePhase
import com.bhan796.anagramarena.viewmodel.ConundrumPracticeViewModel

@Composable
fun ConundrumPracticeScreen(
    contentPadding: PaddingValues,
    timerEnabled: Boolean,
    provider: ConundrumProvider
) {
    val vm: ConundrumPracticeViewModel = viewModel(
        key = "conundrum-$timerEnabled",
        factory = ConundrumPracticeViewModel.factory(provider = provider, timerEnabled = timerEnabled)
    )
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state.phase) {
            ConundrumPracticePhase.READY -> {
                Text("No conundrum data available.")
                Button(onClick = vm::startRound) {
                    Text("Retry")
                }
            }

            ConundrumPracticePhase.SOLVING -> {
                Text("Conundrum")
                Text("Time: ${state.secondsRemaining}s")
                Text(state.conundrum?.scrambled?.uppercase().orEmpty())

                OutlinedTextField(
                    value = state.guess,
                    onValueChange = vm::updateGuess,
                    label = { Text("Your guess") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = vm::submit, enabled = state.canSubmit) {
                    Text("Submit Guess")
                }
            }

            ConundrumPracticePhase.RESULT -> {
                val result = state.result
                if (result != null) {
                    Text("Conundrum Result")
                    Text("Scramble: ${result.conundrum.scrambled.uppercase()}")
                    Text("Your Guess: ${if (result.submittedGuess.isEmpty()) "(none)" else result.submittedGuess}")
                    Text("Answer: ${result.conundrum.answer}")
                    Text(if (result.solved) "Solved" else "Not Solved")
                    Text("Score: ${result.score}")

                    Button(onClick = vm::startRound) {
                        Text("Try Another Conundrum")
                    }
                }
            }
        }
    }
}