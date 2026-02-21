import { useEffect, useMemo, useState } from "react";
import {
  ArcadeBackButton,
  ArcadeButton,
  ArcadeScaffold,
  NeonTitle,
  TimerBar,
  WordTiles
} from "../components/ArcadeComponents";
import { TapLetterComposer } from "../components/TapLetterComposer";
import { scrambleWord, type ConundrumEntry } from "../logic/gameRules";

interface ConundrumPracticeScreenProps {
  timerEnabled: boolean;
  conundrums: ConundrumEntry[];
  conundrumError: string | null;
  onBack: () => void;
}

const TOTAL_SECONDS = 30;

export const ConundrumPracticeScreen = ({ timerEnabled, conundrums, conundrumError, onBack }: ConundrumPracticeScreenProps) => {
  const [secondsRemaining, setSecondsRemaining] = useState(TOTAL_SECONDS);
  const [guess, setGuess] = useState("");
  const [result, setResult] = useState<null | { correct: boolean; answer: string }>(null);
  const [seed, setSeed] = useState(0);

  const current = useMemo(() => {
    if (conundrums.length === 0) return null;
    return conundrums[Math.floor(Math.random() * conundrums.length)];
  }, [conundrums, seed]);

  const displayScramble = useMemo(() => {
    if (!current) return "";
    return scrambleWord(current.answer);
  }, [current]);

  useEffect(() => {
    if (!timerEnabled || result || !current) return;
    if (secondsRemaining <= 0) {
      setResult({ correct: false, answer: current.answer });
      return;
    }

    const timer = window.setTimeout(() => setSecondsRemaining((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [timerEnabled, result, secondsRemaining, current]);

  const submit = () => {
    if (!current) return;
    setResult({ correct: guess.trim().toLowerCase() === current.answer.toLowerCase(), answer: current.answer });
  };

  const reset = () => {
    setGuess("");
    setResult(null);
    setSecondsRemaining(TOTAL_SECONDS);
    setSeed((value) => value + 1);
  };

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Conundrum Practice" />

      {conundrumError ? <div className="card warning">Conundrum data failed to load: {conundrumError}</div> : null}

      {!current ? <div className="text-dim">No conundrum data available.</div> : null}

      {current && !result ? (
        <>
          <div className="card" style={{ textAlign: "center" }}>
            <div
              className="headline"
              style={{
                color: "var(--gold)",
                letterSpacing: "0.5em",
                fontSize: "clamp(18px, 5vw, 28px)",
                lineHeight: 1.5
              }}
            >
              {displayScramble}
            </div>
          </div>

          {timerEnabled ? <TimerBar secondsRemaining={secondsRemaining} totalSeconds={TOTAL_SECONDS} /> : null}
          <TapLetterComposer
            letters={displayScramble.split("")}
            value={guess.toUpperCase()}
            onValueChange={setGuess}
            onSubmit={submit}
          />
          <ArcadeButton text="Submit Guess" onClick={submit} />
        </>
      ) : null}

      {result ? (
        <>
          <NeonTitle text={result.correct ? "Correct" : "Result"} />
          <div className="card" style={{ display: "grid", gap: 10 }}>
            <WordTiles label="Scrambled" word={displayScramble} accent="var(--gold)" />
            <WordTiles label="Your Guess" word={guess || "-"} />
            <WordTiles label="Answer" word={result.answer} accent="var(--green)" />
            <div className="headline" style={{ color: result.correct ? "var(--green)" : "var(--red)" }}>
              {result.correct ? "You solved it" : "Incorrect"}
            </div>
          </div>
          <ArcadeButton text="Play Another Conundrum" onClick={reset} />
        </>
      ) : null}
    </ArcadeScaffold>
  );
};
