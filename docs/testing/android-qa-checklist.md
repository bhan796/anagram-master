# Android QA Checklist (A4)

## Offline Practice

- Home -> Practice Mode opens.
- Timer toggle affects letters and conundrum rounds.
- Letters scoring and 9-letter bonus behave correctly.
- Conundrum correct/incorrect scoring behaves correctly.

## Online Matchmaking

- Two clients can join queue and get matched.
- Cancel queue exits cleanly.
- Connection/reconnecting status messages are visible.

## Letters Online Round

- Picker-only controls enforced.
- Non-picker sees letter updates in real time.
- Submit word only once; waiting state shown.
- Late submissions show server error handling.

## Conundrum Online Round

- Scramble shown on both clients.
- Multiple guesses accepted with rate-limit feedback.
- First-correct solver gets 12 points.

## Reconnect/Recovery

- Disable/re-enable network mid-round.
- Reconnected client resumes current match state.
- No local score/timer authority mismatch.

## End-of-Match

- Final winner/draw shown.
- Round breakdown shown.
- Profile stats endpoint reflects completed matches.
