package com.bhan796.anagramarena.network

import com.bhan796.anagramarena.online.HistoryPlayerScore
import com.bhan796.anagramarena.online.LeaderboardEntry
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
                recentMatchIds = response.getJSONArray("recentMatchIds").toStringList(),
                rating = response.optInt("rating", 1000),
                peakRating = response.optInt("peakRating", 1000),
                rankTier = response.optString("rankTier", "silver"),
                rankedGames = response.optInt("rankedGames", 0),
                rankedWins = response.optInt("rankedWins", 0),
                rankedLosses = response.optInt("rankedLosses", 0),
                rankedDraws = response.optInt("rankedDraws", 0)
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
                mode = item.optString("mode", "casual"),
                winnerPlayerId = if (item.isNull("winnerPlayerId")) null else item.getString("winnerPlayerId"),
                players = item.getJSONArray("players").toHistoryPlayers()
            )
        }
    }

    suspend fun fetchLeaderboard(limit: Int = 20): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getJson("/api/leaderboard?limit=$limit")
            val entries = response.optJSONArray("entries") ?: org.json.JSONArray()
            (0 until entries.length()).map { index ->
                val item = entries.getJSONObject(index)
                LeaderboardEntry(
                    playerId = item.getString("playerId"),
                    displayName = item.getString("displayName"),
                    rating = item.getInt("rating"),
                    rankTier = item.optString("rankTier", "silver"),
                    rankedGames = item.optInt("rankedGames", 0),
                    wins = item.optInt("wins", 0),
                    losses = item.optInt("losses", 0),
                    draws = item.optInt("draws", 0)
                )
            }
        }
    }

    suspend fun fetchPlayersOnline(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getJson("/api/presence")
            response.optInt("playersOnline", 0)
        }
    }

    suspend fun updateDisplayName(playerId: String, displayName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = postJson("/api/profiles/$playerId/display-name", JSONObject().put("displayName", displayName))
            response.getString("displayName")
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

    private fun postJson(path: String, payload: JSONObject): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(payload.toString())
        }

        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val message = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
            throw IllegalStateException(if (message.isNotBlank()) message else "HTTP ${connection.responseCode}: $body")
        }

        return JSONObject(body)
    }
}
