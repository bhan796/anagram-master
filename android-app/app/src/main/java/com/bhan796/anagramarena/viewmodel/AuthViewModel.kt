package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.repository.AuthRepository
import com.bhan796.anagramarena.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val status: String = "guest",
    val userId: String? = null,
    val email: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow(
        AuthUiState(
            status = if (!sessionStore.accessToken.isNullOrBlank()) "authenticated" else "guest",
            userId = sessionStore.authUserId,
            email = sessionStore.authEmail
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun bootstrap() {
        val accessToken = sessionStore.accessToken
        val refreshToken = sessionStore.refreshToken
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            clearSession()
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val meResult = repository.me(accessToken)
            if (meResult.isSuccess) {
                val (userId, email) = meResult.getOrThrow()
                persistSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = email
                )
                return@launch
            }

            val refresh = repository.refresh()
            if (refresh.isFailure) {
                clearSession()
                return@launch
            }

            val session = refresh.getOrThrow()
            val meRetry = repository.me(session.accessToken)
            if (meRetry.isFailure) {
                clearSession()
                return@launch
            }

            val (userId, email) = meRetry.getOrThrow()
            persistSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = userId,
                email = email
            )
        }
    }

    fun login(email: String, password: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repository.login(email, password)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.message ?: "Authentication failed."
                    )
                }
                return@launch
            }
            val payload = result.getOrThrow()
            persistSession(
                accessToken = payload.session.accessToken,
                refreshToken = payload.session.refreshToken,
                userId = payload.userId,
                email = payload.email
            )
        }
    }

    fun register(email: String, password: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repository.register(email, password)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.message ?: "Authentication failed."
                    )
                }
                return@launch
            }
            val payload = result.getOrThrow()
            persistSession(
                accessToken = payload.session.accessToken,
                refreshToken = payload.session.refreshToken,
                userId = payload.userId,
                email = payload.email
            )
        }
    }

    fun logout() {
        clearSession()
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun continueAsGuest() {
        clearSession()
    }

    private fun persistSession(accessToken: String, refreshToken: String, userId: String, email: String) {
        sessionStore.accessToken = accessToken
        sessionStore.refreshToken = refreshToken
        sessionStore.authUserId = userId
        sessionStore.authEmail = email
        _state.value = AuthUiState(
            status = "authenticated",
            userId = userId,
            email = email,
            loading = false,
            error = null
        )
    }

    private fun clearSession() {
        sessionStore.accessToken = null
        sessionStore.refreshToken = null
        sessionStore.authUserId = null
        sessionStore.authEmail = null
        sessionStore.playerId = null
        sessionStore.displayName = null
        sessionStore.matchId = null
        _state.value = AuthUiState(status = "guest", loading = false, error = null)
    }

    companion object {
        fun factory(repository: AuthRepository, sessionStore: SessionStore): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(repository, sessionStore) as T
                }
            }
        }
    }
}
