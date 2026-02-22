package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.AuthApiService
import com.bhan796.anagramarena.network.AuthResultPayload
import com.bhan796.anagramarena.network.AuthSessionPayload
import com.bhan796.anagramarena.storage.SessionStore

class AuthRepository(
    private val apiService: AuthApiService,
    private val sessionStore: SessionStore
) {
    suspend fun register(email: String, password: String): Result<AuthResultPayload> {
        return apiService.register(email.trim(), password, sessionStore.playerId)
    }

    suspend fun login(email: String, password: String): Result<AuthResultPayload> {
        return apiService.login(email.trim(), password, sessionStore.playerId)
    }

    suspend fun loginWithGoogleToken(token: String): Result<AuthResultPayload> {
        return apiService.oauth("google", token, sessionStore.playerId)
    }

    suspend fun loginWithFacebookToken(token: String): Result<AuthResultPayload> {
        return apiService.oauth("facebook", token, sessionStore.playerId)
    }

    suspend fun refresh(): Result<AuthSessionPayload> {
        val refreshToken = sessionStore.refreshToken ?: return Result.failure(IllegalStateException("No refresh token."))
        return apiService.refresh(refreshToken)
    }

    suspend fun logout(): Result<Unit> {
        val refreshToken = sessionStore.refreshToken ?: return Result.success(Unit)
        return apiService.logout(refreshToken)
    }

    suspend fun me(accessToken: String): Result<Triple<String, String, String?>> = apiService.me(accessToken)
}
