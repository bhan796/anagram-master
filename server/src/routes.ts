import type { Request, Response } from "express";
import { Router } from "express";
import { AuthService } from "./auth/authService.js";
import { attachOptionalAuth, requireAuth } from "./auth/httpAuth.js";
import { createRateLimiter } from "./auth/rateLimit.js";
import { toActionError } from "./events/contracts.js";
import { ratingToTier } from "./game/ranking.js";
import { prisma } from "./config/prisma.js";
import { ACHIEVEMENTS, COSMETIC_ITEMS, getChestOdds, rollChest } from "./game/achievements.js";
import type { MatchService } from "./game/matchService.js";
import type { PresenceStore } from "./store/presenceStore.js";
import type { MatchHistoryStore } from "./store/matchHistoryStore.js";

export const canReadOwnedProfile = (requestingUserId: string | undefined, ownerUserId: string | null): boolean => {
  if (!ownerUserId) return true;
  return Boolean(requestingUserId && requestingUserId === ownerUserId);
};

export const canMutateProfile = (requestingUserId: string | undefined, ownerUserId: string | null): boolean => {
  return Boolean(requestingUserId && (!ownerUserId || requestingUserId === ownerUserId));
};

const ensureActiveUser = async (req: Request, res: Response, authService: AuthService): Promise<boolean> => {
  if (!req.auth?.userId) {
    res.status(401).json({ code: "AUTH_REQUIRED", message: "Authentication required." });
    return false;
  }
  const exists = await authService.userExists(req.auth.userId);
  if (!exists) {
    res.status(401).json({ code: "AUTH_INVALID_SESSION", message: "Session is no longer valid." });
    return false;
  }
  return true;
};

export const createApiRouter = (
  matchHistoryStore: MatchHistoryStore,
  presenceStore: PresenceStore,
  matchService: MatchService,
  authService: AuthService
): Router => {
  const router = Router();
  const authLimiter = createRateLimiter("auth", 20, 60);
  const refreshLimiter = createRateLimiter("refresh", 40, 60);
  const profileMutationLimiter = createRateLimiter("profile_mutation", 30, 60);
  const destructiveActionLimiter = createRateLimiter("destructive_action", 10, 60);

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

  router.delete("/auth/account", requireAuth, destructiveActionLimiter, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    try {
      await authService.deleteAccount(req.auth!.userId);
      res.status(204).send();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unable to delete account.";
      res.status(500).json({ code: "AUTH_DELETE_ACCOUNT_FAILED", message });
    }
  });

  router.get("/auth/me", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const me = await authService.me(req.auth!.userId);
    if (!me) {
      res.status(404).json({ code: "AUTH_USER_NOT_FOUND", message: "User not found." });
      return;
    }
    res.json(me);
  });

  router.post("/auth/claim-guest", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
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
    const requestingUserId = req.auth?.userId;
    if (ownerUserId && !requestingUserId) {
      res.status(401).json({ code: "AUTH_REQUIRED", message: "Authentication required." });
      return;
    }
    if (!canReadOwnedProfile(requestingUserId, ownerUserId)) {
      res.status(403).json({ code: "FORBIDDEN", message: "Cannot view another player's profile." });
      return;
    }

    const canViewRanked = Boolean(requestingUserId && ownerUserId && ownerUserId === requestingUserId);

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
        equippedCosmetic: persistedProfile?.equippedCosmetic ?? runtimePlayer?.equippedCosmetic ?? null,
        runes: persistedProfile?.runes ?? 0,
        rankedGames: canViewRanked ? (runtimePlayer?.rankedGames ?? persistedProfile?.rankedGames ?? 0) : 0,
        rankedWins: canViewRanked ? (runtimePlayer?.rankedWins ?? persistedProfile?.rankedWins ?? 0) : 0,
        rankedLosses: canViewRanked ? (runtimePlayer?.rankedLosses ?? persistedProfile?.rankedLosses ?? 0) : 0,
        rankedDraws: canViewRanked ? (runtimePlayer?.rankedDraws ?? persistedProfile?.rankedDraws ?? 0) : 0
      });
      return;
    }

    const persistedProfile = await authService.getPlayerProfile(req.params.playerId);
    res.json({
      ...stats,
      equippedCosmetic: persistedProfile?.equippedCosmetic ?? null,
      runes: persistedProfile?.runes ?? 0,
      rankedGames: canViewRanked ? stats.rankedGames : 0,
      rankedWins: canViewRanked ? stats.rankedWins : 0,
      rankedLosses: canViewRanked ? stats.rankedLosses : 0,
      rankedDraws: canViewRanked ? stats.rankedDraws : 0
    });
  });

  router.post("/profiles/:playerId/display-name", profileMutationLimiter, async (req: Request, res: Response) => {
    const nextDisplayName = String(req.body?.displayName ?? "");
    const normalized = nextDisplayName.trim();
    if (!normalized || normalized.length < 3 || normalized.length > 20 || !/^[A-Za-z0-9_ ]+$/.test(normalized)) {
      res.status(400).json(toActionError("profile:update_display_name", "INVALID_DISPLAY_NAME"));
      return;
    }

    const ownerUserId = await authService.resolveUserIdForPlayer(req.params.playerId);
    const requestingUserId = req.auth?.userId;
    if (!requestingUserId) {
      res.status(401).json({ code: "AUTH_REQUIRED", message: "Authentication required." });
      return;
    }
    const active = await authService.userExists(requestingUserId);
    if (!active) {
      res.status(401).json({ code: "AUTH_INVALID_SESSION", message: "Session is no longer valid." });
      return;
    }
    if (!canMutateProfile(requestingUserId, ownerUserId)) {
      res.status(403).json({ code: "FORBIDDEN", message: "Cannot update another player's profile." });
      return;
    }

    const isTaken = await authService.isDisplayNameTaken(normalized, req.params.playerId);
    if (isTaken) {
      res.status(409).json(toActionError("profile:update_display_name", "DISPLAY_NAME_TAKEN"));
      return;
    }

    const runtimePlayer = matchService.getPlayer(req.params.playerId);
    if (runtimePlayer) {
      const runtimeResult = matchService.updateDisplayName(req.params.playerId, normalized);
      if (!runtimeResult.ok) {
        const payload = toActionError("profile:update_display_name", runtimeResult.code ?? "ACTION_FAILED");
        const status = payload.code === "DISPLAY_NAME_TAKEN" ? 409 : payload.code === "UNKNOWN_PLAYER" ? 404 : 400;
        res.status(status).json(payload);
        return;
      }
    }

    const effectiveOwnerUserId = ownerUserId ?? requestingUserId ?? runtimePlayer?.userId ?? null;
    await authService.upsertPlayerIdentity(req.params.playerId, normalized, effectiveOwnerUserId);
    matchHistoryStore.updateDisplayName(req.params.playerId, normalized);

    res.json({
      playerId: req.params.playerId,
      displayName: normalized
    });
  });

  router.get("/profiles/:playerId/matches", async (req: Request, res: Response) => {
    const targetPlayerId = req.params.playerId;
    const ownerUserId = await authService.resolveUserIdForPlayer(targetPlayerId);
    const requestingUserId = req.auth?.userId;
    if (ownerUserId && !requestingUserId) {
      res.status(401).json({ code: "AUTH_REQUIRED", message: "Authentication required." });
      return;
    }
    if (!canReadOwnedProfile(requestingUserId, ownerUserId)) {
      res.status(403).json({ code: "FORBIDDEN", message: "Cannot view another player's match history." });
      return;
    }

    const limit = Number(req.query.limit ?? 20);
    const history = matchHistoryStore.getPlayerMatchHistory(req.params.playerId, Number.isFinite(limit) ? limit : 20);
    res.json({
      playerId: req.params.playerId,
      count: history.length,
      matches: history
    });
  });

  router.get("/achievements", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.json({ achievements: [] });
      return;
    }

    const unlocked = await prisma.playerAchievement.findMany({
      where: { playerId },
      select: { achievementId: true, unlockedAt: true }
    });
    const unlockedMap = new Map(unlocked.map((u) => [u.achievementId, u.unlockedAt] as const));

    res.json({
      achievements: ACHIEVEMENTS.map((a) => ({
        ...a,
        unlocked: unlockedMap.has(a.id),
        unlockedAt: unlockedMap.get(a.id)?.toISOString() ?? null
      }))
    });
  });

  router.get("/shop", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    res.json({
      items: [
        {
          id: "treasure_chest",
          name: "Treasure Chest",
          cost: 200,
          description: "Contains a random cosmetic for your display name. Opens with a reveal animation."
        }
      ]
    });
  });

  router.get("/shop/odds", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    res.json({ items: getChestOdds() });
  });

  router.post("/shop/purchase", requireAuth, profileMutationLimiter, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.status(404).json({ code: "PLAYER_NOT_FOUND", message: "No player profile found." });
      return;
    }

    try {
      const updated = await prisma.$transaction(async (tx) => {
        const player = await tx.player.findUnique({ where: { id: playerId }, select: { runes: true } });
        if (!player || player.runes < 200) {
          throw new Error("INSUFFICIENT_RUNES");
        }
        return tx.player.update({
          where: { id: playerId },
          data: { runes: { decrement: 200 }, pendingChests: { increment: 1 } },
          select: { runes: true, pendingChests: true }
        });
      });

      res.json({ remainingRunes: updated.runes, pendingChests: updated.pendingChests });
    } catch (error) {
      const msg = error instanceof Error ? error.message : "Purchase failed.";
      res
        .status(msg === "INSUFFICIENT_RUNES" ? 402 : 500)
        .json({ code: msg, message: msg === "INSUFFICIENT_RUNES" ? "Not enough Runes." : "Purchase failed." });
    }
  });

  router.post("/shop/open-chest", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.status(404).json({ code: "PLAYER_NOT_FOUND", message: "No player profile found." });
      return;
    }

    const player = await prisma.player.findUnique({ where: { id: playerId }, select: { pendingChests: true } });
    if (!player || player.pendingChests < 1) {
      res.status(400).json({ code: "NO_CHEST", message: "No chest available to open." });
      return;
    }

    await prisma.player.update({ where: { id: playerId }, data: { pendingChests: { decrement: 1 } } });
    let item = rollChest();
    const existing = await prisma.playerInventory.findUnique({ where: { playerId_itemId: { playerId, itemId: item.id } } });
    const alreadyOwned = Boolean(existing);
    if (existing) {
      item = rollChest();
    }

    await prisma.playerInventory.upsert({
      where: { playerId_itemId: { playerId, itemId: item.id } },
      update: {},
      create: { playerId, itemId: item.id }
    });

    res.json({ item, alreadyOwned });
  });

  router.get("/inventory", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.json({ items: [], equippedCosmetic: null, pendingChests: 0, runes: 0 });
      return;
    }

    const [inventory, player] = await Promise.all([
      prisma.playerInventory.findMany({ where: { playerId }, select: { itemId: true, obtainedAt: true } }),
      prisma.player.findUnique({ where: { id: playerId }, select: { equippedCosmetic: true, pendingChests: true, runes: true } })
    ]);
    const catalogMap = new Map(COSMETIC_ITEMS.map((c) => [c.id, c] as const));
    res.json({
      items: inventory
        .map((item) => {
          const catalog = catalogMap.get(item.itemId);
          if (!catalog) return null;
          return { ...catalog, obtainedAt: item.obtainedAt.toISOString() };
        })
        .filter(Boolean),
      equippedCosmetic: player?.equippedCosmetic ?? null,
      pendingChests: player?.pendingChests ?? 0,
      runes: player?.runes ?? 0
    });
  });

  router.post("/inventory/equip", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.status(404).json({ code: "PLAYER_NOT_FOUND", message: "No player profile found." });
      return;
    }

    const itemId: string | null = req.body?.itemId ?? null;
    if (itemId !== null) {
      const owned = await prisma.playerInventory.findUnique({ where: { playerId_itemId: { playerId, itemId } } });
      if (!owned) {
        res.status(403).json({ code: "NOT_OWNED", message: "Item not in inventory." });
        return;
      }
    }

    await prisma.player.update({ where: { id: playerId }, data: { equippedCosmetic: itemId } });
    res.json({ equippedCosmetic: itemId });
  });

  router.get("/player/runes", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const playerId = await authService.resolvePrimaryPlayerIdForUser(req.auth!.userId);
    if (!playerId) {
      res.json({ runes: 0 });
      return;
    }
    const player = await prisma.player.findUnique({ where: { id: playerId }, select: { runes: true } });
    res.json({ runes: player?.runes ?? 0 });
  });

  router.get("/leaderboard", requireAuth, async (req: Request, res: Response) => {
    if (!(await ensureActiveUser(req, res, authService))) return;
    const limit = Number(req.query.limit ?? 50);
    const entries = matchHistoryStore.getLeaderboard(Number.isFinite(limit) ? limit : 50);
    const cosmetics = await prisma.player.findMany({
      where: { id: { in: entries.map((entry) => entry.playerId) } },
      select: { id: true, equippedCosmetic: true }
    });
    const cosmeticByPlayer = new Map(cosmetics.map((player) => [player.id, player.equippedCosmetic] as const));
    res.json({
      count: entries.length,
      entries: entries.map((entry) => ({
        ...entry,
        equippedCosmetic: cosmeticByPlayer.get(entry.playerId) ?? null
      }))
    });
  });

  return router;
};
