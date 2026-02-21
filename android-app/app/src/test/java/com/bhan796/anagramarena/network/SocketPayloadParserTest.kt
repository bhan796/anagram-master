package com.bhan796.anagramarena.network

import com.bhan796.anagramarena.online.MatchPhase
import com.bhan796.anagramarena.online.RoundType
import com.bhan796.anagramarena.online.SocketPayloadParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketPayloadParserTest {
    @Test
    fun parseMatchState_withLettersRoundAndSubmissions() {
        val raw = JSONObject(
            """
            {
              "matchId": "m1",
              "phase": "round_result",
              "phaseEndsAtMs": 2000,
              "serverNowMs": 1000,
              "roundNumber": 1,
              "roundType": "letters",
              "players": [
                {"playerId":"p1","displayName":"A","connected":true,"score":5},
                {"playerId":"p2","displayName":"B","connected":true,"score":0}
              ],
              "pickerPlayerId":"p1",
              "letters":["S","T","O","N","E","A","B","C","D"],
              "roundResults":[
                {
                  "roundNumber":1,
                  "type":"letters",
                  "awardedScores":{"p1":5,"p2":0},
                  "details":{
                    "letters":["S","T","O","N","E","A","B","C","D"],
                    "submissions":{
                      "p1":{"word":"stone","normalizedWord":"stone","isValid":true,"score":5,"submittedAtMs":1500},
                      "p2":{"word":"","normalizedWord":"","isValid":false,"failureCode":"empty","score":0,"submittedAtMs":1500}
                    }
                  }
                }
              ],
              "winnerPlayerId": null
            }
            """.trimIndent()
        )

        val parsed = SocketPayloadParser.parseMatchState(raw)

        assertEquals("m1", parsed.matchId)
        assertEquals(MatchPhase.ROUND_RESULT, parsed.phase)
        assertEquals(RoundType.LETTERS, parsed.roundType)
        assertEquals(2, parsed.players.size)
        assertEquals(1, parsed.roundResults.size)
        assertEquals(5, parsed.roundResults.first().awardedScores["p1"])
        assertTrue(parsed.roundResults.first().submissions?.get("p1")?.isValid == true)
        assertFalse(parsed.roundResults.first().submissions?.get("p2")?.isValid == true)
    }

    @Test
    fun parseActionError_mapsFields() {
        val parsed = SocketPayloadParser.parseActionError(
            JSONObject("""{"action":"round:submit_word","code":"LATE_SUBMISSION","message":"late"}""")
        )

        assertEquals("round:submit_word", parsed.action)
        assertEquals("LATE_SUBMISSION", parsed.code)
        assertEquals("late", parsed.message)
    }
}
