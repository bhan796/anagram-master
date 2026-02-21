# Android Internal Testing Release Checklist (MVP)

## Build and Config

- [ ] Confirm backend endpoint for this build (`BACKEND_BASE_URL`).
- [ ] `gradlew.bat :app:testDebugUnitTest :core:test` passes.
- [ ] `gradlew.bat :app:assembleDebug` passes.
- [ ] Build metadata/versionCode/versionName updated as needed.

## Functional Smoke

- [ ] Offline practice letters and conundrum flows pass.
- [ ] Online matchmaking and full 5-round match pass.
- [ ] Disconnect/reconnect flow passes.
- [ ] Profile/stats screen loads after at least one online match.

## Release Prep

- [ ] Signing config verified for release keystore.
- [ ] HTTPS backend URL used for release builds.
- [ ] `CLIENT_ORIGIN` and server env tightened for production.
- [ ] Known limitations documented and shared with testers.
