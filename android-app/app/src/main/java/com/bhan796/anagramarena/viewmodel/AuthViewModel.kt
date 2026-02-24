package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.network.AuthResultPayload
import com.bhan796.anagramarena.repository.AuthRepository
import com.bhan796.anagramarena.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.Result

data class AuthUiState(
    val status: String = "guest",
    val userId: String? = null,
    val email: String? = null,
    val loading: Boolean = false,
    val deletingAccount: Boolean = false,
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
                val (userId, email, playerId) = meResult.getOrThrow()
                persistSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = email,
                    playerId = playerId
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

            val (userId, email, playerId) = meRetry.getOrThrow()
            persistSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = userId,
                email = email,
                playerId = playerId
            )
        }
    }

    fun login(email: String, password: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            handleAuthResult(repository.login(email, password))
        }
    }

    fun register(email: String, password: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            handleAuthResult(repository.register(email, password))
        }
    }

    fun loginWithGoogleToken(token: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            handleAuthResult(repository.loginWithGoogleToken(token))
        }
    }

    fun loginWithFacebookToken(token: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            handleAuthResult(repository.loginWithFacebookToken(token))
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

    fun deleteAccount(onSuccess: () -> Unit = {}) {
        if (_state.value.status != "authenticated") return
        _state.update { it.copy(deletingAccount = true, error = null) }
        viewModelScope.launch {
            val result = repository.deleteAccount()
            if (result.isSuccess) {
                clearSession()
                onSuccess()
                return@launch
            }

            _state.update {
                it.copy(
                    deletingAccount = false,
                    error = result.exceptionOrNull()?.message ?: "Unable to delete account."
                )
            }
        }
    }

    fun setError(message: String) {
        _state.update { it.copy(loading = false, error = message) }
    }

    private fun persistSession(accessToken: String, refreshToken: String, userId: String, email: String, playerId: String? = null) {
        sessionStore.accessToken = accessToken
        sessionStore.refreshToken = refreshToken
        sessionStore.authUserId = userId
        sessionStore.authEmail = email
        if (!playerId.isNullOrBlank()) {
            sessionStore.playerId = playerId
        }
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
        _state.value = AuthUiState(status = "guest", loading = false, deletingAccount = false, error = null)
    }

    private fun handleAuthResult(result: Result<AuthResultPayload>) {
        if (result.isFailure) {
            _state.update {
                it.copy(
                    loading = false,
                    deletingAccount = false,
                    error = result.exceptionOrNull()?.message ?: "Authentication failed."
                )
            }
            return
        }
        val payload = result.getOrThrow()
        persistSession(
            accessToken = payload.session.accessToken,
            refreshToken = payload.session.refreshToken,
            userId = payload.userId,
            email = payload.email,
            playerId = payload.playerId
        )
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
