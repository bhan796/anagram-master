import express from "express";
import request from "supertest";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApiRouter } from "../src/routes.js";

vi.mock("../src/auth/rateLimit.js", () => ({
  createRateLimiter: () => (_req: express.Request, _res: express.Response, next: express.NextFunction) => next()
}));

const makeApp = (overrides?: {
  userExists?: (userId: string) => Promise<boolean>;
  deleteAccount?: (userId: string) => Promise<void>;
}) => {
  const app = express();
  app.use(express.json());
  app.use((req, _res, next) => {
    const userId = req.header("x-test-user");
    if (userId) {
      req.auth = { userId, sessionId: "test-session" };
    }
    next();
  });

  const authService = {
    register: vi.fn(),
    login: vi.fn(),
    oauthLogin: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
    claimGuest: vi.fn(),
    resolveUserIdForPlayer: vi.fn(async () => null),
    resolvePrimaryPlayerIdForUser: vi.fn(async () => null),
    getPlayerProfile: vi.fn(async () => null),
    upsertPlayerIdentity: vi.fn(async () => undefined),
    upsertPlayerProgress: vi.fn(async () => undefined),
    isDisplayNameTaken: vi.fn(async () => false),
    userExists: overrides?.userExists ?? (vi.fn(async () => true) as (userId: string) => Promise<boolean>),
    deleteAccount: overrides?.deleteAccount ?? (vi.fn(async () => undefined) as (userId: string) => Promise<void>)
  } as const;

  const matchHistoryStore = {
    getPlayerStats: vi.fn(() => null),
    getPlayerMatchHistory: vi.fn(() => []),
    getLeaderboard: vi.fn(() => []),
    updateDisplayName: vi.fn()
  } as const;

  const presenceStore = { getCount: vi.fn(() => 0) } as const;
  const matchService = {
    getPlayer: vi.fn(() => undefined),
    updateDisplayName: vi.fn(() => ({ ok: true, displayName: "Demo" }))
  } as const;

  app.use(
    "/api",
    createApiRouter(
      matchHistoryStore as never,
      presenceStore as never,
      matchService as never,
      authService as never
    )
  );

  return { app, authService };
};

describe("account deletion route authorization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("rejects account deletion without auth", async () => {
    const { app } = makeApp();
    const response = await request(app).delete("/api/auth/account");
    expect(response.status).toBe(401);
    expect(response.body.code).toBe("AUTH_REQUIRED");
  });

  it("rejects account deletion for invalidated/deleted session user", async () => {
    const { app, authService } = makeApp({
      userExists: async () => false
    });
    const response = await request(app).delete("/api/auth/account").set("x-test-user", "deleted-user");
    expect(response.status).toBe(401);
    expect(response.body.code).toBe("AUTH_INVALID_SESSION");
    expect(authService.deleteAccount).not.toHaveBeenCalled();
  });

  it("deletes account for authenticated active user", async () => {
    const deleteSpy = vi.fn(async () => undefined);
    const { app, authService } = makeApp({
      userExists: async () => true,
      deleteAccount: deleteSpy
    });
    const response = await request(app).delete("/api/auth/account").set("x-test-user", "user-123");
    expect(response.status).toBe(204);
    expect(authService.deleteAccount).toHaveBeenCalledWith("user-123");
  });

  it("rejects display-name mutation for invalidated/deleted session user", async () => {
    const { app } = makeApp({
      userExists: async () => false
    });
    const response = await request(app)
      .post("/api/profiles/player-1/display-name")
      .set("x-test-user", "deleted-user")
      .send({ displayName: "ArcadeOne" });

    expect(response.status).toBe(401);
    expect(response.body.code).toBe("AUTH_INVALID_SESSION");
  });
});
