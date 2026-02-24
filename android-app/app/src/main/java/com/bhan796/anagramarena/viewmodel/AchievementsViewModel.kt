package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.AchievementEntry
import com.bhan796.anagramarena.repository.AchievementsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AchievementsUiState(
    val achievements: List<AchievementEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AchievementsViewModel(private val repo: AchievementsRepository) : ViewModel() {
    private val _state = MutableStateFlow(AchievementsUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repo.fetchAchievements()
            _state.update { current ->
                result.getOrNull()?.let {
                    current.copy(achievements = it, isLoading = false)
                } ?: current.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    companion object {
        fun factory(repo: AchievementsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AchievementsViewModel(repo) as T
                }
            }
        }
    }
}