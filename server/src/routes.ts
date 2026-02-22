import type { Request, Response } from "express";
import { Router } from "express";
import { AuthService } from "./auth/authService.js";
import { attachOptionalAuth, requireAuth } from "./auth/httpAuth.js";
import { createRateLimiter } from "./auth/rateLimit.js";
import { toActionError } from "./events/contracts.js";
import { ratingToTier } from "./game/ranking.js";
import type { MatchService } from "./game/matchService.js";
import type { PresenceStore } from "./store/presenceStore.js";
import type { MatchHistoryStore } from "./store/matchHistoryStore.js";

export const createApiRouter = (
  matchHistoryStore: MatchHistoryStore,
  presenceStore: PresenceStore,
  matchService: MatchService,
  authService: AuthService
): Router => {
  const router = Router();
  const authLimiter = createRateLimiter("auth", 20, 60);
  const refreshLimiter = createRateLimiter("refresh", 40, 60);

  router.use(attachOptionalAuth);

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

  router.post("/auth/register", authLimiter, async (req: Request, res: Response) => {
    try {
      const email = String(req.body?.email ?? "");
      const password = String(req.body?.password ?? "");
      const playerId = String(req.body?.playerId ?? "").trim() || undefined;
      const result = await authService.register(email, password, playerId);
      res.status(201).json(result);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to register.";
      const status = message.includes("already") ? 409 : 400;
      res.status(status).json({ code: "AUTH_REGISTER_FAILED", message });
    }
  });

  router.post("/auth/login", authLimiter, async (req: Request, res: Response) => {
    try {
      const email = String(req.body?.email ?? "");
      const password = String(req.body?.password ?? "");
      const playerId = String(req.body?.playerId ?? "").trim() || undefined;
      const result = await authService.login(email, password, playerId);
      res.json(result);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to login.";
      res.status(401).json({ code: "AUTH_LOGIN_FAILED", message });
    }
  });

  router.post("/auth/oauth/:provider", authLimiter, async (req: Request, res: Response) => {
    try {
      const provider = String(req.params.provider ?? "").toLowerCase();
      if (provider !== "google" && provider !== "facebook") {
        res.status(400).json({ code: "AUTH_OAUTH_PROVIDER_INVALID", message: "Unsupported OAuth provider." });
        return;
      }

      const token = String(req.body?.token ?? "");
      const playerId = String(req.body?.playerId ?? "").trim() || undefined;
      const result = await authService.oauthLogin(provider, token, playerId);
      res.json(result);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to authenticate.";
      const status = message.includes("configured") ? 503 : 401;
      res.status(status).json({ code: "AUTH_OAUTH_FAILED", message });
    }
  });

  router.post("/auth/refresh", refreshLimiter, async (req: Request, res: Response) => {
    try {
      const refreshToken = String(req.body?.refreshToken ?? "");
      const session = await authService.refresh(refreshToken);
      res.json({ session });
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to refresh session.";
      res.status(401).json({ code: "AUTH_REFRESH_FAILED", message });
    }
  });

  router.post("/auth/logout", async (req: Request, res: Response) => {
    try {
      const refreshToken = String(req.body?.refreshToken ?? "");
      await authService.logout(refreshToken);
      res.status(204).send();
    } catch {
      res.status(204).send();
    }
  });

  router.get("/auth/me", requireAuth, async (req: Request, res: Response) => {
    const me = await authService.me(req.auth!.userId);
    if (!me) {
      res.status(404).json({ code: "AUTH_USER_NOT_FOUND", message: "User not found." });
      return;
    }
    res.json(me);
  });

  router.post("/auth/claim-guest", requireAuth, async (req: Request, res: Response) => {
    try {
      const playerId = String(req.body?.playerId ?? "").trim();
      if (!playerId) {
        res.status(400).json({ code: "INVALID_PLAYER_ID", message: "playerId is required." });
        return;
      }
      await authService.claimGuest(req.auth!.userId, playerId);
      res.status(204).send();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to claim guest profile.";
      res.status(409).json({ code: "CLAIM_GUEST_FAILED", message });
    }
  });

  router.get("/profiles/:playerId/stats", async (req: Request, res: Response) => {
    const targetPlayerId = req.params.playerId;
    const ownerUserId = await authService.resolveUserIdForPlayer(targetPlayerId);
    const canViewRanked = Boolean(req.auth?.userId && ownerUserId && ownerUserId === req.auth.userId);

    const stats = matchHistoryStore.getPlayerStats(req.params.playerId);
    if (!stats) {
      const runtimePlayer = matchService.getPlayer(req.params.playerId);
      const persistedProfile = await authService.getPlayerProfile(req.params.playerId);
      res.json({
        playerId: req.params.playerId,
        displayName: runtimePlayer?.displayName ?? persistedProfile?.displayName ?? "Guest",
        matchesPlayed: 0,
        wins: 0,
        losses: 0,
        draws: 0,
        totalScore: 0,
        averageScore: 0,
        recentMatchIds: [],
        rating: runtimePlayer?.rating ?? persistedProfile?.rating ?? 1000,
        peakRating: runtimePlayer?.peakRating ?? persistedProfile?.peakRating ?? 1000,
        rankTier: ratingToTier(runtimePlayer?.rating ?? persistedProfile?.rating ?? 1000),
        rankedGames: canViewRanked ? (runtimePlayer?.rankedGames ?? persistedProfile?.rankedGames ?? 0) : 0,
        rankedWins: canViewRanked ? (runtimePlayer?.rankedWins ?? persistedProfile?.rankedWins ?? 0) : 0,
        rankedLosses: canViewRanked ? (runtimePlayer?.rankedLosses ?? persistedProfile?.rankedLosses ?? 0) : 0,
        rankedDraws: canViewRanked ? (runtimePlayer?.rankedDraws ?? persistedProfile?.rankedDraws ?? 0) : 0
      });
      return;
    }

    res.json({
      ...stats,
      rankedGames: canViewRanked ? stats.rankedGames : 0,
      rankedWins: canViewRanked ? stats.rankedWins : 0,
      rankedLosses: canViewRanked ? stats.rankedLosses : 0,
      rankedDraws: canViewRanked ? stats.rankedDraws : 0
    });
  });

  router.post("/profiles/:playerId/display-name", async (req: Request, res: Response) => {
    const nextDisplayName = String(req.body?.displayName ?? "");
    const result = matchService.updateDisplayName(req.params.playerId, nextDisplayName);
    if (!result.ok) {
      const payload = toActionError("profile:update_display_name", result.code ?? "ACTION_FAILED");
      const status = payload.code === "DISPLAY_NAME_TAKEN" ? 409 : payload.code === "UNKNOWN_PLAYER" ? 404 : 400;
      res.status(status).json(payload);
      return;
    }

    matchHistoryStore.updateDisplayName(req.params.playerId, result.displayName ?? nextDisplayName);
    const ownerUserId = await authService.resolveUserIdForPlayer(req.params.playerId);
    await authService.upsertPlayerIdentity(req.params.playerId, result.displayName ?? nextDisplayName, ownerUserId);

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

  router.get("/leaderboard", requireAuth, (req: Request, res: Response) => {
    const limit = Number(req.query.limit ?? 50);
    const entries = matchHistoryStore.getLeaderboard(Number.isFinite(limit) ? limit : 50);
    res.json({
      count: entries.length,
      entries
    });
  });

  return router;
};
