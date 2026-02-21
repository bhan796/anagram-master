import { describe, expect, it } from "vitest";

const calculateLettersScore = (wordLength: number): number => {
  if (wordLength <= 0) return 0;
  if (wordLength === 9) return 12;
  return wordLength;
};

describe("bootstrap scoring sanity", () => {
  it("awards 12 for nine-letter words", () => {
    expect(calculateLettersScore(9)).toBe(12);
  });

  it("awards length points otherwise", () => {
    expect(calculateLettersScore(7)).toBe(7);
  });
});