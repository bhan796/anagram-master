package com.bhan796.anagramarena.network

import android.util.Log
import com.bhan796.anagramarena.online.SocketEventNames
import com.bhan796.anagramarena.online.SocketPayloadParser
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import java.net.URISyntaxException
import org.json.JSONObject

class SocketIoMultiplayerClient : MultiplayerSocketClient {
    override var connectionStateListener: ((SocketConnectionState) -> Unit)? = null
    override var sessionIdentifyListener: ((com.bhan796.anagramarena.online.SessionIdentifyPayload) -> Unit)? = null
    override var matchmakingStatusListener: ((com.bhan796.anagramarena.online.MatchmakingStatusPayload) -> Unit)? = null
    override var matchFoundListener: ((com.bhan796.anagramarena.online.MatchFoundPayload) -> Unit)? = null
    override var matchStateListener: ((com.bhan796.anagramarena.online.MatchStatePayload) -> Unit)? = null
    override var actionErrorListener: ((com.bhan796.anagramarena.online.ActionErrorPayload) -> Unit)? = null

    private var socket: Socket? = null

    override fun connect(baseUrl: String) {
        if (socket != null) return

        val options = IO.Options.builder()
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(500)
            .setReconnectionDelayMax(5000)
            .setTimeout(10000)
            .build()

        val created = try {
            IO.socket(baseUrl, options)
        } catch (err: URISyntaxException) {
            connectionStateListener?.invoke(SocketConnectionState.Failed(err.message ?: "Invalid URL"))
            return
        }

        created.on(Socket.EVENT_CONNECT) {
            connectionStateListener?.invoke(SocketConnectionState.Connected)
        }

        created.on(Socket.EVENT_DISCONNECT) {
            connectionStateListener?.invoke(SocketConnectionState.Disconnected)
        }

        created.on(Manager.EVENT_RECONNECT_ATTEMPT) { args ->
            val attempt = when (val raw = args.firstOrNull()) {
                is Int -> raw
                is Double -> raw.toInt()
                else -> 0
            }
            connectionStateListener?.invoke(SocketConnectionState.Reconnecting(attempt))
        }

        created.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val reason = args.firstOrNull()?.toString() ?: "connect_error"
            connectionStateListener?.invoke(SocketConnectionState.Failed(reason))
        }

        created.on(SocketEventNames.SESSION_IDENTIFY) { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            sessionIdentifyListener?.invoke(SocketPayloadParser.parseSessionIdentify(obj))
        }

        created.on(SocketEventNames.MATCHMAKING_STATUS) { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            matchmakingStatusListener?.invoke(SocketPayloadParser.parseMatchmakingStatus(obj))
        }

        created.on(SocketEventNames.MATCH_FOUND) { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            matchFoundListener?.invoke(SocketPayloadParser.parseMatchFound(obj))
        }

        created.on(SocketEventNames.MATCH_STATE) { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            matchStateListener?.invoke(SocketPayloadParser.parseMatchState(obj))
        }

        created.on(SocketEventNames.ACTION_ERROR) { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            actionErrorListener?.invoke(SocketPayloadParser.parseActionError(obj))
        }

        socket = created
        connectionStateListener?.invoke(SocketConnectionState.Connecting)
        created.connect()
    }

    override fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    override fun identify(playerId: String?, displayName: String?) {
        emit(SocketEventNames.SESSION_IDENTIFY) {
            if (!playerId.isNullOrBlank()) put("playerId", playerId)
            if (!displayName.isNullOrBlank()) put("displayName", displayName)
        }
    }

    override fun joinQueue() {
        emit(SocketEventNames.QUEUE_JOIN)
    }

    override fun leaveQueue() {
        emit(SocketEventNames.QUEUE_LEAVE)
    }

    override fun resumeMatch(matchId: String) {
        emit(SocketEventNames.MATCH_RESUME) {
            put("matchId", matchId)
        }
    }

    override fun forfeitMatch() {
        emit(SocketEventNames.MATCH_FORFEIT)
    }

    override fun pickLetter(kind: String) {
        emit(SocketEventNames.ROUND_PICK_LETTER) {
            put("kind", kind)
        }
    }

    override fun submitWord(word: String) {
        emit(SocketEventNames.ROUND_SUBMIT_WORD) {
            put("word", word)
        }
    }

    override fun submitConundrumGuess(guess: String) {
        emit(SocketEventNames.ROUND_SUBMIT_CONUNDRUM_GUESS) {
            put("guess", guess)
        }
    }

    private fun emit(event: String, payloadBuilder: (JSONObject.() -> Unit)? = null) {
        val payload = JSONObject()
        payloadBuilder?.invoke(payload)

        try {
            if (payload.length() == 0) {
                socket?.emit(event)
            } else {
                socket?.emit(event, payload)
            }
        } catch (err: Exception) {
            Log.e("SocketIoClient", "Failed emit $event", err)
        }
    }
}
