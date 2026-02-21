package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagramarena.data.ConundrumProvider
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.TimerBar
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorGold
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
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

    ArcadeScaffold(contentPadding = contentPadding) {
        when (state.phase) {
            ConundrumPracticePhase.READY -> {
                NeonTitle("CONUNDRUM")
                Text("No conundrum data available.", style = MaterialTheme.typography.bodyMedium)
                ArcadeButton("RETRY", onClick = vm::startRound)
            }

            ConundrumPracticePhase.SOLVING -> {
                NeonTitle("CONUNDRUM")
                TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = 30)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = state.conundrum?.scrambled?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.displaySmall,
                        color = ColorGold,
                        letterSpacing = 8.sp
                    )
                }

                OutlinedTextField(
                    value = state.guess,
                    onValueChange = vm::updateGuess,
                    label = { Text("Your guess") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )

                ArcadeButton("SUBMIT GUESS", onClick = vm::submit, enabled = state.canSubmit)
            }

            ConundrumPracticePhase.RESULT -> {
                val result = state.result
                if (result != null) {
                    NeonTitle("RESULT")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, ColorCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Scramble: ${result.conundrum.scrambled.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Your Guess: ${if (result.submittedGuess.isEmpty()) "(none)" else result.submittedGuess}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Answer: ${result.conundrum.answer}", style = MaterialTheme.typography.bodyMedium)
                            Text(if (result.solved) "Solved" else "Not Solved", style = MaterialTheme.typography.bodyMedium)
                            Text("Score: ${result.score}", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    ArcadeButton("TRY ANOTHER CONUNDRUM", onClick = vm::startRound)
                }
            }
        }
    }
}
