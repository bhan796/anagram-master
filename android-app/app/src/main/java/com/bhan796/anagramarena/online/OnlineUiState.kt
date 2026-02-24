package com.bhan796.anagramarena.online

import com.bhan796.anagramarena.network.SocketConnectionState
import kotlin.math.ceil

data class OnlineUiState(
    val connectionState: SocketConnectionState = SocketConnectionState.Disconnected,
    val playerId: String? = null,
    val displayName: String? = null,
    val isAuthenticated: Boolean = false,
    val queueState: String = "idle",
    val queueSize: Int = 0,
    val queueMode: String = "casual",
    val isInMatchmaking: Boolean = false,
    val matchId: String? = null,
    val matchState: MatchStatePayload? = null,
    val myPlayer: PlayerSnapshot? = null,
    val opponentPlayer: PlayerSnapshot? = null,
    val isMyTurnToPick: Boolean = false,
    val secondsRemaining: Int = 0,
    val wordInput: String = "",
    val conundrumGuessInput: String = "",
    val hasSubmittedWord: Boolean = false,
    val hasSubmittedConundrumGuess: Boolean = false,
    val opponentSubmittedConundrumGuess: Boolean = false,
    val lastError: ActionErrorPayload? = null,
    val statusMessage: String = "",
    val localValidationMessage: String? = null
)

object OnlineMatchReducer {
    fun reduce(
        previous: OnlineUiState,
        connection: SocketConnectionState = previous.connectionState,
        session: SessionIdentifyPayload? = null,
        matchmaking: MatchmakingStatusPayload? = null,
        matchState: MatchStatePayload? = null,
        actionError: ActionErrorPayload? = null,
        nowMs: Long,
        serverClockOffsetMs: Long
    ): OnlineUiState {
        val updatedMatch = matchState ?: previous.matchState
        val effectiveError = if (updatedMatch != null) null else actionError
        val resolvedPlayerId = session?.playerId ?: previous.playerId
        val me = updatedMatch?.players?.firstOrNull { it.playerId == resolvedPlayerId }
        val opponent = updatedMatch?.players?.firstOrNull { it.playerId != resolvedPlayerId }

        val remaining = computeRemainingSeconds(updatedMatch, nowMs, serverClockOffsetMs)
        val inQueue = (matchmaking?.state ?: previous.queueState) == "searching"
        val conundrumSubmittedPlayerIds = updatedMatch?.conundrumGuessSubmittedPlayerIds.orEmpty()
        val hasSubmittedConundrumGuess = resolvedPlayerId?.let { conundrumSubmittedPlayerIds.contains(it) } ?: false
        val opponentSubmittedConundrumGuess =
            resolvedPlayerId?.let { id -> conundrumSubmittedPlayerIds.any { it != id } } ?: false

        val message = when {
            connection is SocketConnectionState.Reconnecting -> "Reconnecting..."
            connection is SocketConnectionState.Disconnected && updatedMatch != null -> "Disconnected. Trying to recover match..."
            connection is SocketConnectionState.Failed -> "Connection failed. Retry to continue."
            effectiveError != null -> effectiveError.message
            updatedMatch == null && inQueue -> "Finding opponent..."
            updatedMatch?.phase == MatchPhase.AWAITING_LETTERS_PICK -> "Letter picking in progress"
            updatedMatch?.phase == MatchPhase.LETTERS_SOLVING -> "Submit your best word before time expires"
            updatedMatch?.phase == MatchPhase.CONUNDRUM_SOLVING -> "Solve the conundrum"
            updatedMatch?.phase == MatchPhase.ROUND_RESULT -> "Round result"
            updatedMatch?.phase == MatchPhase.FINISHED -> when (updatedMatch.matchEndReason) {
                "forfeit_disconnect" -> "Match ended: opponent disconnected."
                "forfeit_manual" -> "Match ended: opponent left the game."
                else -> "Match finished"
            }
            else -> ""
        }

        val isMyTurnToPick = updatedMatch?.phase == MatchPhase.AWAITING_LETTERS_PICK &&
            updatedMatch.pickerPlayerId == resolvedPlayerId

        return previous.copy(
            connectionState = connection,
            playerId = session?.playerId ?: previous.playerId,
            displayName = session?.displayName ?: previous.displayName,
            isAuthenticated = session?.isAuthenticated ?: previous.isAuthenticated,
            queueState = matchmaking?.state ?: previous.queueState,
            queueSize = matchmaking?.queueSize ?: previous.queueSize,
            queueMode = matchmaking?.mode ?: previous.queueMode,
            isInMatchmaking = inQueue,
            matchId = updatedMatch?.matchId ?: previous.matchId,
            matchState = updatedMatch,
            myPlayer = me,
            opponentPlayer = opponent,
            isMyTurnToPick = isMyTurnToPick,
            secondsRemaining = remaining,
            hasSubmittedConundrumGuess = hasSubmittedConundrumGuess,
            opponentSubmittedConundrumGuess = opponentSubmittedConundrumGuess,
            lastError = effectiveError,
            statusMessage = message
        )
    }

    fun computeRemainingSeconds(matchState: MatchStatePayload?, nowMs: Long, serverClockOffsetMs: Long): Int {
        val phaseEnd = matchState?.phaseEndsAtMs ?: return 0
        val adjustedNow = nowMs + serverClockOffsetMs
        val remainingMs = phaseEnd - adjustedNow
        return ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
    }
}
