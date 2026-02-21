# Android App

Android Phase A0 + A1 + A3 implementation for Anagram Master.

## Scope Implemented

- Kotlin + Jetpack Compose app scaffold
- Offline practice mode parity with iOS Phase 1
- Core game rules in a testable Kotlin module (`:core`)
- Online multiplayer MVP integration (Socket.IO)
- Practice flows:
  - Home
  - Practice menu (timer toggle)
  - Letters practice round (pick -> solve -> result)
  - Conundrum practice round (solve -> result)
- Online flows:
  - Matchmaking (join/cancel)
  - Live round sync (letters + conundrum)
  - Server-authoritative timers and results
  - Reconnect/resume using persisted guest session

## Module Layout

- `android-app/app` Android application (Compose UI + ViewModels + repositories/network)
- `android-app/core` Pure Kotlin game logic + unit tests

## Package Name

- `com.bhan796.anagramarena`

## SDK Targets

- `minSdk = 26`
- `targetSdk = 35`
- `compileSdk = 35`

## Data Files

- Dictionary: `app/src/main/assets/data/dictionary_common_10k.txt` (default)
- Small sample dictionary: `app/src/main/assets/data/dictionary_sample.txt`
- Conundrums: `app/src/main/assets/data/conundrums.json`

To swap larger datasets:

1. Replace those files with larger versions.
2. Keep dictionary format as one word per line.
3. Keep conundrum JSON schema:
   - `[ { "id": "...", "scrambled": "...", "answer": "..." } ]`

## Backend URL Config

Android online mode reads `BuildConfig.BACKEND_BASE_URL`.

Default value:

- `https://anagram-server-production.up.railway.app`

Override at build time:

- Windows: `gradlew.bat :app:assembleDebug -PbackendBaseUrl=https://your-host`
- macOS/Linux: `./gradlew :app:assembleDebug -PbackendBaseUrl=https://your-host`

## Run (Android Studio)

1. Open Android Studio.
2. Open the `android-app` folder as a project.
3. Let Gradle sync finish.
4. Ensure SDK Platform 35 and Build Tools 34 are installed.
5. Run `app` on emulator or device.

## CLI Commands

From `android-app/`:

- Run app/core tests:
  - `./gradlew :app:testDebugUnitTest :core:test` (Windows: `gradlew.bat :app:testDebugUnitTest :core:test`)
- Build debug APK:
  - `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`)

## Online Manual Verification

See `docs/testing/android-online-manual.md` for two-emulator/device script.

## Notes

- Core game logic remains UI-independent for parity and testability.
- Multiplayer uses server authority for timers/scores/round transitions.
- Production auth is intentionally out-of-scope for this MVP phase.
