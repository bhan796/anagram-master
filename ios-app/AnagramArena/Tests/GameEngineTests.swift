import XCTest
@testable import AnagramArena

final class GameEngineTests: XCTestCase {
    func testScoringRulesIncludeNineLetterBonus() {
        let scorer = WordScorer()

        XCTAssertEqual(scorer.scoreLettersWord(length: 0), 0)
        XCTAssertEqual(scorer.scoreLettersWord(length: 7), 7)
        XCTAssertEqual(scorer.scoreLettersWord(length: 9), 12)
    }

    func testConstructabilityCheckerUsesLetterFrequency() {
        let checker = WordConstructabilityChecker()

        XCTAssertTrue(checker.canConstruct(word: "stone", from: Array("STONEXYZ")))
        XCTAssertFalse(checker.canConstruct(word: "stoon", from: Array("STONEABCD")))
        XCTAssertFalse(checker.canConstruct(word: "", from: Array("STONEABCD")))
    }

    func testWeightedGeneratorReturnsOnlyValidPoolLetters() {
        var rng = SeededGenerator(seed: 12345)
        let generator = LetterGenerator()

        for _ in 0..<500 {
            let vowel = generator.generateLetter(kind: .vowel, using: &rng)
            let consonant = generator.generateLetter(kind: .consonant, using: &rng)

            XCTAssertTrue(generator.isValidLetter(vowel, for: .vowel))
            XCTAssertTrue(generator.isValidLetter(consonant, for: .consonant))
        }
    }

    func testPickerConstraintPreventsImpossibleFinalState() {
        let constraints = PickerConstraints(targetSlots: 9)

        let onlyConsonantsSoFar = Array("BCDFGHJK")
        let allowed = constraints.allowedKinds(currentPicks: onlyConsonantsSoFar, vowelCount: 0, consonantCount: 8)

        XCTAssertTrue(allowed.contains(.vowel))
        XCTAssertFalse(allowed.contains(.consonant))
    }

    func testWordValidatorRejectsNonAlphabeticalWordsAndAcceptsValidConstructableWords() {
        let dictionary = InMemoryDictionaryProvider(words: ["stone", "notebooks"])
        let validator = WordValidator(dictionary: dictionary)

        let valid = validator.validate(word: "Stone", against: Array("STONEXYZA"))
        XCTAssertTrue(valid.isValid)
        XCTAssertEqual(valid.score, 5)

        let invalid = validator.validate(word: "stone-1", against: Array("STONEXYZA"))
        XCTAssertFalse(invalid.isValid)
        XCTAssertEqual(invalid.failure, .nonAlphabetical)
    }

    func testConundrumAnswerValidationIsCaseInsensitive() {
        let validator = ConundrumValidator()

        XCTAssertTrue(validator.isCorrect(guess: "Algorithm", answer: "algorithm"))
        XCTAssertFalse(validator.isCorrect(guess: "algorithms", answer: "algorithm"))
    }

    func testMatchProgressionBuildsFourLettersRoundsAndConundrumFinal() {
        let plan = MatchPlan.standard(startingPicker: .playerOne)

        XCTAssertEqual(plan.rounds.count, 5)
        XCTAssertEqual(plan.rounds[0], RoundPlan(roundNumber: 1, type: .letters, picker: .playerOne))
        XCTAssertEqual(plan.rounds[1], RoundPlan(roundNumber: 2, type: .letters, picker: .playerTwo))
        XCTAssertEqual(plan.rounds[2], RoundPlan(roundNumber: 3, type: .letters, picker: .playerOne))
        XCTAssertEqual(plan.rounds[3], RoundPlan(roundNumber: 4, type: .letters, picker: .playerTwo))
        XCTAssertEqual(plan.rounds[4], RoundPlan(roundNumber: 5, type: .conundrum, picker: nil))
        XCTAssertEqual(plan.nextRound(after: 4), plan.rounds[4])
        XCTAssertNil(plan.nextRound(after: 5))
    }
}

private struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64

    init(seed: UInt64) {
        self.state = seed
    }

    mutating func next() -> UInt64 {
        state = 6364136223846793005 &* state &+ 1
        return state
    }
}