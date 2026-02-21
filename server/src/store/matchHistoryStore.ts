import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { FinishedMatchRecord } from "../game/types.js";

interface PersistedShape {
  matches: FinishedMatchRecord[];
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
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const defaultPath = path.resolve(__dirname, "../../data/runtime-history.json");

export class MatchHistoryStore {
  private readonly filePath: string;
  private readonly matches: FinishedMatchRecord[] = [];

  constructor(filePath = defaultPath) {
    this.filePath = filePath;
    this.load();
  }

  recordMatch(match: FinishedMatchRecord): void {
    this.matches.push(match);
    this.persist();
  }

  getPlayerStats(playerId: string): PlayerStats | null {
    const playerMatches = this.matches.filter((match) => match.players.some((player) => player.playerId === playerId));
    if (playerMatches.length === 0) return null;

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

    return {
      playerId,
      displayName: latestPlayer?.displayName ?? "Guest",
      matchesPlayed,
      wins,
      losses,
      draws,
      totalScore,
      averageScore,
      recentMatchIds
    };
  }

  getPlayerMatchHistory(playerId: string, limit = 20): FinishedMatchRecord[] {
    return this.matches
      .filter((match) => match.players.some((player) => player.playerId === playerId))
      .sort((a, b) => b.finishedAtMs - a.finishedAtMs)
      .slice(0, limit);
  }

  private load(): void {
    try {
      if (!fs.existsSync(this.filePath)) {
        return;
      }

      const raw = fs.readFileSync(this.filePath, "utf8");
      const parsed = JSON.parse(raw) as PersistedShape;
      if (Array.isArray(parsed.matches)) {
        this.matches.push(...parsed.matches);
      }
    } catch {
      // Store failures should never crash game runtime.
    }
  }

  private persist(): void {
    try {
      const dir = path.dirname(this.filePath);
      fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(this.filePath, JSON.stringify({ matches: this.matches } satisfies PersistedShape, null, 2));
    } catch {
      // Store failures should never crash game runtime.
    }
  }
}
