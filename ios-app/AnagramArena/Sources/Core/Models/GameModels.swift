import Foundation

enum LetterKind {
    case vowel
    case consonant
}

enum PlayerSide {
    case playerOne
    case playerTwo

    var opponent: PlayerSide {
        self == .playerOne ? .playerTwo : .playerOne
    }
}

enum RoundType {
    case letters
    case conundrum
}

struct RoundPlan: Equatable {
    let roundNumber: Int
    let type: RoundType
    let picker: PlayerSide?
}

struct MatchPlan: Equatable {
    let rounds: [RoundPlan]

    static func standard(startingPicker: PlayerSide) -> MatchPlan {
        var picker = startingPicker
        var rounds: [RoundPlan] = []

        for index in 1...4 {
            rounds.append(RoundPlan(roundNumber: index, type: .letters, picker: picker))
            picker = picker.opponent
        }

        rounds.append(RoundPlan(roundNumber: 5, type: .conundrum, picker: nil))
        return MatchPlan(rounds: rounds)
    }

    func nextRound(after roundNumber: Int) -> RoundPlan? {
        rounds.first(where: { $0.roundNumber == roundNumber + 1 })
    }
}

enum WordValidationFailure: Equatable {
    case empty
    case nonAlphabetical
    case notInDictionary
    case notConstructable
}

struct WordValidationResult: Equatable {
    let normalizedWord: String
    let isValid: Bool
    let failure: WordValidationFailure?
    let score: Int
}

struct LettersRoundResult: Equatable {
    let letters: [Character]
    let submittedWord: String
    let validation: WordValidationResult
}

struct Conundrum: Codable, Equatable {
    let id: String
    let scrambled: String
    let answer: String
}

struct ConundrumRoundResult: Equatable {
    let conundrum: Conundrum
    let submittedGuess: String
    let solved: Bool
    let score: Int
}