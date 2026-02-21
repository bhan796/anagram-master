# Android Online Manual Test Script (Phase A3)

Use two emulators or two physical devices.

## Preconditions

1. Backend running and reachable (local or hosted).
2. Android app built with correct `BACKEND_BASE_URL`.
3. Both clients have internet connectivity.

## Test A: Matchmaking + Match Start

1. Launch app on Device A and Device B.
2. Tap `Play Online` on both.
3. Tap `Find Opponent` on both.
4. Verify both enter a match automatically.
5. Verify both clients show same round number/type and scoreboard.

Expected:

- Queue status updates visible.
- Both clients navigate to live match screen.
- Match IDs match across clients.

## Test B: Letters Round Turn Sync

1. Observe picker on round 1.
2. On picker device, choose letters (vowel/consonant) until 9 slots filled.
3. Verify non-picker sees letter updates in real time.
4. Verify only picker can press pick buttons.
5. Submit words from both clients.

Expected:

- Non-picker cannot pick.
- Letters list identical on both clients.
- Round transitions to result after both submit or timeout.

## Test C: Server Timer Authority

1. In solving phase, do not submit on one device.
2. Wait for server timeout.
3. Attempt late submit.

Expected:

- Late action rejected (`LATE_SUBMISSION` user feedback).
- Result screen appears based on server timing.

## Test D: Conundrum First Correct + Rate Limit

1. Reach conundrum round.
2. Submit correct answer quickly on one device.
3. Submit same answer on other device after.
4. Spam guesses quickly on one device.

Expected:

- First correct gets 12 points.
- Later correct rejected (`ALREADY_SOLVED` or late phase behavior).
- Rapid spam gets `RATE_LIMITED` feedback.

## Test E: Reconnect / Resume

1. During active round, disable network on Device A briefly.
2. Re-enable network.
3. Verify Device A reconnects and receives current `match:state`.

Expected:

- UI shows reconnecting/disconnected states.
- Match state restored automatically.

## Test F: Final Match Result

1. Complete all 5 rounds.
2. Verify winner/loser/draw and round breakdown.

Expected:

- Total score is server-authoritative.
- Round breakdown aligns with backend events.
