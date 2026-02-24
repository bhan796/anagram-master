package com.bhan796.anagramarena.network

import com.bhan796.anagramarena.online.AchievementEntry
import com.bhan796.anagramarena.online.CosmeticItem
import com.bhan796.anagramarena.online.InventoryResponse
import com.bhan796.anagramarena.online.PurchaseResult
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ShopApiService(private val baseUrl: String) {
    suspend fun fetchAchievements(accessToken: String): List<AchievementEntry> = withContext(Dispatchers.IO) {
        val response = getJson("/api/achievements", accessToken)
        val achievements = response.optJSONArray("achievements") ?: JSONArray()
        (0 until achievements.length()).map { index ->
            achievements.getJSONObject(index).toAchievement()
        }
    }

    suspend fun fetchInventory(accessToken: String): InventoryResponse = withContext(Dispatchers.IO) {
        val response = getJson("/api/inventory", accessToken)
        val items = response.optJSONArray("items") ?: JSONArray()
        InventoryResponse(
            items = (0 until items.length()).map { i -> items.getJSONObject(i).toCosmeticItem() },
            equippedCosmetic = response.optString("equippedCosmetic", "").takeIf { it.isNotBlank() },
            pendingChests = response.optInt("pendingChests", 0),
            runes = response.optInt("runes", 0)
        )
    }

    suspend fun purchaseChest(accessToken: String): PurchaseResult = withContext(Dispatchers.IO) {
        val response = postJson("/api/shop/purchase", JSONObject(), accessToken)
        PurchaseResult(
            remainingRunes = response.optInt("remainingRunes", 0),
            pendingChests = response.optInt("pendingChests", 0)
        )
    }

    suspend fun openChest(accessToken: String): CosmeticItem = withContext(Dispatchers.IO) {
        val response = postJson("/api/shop/open-chest", JSONObject(), accessToken)
        response.getJSONObject("item").toCosmeticItem()
    }

    suspend fun equipItem(accessToken: String, itemId: String?) {
        withContext(Dispatchers.IO) {
            postJson(
                "/api/inventory/equip",
                JSONObject().apply {
                    if (itemId == null) put("itemId", JSONObject.NULL) else put("itemId", itemId)
                },
                accessToken
            )
        }
    }

    suspend fun fetchRunes(accessToken: String): Int = withContext(Dispatchers.IO) {
        val response = getJson("/api/player/runes", accessToken)
        response.optInt("runes", 0)
    }

    private fun getJson(path: String, accessToken: String): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return readJson(connection)
    }

    private fun postJson(path: String, payload: JSONObject, accessToken: String): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }
        return readJson(connection)
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val message = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
            throw IllegalStateException(if (message.isNotBlank()) message else "HTTP ${connection.responseCode}: $body")
        }
        return if (body.isBlank()) JSONObject() else JSONObject(body)
    }

    private fun JSONObject.toCosmeticItem(): CosmeticItem = CosmeticItem(
        id = getString("id"),
        name = getString("name"),
        rarity = optString("rarity", "common"),
        cssClass = optString("cssClass", ""),
        description = optString("description", "")
    )

    private fun JSONObject.toAchievement(): AchievementEntry = AchievementEntry(
        id = getString("id"),
        name = getString("name"),
        description = getString("description"),
        tier = optString("tier", "easy"),
        runesReward = optInt("runesReward", 0),
        unlocked = optBoolean("unlocked", false),
        unlockedAt = optString("unlockedAt", "").takeIf { it.isNotBlank() }
    )
}