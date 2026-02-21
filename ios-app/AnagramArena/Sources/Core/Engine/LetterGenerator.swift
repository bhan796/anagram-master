import Foundation

struct WeightedLetterPool {
    private let weightedEntries: [(letter: Character, weight: Int)]
    private let totalWeight: Int

    init(weights: [Character: Int]) {
        self.weightedEntries = weights
            .filter { $0.value > 0 }
            .sorted(by: { $0.key < $1.key })
            .map { (letter: $0.key, weight: $0.value) }
        self.totalWeight = weightedEntries.reduce(0) { $0 + $1.weight }
    }

    func draw<R: RandomNumberGenerator>(using generator: inout R) -> Character {
        precondition(totalWeight > 0, "Weighted pool cannot be empty")

        let target = Int.random(in: 0..<totalWeight, using: &generator)
        var running = 0

        for entry in weightedEntries {
            running += entry.weight
            if target < running {
                return entry.letter
            }
        }

        return weightedEntries[weightedEntries.count - 1].letter
    }

    func contains(_ letter: Character) -> Bool {
        weightedEntries.contains(where: { $0.letter == letter.uppercased().first })
    }

    var letters: Set<Character> {
        Set(weightedEntries.map(\.letter))
    }
}

enum DefaultLetterWeights {
    static let vowels: [Character: Int] = [
        "A": 15,
        "E": 21,
        "I": 13,
        "O": 13,
        "U": 5
    ]

    static let consonants: [Character: Int] = [
        "B": 2,
        "C": 3,
        "D": 6,
        "F": 2,
        "G": 3,
        "H": 2,
        "J": 1,
        "K": 1,
        "L": 5,
        "M": 4,
        "N": 8,
        "P": 4,
        "Q": 1,
        "R": 9,
        "S": 9,
        "T": 9,
        "V": 1,
        "W": 1,
        "X": 1,
        "Y": 1,
        "Z": 1
    ]
}

struct LetterGenerator {
    private let vowelPool: WeightedLetterPool
    private let consonantPool: WeightedLetterPool

    init(
        vowelWeights: [Character: Int] = DefaultLetterWeights.vowels,
        consonantWeights: [Character: Int] = DefaultLetterWeights.consonants
    ) {
        self.vowelPool = WeightedLetterPool(weights: vowelWeights)
        self.consonantPool = WeightedLetterPool(weights: consonantWeights)
    }

    func generateLetter<R: RandomNumberGenerator>(kind: LetterKind, using generator: inout R) -> Character {
        switch kind {
        case .vowel:
            return vowelPool.draw(using: &generator)
        case .consonant:
            return consonantPool.draw(using: &generator)
        }
    }

    func isValidLetter(_ letter: Character, for kind: LetterKind) -> Bool {
        switch kind {
        case .vowel:
            return vowelPool.contains(letter)
        case .consonant:
            return consonantPool.contains(letter)
        }
    }
}
