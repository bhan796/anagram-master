import type { Request, Response } from "express";
import { Router } from "express";
import { toActionError } from "./events/contracts.js";
import type { MatchService } from "./game/matchService.js";
import type { PresenceStore } from "./store/presenceStore.js";
import type { MatchHistoryStore } from "./store/matchHistoryStore.js";

export const createApiRouter = (
  matchHistoryStore: MatchHistoryStore,
  presenceStore: PresenceStore,
  matchService: MatchService
): Router => {
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

  router.post("/profiles/:playerId/display-name", (req: Request, res: Response) => {
    const nextDisplayName = String(req.body?.displayName ?? "");
    const result = matchService.updateDisplayName(req.params.playerId, nextDisplayName);
    if (!result.ok) {
      const payload = toActionError("profile:update_display_name", result.code ?? "ACTION_FAILED");
      const status = payload.code === "DISPLAY_NAME_TAKEN" ? 409 : payload.code === "UNKNOWN_PLAYER" ? 404 : 400;
      res.status(status).json(payload);
      return;
    }

    res.json({
      playerId: req.params.playerId,
      displayName: result.displayName
    });
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
