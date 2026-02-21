# Android Multiplayer Integration Notes (Phase A2)

This document describes the expected Android client flow against the Phase A2 backend.

## Connection URL (local)

- Emulator to host machine on Android emulator:
  - `http://10.0.2.2:4000`
- Physical device on same LAN:
  - `http://<your-local-ip>:4000`

Socket.IO uses the same base URL.

## Suggested Android event flow

1. Connect socket.
2. Emit `session:identify` with optional cached `playerId`.
3. Cache returned `playerId` from server payload.
4. Emit `queue:join` when tapping Play Online.
5. Wait for `match:found` then consume `match:state` updates.
6. Render UI from authoritative `match:state` only.
7. On reconnect, emit `session:identify` + `match:resume` with last known `matchId`.
8. Handle `action:error` codes and show user-friendly toasts/messages.

## Timer synchronization

Use `phaseEndsAtMs` and `serverNowMs` from each `match:state`.

- Do not trust local phase start time.
- Recompute remaining time on each `match:state`.
- If negative remaining time, lock input immediately and wait for next state.

## Local MVP identity

- No production auth in Phase A2.
- Use server-issued guest identity (`playerId`, `displayName`).
- Persist `playerId` in local storage to support resume/reconnect during development.

## Recommended Kotlin model notes

- Use `Long` for `*Ms` fields.
- Keep JSON keys in snake/camel as emitted by server; current payloads are `camelCase`.
- Prefer sealed model for `roundType` handling (`letters` vs `conundrum`).