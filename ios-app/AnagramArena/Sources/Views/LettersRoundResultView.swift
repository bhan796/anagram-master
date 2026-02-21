import SwiftUI

struct LettersRoundResultView: View {
    let result: LettersRoundResult
    let onPlayAgain: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Text("Round Result")
                .font(.title2.bold())

            Text("Letters: \(String(result.letters))")
                .font(.headline)

            Text("Your Word: \(result.submittedWord.isEmpty ? "(none)" : result.submittedWord)")

            if result.validation.isValid {
                Text("Valid")
                    .foregroundStyle(.green)
            } else {
                Text("Invalid: \(failureText(result.validation.failure))")
                    .foregroundStyle(.red)
            }

            Text("Score: \(result.validation.score)")
                .font(.title3.bold())

            Button("Play Another Letters Round", action: onPlayAgain)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func failureText(_ failure: WordValidationFailure?) -> String {
        switch failure {
        case .empty:
            return "No word entered"
        case .nonAlphabetical:
            return "Only alphabetic words allowed"
        case .notInDictionary:
            return "Word not found"
        case .notConstructable:
            return "Cannot be built from letters"
        case .none:
            return "Unknown"
        }
    }
}