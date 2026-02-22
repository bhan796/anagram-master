import { useEffect, useMemo, useState } from "react";
import {
  ArcadeBackButton,
  ArcadeButton,
  ArcadeScaffold,
  LetterTile,
  NeonTitle,
  TimerBar,
  WordTiles
} from "../components/ArcadeComponents";
import { TapLetterComposer } from "../components/TapLetterComposer";
import * as SoundManager from "../sound/SoundManager";
import {
  allowedPickKinds,
  canConstructFromLetters,
  drawWeightedLetter,
  isAlphabetical,
  normalizeWord,
  scoreWord,
  type PickKind
} from "../logic/gameRules";

interface LettersPracticeScreenProps {
  timerEnabled: boolean;
  dictionary: Set<string> | null;
  dictionaryError: string | null;
  onBack: () => void;
}

interface ResultState {
  word: string;
  valid: boolean;
  score: number;
  message: string;
}

const PICK_SECONDS = 20;
const SOLVE_SECONDS = 30;

export const LettersPracticeScreen = ({ timerEnabled, dictionary, dictionaryError, onBack }: LettersPracticeScreenProps) => {
  const [letters, setLetters] = useState<string[]>([]);
  const [vowelCount, setVowelCount] = useState(0);
  const [consonantCount, setConsonantCount] = useState(0);
  const [phase, setPhase] = useState<"pick" | "solve" | "result">("pick");
  const [secondsRemaining, setSecondsRemaining] = useState(PICK_SECONDS);
  const [wordInput, setWordInput] = useState("");
  const [result, setResult] = useState<ResultState | null>(null);

  const allowedKinds = useMemo(
    () => allowedPickKinds(letters.length, vowelCount, consonantCount),
    [letters.length, vowelCount, consonantCount]
  );

  const autoFillToSolve = () => {
    let nextLetters = [...letters];
    let nextVowels = vowelCount;
    let nextConsonants = consonantCount;

    while (nextLetters.length < 9) {
      const kinds = [...allowedPickKinds(nextLetters.length, nextVowels, nextConsonants)];
      if (kinds.length === 0) break;
      const kind = kinds[Math.floor(Math.random() * kinds.length)];
      const letter = drawWeightedLetter(kind);
      nextLetters.push(letter);
      if (kind === "vowel") nextVowels += 1;
      else nextConsonants += 1;
    }

    setLetters(nextLetters);
    setVowelCount(nextVowels);
    setConsonantCount(nextConsonants);
    setPhase("solve");
    setSecondsRemaining(SOLVE_SECONDS);
  };

  useEffect(() => {
    if (!timerEnabled) return;

    if (phase === "pick") {
      if (secondsRemaining <= 0) {
        autoFillToSolve();
        return;
      }
      const timer = window.setTimeout(() => setSecondsRemaining((value) => value - 1), 1000);
      return () => window.clearTimeout(timer);
    }

    if (phase === "solve") {
      if (secondsRemaining <= 0) {
        submit();
        return;
      }
      const timer = window.setTimeout(() => setSecondsRemaining((value) => value - 1), 1000);
      return () => window.clearTimeout(timer);
    }
  }, [phase, secondsRemaining, timerEnabled, letters, vowelCount, consonantCount]);

  const pick = (kind: PickKind) => {
    if (phase !== "pick") return;
    if (!allowedKinds.has(kind)) return;

    const letter = drawWeightedLetter(kind);
    setLetters((previous) => {
      const next = [...previous, letter];
      if (next.length === 9) {
        setPhase("solve");
        setSecondsRemaining(SOLVE_SECONDS);
      }
      return next;
    });

    if (kind === "vowel") {
      setVowelCount((value) => value + 1);
    } else {
      setConsonantCount((value) => value + 1);
    }
  };

  const submit = () => {
    if (!dictionary) return;

    const normalized = normalizeWord(wordInput);
    const alphabetical = isAlphabetical(normalized);
    const constructable = canConstructFromLetters(normalized, letters);
    const found = dictionary.has(normalized);
    const valid = normalized.length > 0 && alphabetical && constructable && found;

    const score = valid ? scoreWord(normalized.length) : 0;
    const message =
      normalized.length === 0
        ? "Invalid: Enter a word"
        : !alphabetical
          ? "Invalid: Alphabetical words only"
          : !constructable
            ? "Invalid: Cannot build from letters"
            : !found
              ? "Invalid: Word not found"
              : "Valid word";

    setResult({ word: wordInput, valid, score, message });
    setPhase("result");
  };

  const reset = () => {
    setLetters([]);
    setVowelCount(0);
    setConsonantCount(0);
    setPhase("pick");
    setSecondsRemaining(PICK_SECONDS);
    setWordInput("");
    setResult(null);
  };

  useEffect(() => {
    if (!result) return;
    if (result.valid) {
      void SoundManager.playWordValid();
    } else {
      void SoundManager.playWordInvalid();
    }
  }, [result?.valid]);

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Letters Practice" />

      {dictionaryError ? <div className="card warning">Dictionary failed to load: {dictionaryError}</div> : null}

      {phase === "pick" ? (
        <>
          <div className="headline">Pick 9 letters</div>
          <div className="text-dim">Must include at least one vowel and one consonant.</div>
          {timerEnabled ? <TimerBar secondsRemaining={secondsRemaining} totalSeconds={PICK_SECONDS} /> : null}
          <div className="letter-row">
            {Array.from({ length: 9 }).map((_, index) => {
              const letter = letters[index] ?? "_";
              return <LetterTile key={`pick-${index}`} letter={letter} empty={letter === "_"} />;
            })}
          </div>
          <div className="text-dim">{letters.length}/9 selected</div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
            <ArcadeButton
              text="Vowel"
              onClick={() => {
                void SoundManager.playTilePlace();
                pick("vowel");
              }}
              disabled={!allowedKinds.has("vowel")}
            />
            <ArcadeButton
              text="Consonant"
              onClick={() => {
                void SoundManager.playTilePlace();
                pick("consonant");
              }}
              disabled={!allowedKinds.has("consonant")}
            />
          </div>
        </>
      ) : null}

      {phase === "solve" ? (
        <>
          {timerEnabled ? <TimerBar secondsRemaining={secondsRemaining} totalSeconds={SOLVE_SECONDS} /> : null}
          <TapLetterComposer
            letters={letters}
            value={wordInput}
            onValueChange={setWordInput}
            onSubmit={() => {
              void SoundManager.playWordSubmit();
              submit();
            }}
          />
          <ArcadeButton
            text="Submit Word"
            onClick={() => {
              void SoundManager.playWordSubmit();
              submit();
            }}
          />
        </>
      ) : null}

      {phase === "result" && result ? (
        <>
          <NeonTitle text="Round Result" />
          <div className="card" style={{ display: "grid", gap: 10 }}>
            <WordTiles label="Letters" word={letters.join("")} accent="var(--gold)" />
            <WordTiles label="Your Word" word={result.word || "-"} />
            <div className="text-dim" style={{ color: result.valid ? "var(--green)" : "var(--red)" }}>
              {result.message}
            </div>
            <div className="headline">Score: {result.score}</div>
          </div>
          <ArcadeButton text="Play Another Letters Round" onClick={reset} />
        </>
      ) : null}
    </ArcadeScaffold>
  );
};
