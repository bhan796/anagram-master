import SwiftUI

@main
struct AnagramArenaApp: App {
    @StateObject private var settings = PracticeSettingsStore()
    private let dependencies = AppDependencies.live

    var body: some Scene {
        WindowGroup {
            HomeView(dependencies: dependencies)
                .environmentObject(settings)
        }
    }
}