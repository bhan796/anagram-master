package com.bhan796.anagramarena.network

import com.bhan796.anagramarena.online.HistoryPlayerScore
import com.bhan796.anagramarena.online.MatchHistoryItem
import com.bhan796.anagramarena.online.MatchHistoryResponse
import com.bhan796.anagramarena.online.PlayerStats
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ProfileApiService(private val baseUrl: String) {
    suspend fun fetchPlayerStats(playerId: String): Result<PlayerStats> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getJson("/api/profiles/$playerId/stats")
            PlayerStats(
                playerId = response.getString("playerId"),
                displayName = response.getString("displayName"),
                matchesPlayed = response.getInt("matchesPlayed"),
                wins = response.getInt("wins"),
                losses = response.getInt("losses"),
                draws = response.getInt("draws"),
                totalScore = response.getInt("totalScore"),
                averageScore = response.getDouble("averageScore"),
                recentMatchIds = response.getJSONArray("recentMatchIds").toStringList()
            )
        }
    }

    suspend fun fetchMatchHistory(playerId: String): Result<MatchHistoryResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getJson("/api/profiles/$playerId/matches")
            MatchHistoryResponse(
                playerId = response.getString("playerId"),
                count = response.getInt("count"),
                matches = response.getJSONArray("matches").toHistoryItems()
            )
        }
    }

    private fun getJson(path: String): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode}: $body")
        }

        return JSONObject(body)
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        (0 until length()).map { index -> getString(index) }

    private fun org.json.JSONArray.toHistoryItems(): List<MatchHistoryItem> {
        return (0 until length()).map { index ->
            val item = getJSONObject(index)
            MatchHistoryItem(
                matchId = item.getString("matchId"),
                createdAtMs = item.getLong("createdAtMs"),
                finishedAtMs = item.getLong("finishedAtMs"),
                winnerPlayerId = if (item.isNull("winnerPlayerId")) null else item.getString("winnerPlayerId"),
                players = item.getJSONArray("players").toHistoryPlayers()
            )
        }
    }

    private fun org.json.JSONArray.toHistoryPlayers(): List<HistoryPlayerScore> {
        return (0 until length()).map { index ->
            val p = getJSONObject(index)
            HistoryPlayerScore(
                playerId = p.getString("playerId"),
                displayName = p.getString("displayName"),
                score = p.getInt("score")
            )
        }
    }
}
