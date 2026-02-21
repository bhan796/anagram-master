import { useMemo, useState } from "react";
import { LetterTile } from "./ArcadeComponents";

interface TapLetterComposerProps {
  letters: string[];
  value: string;
  onValueChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

export const TapLetterComposer = ({
  letters,
  value,
  onValueChange,
  disabled,
  placeholder = "Tap letters to build your word"
}: TapLetterComposerProps) => {
  const selected = useMemo(() => value.split(""), [value]);
  const [selectedIndices, setSelectedIndices] = useState<number[]>([]);

  const syncValue = (indices: number[]) => {
    const nextWord = indices.map((index) => letters[index]).join("");
    onValueChange(nextWord);
  };

  const handleTap = (index: number) => {
    if (disabled || selectedIndices.includes(index)) return;
    const next = [...selectedIndices, index];
    setSelectedIndices(next);
    syncValue(next);
  };

  const handleUndo = () => {
    if (disabled || selectedIndices.length === 0) return;
    const next = selectedIndices.slice(0, -1);
    setSelectedIndices(next);
    syncValue(next);
  };

  const handleClear = () => {
    if (disabled) return;
    setSelectedIndices([]);
    onValueChange("");
  };

  return (
    <div style={{ display: "grid", gap: "12px" }}>
      <div className="card word-target">
        <div className="text-dim">{selected.length ? "Your word" : placeholder}</div>
        <div className="letter-row" style={{ marginTop: 8 }}>
          {Array.from({ length: 9 }).map((_, index) => {
            const letter = selected[index] ?? "_";
            return (
              <LetterTile
                key={`selected-${index}`}
                letter={letter}
                empty={letter === "_"}
                style={{
                  transform: letter === "_" ? "none" : "translateY(0)",
                  transition: "transform 180ms ease"
                }}
              />
            );
          })}
        </div>
      </div>

      <div className="letter-row">
        {letters.map((letter, index) => (
          <LetterTile
            key={`${letter}-${index}`}
            letter={letter}
            selected={selectedIndices.includes(index)}
            onClick={disabled ? undefined : () => handleTap(index)}
          />
        ))}
      </div>

      <div style={{ display: "grid", gap: "10px", gridTemplateColumns: "1fr 1fr" }}>
        <button type="button" className="arcade-button" onClick={handleUndo} disabled={disabled || selectedIndices.length === 0}>
          Undo
        </button>
        <button type="button" className="arcade-button" onClick={handleClear} disabled={disabled || selectedIndices.length === 0}>
          Clear
        </button>
      </div>
    </div>
  );
};
