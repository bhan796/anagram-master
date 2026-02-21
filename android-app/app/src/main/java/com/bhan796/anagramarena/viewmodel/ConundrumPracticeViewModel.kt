package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagram.core.model.Conundrum
import com.bhan796.anagram.core.model.ConundrumRoundResult
import com.bhan796.anagram.core.validation.ConundrumValidator
import com.bhan796.anagramarena.data.ConundrumProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConundrumPracticePhase {
    READY,
    SOLVING,
    RESULT
}

data class ConundrumPracticeUiState(
    val phase: ConundrumPracticePhase = ConundrumPracticePhase.READY,
    val conundrum: Conundrum? = null,
    val guess: String = "",
    val secondsRemaining: Int = 30,
    val result: ConundrumRoundResult? = null
) {
    val canSubmit: Boolean = phase == ConundrumPracticePhase.SOLVING && conundrum != null
}

class ConundrumPracticeViewModel(
    private val provider: ConundrumProvider,
    private val timerEnabled: Boolean,
    private val roundDuration: Int = 30,
    private val validator: ConundrumValidator = ConundrumValidator()
) : ViewModel() {
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(ConundrumPracticeUiState(secondsRemaining = roundDuration))
    val state: StateFlow<ConundrumPracticeUiState> = _state

    init {
        startRound()
    }

    fun updateGuess(value: String) {
        _state.update { it.copy(guess = value) }
    }

    fun startRound() {
        stopTimer()
        val conundrum = provider.randomConundrum()

        _state.value = ConundrumPracticeUiState(
            phase = if (conundrum == null) ConundrumPracticePhase.READY else ConundrumPracticePhase.SOLVING,
            conundrum = conundrum,
            guess = "",
            secondsRemaining = roundDuration,
            result = null
        )

        if (timerEnabled && conundrum != null) {
            startTimer()
        }
    }

    fun submit() {
        val current = _state.value
        val conundrum = current.conundrum ?: return
        if (current.phase != ConundrumPracticePhase.SOLVING) return

        stopTimer()
        val solved = validator.isCorrect(current.guess, conundrum.answer)
        val result = ConundrumRoundResult(
            conundrum = conundrum,
            submittedGuess = current.guess,
            solved = solved,
            score = if (solved) 12 else 0
        )

        _state.update {
            it.copy(phase = ConundrumPracticePhase.RESULT, result = result)
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.phase != ConundrumPracticePhase.SOLVING) {
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

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    companion object {
        fun factory(provider: ConundrumProvider, timerEnabled: Boolean): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConundrumPracticeViewModel(provider = provider, timerEnabled = timerEnabled) as T
                }
            }
        }
    }
}