package com.bhan796.anagramarena.online

enum class MatchPhase {
    AWAITING_LETTERS_PICK,
    LETTERS_SOLVING,
    CONUNDRUM_SOLVING,
    ROUND_RESULT,
    FINISHED,
    UNKNOWN
}

enum class RoundType {
    LETTERS,
    CONUNDRUM,
    UNKNOWN
}

data class SessionIdentifyPayload(
    val playerId: String,
    val displayName: String,
    val isAuthenticated: Boolean = false,
    val serverNowMs: Long
)

data class MatchmakingStatusPayload(
    val queueSize: Int,
    val state: String,
    val mode: String = "casual"
)

data class MatchFoundPayload(
    val matchId: String,
    val serverNowMs: Long
)

data class ActionErrorPayload(
    val action: String,
    val code: String,
    val message: String
)

data class PlayerSnapshot(
    val playerId: String,
    val displayName: String,
    val equippedCosmetic: String? = null,
    val connected: Boolean,
    val score: Int,
    val rating: Int = 1000,
    val rankTier: String = "silver"
)

data class WordSubmissionSnapshot(
    val word: String,
    val normalizedWord: String,
    val isValid: Boolean,
    val failureCode: String?,
    val score: Int,
    val submittedAtMs: Long
)

data class BonusTiles(
    val doubleIndex: Int,
    val tripleIndex: Int
)

data class RoundResultSnapshot(
    val roundNumber: Int,
    val type: RoundType,
    val awardedScores: Map<String, Int>,
    val letters: List<String>? = null,
    val bonusTiles: BonusTiles? = null,
    val submissions: Map<String, WordSubmissionSnapshot>? = null,
    val scrambled: String? = null,
    val answer: String? = null,
    val conundrumSubmissions: Map<String, ConundrumSubmissionSnapshot> = emptyMap(),
    val correctPlayerIds: List<String> = emptyList()
)

data class ConundrumSubmissionSnapshot(
    val guess: String,
    val normalizedGuess: String,
    val isCorrect: Boolean,
    val submittedAtMs: Long
)

data class MatchStatePayload(
    val matchId: String,
    val phase: MatchPhase,
    val phaseEndsAtMs: Long?,
    val serverNowMs: Long,
    val roundNumber: Int,
    val roundType: RoundType,
    val mode: String = "casual",
    val players: List<PlayerSnapshot>,
    val pickerPlayerId: String?,
    val letters: List<String>,
    val bonusTiles: BonusTiles? = null,
    val scrambled: String?,
    val conundrumGuessSubmittedPlayerIds: List<String> = emptyList(),
    val roundResults: List<RoundResultSnapshot>,
    val winnerPlayerId: String?,
    val matchEndReason: String? = null,
    val ratingChanges: Map<String, Int> = emptyMap()
)

object SocketEventNames {
    const val SESSION_IDENTIFY = "session:identify"
    const val QUEUE_JOIN = "queue:join"
    const val QUEUE_LEAVE = "queue:leave"
    const val MATCH_RESUME = "match:resume"
    const val MATCH_FORFEIT = "match:forfeit"
    const val ROUND_PICK_LETTER = "round:pick_letter"
    const val ROUND_SUBMIT_WORD = "round:submit_word"
    const val ROUND_SUBMIT_CONUNDRUM_GUESS = "round:submit_conundrum_guess"

    const val MATCH_STATE = "match:state"
    const val MATCHMAKING_STATUS = "matchmaking:status"
    const val MATCH_FOUND = "match:found"
    const val ACTION_ERROR = "action:error"
}
