# Server

Authoritative multiplayer backend for Anagram Master.

## Stack

- Node.js + TypeScript
- Express + Socket.IO
- In-memory live match state + file-backed match history persistence (MVP)

## Current Capabilities

- Guest session identity (`session:identify`)
- Random matchmaking queue
- Authoritative 5-round match state machine
- Server-owned timers, scoring, and transitions
- Letters validation and conundrum first-correct logic
- Reconnect/resume snapshots
- Match history + basic player stats endpoints

## API Endpoints

- `GET /api/health`
- `GET /api/profiles/:playerId/stats`
- `GET /api/profiles/:playerId/matches?limit=20`

## Setup

1. Install Node.js 20+
2. `npm install`
3. `Copy-Item .env.example .env`
4. `npm run dev`

## Scripts

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run test`
- `npm run lint`

## Notes

- Runtime match state is in-memory for fast MVP iteration.
- Match history persistence is file-based (`server/data/runtime-history.json`) in this phase.
- For stronger production durability, move history/stats to Redis/Postgres adapters in next phase.
