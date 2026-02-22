package com.bhan796.anagramarena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagram.core.model.LetterKind
import com.bhan796.anagram.core.model.WordValidationFailure
import com.bhan796.anagram.core.validation.DictionaryProvider
import com.bhan796.anagramarena.audio.SoundManager
import com.bhan796.anagramarena.ui.components.ArcadeBackButton
import com.bhan796.anagramarena.ui.components.ArcadeButton
import com.bhan796.anagramarena.ui.components.ArcadeScaffold
import com.bhan796.anagramarena.ui.components.LetterTile
import com.bhan796.anagramarena.ui.components.NeonTitle
import com.bhan796.anagramarena.ui.components.TapLetterComposer
import com.bhan796.anagramarena.ui.components.TimerBar
import com.bhan796.anagramarena.ui.theme.ColorBackground
import com.bhan796.anagramarena.ui.theme.ColorCyan
import com.bhan796.anagramarena.ui.theme.ColorDimText
import com.bhan796.anagramarena.ui.theme.ColorSurface
import com.bhan796.anagramarena.ui.theme.ColorSurfaceVariant
import com.bhan796.anagramarena.ui.theme.sdp
import com.bhan796.anagramarena.viewmodel.LettersPracticePhase
import com.bhan796.anagramarena.viewmodel.LettersPracticeViewModel

@Composable
fun LettersPracticeScreen(
    contentPadding: PaddingValues,
    timerEnabled: Boolean,
    dictionaryProvider: DictionaryProvider,
    dictionaryLoaded: Boolean,
    onBack: () -> Unit
) {
    val vm: LettersPracticeViewModel = viewModel(
        key = "letters-$timerEnabled",
        factory = LettersPracticeViewModel.factory(dictionaryProvider = dictionaryProvider, timerEnabled = timerEnabled)
    )
    val state by vm.state.collectAsState()
    val allowedKinds = vm.allowedKinds

    LaunchedEffect(state.result?.validation?.isValid) {
        val valid = state.result?.validation?.isValid ?: return@LaunchedEffect
        if (valid) {
            SoundManager.playWordValid()
        } else {
            SoundManager.playWordInvalid()
        }
    }

    if (state.phase == LettersPracticePhase.SOLVING) {
        Box(
            Modifier
                .fillMaxSize()
                .background(ColorBackground)
                .padding(contentPadding)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = sdp(16.dp))
                        .padding(top = sdp(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(sdp(12.dp))
                ) {
                    ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
                    NeonTitle("SOLVE")
                    if (timerEnabled) {
                        TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = 30)
                    }
                    if (!dictionaryLoaded) {
                        Text(
                            "Dictionary data failed to load. Validation may mark all words invalid.",
                            color = ColorDimText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(topStart = sdp(16.dp), topEnd = sdp(16.dp)))
                        .border(
                            sdp(1.dp),
                            ColorCyan.copy(alpha = 0.25f),
                            RoundedCornerShape(topStart = sdp(16.dp), topEnd = sdp(16.dp))
                        )
                        .padding(sdp(16.dp))
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(sdp(10.dp))
                ) {
                    TapLetterComposer(
                        letters = state.letters,
                        value = state.submittedWord,
                        onValueChange = vm::updateSubmittedWord,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ArcadeButton(
                        "SUBMIT",
                        onClick = {
                            SoundManager.playWordSubmit()
                            vm.submit()
                        },
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = sdp(52.dp))
                    )
                }
            }
        }
        return
    }

    ArcadeScaffold(contentPadding = contentPadding) {
        ArcadeBackButton(onClick = onBack, modifier = Modifier.fillMaxWidth())

        if (!dictionaryLoaded) {
            Text(
                "Dictionary data failed to load. Validation may mark all words invalid.",
                color = ColorDimText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        when (state.phase) {
            LettersPracticePhase.PICKING -> {
                NeonTitle("LETTERS")
                Text("Progress: ${state.letters.size}/9", style = MaterialTheme.typography.headlineSmall)
                if (timerEnabled) {
                    TimerBar(secondsRemaining = state.secondsRemaining, totalSeconds = 20)
                }
                LetterSlots(letters = state.letters)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton(
                        text = "VOWEL",
                        onClick = {
                            SoundManager.playTilePlace()
                            vm.pick(LetterKind.VOWEL)
                        },
                        enabled = allowedKinds.contains(LetterKind.VOWEL),
                        modifier = Modifier.weight(1f)
                    )
                    ArcadeButton(
                        text = "CONSONANT",
                        onClick = {
                            SoundManager.playTilePlace()
                            vm.pick(LetterKind.CONSONANT)
                        },
                        enabled = allowedKinds.contains(LetterKind.CONSONANT),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Vowels: ${state.vowelCount}  Consonants: ${state.consonantCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorDimText
                )
            }
            LettersPracticePhase.SOLVING -> {}
            LettersPracticePhase.RESULT -> {
                val result = state.result
                if (result != null) {
                    NeonTitle("ROUND RESULT")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, ColorCyan.copy(0.3f), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Letters: ${result.letters.joinToString("")}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Your Word: ${if (result.submittedWord.isEmpty()) "(none)" else result.submittedWord}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (result.validation.isValid) {
                                    "Valid"
                                } else {
                                    "Invalid: ${failureText(result.validation.failure)}"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Score: ${result.validation.score}", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    ArcadeButton("PLAY ANOTHER LETTERS ROUND", onClick = vm::resetRound)
                }
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<Char>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            val letter = if (index < letters.size) letters[index].toString() else "_"
            LetterTile(letter = letter, revealed = index < letters.size, index = index)
        }
    }
}

private fun failureText(failure: WordValidationFailure?): String {
    return when (failure) {
        WordValidationFailure.EMPTY -> "No word entered"
        WordValidationFailure.NON_ALPHABETICAL -> "Only alphabetic words allowed"
        WordValidationFailure.NOT_IN_DICTIONARY -> "Word not found"
        WordValidationFailure.NOT_CONSTRUCTABLE -> "Cannot be built from letters"
        null -> "Unknown"
    }
}
