# Server

Authoritative multiplayer backend for Anagram Master.

## Stack

- Node.js + TypeScript
- Express + Socket.IO
- In-memory match state (Phase A2)
- Prisma + Redis scaffolds retained for future persistence layers

## Phase A2 Capabilities

- Guest session identity (`session:identify`)
- Random matchmaking queue
- Authoritative 5-round state machine
- Server-owned round timers and state transitions
- Letters-round validation and scoring
- Conundrum first-correct resolution
- Conundrum guess rate limiting
- Reconnect/resume snapshot support
- Match lifecycle logging

## Setup

1. Install Node.js 20+
2. `npm install`
3. Copy env file:
   - PowerShell: `Copy-Item .env.example .env`
4. Start backend:
   - `npm run dev`

Server default URL: `http://localhost:4000`

## Scripts

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run test`
- `npm run lint`
- `npm run format`

## Android Local Testing

- Android emulator backend URL: `http://10.0.2.2:4000`
- Device on LAN URL: `http://<your-local-ip>:4000`
- Socket contract docs:
  - `docs/api/websocket-contract.md`
  - `docs/api/android-integration.md`

## Persistence Roadmap

Current runtime state is in-memory for MVP speed.

To upgrade without rewriting socket handlers:

1. Replace queue/match maps with Redis-backed store.
2. Persist finalized matches and player stats in Postgres.
3. Keep `MatchService` contract stable and swap store adapters.
