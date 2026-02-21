export type RankTier =
  | "bronze"
  | "silver"
  | "gold"
  | "platinum"
  | "diamond"
  | "master";

export interface EloResult {
  deltaA: number;
  deltaB: number;
}

const tierThresholds: Array<{ min: number; tier: RankTier }> = [
  { min: 1700, tier: "master" },
  { min: 1500, tier: "diamond" },
  { min: 1300, tier: "platinum" },
  { min: 1150, tier: "gold" },
  { min: 1000, tier: "silver" },
  { min: 0, tier: "bronze" }
];

export const ratingToTier = (rating: number): RankTier => {
  for (const threshold of tierThresholds) {
    if (rating >= threshold.min) return threshold.tier;
  }
  return "bronze";
};

const expectedScore = (rating: number, opponentRating: number): number =>
  1 / (1 + 10 ** ((opponentRating - rating) / 400));

const kFactor = (gamesPlayed: number): number => (gamesPlayed < 30 ? 32 : 20);

export const computeEloDelta = (
  ratingA: number,
  ratingB: number,
  scoreA: number,
  gamesA: number,
  gamesB: number
): EloResult => {
  const expectedA = expectedScore(ratingA, ratingB);
  const expectedB = expectedScore(ratingB, ratingA);

  const deltaA = Math.round(kFactor(gamesA) * (scoreA - expectedA));
  const scoreB = 1 - scoreA;
  const deltaB = Math.round(kFactor(gamesB) * (scoreB - expectedB));

  return { deltaA, deltaB };
};
