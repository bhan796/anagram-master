export type PickKind = "vowel" | "consonant";

export const VOWEL_WEIGHTS: Record<string, number> = {
  A: 15,
  E: 21,
  I: 13,
  O: 13,
  U: 5
};

export const CONSONANT_WEIGHTS: Record<string, number> = {
  B: 2,
  C: 3,
  D: 6,
  F: 2,
  G: 3,
  H: 2,
  J: 1,
  K: 1,
  L: 5,
  M: 4,
  N: 8,
  P: 4,
  Q: 1,
  R: 9,
  S: 9,
  T: 9,
  V: 1,
  W: 1,
  X: 1,
  Y: 1,
  Z: 1
};

export const normalizeWord = (value: string): string => value.trim().toLowerCase();

export const isAlphabetical = (value: string): boolean => /^[a-z]+$/i.test(value);

export const scoreWord = (length: number): number => {
  if (length <= 0) return 0;
  return length;
};

export const canConstructFromLetters = (word: string, letters: string[]): boolean => {
  const normalized = normalizeWord(word);
  if (!normalized) return false;

  const counts = new Map<string, number>();
  for (const letter of letters) {
    const upper = letter.toUpperCase();
    counts.set(upper, (counts.get(upper) ?? 0) + 1);
  }

  for (const char of normalized) {
    const upper = char.toUpperCase();
    const remaining = counts.get(upper) ?? 0;
    if (remaining <= 0) return false;
    counts.set(upper, remaining - 1);
  }

  return true;
};

export const allowedPickKinds = (
  picksSoFar: number,
  vowelCount: number,
  consonantCount: number,
  targetSlots = 9
): Set<PickKind> => {
  const remaining = targetSlots - picksSoFar;
  if (remaining <= 0) return new Set<PickKind>();

  const allowed = new Set<PickKind>();

  for (const kind of ["vowel", "consonant"] as const) {
    const nextVowels = vowelCount + (kind === "vowel" ? 1 : 0);
    const nextConsonants = consonantCount + (kind === "consonant" ? 1 : 0);
    const remainingAfterPick = remaining - 1;
    const neededVowels = Math.max(0, 1 - nextVowels);
    const neededConsonants = Math.max(0, 1 - nextConsonants);

    if (neededVowels + neededConsonants <= remainingAfterPick) {
      allowed.add(kind);
    }
  }

  return allowed;
};

export const drawWeightedLetter = (kind: PickKind): string => {
  const weights = kind === "vowel" ? VOWEL_WEIGHTS : CONSONANT_WEIGHTS;
  const entries = Object.entries(weights);
  const total = entries.reduce((sum, [, weight]) => sum + weight, 0);
  const target = Math.random() * total;

  let running = 0;
  for (const [letter, weight] of entries) {
    running += weight;
    if (target < running) return letter;
  }

  return entries[entries.length - 1][0];
};

export interface ConundrumEntry {
  id: string;
  scrambled?: string;
  answer: string;
}

export const scrambleWord = (word: string): string => {
  const normalized = word.trim().toUpperCase();
  if (normalized.length <= 1) return normalized;

  const chars = normalized.split("");
  let candidate = normalized;

  for (let attempt = 0; attempt < 12; attempt += 1) {
    const shuffled = [...chars];
    for (let i = shuffled.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    candidate = shuffled.join("");
    if (candidate !== normalized) {
      return candidate;
    }
  }

  return chars.slice(1).join("") + chars[0];
};
