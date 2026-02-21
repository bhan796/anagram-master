package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bhan796.anagramarena.storage.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PracticeSettingsState(
    val timerEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

class PracticeSettingsViewModel(private val settingsStore: AppSettingsStore) : ViewModel() {
    private val _state = MutableStateFlow(
        PracticeSettingsState(
            timerEnabled = settingsStore.timerEnabled,
            soundEnabled = settingsStore.soundEnabled,
            vibrationEnabled = settingsStore.vibrationEnabled
        )
    )
    val state: StateFlow<PracticeSettingsState> = _state

    fun setTimerEnabled(enabled: Boolean) {
        settingsStore.timerEnabled = enabled
        _state.update { it.copy(timerEnabled = enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        settingsStore.soundEnabled = enabled
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        settingsStore.vibrationEnabled = enabled
        _state.update { it.copy(vibrationEnabled = enabled) }
    }

    companion object {
        fun factory(settingsStore: AppSettingsStore): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PracticeSettingsViewModel(settingsStore) as T
                }
            }
        }
    }
}
