package com.bhan796.anagramarena.repository

import com.bhan796.anagramarena.network.MultiplayerSocketClient
import com.bhan796.anagramarena.network.SocketConnectionState
import com.bhan796.anagramarena.online.ActionErrorPayload
import com.bhan796.anagramarena.online.MatchFoundPayload
import com.bhan796.anagramarena.online.MatchStatePayload
import com.bhan796.anagramarena.online.MatchmakingStatusPayload
import com.bhan796.anagramarena.online.SessionIdentifyPayload
import com.bhan796.anagramarena.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnlineMatchRepository(
    private val socketClient: MultiplayerSocketClient,
    private val sessionStore: SessionStore,
    private val backendUrl: String,
    private val telemetry: TelemetryLogger = NoOpTelemetryLogger()
) {
    private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected)
    private val _session = MutableStateFlow<SessionIdentifyPayload?>(null)
    private val _matchmaking = MutableStateFlow(MatchmakingStatusPayload(queueSize = 0, state = "idle"))
    private val _matchFound = MutableStateFlow<MatchFoundPayload?>(null)
    private val _matchState = MutableStateFlow<MatchStatePayload?>(null)
    private val _actionError = MutableStateFlow<ActionErrorPayload?>(null)

    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()
    val session: StateFlow<SessionIdentifyPayload?> = _session.asStateFlow()
    val matchmaking: StateFlow<MatchmakingStatusPayload> = _matchmaking.asStateFlow()
    val matchFound: StateFlow<MatchFoundPayload?> = _matchFound.asStateFlow()
    val matchState: StateFlow<MatchStatePayload?> = _matchState.asStateFlow()
    val actionError: StateFlow<ActionErrorPayload?> = _actionError.asStateFlow()

    init {
        socketClient.connectionStateListener = { state ->
            _connectionState.value = state
            telemetry.log("socket_connection_state", mapOf("state" to state::class.simpleName.orEmpty()))
            if (state is SocketConnectionState.Connected) {
                identify()
            }
        }

        socketClient.sessionIdentifyListener = { payload ->
            _session.value = payload
            telemetry.log("session_identified", mapOf("playerId" to payload.playerId))
            sessionStore.playerId = payload.playerId
            sessionStore.displayName = payload.displayName

            val matchId = sessionStore.matchId
            if (!matchId.isNullOrBlank()) {
                socketClient.resumeMatch(matchId)
            }
        }

        socketClient.matchmakingStatusListener = { payload ->
            _matchmaking.value = payload
        }

        socketClient.matchFoundListener = { payload ->
            _matchFound.value = payload
            telemetry.log("match_found", mapOf("matchId" to payload.matchId))
            sessionStore.matchId = payload.matchId
        }

        socketClient.matchStateListener = { payload ->
            _matchState.value = payload
            telemetry.log("match_state", mapOf("phase" to payload.phase.name, "round" to payload.roundNumber.toString()))
            sessionStore.matchId = payload.matchId

            if (payload.phase.name == "FINISHED") {
                sessionStore.matchId = null
            }
        }

        socketClient.actionErrorListener = { payload ->
            _actionError.value = payload
        }
    }

    fun connect() {
        socketClient.connect(backendUrl)
    }

    fun disconnect() {
        socketClient.disconnect()
    }

    fun identify(displayName: String? = sessionStore.displayName) {
        socketClient.identify(sessionStore.playerId, displayName)
    }

    fun joinQueue() {
        _actionError.value = null
        socketClient.joinQueue()
    }

    fun leaveQueue() {
        socketClient.leaveQueue()
        _matchmaking.value = MatchmakingStatusPayload(queueSize = 0, state = "idle")
    }

    fun resume() {
        val matchId = sessionStore.matchId ?: return
        socketClient.resumeMatch(matchId)
    }

    fun pickVowel() {
        socketClient.pickLetter("vowel")
    }

    fun pickConsonant() {
        socketClient.pickLetter("consonant")
    }

    fun submitWord(word: String) {
        socketClient.submitWord(word)
    }

    fun submitConundrumGuess(guess: String) {
        socketClient.submitConundrumGuess(guess)
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
