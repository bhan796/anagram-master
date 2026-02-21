# Backend Deployment + Android Endpoint Guide

This guide covers local and hosted deployment with Android client implications.

## Local Backend

From repo root:

1. `npm install`
2. `Copy-Item server/.env.example server/.env`
3. `npm run server:dev`

Health check:

- `http://localhost:4000/api/health`

Android local endpoint:

- Emulator: `http://10.0.2.2:4000`
- Physical device: `http://<LAN-IP>:4000`

Build with local endpoint:

- `cd android-app`
- `gradlew.bat :app:assembleDebug -PbackendBaseUrl=http://10.0.2.2:4000`

## Hosted Backend (Railway)

### Minimum service setup

- Root directory: `/server`
- Build command: `npm install && npm run build`
- Start command: `npm run start`

### Minimum environment variables

- `NODE_ENV=production`
- `LOG_LEVEL=info`
- `CLIENT_ORIGIN=*` (tighten later)
- Optional but recommended for future persistence integration:
  - `DATABASE_URL`
  - `REDIS_URL`

### Domain routing

- Ensure generated domain target port matches app listener.
- If using Railway port injection, do not hardcode mismatched values.

## Android endpoint policy

`android-app/app/build.gradle.kts` uses `BuildConfig.BACKEND_BASE_URL`.

Default:

- `https://anagram-server-production.up.railway.app`

Override per build:

- `-PbackendBaseUrl=https://your-staging-host`

## Values that should not be permanently hardcoded

- Staging/prod backend hostnames per release flavor.
- API keys/secrets (none in app currently, keep it that way).
- Environment-only debug overrides.
