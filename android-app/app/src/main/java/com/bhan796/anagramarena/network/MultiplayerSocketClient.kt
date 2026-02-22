package com.bhan796.anagramarena.network

import com.bhan796.anagramarena.online.ActionErrorPayload
import com.bhan796.anagramarena.online.MatchFoundPayload
import com.bhan796.anagramarena.online.MatchStatePayload
import com.bhan796.anagramarena.online.MatchmakingStatusPayload
import com.bhan796.anagramarena.online.SessionIdentifyPayload

sealed class SocketConnectionState {
    data object Disconnected : SocketConnectionState()
    data object Connecting : SocketConnectionState()
    data object Connected : SocketConnectionState()
    data class Reconnecting(val attempt: Int) : SocketConnectionState()
    data class Failed(val reason: String) : SocketConnectionState()
}

interface MultiplayerSocketClient {
    var connectionStateListener: ((SocketConnectionState) -> Unit)?
    var sessionIdentifyListener: ((SessionIdentifyPayload) -> Unit)?
    var matchmakingStatusListener: ((MatchmakingStatusPayload) -> Unit)?
    var matchFoundListener: ((MatchFoundPayload) -> Unit)?
    var matchStateListener: ((MatchStatePayload) -> Unit)?
    var actionErrorListener: ((ActionErrorPayload) -> Unit)?

    fun connect(baseUrl: String)
    fun disconnect()

    fun identify(playerId: String?, displayName: String?, accessToken: String?)
    fun joinQueue(mode: String = "casual")
    fun leaveQueue()
    fun resumeMatch(matchId: String)
    fun forfeitMatch()
    fun pickLetter(kind: String)
    fun submitWord(word: String)
    fun submitConundrumGuess(guess: String)
}
