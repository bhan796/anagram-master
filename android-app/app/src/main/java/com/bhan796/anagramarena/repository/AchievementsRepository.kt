package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.ShopApiService
import com.bhan796.anagramarena.online.AchievementEntry
import com.bhan796.anagramarena.storage.SessionStore

class AchievementsRepository(
    private val shopApiService: ShopApiService,
    private val sessionStore: SessionStore
) {
    suspend fun fetchAchievements(): Result<List<AchievementEntry>> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.fetchAchievements(accessToken)
    }
}