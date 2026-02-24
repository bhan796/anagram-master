import { useEffect, useMemo, useState } from "react";
import { ArcadeBackButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";

type Achievement = {
  id: string;
  name: string;
  description: string;
  tier: "easy" | "medium" | "hard" | "legendary";
  runesReward: number;
  unlocked: boolean;
  unlockedAt: string | null;
};

interface AchievementsScreenProps {
  accessToken: string;
  onBack: () => void;
}

const TIER_COLORS: Record<string, string> = {
  easy: "#39ff14",
  medium: "#00f5ff",
  hard: "#cc44ff",
  legendary: "#ffd700"
};
const TIER_ORDER = ["easy", "medium", "hard", "legendary"] as const;

const normalizeBackendUrl = (raw: string | undefined): string => {
  const candidate = (raw ?? "").trim();
  if (!candidate) return "http://localhost:4000";
  if (/^https?:\/\//i.test(candidate)) return candidate.replace(/\/+$/, "");
  return `https://${candidate.replace(/\/+$/, "")}`;
};
const apiBaseUrl = normalizeBackendUrl(import.meta.env.VITE_SERVER_URL as string | undefined);

export const AchievementsScreen = ({ accessToken, onBack }: AchievementsScreenProps) => {
  const [achievements, setAchievements] = useState<Achievement[]>([]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      const response = await fetch(`${apiBaseUrl}/api/achievements`, {
        headers: { Authorization: `Bearer ${accessToken}` }
      });
      if (!response.ok) return;
      const payload = (await response.json()) as { achievements: Achievement[] };
      if (!cancelled) setAchievements(payload.achievements ?? []);
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const grouped = useMemo(() => {
    const byTier = new Map<string, Achievement[]>();
    for (const tier of TIER_ORDER) byTier.set(tier, []);
    for (const item of achievements) byTier.get(item.tier)?.push(item);
    return byTier;
  }, [achievements]);

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="ACHIEVEMENTS" />
      {TIER_ORDER.map((tier) => (
        <div key={tier} style={{ display: "grid", gap: 8 }}>
          <div className="headline" style={{ color: TIER_COLORS[tier] }}>{tier.toUpperCase()}</div>
          {(grouped.get(tier) ?? []).map((a) => (
            <div
              key={a.id}
              style={{
                background: "var(--surface-variant)",
                border: `1px solid ${TIER_COLORS[tier]}66`,
                borderRadius: 4,
                padding: "10px 12px",
                display: "grid",
                gap: 6,
                opacity: a.unlocked ? 1 : 0.55
              }}
            >
              <div className="label">{a.name}</div>
              <div className="text-dim">{a.description}</div>
              <div className="text-dim" style={{ color: "var(--gold)" }}>Reward: {a.runesReward} RUNES</div>
              <div className="text-dim">
                {a.unlocked ? `Unlocked ${a.unlockedAt ? new Date(a.unlockedAt).toLocaleDateString("en-GB") : ""}`.trim() : "Locked"}
              </div>
            </div>
          ))}
        </div>
      ))}
    </ArcadeScaffold>
  );
};
