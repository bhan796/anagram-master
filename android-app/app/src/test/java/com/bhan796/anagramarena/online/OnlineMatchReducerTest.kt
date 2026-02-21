package com.bhan796.anagramarena.online

import com.bhan796.anagramarena.network.SocketConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMatchReducerTest {
    @Test
    fun computeRemainingSeconds_usesServerOffset() {
        val matchState = MatchStatePayload(
            matchId = "m1",
            phase = MatchPhase.LETTERS_SOLVING,
            phaseEndsAtMs = 11_000,
            serverNowMs = 10_000,
            roundNumber = 1,
            roundType = RoundType.LETTERS,
            players = emptyList(),
            pickerPlayerId = null,
            letters = emptyList(),
            scrambled = null,
            roundResults = emptyList(),
            winnerPlayerId = null
        )

        val remaining = OnlineMatchReducer.computeRemainingSeconds(matchState, nowMs = 9_000, serverClockOffsetMs = 1_000)
        assertEquals(1, remaining)
    }

    @Test
    fun reduce_marksMyTurnWhenPickerMatchesPlayer() {
        val state = MatchStatePayload(
            matchId = "m1",
            phase = MatchPhase.AWAITING_LETTERS_PICK,
            phaseEndsAtMs = null,
            serverNowMs = 10_000,
            roundNumber = 1,
            roundType = RoundType.LETTERS,
            players = listOf(
                PlayerSnapshot("p1", "Me", true, 0),
                PlayerSnapshot("p2", "Other", true, 0)
            ),
            pickerPlayerId = "p1",
            letters = emptyList(),
            scrambled = null,
            roundResults = emptyList(),
            winnerPlayerId = null
        )

        val reduced = OnlineMatchReducer.reduce(
            previous = OnlineUiState(),
            connection = SocketConnectionState.Connected,
            session = SessionIdentifyPayload("p1", "Me", 10_000),
            matchmaking = MatchmakingStatusPayload(0, "idle"),
            matchState = state,
            actionError = null,
            nowMs = 10_000,
            serverClockOffsetMs = 0
        )

        assertTrue(reduced.isMyTurnToPick)
        assertEquals("Letter picking in progress", reduced.statusMessage)
    }
}
