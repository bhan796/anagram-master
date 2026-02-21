# Balancing Configuration Guide

This document centralizes tunable gameplay balance knobs for MVP.

## Letter Weights

Primary files:

- Server authority: `server/src/game/rules.ts`
- Android offline parity: `android-app/core/src/main/kotlin/com/bhan796/anagram/core/engine/LetterGenerator.kt`
- iOS offline parity: `ios-app/AnagramArena/Sources/Core/Engine/LetterGenerator.swift`

### Current distribution

- Vowels: `A E I O U` weighted for playability.
- Consonants: common letters weighted higher than rare letters.

### Change process

1. Update server weights first (authoritative online behavior).
2. Update Android/iOS offline weights to keep practice parity.
3. Run tests:
   - `npm run server:test`
   - `cd android-app && gradlew.bat :core:test`

## Dictionary Configuration

Primary files:

- Server dictionary: `server/data/dictionary_common_10k.txt`
- Android dictionary: `android-app/app/src/main/assets/data/dictionary_common_10k.txt`
- iOS dictionary: `ios-app/AnagramArena/Resources/Data/dictionary_common_10k.txt`

Format:

- One lowercase/uppercase word per line.
- Alphabetical words only.

### Replace dictionary

1. Replace all three dictionary files.
2. Keep format as one word per line.
3. Re-run tests and manual online/offline checks.

## Conundrum Dataset Configuration

Primary files:

- Server: `server/data/conundrums.json`
- Android: `android-app/app/src/main/assets/data/conundrums.json`
- iOS: `ios-app/AnagramArena/Resources/Data/conundrums.json`

Schema:

```json
[
  { "id": "1", "scrambled": "LAROGIMTH", "answer": "algorithm" }
]
```

### Replace conundrum set

1. Keep valid 9-letter `answer` values.
2. Ensure `scrambled` is an anagram of `answer`.
3. Keep IDs unique.
