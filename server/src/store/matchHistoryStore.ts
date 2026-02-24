import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { Redis } from "ioredis";
import { getRedis } from "../config/redis.js";
import { ratingToTier } from "../game/ranking.js";
import { logger } from "../config/logger.js";
import type { PlayerRuntime } from "../game/types.js";
import type { FinishedMatchRecord } from "../game/types.js";

interface PersistedShape {
  matches: FinishedMatchRecord[];
  players: Record<
    string,
    {
      playerId: string;
      displayName: string;
      rating: number;
      peakRating: number;
      rankedGames: number;
      rankedWins: number;
      rankedLosses: number;
      rankedDraws: number;
      updatedAtMs: number;
    }
  >;
}

export interface PersistedPlayerProfile {
  playerId: string;
  displayName: string;
  rating: number;
  peakRating: number;
  rankedGames: number;
  rankedWins: number;
  rankedLosses: number;
  rankedDraws: number;
  updatedAtMs: number;
}

export interface PlayerStats {
  playerId: string;
  displayName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  totalScore: number;
  averageScore: number;
  recentMatchIds: string[];
  rating: number;
  peakRating: number;
  rankTier: string;
  rankedGames: number;
  rankedWins: number;
  rankedLosses: number;
  rankedDraws: number;
}

export interface LeaderboardEntry {
  playerId: string;
  displayName: string;
  equippedCosmetic?: string | null;
  rating: number;
  rankTier: string;
  rankedGames: number;
  wins: number;
  losses: number;
  draws: number;
  peakRating: number;
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const defaultPath = path.resolve(__dirname, "../../data/runtime-history.json");
const REDIS_KEY = "anagram:history:v1";

export class MatchHistoryStore {
  private readonly filePath: string;
  private readonly matches: FinishedMatchRecord[] = [];
  private readonly players = new Map<string, PersistedPlayerProfile>();
  private redis: Redis | null = null;

  constructor(filePath = defaultPath) {
    this.filePath = filePath;
  }

  async initialize(): Promise<void> {
    await this.loadFromRedisOrFile();
  }

  recordMatch(match: FinishedMatchRecord): void {
    this.matches.push(match);
    for (const player of match.players) {
      const current = this.players.get(player.playerId);
      const isRanked = match.mode === "ranked";
      const didWin = isRanked && match.winnerPlayerId === player.playerId;
      const didDraw = isRanked && match.winnerPlayerId === null;
      const didLose = isRanked && match.winnerPlayerId !== null && match.winnerPlayerId !== player.playerId;

      this.players.set(player.playerId, {
        playerId: player.playerId,
        displayName: player.displayName,
        rating: player.ratingAfter,
        peakRating: Math.max(current?.peakRating ?? 1000, player.ratingAfter),
        rankedGames: (current?.rankedGames ?? 0) + (isRanked ? 1 : 0),
        rankedWins: (current?.rankedWins ?? 0) + (didWin ? 1 : 0),
        rankedDraws: (current?.rankedDraws ?? 0) + (didDraw ? 1 : 0),
        rankedLosses: (current?.rankedLosses ?? 0) + (didLose ? 1 : 0),
        updatedAtMs: match.finishedAtMs
      });
    }
    this.persist();
  }

  touchPlayer(player: Pick<PlayerRuntime, "playerId" | "displayName" | "rating" | "peakRating" | "rankedGames" | "rankedWins" | "rankedLosses" | "rankedDraws">): void {
    const current = this.players.get(player.playerId);
    this.players.set(player.playerId, {
      playerId: player.playerId,
      displayName: player.displayName,
      rating: player.rating,
      peakRating: Math.max(current?.peakRating ?? 1000, player.peakRating),
      rankedGames: player.rankedGames,
      rankedWins: player.rankedWins,
      rankedLosses: player.rankedLosses,
      rankedDraws: player.rankedDraws,
      updatedAtMs: Date.now()
    });
    this.persist();
  }

  updateDisplayName(playerId: string, displayName: string): void {
    const current = this.players.get(playerId);
    if (!current) return;
    this.players.set(playerId, { ...current, displayName, updatedAtMs: Date.now() });
    this.persist();
  }

  getPersistedPlayerProfile(playerId: string): PersistedPlayerProfile | null {
    const profile = this.players.get(playerId);
    return profile ?? null;
  }

  getPlayerStats(playerId: string): PlayerStats | null {
    const playerMatches = this.matches.filter((match) => match.players.some((player) => player.playerId === playerId));
    const profile = this.players.get(playerId);
    if (playerMatches.length === 0 && !profile) return null;

    const latestPlayer = [...playerMatches]
      .sort((a, b) => b.finishedAtMs - a.finishedAtMs)
      .flatMap((match) => match.players)
      .find((player) => player.playerId === playerId);

    const matchesPlayed = playerMatches.length;
    const wins = playerMatches.filter((match) => match.winnerPlayerId === playerId).length;
    const draws = playerMatches.filter((match) => match.winnerPlayerId === null).length;
    const losses = matchesPlayed - wins - draws;

    const totalScore = playerMatches.reduce((sum, match) => {
      const player = match.players.find((entry) => entry.playerId === playerId);
      return sum + (player?.score ?? 0);
    }, 0);

    const averageScore = matchesPlayed > 0 ? Number((totalScore / matchesPlayed).toFixed(2)) : 0;
    const recentMatchIds = playerMatches
      .sort((a, b) => b.finishedAtMs - a.finishedAtMs)
      .slice(0, 10)
      .map((match) => match.matchId);

    const rankedMatches = playerMatches.filter((match) => match.mode === "ranked");

    const ratingFromMatches = latestPlayer?.ratingAfter ?? 1000;
    const peakFromMatches = playerMatches.reduce((max, match) => {
      const entry = match.players.find((player) => player.playerId === playerId);
      return Math.max(max, entry?.ratingAfter ?? 1000);
    }, 1000);
    const rating = profile?.rating ?? ratingFromMatches;
    const peakRating = Math.max(profile?.peakRating ?? 1000, peakFromMatches);
    const rankedGames = Math.max(
      rankedMatches.length,
      profile?.rankedGames ?? 0
    );
    const rankedWins = Math.max(
      rankedMatches.filter((match) => match.winnerPlayerId === playerId).length,
      profile?.rankedWins ?? 0
    );
    const rankedDraws = Math.max(
      rankedMatches.filter((match) => match.winnerPlayerId === null).length,
      profile?.rankedDraws ?? 0
    );
    const rankedLosses = Math.max(rankedGames - rankedWins - rankedDraws, profile?.rankedLosses ?? 0);

    return {
      playerId,
      displayName: profile?.displayName ?? latestPlayer?.displayName ?? "Guest",
      matchesPlayed,
      wins,
      losses,
      draws,
      totalScore,
      averageScore,
      recentMatchIds,
      rating,
      peakRating,
      rankTier: ratingToTier(rating),
      rankedGames: Math.max(0, rankedGames),
      rankedWins: Math.max(0, rankedWins),
      rankedLosses: Math.max(0, rankedLosses),
      rankedDraws: Math.max(0, rankedDraws)
    };
  }

  getPlayerMatchHistory(playerId: string, limit = 20): FinishedMatchRecord[] {
    return this.matches
      .filter((match) => match.players.some((player) => player.playerId === playerId))
      .sort((a, b) => b.finishedAtMs - a.finishedAtMs)
      .slice(0, limit);
  }

  getLeaderboard(limit = 50): LeaderboardEntry[] {
    const latestByPlayer = new Map<string, LeaderboardEntry>();

    const byFinishedAsc = [...this.matches].sort((a, b) => a.finishedAtMs - b.finishedAtMs);
    for (const match of byFinishedAsc) {
      for (const player of match.players) {
        const current = latestByPlayer.get(player.playerId);
        const updated: LeaderboardEntry = {
          playerId: player.playerId,
          displayName: player.displayName,
          rating: player.ratingAfter,
          rankTier: player.rankTier,
          rankedGames: (current?.rankedGames ?? 0) + (match.mode === "ranked" ? 1 : 0),
          wins:
            (current?.wins ?? 0) + (match.mode === "ranked" && match.winnerPlayerId === player.playerId ? 1 : 0),
          draws:
            (current?.draws ?? 0) + (match.mode === "ranked" && match.winnerPlayerId === null ? 1 : 0),
          losses:
            (current?.losses ?? 0) +
            (match.mode === "ranked" && match.winnerPlayerId !== null && match.winnerPlayerId !== player.playerId ? 1 : 0),
          peakRating: Math.max(current?.peakRating ?? 1000, player.ratingAfter)
        };

        latestByPlayer.set(player.playerId, updated);
      }
    }

    return [...latestByPlayer.values()]
      .filter((entry) => entry.rankedGames > 0)
      .sort((a, b) => (b.rating === a.rating ? b.rankedGames - a.rankedGames : b.rating - a.rating))
      .slice(0, limit);
  }

  private async loadFromRedisOrFile(): Promise<void> {
    try {
      this.redis = getRedis();
      await this.redis.connect();
      const raw = await this.redis.get(REDIS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as PersistedShape;
        if (Array.isArray(parsed.matches)) {
          this.matches.push(...parsed.matches);
        }
        const persistedPlayers = parsed.players ?? {};
        for (const [playerId, profile] of Object.entries(persistedPlayers)) {
          this.players.set(playerId, profile);
        }
        return;
      }
    } catch (err) {
      logger.warn({ err }, "Failed to load match history from Redis. Falling back to file store.");
      this.redis = null;
    }

    try {
      if (!fs.existsSync(this.filePath)) {
        return;
      }

      const raw = fs.readFileSync(this.filePath, "utf8");
      const parsed = JSON.parse(raw) as PersistedShape;
      if (Array.isArray(parsed.matches)) {
        this.matches.push(...parsed.matches);
      }
      const persistedPlayers = parsed.players ?? {};
      for (const [playerId, profile] of Object.entries(persistedPlayers)) {
        this.players.set(playerId, profile);
      }
    } catch {
      // Store failures should never crash game runtime.
    }
  }

  private persist(): void {
    const payload = {
      matches: this.matches,
      players: Object.fromEntries(this.players.entries())
    } satisfies PersistedShape;

    if (this.redis) {
      void this.redis
        .set(REDIS_KEY, JSON.stringify(payload))
        .catch((err) => logger.warn({ err }, "Failed to persist match history to Redis"));
    }

    try {
      const dir = path.dirname(this.filePath);
      fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(this.filePath, JSON.stringify(payload, null, 2));
    } catch {
      // Store failures should never crash game runtime.
    }
  }
}
