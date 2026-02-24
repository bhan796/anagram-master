import { prisma } from "../config/prisma.js";
import { ACHIEVEMENTS, type AchievementDef } from "./achievements.js";

export interface AchievementCheckContext {
  mode: "casual" | "ranked";
  won: boolean;
  ratingAfter: number;
  newRankedGames: number;
  newRankedWins: number;
  newWinStreak: number;
  newConundrumSolves: number;
  conundrumElapsedMs: number | null;
  trailedBy20AndWon: boolean;
  maxRoundScore: number;
  longestWordLength: number;
  maxVowelsPicked: number;
  submittedAnyWord: boolean;
}

export async function checkAndGrantAchievements(
  playerId: string,
  ctx: AchievementCheckContext
): Promise<{ newAchievements: AchievementDef[]; runesGranted: number }> {
  const unlockedRows = await prisma.playerAchievement.findMany({
    where: { playerId },
    select: { achievementId: true }
  });
  const unlocked = new Set(unlockedRows.map((row) => row.achievementId));

  const shouldUnlock = (id: string, condition: boolean): boolean => !unlocked.has(id) && condition;

  const newlyUnlockedIds = new Set<string>();
  if (shouldUnlock("first_win", ctx.newRankedWins === 1)) newlyUnlockedIds.add("first_win");
  if (shouldUnlock("first_word", ctx.submittedAnyWord)) newlyUnlockedIds.add("first_word");
  if (shouldUnlock("long_word_6", ctx.longestWordLength >= 6)) newlyUnlockedIds.add("long_word_6");
  if (shouldUnlock("vowel_baron", ctx.maxVowelsPicked >= 5)) newlyUnlockedIds.add("vowel_baron");
  if (shouldUnlock("fast_conundrum_5s", ctx.conundrumElapsedMs !== null && ctx.conundrumElapsedMs < 5000)) newlyUnlockedIds.add("fast_conundrum_5s");
  if (shouldUnlock("hat_trick", ctx.newWinStreak >= 3)) newlyUnlockedIds.add("hat_trick");
  if (shouldUnlock("silver_tier", ctx.ratingAfter >= 1200)) newlyUnlockedIds.add("silver_tier");
  if (shouldUnlock("century", ctx.newRankedGames >= 100)) newlyUnlockedIds.add("century");
  if (shouldUnlock("conundrum_25", ctx.newConundrumSolves >= 25)) newlyUnlockedIds.add("conundrum_25");
  if (shouldUnlock("nine_letters", ctx.longestWordLength >= 9)) newlyUnlockedIds.add("nine_letters");
  if (shouldUnlock("gold_tier", ctx.ratingAfter >= 1400)) newlyUnlockedIds.add("gold_tier");
  if (shouldUnlock("streak_5", ctx.newWinStreak >= 5)) newlyUnlockedIds.add("streak_5");
  if (shouldUnlock("games_250", ctx.newRankedGames >= 250)) newlyUnlockedIds.add("games_250");
  if (shouldUnlock("comeback", ctx.trailedBy20AndWon)) newlyUnlockedIds.add("comeback");
  if (shouldUnlock("max_round_30", ctx.maxRoundScore >= 30)) newlyUnlockedIds.add("max_round_30");
  if (shouldUnlock("platinum_tier", ctx.ratingAfter >= 1600)) newlyUnlockedIds.add("platinum_tier");
  if (shouldUnlock("streak_10", ctx.newWinStreak >= 10)) newlyUnlockedIds.add("streak_10");
  if (shouldUnlock("diamond_tier", ctx.ratingAfter >= 1800)) newlyUnlockedIds.add("diamond_tier");

  const newAchievements = ACHIEVEMENTS.filter((a) => newlyUnlockedIds.has(a.id));
  if (newAchievements.length > 0) {
    await prisma.playerAchievement.createMany({
      data: newAchievements.map((a) => ({ playerId, achievementId: a.id })),
      skipDuplicates: true
    });
  }

  const runesGranted = newAchievements.reduce((sum, item) => sum + item.runesReward, 0);
  return { newAchievements, runesGranted };
}

export { computeMatchRunes } from "./achievements.js";