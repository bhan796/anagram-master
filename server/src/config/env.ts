import { z } from "zod";

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().positive().default(4000),
  CLIENT_ORIGIN: z.string().default("http://localhost:3000"),
  DATABASE_URL: z.string().url().default("postgresql://postgres:postgres@localhost:5432/anagram_master"),
  REDIS_URL: z.string().url().default("redis://localhost:6379"),
  JWT_ACCESS_SECRET: z.string().min(16).default("dev-access-secret-change-me"),
  JWT_REFRESH_SECRET: z.string().min(16).default("dev-refresh-secret-change-me"),
  GOOGLE_OAUTH_CLIENT_IDS: z.string().default(""),
  FACEBOOK_APP_ID: z.string().default(""),
  FACEBOOK_APP_SECRET: z.string().default(""),
  ACCESS_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().default(900),
  REFRESH_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().default(1209600),
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace"]).default("info")
});

export type Env = z.infer<typeof envSchema>;

export const loadEnv = (): Env => {
  const parsed = envSchema.safeParse(process.env);

  if (!parsed.success) {
    const details = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new Error(`Invalid environment configuration: ${details}`);
  }

  return parsed.data;
};
