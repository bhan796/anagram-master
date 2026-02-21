package com.bhan796.anagram.core.engine

import com.bhan796.anagram.core.model.LetterKind

class PickerConstraints(private val targetSlots: Int = 9) {
    fun allowedKinds(currentPicks: List<Char>, vowelCount: Int, consonantCount: Int): Set<LetterKind> {
        val remaining = targetSlots - currentPicks.size
        if (remaining <= 0) return emptySet()

        val allowed = mutableSetOf<LetterKind>()

        if (isPickAllowed(LetterKind.VOWEL, vowelCount, consonantCount, remaining)) {
            allowed += LetterKind.VOWEL
        }

        if (isPickAllowed(LetterKind.CONSONANT, vowelCount, consonantCount, remaining)) {
            allowed += LetterKind.CONSONANT
        }

        return allowed
    }

    fun isPickAllowed(
        kind: LetterKind,
        currentVowels: Int,
        currentConsonants: Int,
        remainingSlots: Int
    ): Boolean {
        if (remainingSlots <= 0) return false

        val nextVowels = currentVowels + if (kind == LetterKind.VOWEL) 1 else 0
        val nextConsonants = currentConsonants + if (kind == LetterKind.CONSONANT) 1 else 0

        return canStillSatisfyMinimums(nextVowels, nextConsonants, remainingSlots - 1)
    }

    private fun canStillSatisfyMinimums(vowels: Int, consonants: Int, remainingSlots: Int): Boolean {
        val neededVowels = maxOf(0, 1 - vowels)
        val neededConsonants = maxOf(0, 1 - consonants)
        return neededVowels + neededConsonants <= remainingSlots
    }
}