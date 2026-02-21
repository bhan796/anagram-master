import Combine
import Foundation

final class ConundrumPracticeViewModel: ObservableObject {
    enum Phase {
        case ready
        case solving
        case result
    }

    @Published private(set) var phase: Phase = .ready
    @Published private(set) var conundrum: Conundrum?
    @Published var guess: String = ""
    @Published private(set) var secondsRemaining: Int = 30
    @Published private(set) var result: ConundrumRoundResult?

    private let provider: ConundrumProviding
    private let validator: ConundrumValidator
    private let timerEnabled: Bool
    private let roundDuration: Int
    private var timerCancellable: AnyCancellable?

    init(
        provider: ConundrumProviding,
        timerEnabled: Bool,
        roundDuration: Int = 30,
        validator: ConundrumValidator = ConundrumValidator()
    ) {
        self.provider = provider
        self.timerEnabled = timerEnabled
        self.roundDuration = roundDuration
        self.validator = validator
        self.secondsRemaining = roundDuration

        startRound()
    }

    var canSubmit: Bool {
        phase == .solving && conundrum != nil
    }

    func startRound() {
        stopTimer()

        conundrum = provider.randomConundrum()
        guess = ""
        secondsRemaining = roundDuration
        result = nil
        phase = conundrum == nil ? .ready : .solving

        guard timerEnabled, phase == .solving else { return }

        timerCancellable = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.onTick()
            }
    }

    func submit() {
        guard phase == .solving, let conundrum else { return }

        stopTimer()
        let solved = validator.isCorrect(guess: guess, answer: conundrum.answer)
        result = ConundrumRoundResult(
            conundrum: conundrum,
            submittedGuess: guess,
            solved: solved,
            score: solved ? 12 : 0
        )
        phase = .result
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