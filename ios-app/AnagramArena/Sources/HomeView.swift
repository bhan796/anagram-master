import SwiftUI

struct HomeView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Anagram Arena")
                    .font(.largeTitle.bold())

                Text("Phase 0 scaffold")
                    .foregroundStyle(.secondary)

                Button("Play Online") {}
                    .buttonStyle(.borderedProminent)

                Button("Practice Mode") {}
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
    HomeView()
}