package com.bhan796.anagramarena.online

data class PlayerStats(
    val playerId: String,
    val displayName: String,
    val matchesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val totalScore: Int,
    val averageScore: Double,
    val recentMatchIds: List<String>,
    val rating: Int,
    val peakRating: Int,
    val rankTier: String,
    val rankedGames: Int,
    val rankedWins: Int,
    val rankedLosses: Int,
    val rankedDraws: Int
)

data class MatchHistoryResponse(
    val playerId: String,
    val count: Int,
    val matches: List<MatchHistoryItem>
)

data class MatchHistoryItem(
    val matchId: String,
    val createdAtMs: Long,
    val finishedAtMs: Long,
    val mode: String,
    val winnerPlayerId: String?,
    val players: List<HistoryPlayerScore>
)

data class HistoryPlayerScore(
    val playerId: String,
    val displayName: String,
    val score: Int
)

data class LeaderboardEntry(
    val playerId: String,
    val displayName: String,
    val rating: Int,
    val rankTier: String,
    val rankedGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int
)
