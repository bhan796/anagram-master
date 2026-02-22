import type { NextFunction, Request, Response } from "express";
import { ensureRedisConnected } from "../config/redis.js";

export const createRateLimiter = (bucket: string, limit: number, windowSeconds: number) => {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const redis = await ensureRedisConnected();
      const ip = req.ip || "unknown";
      const key = `rate:${bucket}:${ip}`;
      const hits = await redis.incr(key);
      if (hits === 1) {
        await redis.expire(key, windowSeconds);
      }

      if (hits > limit) {
        res.status(429).json({
          code: "RATE_LIMITED",
          message: "Too many requests. Please try again shortly."
        });
        return;
      }
    } catch {
      // fail-open for availability in case Redis is unavailable
    }

    next();
  };
};
