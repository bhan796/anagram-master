package com.bhan796.anagram.core.validation

class LetterFrequency(letters: List<Char>) {
    private val counts: MutableMap<Char, Int> = mutableMapOf()

    init {
        for (char in letters) {
            val normalized = char.uppercaseChar()
            counts[normalized] = (counts[normalized] ?: 0) + 1
        }
    }

    fun consume(char: Char): Boolean {
        val normalized = char.uppercaseChar()
        val current = counts[normalized] ?: return false
        if (current <= 0) return false

        counts[normalized] = current - 1
        return true
    }
}

class WordConstructabilityChecker {
    fun canConstruct(word: String, from: List<Char>): Boolean {
        val normalizedWord = WordNormalizer.normalize(word)
        if (normalizedWord.isEmpty()) return false

        val frequency = LetterFrequency(from)
        for (char in normalizedWord) {
            if (!frequency.consume(char)) {
                return false
            }
        }

        return true
    }
}

object WordNormalizer {
    fun normalize(input: String): String = input.trim().lowercase()

    fun isAlphabetical(input: String): Boolean = input.isNotEmpty() && input.all { it.isLetter() }
}