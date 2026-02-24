ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "runes" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "equippedCosmetic" TEXT;
ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "currentWinStreak" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "conundrumSolves" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "pendingChests" INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS "PlayerAchievement" (
  "id" TEXT NOT NULL,
  "playerId" TEXT NOT NULL,
  "achievementId" TEXT NOT NULL,
  "unlockedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "PlayerAchievement_pkey" PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "PlayerInventory" (
  "id" TEXT NOT NULL,
  "playerId" TEXT NOT NULL,
  "itemId" TEXT NOT NULL,
  "obtainedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "PlayerInventory_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "PlayerAchievement_playerId_achievementId_key" ON "PlayerAchievement"("playerId", "achievementId");
CREATE INDEX IF NOT EXISTS "PlayerAchievement_playerId_idx" ON "PlayerAchievement"("playerId");
CREATE UNIQUE INDEX IF NOT EXISTS "PlayerInventory_playerId_itemId_key" ON "PlayerInventory"("playerId", "itemId");
CREATE INDEX IF NOT EXISTS "PlayerInventory_playerId_idx" ON "PlayerInventory"("playerId");

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'PlayerAchievement_playerId_fkey'
  ) THEN
    ALTER TABLE "PlayerAchievement"
    ADD CONSTRAINT "PlayerAchievement_playerId_fkey"
    FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'PlayerInventory_playerId_fkey'
  ) THEN
    ALTER TABLE "PlayerInventory"
    ADD CONSTRAINT "PlayerInventory_playerId_fkey"
    FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;
