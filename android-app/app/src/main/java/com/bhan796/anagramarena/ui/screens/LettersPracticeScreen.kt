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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhan796.anagram.core.model.LetterKind
import com.bhan796.anagram.core.model.WordValidationFailure
import com.bhan796.anagram.core.validation.DictionaryProvider
import com.bhan796.anagramarena.viewmodel.LettersPracticePhase
import com.bhan796.anagramarena.viewmodel.LettersPracticeViewModel

@Composable
fun LettersPracticeScreen(
    contentPadding: PaddingValues,
    timerEnabled: Boolean,
    dictionaryProvider: DictionaryProvider,
    dictionaryLoaded: Boolean
) {
    val vm: LettersPracticeViewModel = viewModel(
        key = "letters-$timerEnabled",
        factory = LettersPracticeViewModel.factory(dictionaryProvider = dictionaryProvider, timerEnabled = timerEnabled)
    )
    val state by vm.state.collectAsState()
    val allowedKinds = vm.allowedKinds

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!dictionaryLoaded) {
            Text("Dictionary data failed to load. Validation may mark all words invalid.")
        }

        when (state.phase) {
            LettersPracticePhase.PICKING -> {
                Text("Pick 9 Letters")
                Text("Progress: ${state.letters.size}/9")
                LetterSlots(letters = state.letters)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { vm.pick(LetterKind.VOWEL) },
                        enabled = allowedKinds.contains(LetterKind.VOWEL)
                    ) {
                        Text("Vowel")
                    }
                    Button(
                        onClick = { vm.pick(LetterKind.CONSONANT) },
                        enabled = allowedKinds.contains(LetterKind.CONSONANT)
                    ) {
                        Text("Consonant")
                    }
                }
                Text("Vowels: ${state.vowelCount}  Consonants: ${state.consonantCount}")
            }

            LettersPracticePhase.SOLVING -> {
                Text("Solve")
                Text("Time: ${state.secondsRemaining}s")
                LetterSlots(letters = state.letters)
                OutlinedTextField(
                    value = state.submittedWord,
                    onValueChange = vm::updateSubmittedWord,
                    label = { Text("Enter your word") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = vm::submit, enabled = state.canSubmit) {
                    Text("Submit")
                }
            }

            LettersPracticePhase.RESULT -> {
                val result = state.result
                if (result != null) {
                    Text("Round Result")
                    Text("Letters: ${result.letters.joinToString("")}")
                    Text("Your Word: ${if (result.submittedWord.isEmpty()) "(none)" else result.submittedWord}")
                    Text(
                        text = if (result.validation.isValid) {
                            "Valid"
                        } else {
                            "Invalid: ${failureText(result.validation.failure)}"
                        }
                    )
                    Text("Score: ${result.validation.score}")
                    Button(onClick = vm::resetRound) {
                        Text("Play Another Letters Round")
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterSlots(letters: List<Char>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(9) { index ->
            Card {
                Text(
                    text = if (index < letters.size) letters[index].toString() else "_",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                )
            }
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
