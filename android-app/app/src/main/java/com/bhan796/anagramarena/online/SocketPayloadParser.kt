package com.bhan796.anagramarena.online

import org.json.JSONArray
import org.json.JSONObject

object SocketPayloadParser {
    fun parseSessionIdentify(obj: JSONObject): SessionIdentifyPayload {
        return SessionIdentifyPayload(
            playerId = obj.optString("playerId"),
            displayName = obj.optString("displayName"),
            isAuthenticated = obj.optBoolean("isAuthenticated", false),
            serverNowMs = obj.optLong("serverNowMs")
        )
    }

    fun parseMatchmakingStatus(obj: JSONObject): MatchmakingStatusPayload {
        return MatchmakingStatusPayload(
            queueSize = obj.optInt("queueSize"),
            state = obj.optString("state"),
            mode = obj.optString("mode", "casual")
        )
    }

    fun parseMatchFound(obj: JSONObject): MatchFoundPayload {
        return MatchFoundPayload(
            matchId = obj.optString("matchId"),
            serverNowMs = obj.optLong("serverNowMs")
        )
    }

    fun parseActionError(obj: JSONObject): ActionErrorPayload {
        return ActionErrorPayload(
            action = obj.optString("action"),
            code = obj.optString("code"),
            message = obj.optString("message")
        )
    }

    fun parseMatchState(obj: JSONObject): MatchStatePayload {
        val players = obj.optJSONArray("players")?.toPlayerList().orEmpty()
        val roundResults = obj.optJSONArray("roundResults")?.toRoundResultList().orEmpty()

        return MatchStatePayload(
            matchId = obj.optString("matchId"),
            phase = parseMatchPhase(obj.optString("phase")),
            phaseEndsAtMs = if (obj.has("phaseEndsAtMs") && !obj.isNull("phaseEndsAtMs")) obj.optLong("phaseEndsAtMs") else null,
            serverNowMs = obj.optLong("serverNowMs"),
            roundNumber = obj.optInt("roundNumber"),
            roundType = parseRoundType(obj.optString("roundType")),
            mode = obj.optString("mode", "casual"),
            players = players,
            pickerPlayerId = obj.optStringOrNull("pickerPlayerId"),
            letters = obj.optJSONArray("letters")?.toStringList().orEmpty(),
            bonusTiles = obj.optJSONObject("bonusTiles")?.toBonusTiles(),
            scrambled = obj.optStringOrNull("scrambled"),
            conundrumGuessSubmittedPlayerIds = obj.optJSONArray("conundrumGuessSubmittedPlayerIds")?.toStringList().orEmpty(),
            roundResults = roundResults,
            winnerPlayerId = obj.optStringOrNull("winnerPlayerId"),
            matchEndReason = obj.optStringOrNull("matchEndReason"),
            ratingChanges = obj.optJSONObject("ratingChanges")?.toIntMap().orEmpty()
        )
    }

    private fun parseMatchPhase(value: String): MatchPhase {
        return when (value) {
            "awaiting_letters_pick" -> MatchPhase.AWAITING_LETTERS_PICK
            "letters_solving" -> MatchPhase.LETTERS_SOLVING
            "conundrum_solving" -> MatchPhase.CONUNDRUM_SOLVING
            "round_result" -> MatchPhase.ROUND_RESULT
            "finished" -> MatchPhase.FINISHED
            else -> MatchPhase.UNKNOWN
        }
    }

    private fun parseRoundType(value: String): RoundType {
        return when (value) {
            "letters" -> RoundType.LETTERS
            "conundrum" -> RoundType.CONUNDRUM
            else -> RoundType.UNKNOWN
        }
    }

    private fun JSONArray.toPlayerList(): List<PlayerSnapshot> {
        return buildList {
            for (idx in 0 until length()) {
                val obj = optJSONObject(idx) ?: continue
                add(
                    PlayerSnapshot(
                        playerId = obj.optString("playerId"),
                        displayName = obj.optString("displayName"),
                        connected = obj.optBoolean("connected"),
                        score = obj.optInt("score"),
                        rating = obj.optInt("rating", 1000),
                        rankTier = obj.optString("rankTier", "silver")
                    )
                )
            }
        }
    }

    private fun JSONArray.toRoundResultList(): List<RoundResultSnapshot> {
        return buildList {
            for (idx in 0 until length()) {
                val obj = optJSONObject(idx) ?: continue
                val details = obj.optJSONObject("details")

                val awardedScores = obj.optJSONObject("awardedScores")?.toIntMap().orEmpty()
                val type = parseRoundType(obj.optString("type"))

                add(
                    RoundResultSnapshot(
                        roundNumber = obj.optInt("roundNumber"),
                        type = type,
                        awardedScores = awardedScores,
                        letters = details?.optJSONArray("letters")?.toStringList(),
                        bonusTiles = details?.optJSONObject("bonusTiles")?.toBonusTiles(),
                        submissions = details?.optJSONObject("submissions")?.toSubmissionMap(),
                        scrambled = details?.optStringOrNull("scrambled"),
                        answer = details?.optStringOrNull("answer"),
                        conundrumSubmissions = details?.optJSONObject("conundrumSubmissions")?.toConundrumSubmissionMap().orEmpty(),
                        correctPlayerIds = details?.optJSONArray("correctPlayerIds")?.toStringList().orEmpty()
                    )
                )
            }
        }
    }

    private fun JSONObject.toConundrumSubmissionMap(): Map<String, ConundrumSubmissionSnapshot> {
        return keys().asSequence().associateWith { key ->
            val obj = optJSONObject(key) ?: JSONObject()
            ConundrumSubmissionSnapshot(
                guess = obj.optString("guess"),
                normalizedGuess = obj.optString("normalizedGuess"),
                isCorrect = obj.optBoolean("isCorrect"),
                submittedAtMs = obj.optLong("submittedAtMs")
            )
        }
    }

    private fun JSONObject.toSubmissionMap(): Map<String, WordSubmissionSnapshot> {
        return keys().asSequence().associateWith { key ->
            val obj = optJSONObject(key) ?: JSONObject()
            WordSubmissionSnapshot(
                word = obj.optString("word"),
                normalizedWord = obj.optString("normalizedWord"),
                isValid = obj.optBoolean("isValid"),
                failureCode = obj.optStringOrNull("failureCode"),
                score = obj.optInt("score"),
                submittedAtMs = obj.optLong("submittedAtMs")
            )
        }
    }

    private fun JSONObject.toIntMap(): Map<String, Int> {
        return keys().asSequence().associateWith { key -> optInt(key) }
    }

    private fun JSONObject.toBonusTiles(): BonusTiles {
        return BonusTiles(
            doubleIndex = optInt("doubleIndex"),
            tripleIndex = optInt("tripleIndex")
        )
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (idx in 0 until length()) {
                val value = optString(idx)
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.ifEmpty { null }
    }
}
