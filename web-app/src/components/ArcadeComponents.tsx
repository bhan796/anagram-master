import type { CSSProperties, ReactNode } from "react";

interface ArcadeButtonProps {
  text: string;
  onClick: () => void;
  disabled?: boolean;
  accent?: "cyan" | "gold" | "magenta";
}

export const ArcadeScaffold = ({ children }: { children: ReactNode }) => (
  <div className="app-shell">
    <div className="arcade-scaffold">{children}</div>
  </div>
);

export const NeonTitle = ({ text }: { text: string }) => <div className="neon-title">{text}</div>;

export const ArcadeButton = ({ text, onClick, disabled, accent = "cyan" }: ArcadeButtonProps) => {
  const accentClass = accent === "gold" ? "gold" : accent === "magenta" ? "magenta" : "";
  return (
    <button type="button" className={`arcade-button ${accentClass}`.trim()} onClick={onClick} disabled={disabled}>
      {text}
    </button>
  );
};

export const ArcadeBackButton = ({ text = "<  BACK", onClick }: { text?: string; onClick: () => void }) => (
  <ArcadeButton text={text} onClick={onClick} accent="gold" />
);

export const LetterTile = ({
  letter,
  accent = "var(--cyan)",
  empty = false,
  selected = false,
  onClick,
  style
}: {
  letter: string;
  accent?: string;
  empty?: boolean;
  selected?: boolean;
  onClick?: () => void;
  style?: CSSProperties;
}) => (
  <button
    type="button"
    className={`letter-tile ${empty ? "empty" : ""} ${selected ? "selected" : ""}`.trim()}
    onClick={onClick}
    style={{
      borderColor: empty ? undefined : accent,
      color: empty ? undefined : accent,
      cursor: onClick ? "pointer" : "default",
      ...style
    }}
    disabled={!onClick}
  >
    {letter}
  </button>
);

export const ScoreBadge = ({ label, score, color = "var(--cyan)" }: { label: string; score: number; color?: string }) => (
  <div className="score-badge">
    <div className="name">{label}</div>
    <div className="value" style={{ color }}>
      {score}
    </div>
  </div>
);

export const TimerBar = ({ secondsRemaining, totalSeconds }: { secondsRemaining: number; totalSeconds: number }) => {
  const fraction = totalSeconds > 0 ? Math.max(0, Math.min(1, secondsRemaining / totalSeconds)) : 0;
  const color = fraction > 0.5 ? "var(--cyan)" : fraction > 0.25 ? "var(--gold)" : "var(--red)";

  return (
    <div className="timer-wrap">
      <div className="label" style={{ color }}>
        {secondsRemaining}s
      </div>
      <div className="timer-bar">
        <div className="timer-fill" style={{ width: `${fraction * 100}%`, background: color }} />
      </div>
    </div>
  );
};

export const NeonDivider = () => (
  <div
    style={{
      height: 1,
      width: "100%",
      background: "linear-gradient(90deg, transparent, rgba(0,245,255,.35), transparent)"
    }}
  />
);

const LOGO_TOP = "ANAGRAM".split("");
const LOGO_BOTTOM = "ARENA".split("");
const LOGO_COLORS = ["var(--cyan)", "var(--gold)", "var(--magenta)", "var(--green)"];

export const TileLogo = () => (
  <div style={{ display: "grid", gap: "8px", justifyItems: "center" }}>
    <div className="letter-row" style={{ justifyContent: "center" }}>
      {LOGO_TOP.map((letter, index) => (
        <LetterTile
          key={`top-${letter}-${index}`}
          letter={letter}
          accent={LOGO_COLORS[index % LOGO_COLORS.length]}
          style={{ width: "clamp(32px, 4.8vw, 42px)", height: "clamp(32px, 4.8vw, 42px)" }}
        />
      ))}
    </div>
    <div className="letter-row" style={{ justifyContent: "center" }}>
      {LOGO_BOTTOM.map((letter, index) => (
        <LetterTile
          key={`bot-${letter}-${index}`}
          letter={letter}
          accent={LOGO_COLORS[(index + 2) % LOGO_COLORS.length]}
          style={{ width: "clamp(32px, 4.8vw, 42px)", height: "clamp(32px, 4.8vw, 42px)" }}
        />
      ))}
    </div>
  </div>
);

export const WordTiles = ({
  word,
  accent = "var(--cyan)",
  label
}: {
  word: string;
  accent?: string;
  label?: string;
}) => {
  const cleaned = word.toUpperCase().replace(/[^A-Z]/g, "");
  const letters = cleaned.length ? cleaned.split("") : ["-"];

  return (
    <div style={{ display: "grid", gap: "6px" }}>
      {label ? <div className="text-dim">{label}</div> : null}
      <div className="letter-row">
        {letters.map((letter, index) => (
          <LetterTile key={`${letter}-${index}`} letter={letter} accent={accent} />
        ))}
      </div>
    </div>
  );
};
