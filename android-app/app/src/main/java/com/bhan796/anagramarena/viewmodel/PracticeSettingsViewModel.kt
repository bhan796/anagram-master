package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PracticeSettingsState(
    val timerEnabled: Boolean = true
)

class PracticeSettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(PracticeSettingsState())
    val state: StateFlow<PracticeSettingsState> = _state

    fun setTimerEnabled(enabled: Boolean) {
        _state.update { it.copy(timerEnabled = enabled) }
    }
}