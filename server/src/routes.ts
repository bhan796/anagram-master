import type { Request, Response } from "express";
import { Router } from "express";
import type { MatchHistoryStore } from "./store/matchHistoryStore.js";

export const createApiRouter = (matchHistoryStore: MatchHistoryStore): Router => {
  const router = Router();

  router.get("/health", (_req: Request, res: Response) => {
    res.json({
      status: "ok",
      service: "anagram-server",
      timestamp: new Date().toISOString()
    });
  });

  router.get("/profiles/:playerId/stats", (req: Request, res: Response) => {
    const stats = matchHistoryStore.getPlayerStats(req.params.playerId);
    if (!stats) {
      res.status(404).json({
        code: "PROFILE_NOT_FOUND",
        message: "No stats found for player."
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

  return router;
};
