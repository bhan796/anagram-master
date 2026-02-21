import SwiftUI

struct ConundrumPracticeView: View {
    @StateObject private var viewModel: ConundrumPracticeViewModel

    init(timerEnabled: Bool, provider: ConundrumProviding) {
        _viewModel = StateObject(wrappedValue: ConundrumPracticeViewModel(provider: provider, timerEnabled: timerEnabled))
    }

    var body: some View {
        VStack(spacing: 16) {
            switch viewModel.phase {
            case .ready:
                Text("No conundrum data available.")
                    .foregroundStyle(.secondary)
            case .solving:
                solvingView
            case .result:
                if let result = viewModel.result {
                    ConundrumResultView(result: result) {
                        viewModel.startRound()
                    }
                }
            }
        }
        .padding()
        .navigationTitle("Conundrum Practice")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var solvingView: some View {
        VStack(spacing: 14) {
            Text("Conundrum")
                .font(.title2.bold())

            Text("Time: \(viewModel.secondsRemaining)s")
                .font(.headline.monospacedDigit())

            Text(viewModel.conundrum?.scrambled.uppercased() ?? "")
                .font(.system(size: 34, weight: .heavy, design: .rounded))
                .tracking(2)

            TextField("Your guess", text: $viewModel.guess)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            Button("Submit Guess") {
                viewModel.submit()
            }
            .buttonStyle(.borderedProminent)
            .disabled(!viewModel.canSubmit)
        }
    }
}