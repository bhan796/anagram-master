import { Redis } from "ioredis";
import { loadEnv } from "./env.js";
import { logger } from "./logger.js";

let redisClient: Redis | null = null;

export const getRedis = (): Redis => {
  if (!redisClient) {
    const env = loadEnv();
    redisClient = new Redis(env.REDIS_URL, {
      lazyConnect: true,
      maxRetriesPerRequest: 3
    });

    redisClient.on("error", (err: unknown) => {
      logger.warn({ err }, "Redis error");
    });
  }

  return redisClient;
};
