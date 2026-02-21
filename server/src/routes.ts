import type { Request, Response } from "express";
import { Router } from "express";
import type { PresenceStore } from "./store/presenceStore.js";
import type { MatchHistoryStore } from "./store/matchHistoryStore.js";

export const createApiRouter = (matchHistoryStore: MatchHistoryStore, presenceStore: PresenceStore): Router => {
  const router = Router();

  router.get("/health", (_req: Request, res: Response) => {
    res.json({
      status: "ok",
      service: "anagram-server",
      timestamp: new Date().toISOString()
    });
  });

  router.get("/presence", (_req: Request, res: Response) => {
    res.json({
      playersOnline: presenceStore.getCount(),
      serverNowMs: Date.now()
    });
  });

  router.get("/profiles/:playerId/stats", (req: Request, res: Response) => {
    const stats = matchHistoryStore.getPlayerStats(req.params.playerId);
    if (!stats) {
      res.json({
        playerId: req.params.playerId,
        displayName: "Guest",
        matchesPlayed: 0,
        wins: 0,
        losses: 0,
        draws: 0,
        totalScore: 0,
        averageScore: 0,
        recentMatchIds: [],
        rating: 1000,
        peakRating: 1000,
        rankTier: "silver",
        rankedGames: 0,
        rankedWins: 0,
        rankedLosses: 0,
        rankedDraws: 0
      });
      return;
    }

    res.json(stats);
  });

  router.get("/profiles/:playerId/matches", (req: Request, res: Response) => {
    const limit = Number(req.query.limit ?? 20);
    const history = matchHistoryStore.getPlayerMatchHistory(req.params.playerId, Number.isFinite(limit) ? limit : 20);
    res.json({
      playerId: req.params.playerId,
      count: history.length,
      matches: history
    });
  });

  router.get("/leaderboard", (req: Request, res: Response) => {
    const limit = Number(req.query.limit ?? 50);
    const entries = matchHistoryStore.getLeaderboard(Number.isFinite(limit) ? limit : 50);
    res.json({
      count: entries.length,
      entries
    });
  });

  return router;
};
