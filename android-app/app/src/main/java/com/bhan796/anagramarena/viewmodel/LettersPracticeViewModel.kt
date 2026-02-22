package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagram.core.engine.LetterGenerator
import com.bhan796.anagram.core.engine.PickerConstraints
import com.bhan796.anagram.core.model.LetterKind
import com.bhan796.anagram.core.model.LettersRoundResult
import com.bhan796.anagram.core.validation.DictionaryProvider
import com.bhan796.anagram.core.validation.WordValidator
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LettersPracticePhase {
    PICKING,
    SOLVING,
    RESULT
}

data class LettersPracticeUiState(
    val phase: LettersPracticePhase = LettersPracticePhase.PICKING,
    val letters: List<Char> = emptyList(),
    val vowelCount: Int = 0,
    val consonantCount: Int = 0,
    val submittedWord: String = "",
    val secondsRemaining: Int = 30,
    val result: LettersRoundResult? = null
) {
    val canPick: Boolean = phase == LettersPracticePhase.PICKING && letters.size < 9
    val canSubmit: Boolean = phase == LettersPracticePhase.SOLVING
}

class LettersPracticeViewModel(
    dictionaryProvider: DictionaryProvider,
    private val timerEnabled: Boolean,
    private val pickDuration: Int = 20,
    private val solveDuration: Int = 30,
    private val generator: LetterGenerator = LetterGenerator(),
    private val constraints: PickerConstraints = PickerConstraints(),
    private val random: Random = Random.Default
) : ViewModel() {
    private val validator = WordValidator(dictionaryProvider)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(LettersPracticeUiState(secondsRemaining = pickDuration))
    val state: StateFlow<LettersPracticeUiState> = _state

    init {
        startPickTimerIfNeeded()
    }

    val allowedKinds: Set<LetterKind>
        get() {
            val s = _state.value
            return constraints.allowedKinds(s.letters, s.vowelCount, s.consonantCount)
        }

    fun updateSubmittedWord(word: String) {
        _state.update { it.copy(submittedWord = word) }
    }

    fun pick(kind: LetterKind) {
        val current = _state.value
        if (!current.canPick || !allowedKinds.contains(kind)) return

        val letter = generator.generateLetter(kind, random)
        val nextVowels = current.vowelCount + if (kind == LetterKind.VOWEL) 1 else 0
        val nextConsonants = current.consonantCount + if (kind == LetterKind.CONSONANT) 1 else 0
        val nextLetters = current.letters + letter

        if (nextLetters.size == 9) {
            _state.update {
                it.copy(
                    phase = LettersPracticePhase.SOLVING,
                    letters = nextLetters,
                    vowelCount = nextVowels,
                    consonantCount = nextConsonants,
                    secondsRemaining = solveDuration
                )
            }
            startSolveTimerIfNeeded()
        } else {
            _state.update {
                it.copy(
                    letters = nextLetters,
                    vowelCount = nextVowels,
                    consonantCount = nextConsonants
                )
            }
        }
    }

    fun submit() {
        val current = _state.value
        if (current.phase != LettersPracticePhase.SOLVING) return

        stopTimer()
        val validation = validator.validate(current.submittedWord, current.letters)
        val result = LettersRoundResult(
            letters = current.letters,
            submittedWord = current.submittedWord,
            validation = validation
        )

        _state.update {
            it.copy(
                phase = LettersPracticePhase.RESULT,
                result = result
            )
        }
    }

    fun resetRound() {
        stopTimer()
        _state.value = LettersPracticeUiState(secondsRemaining = pickDuration)
        startPickTimerIfNeeded()
    }

    private fun startPickTimerIfNeeded() {
        if (!timerEnabled) return
        stopTimer()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.phase != LettersPracticePhase.PICKING) {
                    stopTimer()
                    break
                }

                val remaining = (current.secondsRemaining - 1).coerceAtLeast(0)
                _state.update { it.copy(secondsRemaining = remaining) }

                if (remaining == 0) {
                    autoFillAndStartSolve()
                    break
                }
            }
        }
    }

    private fun startSolveTimerIfNeeded() {
        if (!timerEnabled) return
        stopTimer()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.phase != LettersPracticePhase.SOLVING) {
                    stopTimer()
                    break
                }

                val remaining = (current.secondsRemaining - 1).coerceAtLeast(0)
                _state.update { it.copy(secondsRemaining = remaining) }

                if (remaining == 0) {
                    submit()
                    break
                }
            }
        }
    }

    private fun autoFillAndStartSolve() {
        var letters = _state.value.letters
        var vowels = _state.value.vowelCount
        var consonants = _state.value.consonantCount

        while (letters.size < 9) {
            val allowed = constraints.allowedKinds(letters, vowels, consonants).toList()
            if (allowed.isEmpty()) break
            val kind = allowed[random.nextInt(allowed.size)]
            val letter = generator.generateLetter(kind, random)
            letters = letters + letter
            if (kind == LetterKind.VOWEL) vowels += 1 else consonants += 1
        }

        _state.update {
            it.copy(
                phase = LettersPracticePhase.SOLVING,
                letters = letters,
                vowelCount = vowels,
                consonantCount = consonants,
                secondsRemaining = solveDuration
            )
        }
        startSolveTimerIfNeeded()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    companion object {
        fun factory(
            dictionaryProvider: DictionaryProvider,
            timerEnabled: Boolean
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LettersPracticeViewModel(
                        dictionaryProvider = dictionaryProvider,
                        timerEnabled = timerEnabled
                    ) as T
                }
            }
        }
    }
}
