# Anagram Master (iOS + Backend)

Production-oriented MVP scaffold for a real-time head-to-head anagram game.

## Repository Layout

- `ios-app/` SwiftUI iOS client scaffold (iOS-first)
- `server/` Node.js + TypeScript + Socket.IO backend scaffold
- `shared/` shared docs/specs/data placeholders
- `docs/` architecture, API, setup, and rollout docs

## Current Status

- Phase 0 complete: bootstrap, structure, tooling, and docs skeleton
- Phase 1 next: shared game engine + fully offline iOS practice mode

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

## Commands

- `npm run server:dev`
- `npm run server:build`
- `npm run server:test`
- `npm run server:lint`
- `npm run server:format`

## Notes

- Practice mode will be fully offline in Phase 1.
- Multiplayer services are scaffolded now and will become authoritative in Phase 2.
- See `docs/manual-setup.md` for external account/service setup steps.