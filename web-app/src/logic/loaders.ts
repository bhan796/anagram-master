import type { ConundrumEntry } from "../logic/gameRules";

let dictionaryCache: Set<string> | null = null;
let conundrumCache: ConundrumEntry[] | null = null;

export const loadDictionary = async (): Promise<Set<string>> => {
  if (dictionaryCache) return dictionaryCache;

  const response = await fetch("/data/dictionary_common_10k.txt");
  if (!response.ok) {
    throw new Error("Failed to load dictionary dataset.");
  }

  const text = await response.text();
  dictionaryCache = new Set(
    text
      .split(/\r?\n/)
      .map((line) => line.trim().toLowerCase())
      .filter((line) => line.length > 0)
  );

  return dictionaryCache;
};

export const loadConundrums = async (): Promise<ConundrumEntry[]> => {
  if (conundrumCache) return conundrumCache;

  const response = await fetch("/data/conundrums.json");
  if (!response.ok) {
    throw new Error("Failed to load conundrum dataset.");
  }

  const parsed = (await response.json()) as ConundrumEntry[];
  conundrumCache = parsed;
  return conundrumCache;
};
