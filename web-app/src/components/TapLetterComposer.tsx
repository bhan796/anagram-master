import { useEffect, useMemo, useState } from "react";
import { LetterTile } from "./ArcadeComponents";

interface TapLetterComposerProps {
  letters: string[];
  value: string;
  onValueChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
  onSubmit?: () => void;
}

const buildIndicesFromValue = (letters: string[], value: string): number[] => {
  const used = new Set<number>();
  const normalizedChars = value
    .toUpperCase()
    .split("")
    .filter((char) => /^[A-Z]$/.test(char));

  const indices: number[] = [];
  for (const char of normalizedChars) {
    const index = letters.findIndex((letter, idx) => !used.has(idx) && letter.toUpperCase() === char);
    if (index < 0) break;
    used.add(index);
    indices.push(index);
  }
  return indices;
};

export const TapLetterComposer = ({
  letters,
  value,
  onValueChange,
  disabled,
  placeholder = "Tap letters to build your word",
  onSubmit
}: TapLetterComposerProps) => {
  const selected = useMemo(() => value.split(""), [value]);
  const lettersKey = useMemo(() => letters.join(""), [letters]);
  const [selectedIndices, setSelectedIndices] = useState<number[]>(() => buildIndicesFromValue(letters, value));
  const [displayOrder, setDisplayOrder] = useState<number[]>(() => letters.map((_, index) => index));

  const syncValue = (indices: number[]) => {
    const nextWord = indices.map((index) => letters[index]).join("");
    onValueChange(nextWord);
  };

  useEffect(() => {
    setDisplayOrder(letters.map((_, index) => index));
  }, [lettersKey]);

  useEffect(() => {
    setSelectedIndices(buildIndicesFromValue(letters, value));
  }, [lettersKey, value]);

  useEffect(() => {
    if (disabled) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        if (onSubmit) {
          event.preventDefault();
          onSubmit();
        }
        return;
      }

      if (event.key === "Backspace") {
        event.preventDefault();
        setSelectedIndices((previous) => {
          if (previous.length === 0) return previous;
          const next = previous.slice(0, -1);
          syncValue(next);
          return next;
        });
        return;
      }

      if (!/^[a-z]$/i.test(event.key)) return;
      const char = event.key.toUpperCase();
      setSelectedIndices((previous) => {
        const used = new Set(previous);
        const matchIndex = letters.findIndex((letter, idx) => !used.has(idx) && letter.toUpperCase() === char);
        if (matchIndex < 0) return previous;
        const next = [...previous, matchIndex];
        syncValue(next);
        return next;
      });
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [disabled, lettersKey, onSubmit, onValueChange]);

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

  const handleShuffle = () => {
    if (disabled) return;
    setDisplayOrder((previous) => {
      const next = [...previous];
      for (let i = next.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [next[i], next[j]] = [next[j], next[i]];
      }
      return next;
    });
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
        {displayOrder.map((sourceIndex) => (
          <LetterTile
            key={`${letters[sourceIndex]}-${sourceIndex}`}
            letter={letters[sourceIndex]}
            selected={selectedIndices.includes(sourceIndex)}
            onClick={disabled ? undefined : () => handleTap(sourceIndex)}
          />
        ))}
      </div>

      <div style={{ display: "grid", gap: "10px", gridTemplateColumns: "repeat(3, minmax(0, 1fr))" }}>
        <button type="button" className="arcade-button" onClick={handleUndo} disabled={disabled || selectedIndices.length === 0}>
          Undo
        </button>
        <button type="button" className="arcade-button" onClick={handleShuffle} disabled={disabled || letters.length <= 1}>
          Shuffle
        </button>
        <button type="button" className="arcade-button" onClick={handleClear} disabled={disabled || selectedIndices.length === 0}>
          Clear
        </button>
      </div>
    </div>
  );
};
