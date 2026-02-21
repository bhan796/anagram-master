# Android App

Android Phase A0 + A1 implementation for Anagram Master.

## Scope Implemented

- Kotlin + Jetpack Compose app scaffold
- Offline practice mode parity with iOS Phase 1
- Core game rules in a testable Kotlin module (`:core`)
- Practice flows:
  - Home
  - Practice menu (timer toggle)
  - Letters practice round (pick -> solve -> result)
  - Conundrum practice round (solve -> result)

## Module Layout

- `android-app/app` Android application (Compose UI + ViewModels + asset loaders)
- `android-app/core` Pure Kotlin game logic + unit tests

## Package Name

- `com.bhan796.anagramarena`

## SDK Targets

- `minSdk = 26`
- `targetSdk = 35`
- `compileSdk = 35`

## Data Files

- Dictionary: `app/src/main/assets/data/dictionary_sample.txt`
- Conundrums: `app/src/main/assets/data/conundrums.json`

To swap larger datasets:

1. Replace those files with larger versions.
2. Keep dictionary format as one word per line.
3. Keep conundrum JSON schema:
   - `[ { "id": "...", "scrambled": "...", "answer": "..." } ]`

## Run (Android Studio)

1. Open Android Studio.
2. Open the `android-app` folder as a project.
3. Let Gradle sync finish.
4. Ensure SDK Platform 35 and Build Tools 34 are installed.
5. Run `app` on emulator or device.

## CLI Commands

From `android-app/`:

- Run core logic tests:
  - `./gradlew :core:test` (Windows: `gradlew.bat :core:test`)
- Build debug APK:
  - `./gradlew :app:assembleDebug` (requires Android SDK licenses accepted)

## Notes

- Core logic is intentionally UI-independent for parity and testability.
- Multiplayer is intentionally not implemented in Android yet (Phase A1 only).