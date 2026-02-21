import SwiftUI

struct PracticeMenuView: View {
    let dependencies: AppDependencies
    @EnvironmentObject private var settings: PracticeSettingsStore

    var body: some View {
        List {
            Section("Practice") {
                NavigationLink("Practice Letters Round") {
                    LettersPracticeView(
                        timerEnabled: settings.timerEnabled,
                        dictionary: dependencies.dictionary
                    )
                }

                NavigationLink("Practice Conundrum") {
                    ConundrumPracticeView(
                        timerEnabled: settings.timerEnabled,
                        provider: dependencies.conundrums
                    )
                }
            }

            Section("Options") {
                Toggle("Enable 30s Timer", isOn: $settings.timerEnabled)
            }
        }
        .navigationTitle("Practice Mode")
    }
}