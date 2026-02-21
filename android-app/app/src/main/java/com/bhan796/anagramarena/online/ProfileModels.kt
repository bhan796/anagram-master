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
    val recentMatchIds: List<String>
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
    val winnerPlayerId: String?,
    val players: List<HistoryPlayerScore>
)

data class HistoryPlayerScore(
    val playerId: String,
    val displayName: String,
    val score: Int
)
