package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.ProfileApiService
import com.bhan796.anagramarena.online.MatchHistoryResponse
import com.bhan796.anagramarena.online.PlayerStats

class ProfileRepository(private val apiService: ProfileApiService) {
    suspend fun loadStats(playerId: String): Result<PlayerStats> = apiService.fetchPlayerStats(playerId)

    suspend fun loadHistory(playerId: String): Result<MatchHistoryResponse> = apiService.fetchMatchHistory(playerId)
}
