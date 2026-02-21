import SwiftUI

struct HomeView: View {
    let dependencies: AppDependencies

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Anagram Arena")
                    .font(.largeTitle.bold())

                NavigationLink("Play Online") {
                    Text("Online multiplayer arrives in Phase 2.")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.borderedProminent)

                NavigationLink("Practice Mode") {
                    PracticeMenuView(dependencies: dependencies)
                }
                .buttonStyle(.bordered)

                Button("Profile / Stats") {}
                    .buttonStyle(.bordered)

                Button("Settings") {}
                    .buttonStyle(.bordered)
            }
            .padding(24)
        }
    }
}

#Preview {
    HomeView(dependencies: .live)
        .environmentObject(PracticeSettingsStore())
}