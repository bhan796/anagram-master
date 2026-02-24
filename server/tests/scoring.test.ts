import { describe, expect, it } from "vitest";

const calculateLettersScore = (wordLength: number): number => {
  if (wordLength <= 0) return 0;
  return wordLength;
};

describe("bootstrap scoring sanity", () => {
  it("awards length score for nine-letter words", () => {
    expect(calculateLettersScore(9)).toBe(9);
  });

  it("awards length points otherwise", () => {
    expect(calculateLettersScore(7)).toBe(7);
  });
});
