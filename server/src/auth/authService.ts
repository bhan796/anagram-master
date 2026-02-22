import argon2 from "argon2";
import type { Prisma } from "@prisma/client";
import { createHash, randomUUID } from "node:crypto";
import { prisma } from "../config/prisma.js";
import { ensureRedisConnected } from "../config/redis.js";
import { loadEnv } from "../config/env.js";
import { logger } from "../config/logger.js";
import { signAccessToken, signRefreshToken, verifyRefreshToken } from "./jwt.js";

const env = loadEnv();

const refreshRedisKey = (sessionId: string) => `auth:refresh:${sessionId}`;

const hashToken = (value: string): string => createHash("sha256").update(value).digest("hex");

const normalizeEmail = (email: string): string => email.trim().toLowerCase();

const assertPasswordPolicy = (password: string): void => {
  if (password.length < 8) {
    throw new Error("Password must be at least 8 characters.");
  }
};

interface SessionPair {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  refreshExpiresInSeconds: number;
}

interface AuthResult {
  userId: string;
  email: string;
  session: SessionPair;
}

export class AuthService {
  private async issueSession(userId: string, playerId?: string | null): Promise<SessionPair> {
    const sessionId = randomUUID();
    const accessToken = signAccessToken({ sub: userId, sid: sessionId });
    const refreshToken = signRefreshToken({ sub: userId, sid: sessionId });
    const refreshTokenHash = hashToken(refreshToken);
    const expiresAt = new Date(Date.now() + env.REFRESH_TOKEN_TTL_SECONDS * 1000);

    await prisma.authSession.create({
      data: {
        id: sessionId,
        userId,
        playerId: playerId ?? null,
        refreshTokenHash,
        expiresAt
      }
    });

    const redis = await ensureRedisConnected();
    await redis.set(refreshRedisKey(sessionId), refreshTokenHash, "EX", env.REFRESH_TOKEN_TTL_SECONDS);

    return {
      accessToken,
      refreshToken,
      expiresInSeconds: env.ACCESS_TOKEN_TTL_SECONDS,
      refreshExpiresInSeconds: env.REFRESH_TOKEN_TTL_SECONDS
    };
  }

  private async revokeSessionById(sessionId: string): Promise<void> {
    const redis = await ensureRedisConnected();
    await redis.del(refreshRedisKey(sessionId));

    await prisma.authSession.updateMany({
      where: {
        id: sessionId,
        revokedAt: null
      },
      data: {
        revokedAt: new Date()
      }
    });
  }

  async register(emailInput: string, password: string, playerId?: string | null): Promise<AuthResult> {
    const email = normalizeEmail(emailInput);
    assertPasswordPolicy(password);

    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      throw new Error("Email already registered.");
    }

    const passwordHash = await argon2.hash(password, { type: argon2.argon2id });

    const user = await prisma.user.create({
      data: {
        email,
        passwordHash
      }
    });

    if (playerId) {
      await this.claimGuest(user.id, playerId);
    }

    const session = await this.issueSession(user.id, playerId);
    logger.info({ userId: user.id, email, playerId: playerId ?? null }, "auth_register_success");

    return {
      userId: user.id,
      email: user.email,
      session
    };
  }

  async login(emailInput: string, password: string, playerId?: string | null): Promise<AuthResult> {
    const email = normalizeEmail(emailInput);
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) {
      throw new Error("Invalid email or password.");
    }

    const valid = await argon2.verify(user.passwordHash, password);
    if (!valid) {
      throw new Error("Invalid email or password.");
    }

    if (playerId) {
      await this.claimGuest(user.id, playerId);
    }

    const session = await this.issueSession(user.id, playerId);
    logger.info({ userId: user.id, email, playerId: playerId ?? null }, "auth_login_success");

    return {
      userId: user.id,
      email: user.email,
      session
    };
  }

  async refresh(refreshToken: string): Promise<SessionPair> {
    const payload = verifyRefreshToken(refreshToken);
    const sessionId = payload.sid;
    const hashed = hashToken(refreshToken);

    const redis = await ensureRedisConnected();
    const redisHash = await redis.get(refreshRedisKey(sessionId));
    if (!redisHash || redisHash !== hashed) {
      await this.revokeSessionById(sessionId);
      throw new Error("Refresh token invalid or expired.");
    }

    const existing = await prisma.authSession.findUnique({ where: { id: sessionId } });
    if (!existing || existing.revokedAt || existing.expiresAt.getTime() <= Date.now() || existing.refreshTokenHash !== hashed) {
      await this.revokeSessionById(sessionId);
      throw new Error("Refresh token invalid or expired.");
    }

    const next = await this.issueSession(existing.userId, existing.playerId);

    await prisma.authSession.update({
      where: { id: sessionId },
      data: {
        revokedAt: new Date(),
        replacedById: verifyRefreshToken(next.refreshToken).sid
      }
    });
    await redis.del(refreshRedisKey(sessionId));
    logger.info({ userId: existing.userId, sessionId }, "auth_refresh_success");

    return next;
  }

  async logout(refreshToken: string): Promise<void> {
    const payload = verifyRefreshToken(refreshToken);
    await this.revokeSessionById(payload.sid);
    logger.info({ userId: payload.sub, sessionId: payload.sid }, "auth_logout_success");
  }

  async me(userId: string): Promise<{ userId: string; email: string; playerIds: string[] } | null> {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        players: {
          select: { id: true }
        }
      }
    });
    if (!user) return null;

    return {
      userId: user.id,
      email: user.email,
      playerIds: user.players.map((p: { id: string }) => p.id)
    };
  }

  async claimGuest(userId: string, playerId: string): Promise<void> {
    await prisma.$transaction(async (tx: Prisma.TransactionClient) => {
      const player = await tx.player.findUnique({ where: { id: playerId } });
      if (!player) {
        await tx.player.create({
          data: {
            id: playerId,
            userId
          }
        });
        return;
      }

      if (player.userId && player.userId !== userId) {
        throw new Error("Guest profile is already linked to another account.");
      }

      if (player.userId !== userId) {
        await tx.player.update({
          where: { id: playerId },
          data: { userId }
        });
      }
    });
    logger.info({ userId, playerId }, "auth_claim_guest_success");
  }

  async resolveUserIdForPlayer(playerId: string): Promise<string | null> {
    const player = await prisma.player.findUnique({ where: { id: playerId }, select: { userId: true } });
    return player?.userId ?? null;
  }

  async upsertPlayerIdentity(playerId: string, displayName: string | null, userId: string | null): Promise<void> {
    await prisma.player.upsert({
      where: { id: playerId },
      update: {
        displayName,
        userId: userId ?? undefined
      },
      create: {
        id: playerId,
        displayName,
        userId: userId ?? undefined
      }
    });
  }
}
