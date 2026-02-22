ALTER TABLE "Player" ADD COLUMN IF NOT EXISTS "displayNameNormalized" TEXT;

UPDATE "Player"
SET
  "displayName" = NULL,
  "displayNameNormalized" = NULL
WHERE "displayName" IS NOT NULL AND btrim("displayName") = '';

WITH ranked AS (
  SELECT
    id,
    row_number() OVER (
      PARTITION BY lower(trim("displayName"))
      ORDER BY "updatedAt" DESC, id DESC
    ) AS rn
  FROM "Player"
  WHERE "displayName" IS NOT NULL AND btrim("displayName") <> ''
)
UPDATE "Player" AS p
SET
  "displayName" = CONCAT(p."displayName", '_', substring(p.id FROM 1 FOR 6)),
  "displayNameNormalized" = lower(CONCAT(p."displayName", '_', substring(p.id FROM 1 FOR 6)))
FROM ranked AS r
WHERE p.id = r.id AND r.rn > 1;

UPDATE "Player"
SET "displayNameNormalized" = lower(trim("displayName"))
WHERE "displayName" IS NOT NULL
  AND btrim("displayName") <> ''
  AND ("displayNameNormalized" IS NULL OR "displayNameNormalized" <> lower(trim("displayName")));

CREATE UNIQUE INDEX IF NOT EXISTS "Player_displayNameNormalized_key" ON "Player"("displayNameNormalized");
