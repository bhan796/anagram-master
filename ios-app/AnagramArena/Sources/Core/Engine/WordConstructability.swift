import Foundation

struct LetterFrequency {
    private(set) var counts: [Character: Int] = [:]

    init(_ letters: [Character]) {
        for char in letters {
            let normalized = Character(String(char).uppercased())
            counts[normalized, default: 0] += 1
        }
    }

    mutating func consume(_ char: Character) -> Bool {
        let normalized = Character(String(char).uppercased())
        guard let current = counts[normalized], current > 0 else {
            return false
        }

        counts[normalized] = current - 1
        return true
    }
}

struct WordConstructabilityChecker {
    func canConstruct(word: String, from letters: [Character]) -> Bool {
        let normalizedWord = WordNormalizer.normalize(word)
        guard !normalizedWord.isEmpty else { return false }

        var frequency = LetterFrequency(letters)

        for char in normalizedWord {
            if !frequency.consume(char) {
                return false
            }
        }

        return true
    }
}

enum WordNormalizer {
    static func normalize(_ input: String) -> String {
        input
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
    }

    static func isAlphabetical(_ input: String) -> Bool {
        !input.isEmpty && input.unicodeScalars.allSatisfy(CharacterSet.letters.contains)
    }
}