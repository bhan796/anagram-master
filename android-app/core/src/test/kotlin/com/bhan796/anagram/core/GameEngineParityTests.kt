package com.bhan796.anagram.core

import com.bhan796.anagram.core.engine.LetterGenerator
import com.bhan796.anagram.core.engine.PickerConstraints
import com.bhan796.anagram.core.model.LetterKind
import com.bhan796.anagram.core.model.MatchPlan
import com.bhan796.anagram.core.model.PlayerSide
import com.bhan796.anagram.core.model.RoundPlan
import com.bhan796.anagram.core.model.RoundType
import com.bhan796.anagram.core.model.WordValidationFailure
import com.bhan796.anagram.core.validation.ConundrumValidator
import com.bhan796.anagram.core.validation.InMemoryDictionaryProvider
import com.bhan796.anagram.core.validation.WordConstructabilityChecker
import com.bhan796.anagram.core.validation.WordScorer
import com.bhan796.anagram.core.validation.WordValidator
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineParityTests {
    @Test
    fun scoringRulesUseWordLength() {
        val scorer = WordScorer()

        assertEquals(0, scorer.scoreLettersWord(0))
        assertEquals(7, scorer.scoreLettersWord(7))
        assertEquals(9, scorer.scoreLettersWord(9))
    }

    @Test
    fun constructabilityCheckerUsesLetterFrequency() {
        val checker = WordConstructabilityChecker()

        assertTrue(checker.canConstruct("stone", "STONEXYZ".toList()))
        assertFalse(checker.canConstruct("stoon", "STONEABCD".toList()))
        assertFalse(checker.canConstruct("", "STONEABCD".toList()))
    }

    @Test
    fun weightedGeneratorReturnsOnlyValidPoolLetters() {
        val generator = LetterGenerator()
        val random = Random(12345)

        repeat(500) {
            val vowel = generator.generateLetter(LetterKind.VOWEL, random)
            val consonant = generator.generateLetter(LetterKind.CONSONANT, random)

            assertTrue(generator.isValidLetter(vowel, LetterKind.VOWEL))
            assertTrue(generator.isValidLetter(consonant, LetterKind.CONSONANT))
        }
    }

    @Test
    fun pickerConstraintPreventsImpossibleFinalState() {
        val constraints = PickerConstraints(targetSlots = 9)

        val onlyConsonantsSoFar = "BCDFGHJK".toList()
        val allowed = constraints.allowedKinds(onlyConsonantsSoFar, vowelCount = 0, consonantCount = 8)

        assertTrue(allowed.contains(LetterKind.VOWEL))
        assertFalse(allowed.contains(LetterKind.CONSONANT))
    }

    @Test
    fun wordValidatorRejectsNonAlphabeticalAndAcceptsValidConstructableWords() {
        val dictionary = InMemoryDictionaryProvider(setOf("stone", "notebooks"))
        val validator = WordValidator(dictionary)

        val valid = validator.validate("Stone", "STONEXYZA".toList())
        assertTrue(valid.isValid)
        assertEquals(5, valid.score)

        val invalid = validator.validate("stone-1", "STONEXYZA".toList())
        assertFalse(invalid.isValid)
        assertEquals(WordValidationFailure.NON_ALPHABETICAL, invalid.failure)
    }

    @Test
    fun conundrumAnswerValidationIsCaseInsensitive() {
        val validator = ConundrumValidator()

        assertTrue(validator.isCorrect("Algorithm", "algorithm"))
        assertFalse(validator.isCorrect("algorithms", "algorithm"))
    }

    @Test
    fun matchProgressionBuildsFourLettersRoundsAndConundrumFinal() {
        val plan = MatchPlan.standard(PlayerSide.PLAYER_ONE)

        assertEquals(5, plan.rounds.size)
        assertEquals(RoundPlan(1, RoundType.LETTERS, PlayerSide.PLAYER_ONE), plan.rounds[0])
        assertEquals(RoundPlan(2, RoundType.LETTERS, PlayerSide.PLAYER_TWO), plan.rounds[1])
        assertEquals(RoundPlan(3, RoundType.LETTERS, PlayerSide.PLAYER_ONE), plan.rounds[2])
        assertEquals(RoundPlan(4, RoundType.LETTERS, PlayerSide.PLAYER_TWO), plan.rounds[3])
        assertEquals(RoundPlan(5, RoundType.CONUNDRUM, null), plan.rounds[4])
        assertEquals(plan.rounds[4], plan.nextRound(4))
        assertNull(plan.nextRound(5))
    }
}
