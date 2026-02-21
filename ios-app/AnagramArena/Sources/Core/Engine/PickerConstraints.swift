import Foundation

struct PickerConstraints {
    let targetSlots: Int

    init(targetSlots: Int = 9) {
        self.targetSlots = targetSlots
    }

    func allowedKinds(currentPicks: [Character], vowelCount: Int, consonantCount: Int) -> Set<LetterKind> {
        let remaining = targetSlots - currentPicks.count
        guard remaining > 0 else { return [] }

        var allowed: Set<LetterKind> = []

        if isPickAllowed(.vowel, currentVowels: vowelCount, currentConsonants: consonantCount, remainingSlots: remaining) {
            allowed.insert(.vowel)
        }

        if isPickAllowed(.consonant, currentVowels: vowelCount, currentConsonants: consonantCount, remainingSlots: remaining) {
            allowed.insert(.consonant)
        }

        return allowed
    }

    func isPickAllowed(
        _ kind: LetterKind,
        currentVowels: Int,
        currentConsonants: Int,
        remainingSlots: Int
    ) -> Bool {
        guard remainingSlots > 0 else { return false }

        let nextVowels = currentVowels + (kind == .vowel ? 1 : 0)
        let nextConsonants = currentConsonants + (kind == .consonant ? 1 : 0)
        return canStillSatisfyMinimums(vowels: nextVowels, consonants: nextConsonants, remainingSlots: remainingSlots - 1)
    }

    private func canStillSatisfyMinimums(vowels: Int, consonants: Int, remainingSlots: Int) -> Bool {
        let neededVowels = max(0, 1 - vowels)
        let neededConsonants = max(0, 1 - consonants)
        return neededVowels + neededConsonants <= remainingSlots
    }
}