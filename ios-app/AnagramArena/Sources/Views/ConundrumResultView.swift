import SwiftUI

struct ConundrumResultView: View {
    let result: ConundrumRoundResult
    let onPlayAgain: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Text("Conundrum Result")
                .font(.title2.bold())

            Text("Scramble: \(result.conundrum.scrambled.uppercased())")
                .font(.headline)

            Text("Your Guess: \(result.submittedGuess.isEmpty ? "(none)" : result.submittedGuess)")

            Text("Answer: \(result.conundrum.answer)")
                .font(.body.weight(.semibold))

            Text(result.solved ? "Solved" : "Not Solved")
                .foregroundStyle(result.solved ? .green : .red)

            Text("Score: \(result.score)")
                .font(.title3.bold())

            Button("Try Another Conundrum", action: onPlayAgain)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}