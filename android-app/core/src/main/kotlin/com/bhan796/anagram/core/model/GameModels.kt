package com.bhan796.anagram.core.model

enum class LetterKind {
    VOWEL,
    CONSONANT
}

enum class PlayerSide {
    PLAYER_ONE,
    PLAYER_TWO;

    val opponent: PlayerSide
        get() = if (this == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
}

enum class RoundType {
    LETTERS,
    CONUNDRUM
}

data class RoundPlan(
    val roundNumber: Int,
    val type: RoundType,
    val picker: PlayerSide?
)

data class MatchPlan(val rounds: List<RoundPlan>) {
    companion object {
        fun standard(startingPicker: PlayerSide): MatchPlan {
            var picker = startingPicker
            val rounds = mutableListOf<RoundPlan>()

            for (index in 1..4) {
                rounds += RoundPlan(roundNumber = index, type = RoundType.LETTERS, picker = picker)
                picker = picker.opponent
            }

            rounds += RoundPlan(roundNumber = 5, type = RoundType.CONUNDRUM, picker = null)
            return MatchPlan(rounds)
        }
    }

    fun nextRound(afterRoundNumber: Int): RoundPlan? =
        rounds.firstOrNull { it.roundNumber == afterRoundNumber + 1 }
}

enum class WordValidationFailure {
    EMPTY,
    NON_ALPHABETICAL,
    NOT_IN_DICTIONARY,
    NOT_CONSTRUCTABLE
}

data class WordValidationResult(
    val normalizedWord: String,
    val isValid: Boolean,
    val failure: WordValidationFailure?,
    val score: Int
)

data class LettersRoundResult(
    val letters: List<Char>,
    val submittedWord: String,
    val validation: WordValidationResult
)

data class Conundrum(
    val id: String,
    val scrambled: String,
    val answer: String
)

data class ConundrumRoundResult(
    val conundrum: Conundrum,
    val submittedGuess: String,
    val solved: Boolean,
    val score: Int
)