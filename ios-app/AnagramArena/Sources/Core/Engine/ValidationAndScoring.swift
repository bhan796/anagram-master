import Foundation

protocol DictionaryProviding {
    func contains(_ normalizedWord: String) -> Bool
}

struct WordScorer {
    func scoreLettersWord(length: Int) -> Int {
        guard length > 0 else { return 0 }
        return length == 9 ? 12 : length
    }
}

struct WordValidator {
    private let dictionary: DictionaryProviding
    private let checker: WordConstructabilityChecker
    private let scorer: WordScorer

    init(
        dictionary: DictionaryProviding,
        checker: WordConstructabilityChecker = WordConstructabilityChecker(),
        scorer: WordScorer = WordScorer()
    ) {
        self.dictionary = dictionary
        self.checker = checker
        self.scorer = scorer
    }

    func validate(word rawWord: String, against letters: [Character]) -> WordValidationResult {
        let normalized = WordNormalizer.normalize(rawWord)

        guard !normalized.isEmpty else {
            return WordValidationResult(normalizedWord: normalized, isValid: false, failure: .empty, score: 0)
        }

        guard WordNormalizer.isAlphabetical(normalized) else {
            return WordValidationResult(normalizedWord: normalized, isValid: false, failure: .nonAlphabetical, score: 0)
        }

        guard dictionary.contains(normalized) else {
            return WordValidationResult(normalizedWord: normalized, isValid: false, failure: .notInDictionary, score: 0)
        }

        guard checker.canConstruct(word: normalized, from: letters) else {
            return WordValidationResult(normalizedWord: normalized, isValid: false, failure: .notConstructable, score: 0)
        }

        let score = scorer.scoreLettersWord(length: normalized.count)
        return WordValidationResult(normalizedWord: normalized, isValid: true, failure: nil, score: score)
    }
}

struct ConundrumValidator {
    func isCorrect(guess: String, answer: String) -> Bool {
        WordNormalizer.normalize(guess) == WordNormalizer.normalize(answer)
    }
}