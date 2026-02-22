package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.ProfileApiService
import com.bhan796.anagramarena.online.LeaderboardEntry
import com.bhan796.anagramarena.online.MatchHistoryResponse
import com.bhan796.anagramarena.online.PlayerStats

class ProfileRepository(private val apiService: ProfileApiService) {
    suspend fun loadStats(playerId: String, accessToken: String? = null): Result<PlayerStats> =
        apiService.fetchPlayerStats(playerId, accessToken)

    suspend fun loadHistory(playerId: String, accessToken: String? = null): Result<MatchHistoryResponse> =
        apiService.fetchMatchHistory(playerId, accessToken)

    suspend fun loadLeaderboard(limit: Int = 20, accessToken: String? = null): Result<List<LeaderboardEntry>> =
        apiService.fetchLeaderboard(limit, accessToken)

    suspend fun loadPlayersOnline(): Result<Int> = apiService.fetchPlayersOnline()

    suspend fun updateDisplayName(playerId: String, displayName: String, accessToken: String? = null): Result<String> =
        apiService.updateDisplayName(playerId, displayName, accessToken)
}
