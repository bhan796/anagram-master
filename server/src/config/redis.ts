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

export const ensureRedisConnected = async (): Promise<Redis> => {
  const client = getRedis();
  const status = client.status;

  if (status === "ready" || status === "connect") {
    return client;
  }

  if (status === "wait" || status === "end") {
    await client.connect();
    return client;
  }

  if (status === "connecting" || status === "reconnecting" || status === "close") {
    await new Promise<void>((resolve, reject) => {
      const onReady = () => {
        cleanup();
        resolve();
      };
      const onError = (error: unknown) => {
        cleanup();
        reject(error);
      };
      const cleanup = () => {
        client.off("ready", onReady);
        client.off("error", onError);
      };

      client.on("ready", onReady);
      client.on("error", onError);
    });
  }

  return client;
};
