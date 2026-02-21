import SwiftUI

struct LettersPracticeView: View {
    @StateObject private var viewModel: LettersPracticeViewModel

    init(timerEnabled: Bool, dictionary: DictionaryProviding) {
        _viewModel = StateObject(wrappedValue: LettersPracticeViewModel(dictionary: dictionary, timerEnabled: timerEnabled))
    }

    var body: some View {
        VStack(spacing: 16) {
            switch viewModel.phase {
            case .picking:
                pickingPhaseView
            case .solving:
                solvingPhaseView
            case .result:
                if let result = viewModel.result {
                    LettersRoundResultView(result: result) {
                        viewModel.resetRound()
                    }
                }
            }
        }
        .padding()
        .navigationTitle("Letters Practice")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var pickingPhaseView: some View {
        VStack(spacing: 14) {
            Text("Pick 9 Letters")
                .font(.title2.bold())

            Text("Progress: \(viewModel.letters.count)/9")
                .foregroundStyle(.secondary)

            letterSlots

            HStack(spacing: 12) {
                Button("Vowel") {
                    viewModel.pick(.vowel)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.allowedKinds.contains(.vowel))

                Button("Consonant") {
                    viewModel.pick(.consonant)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.allowedKinds.contains(.consonant))
            }

            Text("Vowels: \(viewModel.vowelCount)  Consonants: \(viewModel.consonantCount)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private var solvingPhaseView: some View {
        VStack(spacing: 14) {
            Text("Solve")
                .font(.title2.bold())

            Text("Time: \(viewModel.secondsRemaining)s")
                .font(.headline.monospacedDigit())

            letterSlots

            TextField("Enter your word", text: $viewModel.submittedWord)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            Button("Submit") {
                viewModel.submit()
            }
            .buttonStyle(.borderedProminent)
            .disabled(!viewModel.canSubmit)
        }
    }

    private var letterSlots: some View {
        HStack(spacing: 6) {
            ForEach(0..<9, id: \.self) { index in
                Text(index < viewModel.letters.count ? String(viewModel.letters[index]) : "_")
                    .font(.title3.weight(.semibold))
                    .frame(width: 32, height: 42)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
    }
}