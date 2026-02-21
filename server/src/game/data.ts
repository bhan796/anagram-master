import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export interface ConundrumEntry {
  id: string;
  scrambled?: string;
  answer: string;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const dataDir = path.resolve(__dirname, "../../data");

export const loadDictionarySet = (): Set<string> => {
  const dictionaryPath = path.join(dataDir, "dictionary_common_10k.txt");
  const raw = fs.readFileSync(dictionaryPath, "utf8");

  return new Set(
    raw
      .split(/\r?\n/)
      .map((line) => line.trim().toLowerCase())
      .filter((line) => line.length > 0)
  );
};

export const loadConundrums = (): ConundrumEntry[] => {
  const conundrumPath = path.join(dataDir, "conundrums.json");
  const raw = fs.readFileSync(conundrumPath, "utf8");
  return JSON.parse(raw) as ConundrumEntry[];
};
