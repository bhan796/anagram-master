# Architecture Overview

## MVP Principles

- Server authoritative for all multiplayer scoring and timers.
- Core game rule logic isolated from UI/controller layers.
- Practice mode fully offline on iOS client.
- Reconnect support for online match continuity.

## Phase 1 Implementation Notes

- Core rules are in `ios-app/AnagramArena/Sources/Core`.
- SwiftUI views delegate behavior to view models in `ios-app/AnagramArena/Sources/ViewModels`.
- Offline providers load bundled data from `ios-app/AnagramArena/Resources/Data`.
- Dictionary and conundrum providers are protocol-based for easy replacement in later phases.

## Android Phase A1 Notes

- Android logic lives in `android-app/core` (pure Kotlin, UI-independent).
- Android Compose UI and view models live in `android-app/app/src/main/java/com/bhan796/anagramarena`.
- Offline providers load bundled assets from `android-app/app/src/main/assets/data`.
- Android tests in `android-app/core/src/test` mirror iOS rule tests for parity.

## Phases

- Phase 0: Bootstrap
- Phase 1: Shared game engine + offline practice
- Phase 2: Backend multiplayer state machine + matchmaking
- Phase 3: iOS online integration
- Phase 4: Polish + persistence
