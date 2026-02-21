# iOS App

SwiftUI iOS-first client for Anagram Master.

## Minimum Target

- iOS 17.0

## Generate Project (macOS)

1. Install Xcode 15+
2. Install XcodeGen: `brew install xcodegen`
3. In `ios-app/AnagramArena`, run: `xcodegen generate`
4. Open `AnagramArena.xcodeproj`

## Run Practice Mode (Offline)

1. Build and run in iPhone simulator.
2. Open `Practice Mode` from Home.
3. Toggle timer on/off in Practice menu.
4. Use `Practice Letters Round` and `Practice Conundrum`.

## Unit Tests

Run in Xcode:

- Product > Test (or `Cmd+U`)
- Test target: `AnagramArenaTests`

Current tests cover:

- scoring including 9-letter bonus
- constructability by letter frequency
- weighted generation validity
- vowel/consonant pick constraints
- conundrum answer validation
- match round progression

## Data Files (Replaceable)

- Dictionary sample: `ios-app/AnagramArena/Resources/Data/dictionary_sample.txt`
- Conundrums sample: `ios-app/AnagramArena/Resources/Data/conundrums.json`

To swap in larger datasets:

1. Replace file contents with your larger list/dataset.
2. Keep dictionary one word per line, lowercase/uppercase accepted.
3. Keep conundrum JSON schema:
   - `[ { "id": "...", "scrambled": "...", "answer": "..." } ]`
4. Rebuild app.

## Phase 1 scope

- Core game logic isolated from SwiftUI in `Sources/Core`.
- Offline dictionary and conundrum providers in `Sources/Services`.
- Practice mode MVVM + UI for letters and conundrum rounds.