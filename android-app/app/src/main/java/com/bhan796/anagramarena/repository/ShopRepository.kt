package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.ShopApiService
import com.bhan796.anagramarena.online.CosmeticItem
import com.bhan796.anagramarena.online.InventoryResponse
import com.bhan796.anagramarena.online.PurchaseResult
import com.bhan796.anagramarena.storage.SessionStore

class ShopRepository(
    private val shopApiService: ShopApiService,
    private val sessionStore: SessionStore
) {
    suspend fun fetchInventory(): Result<InventoryResponse> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.fetchInventory(accessToken)
    }

    suspend fun purchaseChest(): Result<PurchaseResult> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.purchaseChest(accessToken)
    }

    suspend fun openChest(): Result<CosmeticItem> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.openChest(accessToken)
    }

    suspend fun equipItem(itemId: String?): Result<Unit> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.equipItem(accessToken, itemId)
    }

    suspend fun fetchRunes(): Result<Int> = runCatching {
        val accessToken = sessionStore.accessToken ?: throw Exception("Not authenticated")
        shopApiService.fetchRunes(accessToken)
    }
}