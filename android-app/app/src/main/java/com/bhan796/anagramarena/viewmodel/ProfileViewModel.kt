package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.MatchHistoryResponse
import com.bhan796.anagramarena.online.PlayerStats
import com.bhan796.anagramarena.online.LeaderboardEntry
import com.bhan796.anagramarena.repository.ProfileRepository
import com.bhan796.anagramarena.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val playerId: String? = null,
    val isLoading: Boolean = false,
    val stats: PlayerStats? = null,
    val history: MatchHistoryResponse? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(playerId = sessionStore.playerId))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun refresh() {
        val playerId = sessionStore.playerId
        if (playerId.isNullOrBlank()) {
            _state.update {
                it.copy(errorMessage = "Play an online match first to create a profile.")
            }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null, playerId = playerId) }

        viewModelScope.launch {
            val statsResult = repository.loadStats(playerId)
            val historyResult = repository.loadHistory(playerId)
            val leaderboardResult = repository.loadLeaderboard(20)

            val error = statsResult.exceptionOrNull()?.message ?: historyResult.exceptionOrNull()?.message
            _state.update {
                it.copy(
                    isLoading = false,
                    stats = statsResult.getOrNull(),
                    history = historyResult.getOrNull(),
                    leaderboard = leaderboardResult.getOrDefault(emptyList()),
                    errorMessage = error
                )
            }
        }
    }

    companion object {
        fun factory(repository: ProfileRepository, sessionStore: SessionStore): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(repository, sessionStore) as T
                }
            }
        }
    }
}
