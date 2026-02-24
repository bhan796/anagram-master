package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.LeaderboardEntry
import com.bhan796.anagramarena.repository.ProfileRepository
import com.bhan796.anagramarena.repository.ShopRepository
import com.bhan796.anagramarena.storage.SessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeStatusUiState(
    val playersOnline: Int = 0,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val runes: Int = 0
)

class HomeStatusViewModel(
    private val repository: ProfileRepository,
    private val shopRepository: ShopRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow(HomeStatusUiState())
    val state: StateFlow<HomeStatusUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refreshNow()
                delay(10_000)
            }
        }
    }

    suspend fun refreshNow() {
        val result = repository.loadPlayersOnline()
        result.getOrNull()?.let { count ->
            _state.value = _state.value.copy(playersOnline = count)
        }
        val accessToken = sessionStore.accessToken
        if (!accessToken.isNullOrBlank()) {
            val leaderboardResult = repository.loadLeaderboard(20, accessToken)
            leaderboardResult.getOrNull()?.let { entries ->
                _state.value = _state.value.copy(leaderboard = entries)
            }
            val runesResult = shopRepository.fetchRunes()
            runesResult.getOrNull()?.let { runes ->
                _state.value = _state.value.copy(runes = runes)
            }
        } else {
            _state.value = _state.value.copy(leaderboard = emptyList(), runes = 0)
        }
    }

    companion object {
        fun factory(repository: ProfileRepository, shopRepository: ShopRepository, sessionStore: SessionStore): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeStatusViewModel(repository, shopRepository, sessionStore) as T
                }
            }
        }
    }
}
