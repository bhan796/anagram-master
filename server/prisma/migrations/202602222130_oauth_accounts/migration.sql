CREATE TABLE IF NOT EXISTS "OauthAccount" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "providerUserId" TEXT NOT NULL,
    "email" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "OauthAccount_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "OauthAccount_provider_providerUserId_key"
ON "OauthAccount"("provider", "providerUserId");

CREATE INDEX IF NOT EXISTS "OauthAccount_userId_idx"
ON "OauthAccount"("userId");

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'OauthAccount_userId_fkey'
  ) THEN
    ALTER TABLE "OauthAccount"
    ADD CONSTRAINT "OauthAccount_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "User"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;
