# API and Event Docs

- Socket contract: `docs/api/websocket-contract.md`
- Android integration notes: `docs/api/android-integration.md`

## Current Backend Endpoints

- `GET /api/health`
- `GET /api/profiles/:playerId/stats`
- `GET /api/profiles/:playerId/matches?limit=20`

## Transport

- Realtime gameplay: Socket.IO
- Authoritative server timer and score logic
