package com.bhan796.anagram.core.validation

import com.bhan796.anagram.core.model.WordValidationFailure
import com.bhan796.anagram.core.model.WordValidationResult

interface DictionaryProvider {
    fun contains(normalizedWord: String): Boolean
}

class InMemoryDictionaryProvider(private val words: Set<String>) : DictionaryProvider {
    override fun contains(normalizedWord: String): Boolean = words.contains(normalizedWord)
}

class WordScorer {
    fun scoreLettersWord(length: Int): Int {
        if (length <= 0) return 0
        return if (length == 9) 12 else length
    }
}

class WordValidator(
    private val dictionary: DictionaryProvider,
    private val checker: WordConstructabilityChecker = WordConstructabilityChecker(),
    private val scorer: WordScorer = WordScorer()
) {
    fun validate(word: String, letters: List<Char>): WordValidationResult {
        val normalized = WordNormalizer.normalize(word)

        if (normalized.isEmpty()) {
            return WordValidationResult(normalizedWord = normalized, isValid = false, failure = WordValidationFailure.EMPTY, score = 0)
        }

        if (!WordNormalizer.isAlphabetical(normalized)) {
            return WordValidationResult(normalizedWord = normalized, isValid = false, failure = WordValidationFailure.NON_ALPHABETICAL, score = 0)
        }

        if (!dictionary.contains(normalized)) {
            return WordValidationResult(normalizedWord = normalized, isValid = false, failure = WordValidationFailure.NOT_IN_DICTIONARY, score = 0)
        }

        if (!checker.canConstruct(normalized, letters)) {
            return WordValidationResult(normalizedWord = normalized, isValid = false, failure = WordValidationFailure.NOT_CONSTRUCTABLE, score = 0)
        }

        val score = scorer.scoreLettersWord(normalized.length)
        return WordValidationResult(normalizedWord = normalized, isValid = true, failure = null, score = score)
    }
}

class ConundrumValidator {
    fun isCorrect(guess: String, answer: String): Boolean {
        return WordNormalizer.normalize(guess) == WordNormalizer.normalize(answer)
    }
}