package com.bhan796.anagram.core.engine

import com.bhan796.anagram.core.model.LetterKind
import kotlin.random.Random

class WeightedLetterPool(weights: Map<Char, Int>) {
    private val weightedEntries: List<Pair<Char, Int>> =
        weights
            .filterValues { it > 0 }
            .toList()
            .sortedBy { it.first }

    private val totalWeight: Int = weightedEntries.sumOf { it.second }

    fun draw(random: Random): Char {
        require(totalWeight > 0) { "Weighted pool cannot be empty" }

        val target = random.nextInt(totalWeight)
        var running = 0

        for ((letter, weight) in weightedEntries) {
            running += weight
            if (target < running) {
                return letter
            }
        }

        return weightedEntries.last().first
    }

    fun contains(letter: Char): Boolean {
        val uppercase = letter.uppercaseChar()
        return weightedEntries.any { it.first == uppercase }
    }
}

object DefaultLetterWeights {
    val vowels: Map<Char, Int> = mapOf(
        'A' to 15,
        'E' to 21,
        'I' to 13,
        'O' to 13,
        'U' to 5
    )

    val consonants: Map<Char, Int> = mapOf(
        'B' to 2,
        'C' to 3,
        'D' to 6,
        'F' to 2,
        'G' to 3,
        'H' to 2,
        'J' to 1,
        'K' to 1,
        'L' to 5,
        'M' to 4,
        'N' to 8,
        'P' to 4,
        'Q' to 1,
        'R' to 9,
        'S' to 9,
        'T' to 9,
        'V' to 1,
        'W' to 1,
        'X' to 1,
        'Y' to 1,
        'Z' to 1
    )
}

class LetterGenerator(
    vowelWeights: Map<Char, Int> = DefaultLetterWeights.vowels,
    consonantWeights: Map<Char, Int> = DefaultLetterWeights.consonants
) {
    private val vowelPool = WeightedLetterPool(vowelWeights)
    private val consonantPool = WeightedLetterPool(consonantWeights)

    fun generateLetter(kind: LetterKind, random: Random): Char {
        return when (kind) {
            LetterKind.VOWEL -> vowelPool.draw(random)
            LetterKind.CONSONANT -> consonantPool.draw(random)
        }
    }

    fun isValidLetter(letter: Char, kind: LetterKind): Boolean {
        return when (kind) {
            LetterKind.VOWEL -> vowelPool.contains(letter)
            LetterKind.CONSONANT -> consonantPool.contains(letter)
        }
    }
}