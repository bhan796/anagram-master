# WebSocket Event Contract (Phase A2)

Transport: Socket.IO

Namespace: default `/`

## Timestamp Format

- All server timestamps are Unix epoch milliseconds (`Long` in Kotlin).
- Clients should compute timer remaining as:
  - `remainingMs = phaseEndsAtMs - serverNowMs + clientClockOffsetMs`
- Recommended Android offset estimation:
  - on each `match:state`, sample `serverNowMs - System.currentTimeMillis()` and smooth with moving average.

## Client -> Server

### `session:identify`

Payload:

```json
{ "playerId": "optional-existing-id", "displayName": "optional-name" }
```

Behavior:

- If `playerId` is omitted/unknown, server creates a guest session.
- Server replies on the same event name with resolved identity payload.

### `queue:join`

Payload: `{}`

Behavior:

- Adds player to matchmaking queue.
- When paired, server emits `match:found` then `match:state`.

### `queue:leave`

Payload: `{}`

Behavior:

- Removes player from queue.

### `match:resume`

Payload:

```json
{ "matchId": "uuid" }
```

Behavior:

- If player belongs to match, server emits latest `match:state` snapshot.

### `round:pick_letter`

Payload:

```json
{ "kind": "vowel" }
```

or

```json
{ "kind": "consonant" }
```

Behavior:

- Valid only for current letters-round picker.
- Server generates weighted random letter authoritatively.

### `round:submit_word`

Payload:

```json
{ "word": "example" }
```

Behavior:

- Valid only during `letters_solving` phase.
- One submission per player for letters round.

### `round:submit_conundrum_guess`

Payload:

```json
{ "guess": "algorithm" }
```

Behavior:

- Valid only during `conundrum_solving`.
- Multiple guesses allowed with server cooldown/rate-limit.
- First correct guess wins round (12 points).

## Server -> Client

### `session:identify`

```json
{ "playerId": "uuid", "displayName": "Guest-abc123", "serverNowMs": 1730000000000 }
```

### `matchmaking:status`

```json
{ "queueSize": 3, "state": "searching" }
```

or

```json
{ "queueSize": 0, "state": "idle" }
```

### `match:found`

```json
{ "matchId": "uuid", "serverNowMs": 1730000000000 }
```

### `match:state`

```json
{
  "matchId": "uuid",
  "phase": "letters_solving",
  "phaseEndsAtMs": 1730000030000,
  "serverNowMs": 1730000001200,
  "roundNumber": 1,
  "roundType": "letters",
  "players": [
    { "playerId": "p1", "displayName": "Guest-a1b2c3", "connected": true, "score": 0 },
    { "playerId": "p2", "displayName": "Guest-d4e5f6", "connected": true, "score": 0 }
  ],
  "pickerPlayerId": "p1",
  "letters": ["A", "E", "I", "O", "U", "S", "T", "R", "N"],
  "roundResults": [],
  "winnerPlayerId": null
}
```

Notes:

- `letters` present on letters rounds.
- `scrambled` present on conundrum round.
- `roundResults` accumulates finalized round outcomes.
- `winnerPlayerId` is `null` for draw.

### `action:error`

```json
{ "action": "round:submit_word", "code": "LATE_SUBMISSION", "message": "Round submission window has ended." }
```

## Error Codes Android should handle

- `UNKNOWN_PLAYER`
- `ALREADY_IN_MATCH`
- `MATCH_NOT_FOUND`
- `NOT_MATCH_PARTICIPANT`
- `INVALID_PHASE`
- `INVALID_ROUND`
- `NOT_PICKER`
- `PICK_CONSTRAINT_VIOLATION`
- `LATE_SUBMISSION`
- `DUPLICATE_SUBMISSION`
- `RATE_LIMITED`
- `ALREADY_SOLVED`