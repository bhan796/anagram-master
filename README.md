# Anagram Master (iOS + Backend)

Production-oriented MVP scaffold for a real-time head-to-head anagram game.

## Repository Layout

- `ios-app/` SwiftUI iOS client scaffold (iOS-first)
- `android-app/` Kotlin + Compose Android client scaffold
- `server/` Node.js + TypeScript + Socket.IO backend scaffold
- `shared/` shared docs/specs/data placeholders
- `docs/` architecture, API, setup, and rollout docs

## Current Status

- Phase 0 complete: bootstrap, structure, tooling, and docs skeleton
- Phase 1 complete: shared core game logic + offline iOS practice mode
- Phase A0+A1 complete: Android scaffold + offline practice mode parity
- Phase 2 next: real-time backend authoritative multiplayer

## Technology Decisions (MVP)

- iOS client: SwiftUI + MVVM style separation
- Backend: Node.js + TypeScript + Socket.IO
- Data stores (target): Postgres + Redis
- iOS minimum deployment target: **iOS 17.0**

## Quick Start

### Backend

1. Install Node.js 20+ and npm 10+
2. Install dependencies:
   - `npm install`
3. Copy env file:
   - `Copy-Item server/.env.example server/.env`
4. Start dev server:
   - `npm run server:dev`

### iOS App (on macOS)

This workspace includes an XcodeGen spec to create the Xcode project.

1. Install Xcode 15+
2. Install XcodeGen (`brew install xcodegen`)
3. From `ios-app/AnagramArena`, run:
   - `xcodegen generate`
4. Open `AnagramArena.xcodeproj` and run on an iPhone simulator

### Android App

1. Open `android-app` in Android Studio
2. Allow Gradle sync to complete
3. Install required SDK components if prompted
4. Run `app` on emulator/device

## Commands

- `npm run server:dev`
- `npm run server:build`
- `npm run server:test`
- `npm run server:lint`
- `npm run server:format`
- `cd android-app && ./gradlew :core:test`
- `cd android-app && ./gradlew :app:assembleDebug`

## Notes

- Practice mode works fully offline with bundled dictionary/conundrum data.
- Android and iOS both support offline practice mode in current state.
- Multiplayer services are scaffolded now and become authoritative in Phase 2.
- See `docs/manual-setup.md` for external account/service setup steps.
