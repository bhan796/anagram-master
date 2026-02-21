# Android App

Android MVP client for Anagram Master.

## Implemented Scope

- Offline practice mode (letters + conundrum)
- Online multiplayer flow (matchmaking -> live rounds -> results)
- Reconnect/resume support for active matches
- Profile/stats screen backed by backend stats/history endpoints
- Settings screen with persisted toggles (timer, sound/vibration placeholders)

## Modules

- `app/` Compose UI, networking, repositories, view models
- `core/` pure Kotlin game logic and tests

## Requirements

- Android Studio (latest stable)
- Android SDK Platform 35
- Android SDK Build Tools 34.0.0
- JDK 17 (Gradle JDK)

## Build and Test

From `android-app/`:

- Unit tests:
  - `gradlew.bat :app:testDebugUnitTest :core:test`
- Debug build:
  - `gradlew.bat :app:assembleDebug`

## Backend URL Configuration

Default endpoint is baked into `BuildConfig.BACKEND_BASE_URL` via Gradle.

- Default: `https://anagram-server-production.up.railway.app`

Override for debug/local testing:

- `gradlew.bat :app:assembleDebug -PbackendBaseUrl=http://10.0.2.2:4000`

## Online Testing

Manual multiplayer flow:

- `docs/testing/android-online-manual.md`

Comprehensive QA checklist:

- `docs/testing/android-qa-checklist.md`

Internal release checklist:

- `docs/testing/android-release-checklist.md`

## Known Limitations / Next Improvements

- No production auth (guest sessions only)
- Limited visual polish on online round transitions
- Telemetry is local log abstraction only (no external analytics pipeline yet)
- No ranked matchmaking/private rooms yet
