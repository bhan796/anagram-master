# Manual Setup Checklist

This project is designed to progress even when external services are not configured. Use this checklist when enabling full multiplayer and device distribution.

## 1. Apple Developer + Xcode Signing

Needed for: running on physical iPhone and App Store/TestFlight distribution.

### Steps

1. Create/sign in to Apple Developer account.
2. In Xcode, add your Apple ID under Settings > Accounts.
3. Select the app target and choose a unique bundle identifier.
4. Enable Automatic Signing with your Team.

### Secrets/IDs to copy

- Team ID
- Bundle Identifier

### Where to apply

- `ios-app/AnagramArena/project.yml` (`PRODUCT_BUNDLE_IDENTIFIER`)
- Generated Xcode project Signing settings

### Verify

- Build and run on a physical iPhone with no signing errors.

## 2. Postgres Database

Needed for: persisted match/player data in backend.

### Steps

1. Install local Postgres (or provision managed instance).
2. Create database `anagram_master`.
3. Update connection string in `server/.env`.
4. Run migrations.

### Secrets/keys to copy

- Postgres connection URL (`DATABASE_URL`)

### Where to apply

- `server/.env`

### Verify

- `npm run prisma:migrate --workspace server -- --name init` succeeds.

## 3. Redis

Needed for: matchmaking queue and transient realtime state.

### Steps

1. Install local Redis (or provision managed Redis).
2. Ensure Redis is reachable.
3. Set `REDIS_URL` in `server/.env`.

### Secrets/keys to copy

- Redis URL (`REDIS_URL`)

### Where to apply

- `server/.env`

### Verify

- Start backend and confirm no Redis connection errors in logs.

## 4. Optional Hosting (later phases)

Needed for: internet-accessible multiplayer.

### Steps

1. Choose host (Railway/Fly.io/Render/AWS).
2. Provision Node runtime, Postgres, Redis.
3. Set environment variables in host dashboard.
4. Deploy and capture public backend URL.

### Secrets/keys to copy

- Host app URL
- Host-provided DB/Redis credentials

### Where to apply

- iOS config for production API endpoint
- host env settings

### Verify

- iOS app can connect and complete a live match.

## 6. Backend Services for Online Multiplayer (when moving beyond local)

Needed for: production-style reliability and persistence (Postgres + Redis).

### Recommended free-tier starter stack

1. Backend host: Railway
2. Postgres: Neon
3. Redis: Upstash Redis

### Steps

1. Create Railway project and deploy `server/` as Node service.
2. Create Neon database; copy connection string.
3. Create Upstash Redis database; copy Redis URL.
4. In Railway service variables, set:
   - `DATABASE_URL`
   - `REDIS_URL`
   - `PORT` (host default if required)
   - `CLIENT_ORIGIN` (Android test origin for web tools if needed)
5. Configure Android app base socket URL to Railway endpoint.

### Secrets/keys to copy

- Neon `DATABASE_URL`
- Upstash `REDIS_URL`
- Railway service public URL

### Where to apply

- `server/.env` for local mirror config
- host environment variable dashboard
- Android app network config (future Phase A3)

### Verify

- `GET /api/health` returns status from deployed host
- two Android clients can complete one full online match

## 5. Android Local Build and Run

Needed for: running and testing the Android practice-mode app.

### Steps

1. Install Android Studio (latest stable).
2. Install Android SDK components from SDK Manager:
   - Android SDK Platform 35
   - Android SDK Build-Tools 34.0.0
   - Android SDK Platform-Tools
3. Install JDK 17 or let Android Studio use bundled JetBrains Runtime.
4. Open `android-app` folder in Android Studio.
5. Allow Gradle sync to complete.
6. Accept SDK licenses if prompted (or via `sdkmanager --licenses`).
7. Create/start an emulator (Pixel API 35 recommended) or connect a device with USB debugging.
8. Run the `app` configuration.

### Secrets/keys to copy

- None for offline Phase A1.

### Where to apply

- SDK path in local machine config (`local.properties`, not committed).

### Verify

- Home -> Practice Mode loads.
- Letters and Conundrum practice flows work offline.
- Core tests pass: `cd android-app && gradlew.bat :core:test` (Windows) or `./gradlew :core:test` (macOS/Linux).

### Common fixes

- If build fails with license errors:
  - Install missing SDK packages in Android Studio SDK Manager.
  - Accept licenses (`sdkmanager --licenses`).
- If Gradle sync fails due JDK:
  - Set Gradle JDK to 17 in Android Studio:
    - Settings -> Build, Execution, Deployment -> Build Tools -> Gradle.

## 7. Android Online Networking Tips (Phase A3)

Needed for: connecting Android app to multiplayer backend.

### Base URL notes

- Hosted backend (Railway): use HTTPS URL, e.g. `https://anagram-server-production.up.railway.app`
- Android emulator to local backend:
  - `http://10.0.2.2:4000` (not `localhost`)
- Physical Android device to local backend:
  - `http://<your-computer-lan-ip>:4000`

### TLS / cleartext notes

- HTTPS recommended for hosted environments.
- If using local HTTP URLs for debug, ensure Android network security policy allows cleartext for debug builds if needed.

### Verify

1. Hit backend health endpoint from Android browser:
   - `<baseUrl>/api/health`
2. Launch Play Online on two clients and verify matchmaking succeeds.
