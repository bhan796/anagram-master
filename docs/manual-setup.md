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