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
    val vibrationEnabled: Boolean = true,
    val masterMuted: Boolean = false,
    val musicEnabled: Boolean = true,
    val uiSfxEnabled: Boolean = true,
    val gameSfxEnabled: Boolean = true,
    val musicVolume: Float = 0.5f,
    val uiSfxVolume: Float = 0.8f,
    val gameSfxVolume: Float = 0.85f
)

class PracticeSettingsViewModel(private val settingsStore: AppSettingsStore) : ViewModel() {
    private val _state = MutableStateFlow(
        PracticeSettingsState(
            timerEnabled = settingsStore.timerEnabled,
            soundEnabled = settingsStore.soundEnabled,
            vibrationEnabled = settingsStore.vibrationEnabled,
            masterMuted = settingsStore.masterMuted,
            musicEnabled = settingsStore.musicEnabled,
            uiSfxEnabled = settingsStore.uiSfxEnabled,
            gameSfxEnabled = settingsStore.gameSfxEnabled,
            musicVolume = settingsStore.musicVolume,
            uiSfxVolume = settingsStore.uiSfxVolume,
            gameSfxVolume = settingsStore.gameSfxVolume
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

    fun setMasterMuted(muted: Boolean) {
        settingsStore.masterMuted = muted
        _state.update { it.copy(masterMuted = muted) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        settingsStore.musicEnabled = enabled
        _state.update { it.copy(musicEnabled = enabled) }
    }

    fun setUiSfxEnabled(enabled: Boolean) {
        settingsStore.uiSfxEnabled = enabled
        _state.update { it.copy(uiSfxEnabled = enabled) }
    }

    fun setGameSfxEnabled(enabled: Boolean) {
        settingsStore.gameSfxEnabled = enabled
        _state.update { it.copy(gameSfxEnabled = enabled) }
    }

    fun setMusicVolume(volume: Float) {
        settingsStore.musicVolume = volume
        _state.update { it.copy(musicVolume = volume.coerceIn(0f, 1f)) }
    }

    fun setUiSfxVolume(volume: Float) {
        settingsStore.uiSfxVolume = volume
        _state.update { it.copy(uiSfxVolume = volume.coerceIn(0f, 1f)) }
    }

    fun setGameSfxVolume(volume: Float) {
        settingsStore.gameSfxVolume = volume
        _state.update { it.copy(gameSfxVolume = volume.coerceIn(0f, 1f)) }
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
