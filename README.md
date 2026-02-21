# Anagram Master

Production-oriented MVP for a real-time head-to-head anagram game.

## Repo Layout

- `android-app/` Kotlin + Compose client (offline + online MVP)
- `ios-app/` SwiftUI client scaffold (offline practice complete)
- `server/` authoritative Node.js + TypeScript backend
- `shared/` shared datasets/specs
- `docs/` architecture, API, setup, deployment, testing

## Current Phase Status

- Phase 0/1 complete: core game logic + offline practice
- Phase A2 complete: backend realtime foundation
- Phase A3 complete: Android online multiplayer integration
- Phase A4 complete: Android polish + stats/history API + deployment/readiness docs

## Fresh Machine Quick Start

### Backend

1. Install Node.js 20+
2. `npm install`
3. `Copy-Item server/.env.example server/.env`
4. `npm run server:dev`
5. Verify `http://localhost:4000/api/health`

### Android

1. Install Android Studio + SDK 35 + Build Tools 34
2. Open `android-app`
3. Run tests:
   - `cd android-app`
   - `gradlew.bat :app:testDebugUnitTest :core:test`
4. Build debug APK:
   - `gradlew.bat :app:assembleDebug`

### Android Backend Endpoint

- Default: `https://anagram-server-production.up.railway.app`
- Override per build:
  - `gradlew.bat :app:assembleDebug -PbackendBaseUrl=http://10.0.2.2:4000`

## Key Docs

- Socket contract: `docs/api/websocket-contract.md`
- Android integration notes: `docs/api/android-integration.md`
- Deployment guide: `docs/deployment-backend-android.md`
- Balancing guide: `docs/balancing-config.md`
- Manual setup checklist: `docs/manual-setup.md`
- QA checklist: `docs/testing/android-qa-checklist.md`
- Release checklist: `docs/testing/android-release-checklist.md`

## Known Limitations / Next Improvements

- Guest identity only (no production auth yet)
- Match history persistence currently file-based MVP path
- No private rooms/ranked mode yet
- iOS online parity still pending
