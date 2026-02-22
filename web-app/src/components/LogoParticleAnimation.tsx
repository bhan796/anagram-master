import { useEffect, useMemo, useRef, useState } from "react";
import { LetterTile } from "./ArcadeComponents";

const TOP_WORD = "ANAGRAM";
const BOTTOM_WORD = "ARENA";
const LOGO_COLORS = ["var(--cyan)", "var(--gold)", "var(--magenta)", "var(--green)"];
const TOP_STAGGER_MS = 90;
const BOTTOM_START_MS = TOP_WORD.length * TOP_STAGGER_MS - 140;
const SLIDE_DURATION_MS = 520;

export const LogoParticleAnimation = ({ onComplete }: { onComplete?: () => void }) => {
  const onCompleteRef = useRef(onComplete);
  const [active, setActive] = useState(false);

  const topLetters = useMemo(
    () =>
      TOP_WORD.split("").map((letter, index) => ({
        letter,
        accent: LOGO_COLORS[index % LOGO_COLORS.length],
        delayMs: index * TOP_STAGGER_MS
      })),
    []
  );

  const bottomLetters = useMemo(
    () =>
      BOTTOM_WORD.split("").map((letter, index) => ({
        letter,
        accent: LOGO_COLORS[(index + 2) % LOGO_COLORS.length],
        delayMs: BOTTOM_START_MS + index * TOP_STAGGER_MS
      })),
    []
  );

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    const activation = window.requestAnimationFrame(() => setActive(true));
    const maxDelayMs = Math.max(...topLetters.map((letter) => letter.delayMs), ...bottomLetters.map((letter) => letter.delayMs));
    const doneAtMs = maxDelayMs + SLIDE_DURATION_MS + 70;
    const completed = window.setTimeout(() => onCompleteRef.current?.(), doneAtMs);
    return () => {
      window.cancelAnimationFrame(activation);
      window.clearTimeout(completed);
    };
  }, [topLetters, bottomLetters]);

  const letterStyle = (delayMs: number) => ({
    width: "clamp(32px, 4.8vw, 42px)",
    height: "clamp(32px, 4.8vw, 42px)",
    transform: active ? "translateX(0)" : "translateX(calc(-100vw - 120px))",
    opacity: active ? 1 : 0,
    transition: [
      `transform ${SLIDE_DURATION_MS}ms cubic-bezier(0.22, 1, 0.36, 1) ${delayMs}ms`,
      `opacity 220ms ease ${delayMs}ms`
    ].join(", ")
  });

  return (
    <div style={{ width: "100%", minHeight: 120, display: "grid", alignContent: "center", gap: 8 }}>
      <div className="letter-row" style={{ justifyContent: "center" }}>
        {topLetters.map(({ letter, accent, delayMs }, index) => (
          <LetterTile key={`top-${letter}-${index}`} letter={letter} accent={accent} style={letterStyle(delayMs)} />
        ))}
      </div>
      <div className="letter-row" style={{ justifyContent: "center" }}>
        {bottomLetters.map(({ letter, accent, delayMs }, index) => (
          <LetterTile key={`bot-${letter}-${index}`} letter={letter} accent={accent} style={letterStyle(delayMs)} />
        ))}
      </div>
    </div>
  );
};
