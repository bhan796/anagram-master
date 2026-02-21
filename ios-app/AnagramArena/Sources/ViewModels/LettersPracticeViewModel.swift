import Combine
import Foundation

final class LettersPracticeViewModel: ObservableObject {
    enum Phase {
        case picking
        case solving
        case result
    }

    @Published private(set) var phase: Phase = .picking
    @Published private(set) var letters: [Character] = []
    @Published private(set) var vowelCount = 0
    @Published private(set) var consonantCount = 0
    @Published var submittedWord: String = ""
    @Published private(set) var secondsRemaining: Int = 30
    @Published private(set) var result: LettersRoundResult?

    private let generator: LetterGenerator
    private let constraints: PickerConstraints
    private let validator: WordValidator
    private let timerEnabled: Bool
    private let roundDuration: Int
    private var timerCancellable: AnyCancellable?
    private var rng = SystemRandomNumberGenerator()

    init(
        dictionary: DictionaryProviding,
        timerEnabled: Bool,
        roundDuration: Int = 30,
        generator: LetterGenerator = LetterGenerator(),
        constraints: PickerConstraints = PickerConstraints()
    ) {
        self.generator = generator
        self.constraints = constraints
        self.validator = WordValidator(dictionary: dictionary)
        self.timerEnabled = timerEnabled
        self.roundDuration = roundDuration
        self.secondsRemaining = roundDuration
    }

    var allowedKinds: Set<LetterKind> {
        constraints.allowedKinds(currentPicks: letters, vowelCount: vowelCount, consonantCount: consonantCount)
    }

    var canPick: Bool {
        phase == .picking && letters.count < 9
    }

    var canSubmit: Bool {
        phase == .solving
    }

    func pick(_ kind: LetterKind) {
        guard canPick, allowedKinds.contains(kind) else { return }

        let letter = generator.generateLetter(kind: kind, using: &rng)
        letters.append(letter)

        if kind == .vowel {
            vowelCount += 1
        } else {
            consonantCount += 1
        }

        if letters.count == 9 {
            beginSolvingPhase()
        }
    }

    func submit() {
        guard phase == .solving else { return }

        stopTimer()
        let validation = validator.validate(word: submittedWord, against: letters)
        result = LettersRoundResult(letters: letters, submittedWord: submittedWord, validation: validation)
        phase = .result
    }

    func resetRound() {
        stopTimer()
        phase = .picking
        letters = []
        vowelCount = 0
        consonantCount = 0
        submittedWord = ""
        secondsRemaining = roundDuration
        result = nil
    }

    private func beginSolvingPhase() {
        phase = .solving
        secondsRemaining = roundDuration

        guard timerEnabled else { return }

        timerCancellable = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.onTick()
            }
    }

    private func onTick() {
        guard phase == .solving else {
            stopTimer()
            return
        }

        if secondsRemaining > 0 {
            secondsRemaining -= 1
        }

        if secondsRemaining == 0 {
            submit()
        }
    }

    private func stopTimer() {
        timerCancellable?.cancel()
        timerCancellable = nil
    }
}