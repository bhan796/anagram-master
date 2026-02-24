import { useMemo, useState } from "react";
import { ArcadeBackButton, ArcadeScaffold, NeonDivider, NeonTitle, RankBadge } from "../components/ArcadeComponents";
import { getCosmeticClass } from "../lib/cosmetics";

interface StatsSummary {
  playerId: string;
  displayName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  totalScore: number;
  rating: number;
  peakRating: number;
  rankTier: string;
  rankedGames: number;
  rankedWins: number;
  rankedLosses: number;
  rankedDraws: number;
  equippedCosmetic?: string | null;
  runes?: number;
}

interface HistoryPlayer {
  playerId: string;
  displayName: string;
  score: number;
}

interface HistoryItem {
  matchId: string;
  createdAtMs: number;
  finishedAtMs: number;
  mode: "casual" | "ranked";
  winnerPlayerId: string | null;
  players: HistoryPlayer[];
}

interface ProfileScreenProps {
  isLoading: boolean;
  error: string | null;
  stats: StatsSummary | null;
  history: HistoryItem[];
  onBack: () => void;
  onRetry: () => void;
  onUpdateDisplayName: (value: string) => Promise<void>;
  isAuthenticated: boolean;
  runes: number;
}

export const ProfileScreen = ({ isLoading, error, stats, history, onBack, onRetry, onUpdateDisplayName, isAuthenticated, runes }: ProfileScreenProps) => {
  const runeIcon = (
    <span style={{ fontFamily: '"Segoe UI Symbol", "Arial Unicode MS", Arial, sans-serif' }}>\u2666</span>
  );

  const [nameDraft, setNameDraft] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [savingName, setSavingName] = useState(false);

  const rankProgress = useMemo(() => {
    const rating = stats?.rating ?? 1000;
    const tiers = [
      { tier: "bronze", min: 0 },
      { tier: "silver", min: 1000 },
      { tier: "gold", min: 1150 },
      { tier: "platinum", min: 1300 },
      { tier: "diamond", min: 1500 },
      { tier: "master", min: 1700 }
    ];

    let currentIndex = 0;
    for (let index = tiers.length - 1; index >= 0; index -= 1) {
      if (rating >= tiers[index].min) {
        currentIndex = index;
        break;
      }
    }

    const current = tiers[currentIndex];
    const next = tiers[currentIndex + 1] ?? null;
    if (!next) {
      return {
        currentTier: current.tier,
        nextTier: null as string | null,
        eloToNext: 0,
        progressPct: 100
      };
    }

    const span = Math.max(1, next.min - current.min);
    const progress = Math.max(0, Math.min(1, (rating - current.min) / span));
    return {
      currentTier: current.tier,
      nextTier: next.tier,
      eloToNext: Math.max(0, next.min - rating),
      progressPct: Math.round(progress * 100)
    };
  }, [stats?.rating]);

  const statRows = useMemo(
    () =>
      stats
        ? [
            ["Matches", stats.matchesPlayed],
            ["Wins", stats.wins],
            ["Losses", stats.losses],
            ["Draws", stats.draws],
            ["Total Score", stats.totalScore],
            ["Peak Rating", stats.peakRating],
            ...(isAuthenticated ? ([["Ranked", `${stats.rankedWins}-${stats.rankedLosses}-${stats.rankedDraws}`]] as [string, string][]) : [])
          ]
        : [],
    [isAuthenticated, stats]
  );

  return (
    <ArcadeScaffold className="accent-gold">
      <ArcadeBackButton onClick={onBack} />
      <div className={getCosmeticClass(stats?.equippedCosmetic)}>
        <NeonTitle text={stats?.displayName ?? "Profile"} />
      </div>
      {isAuthenticated ? <div className="headline" style={{ color: "var(--gold)" }}>{runeIcon} {runes.toLocaleString()} RUNES</div> : null}

      {stats ? (
        <div className="card" style={{ display: "grid", gap: 10 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
            <div>
              <div className="text-dim">Current Rank</div>
              <div className="headline" style={{ color: "var(--gold)", marginTop: 6 }}>
                {rankProgress.currentTier.toUpperCase()} - {stats.rating} ELO
              </div>
            </div>
            <RankBadge tier={stats.rankTier} />
          </div>

          {rankProgress.nextTier ? (
            <>
              <div className="text-dim" style={{ color: "var(--gold)" }}>
                {rankProgress.eloToNext} ELO to {rankProgress.nextTier.toUpperCase()}
              </div>
              <div
                style={{
                  width: "100%",
                  height: 8,
                  background: "var(--surface)",
                  borderRadius: 999,
                  overflow: "hidden",
                  border: "1px solid rgba(255,215,0,.25)"
                }}
              >
                <div
                  style={{
                    width: `${rankProgress.progressPct}%`,
                    height: "100%",
                    background: "linear-gradient(90deg, rgba(255,215,0,.55), rgba(255,215,0,.95))"
                  }}
                />
              </div>
            </>
          ) : (
            <div className="text-dim" style={{ color: "var(--gold)" }}>
              Max rank reached.
            </div>
          )}
        </div>
      ) : null}

      <div className="card" style={{ display: "grid", gap: 10 }}>
        <div className="text-dim">Change Username</div>
        <input
          value={nameDraft}
          onChange={(event) => setNameDraft(event.target.value)}
          placeholder={stats?.displayName ?? "Enter username"}
          style={{
            width: "100%",
            boxSizing: "border-box",
            background: "var(--surface-variant)",
            border: "1px solid rgba(255,215,0,.5)",
            color: "var(--white)",
            borderRadius: 6,
            padding: "10px 12px",
            fontFamily: "var(--font-pixel)",
            fontSize: "var(--text-label)"
          }}
        />
        <button
          type="button"
          className="arcade-button"
          disabled={savingName || !nameDraft.trim()}
          onClick={async () => {
            try {
              setSavingName(true);
              setNameError(null);
              await onUpdateDisplayName(nameDraft.trim());
              setNameDraft("");
            } catch (err) {
              setNameError(err instanceof Error ? err.message : "Could not update username.");
            } finally {
              setSavingName(false);
            }
          }}
        >
          {savingName ? "Saving..." : "Save Username"}
        </button>
        {nameError ? <div className="text-dim" style={{ color: "var(--red)" }}>{nameError}</div> : null}
      </div>

      {error ? (
        <div className="card warning" style={{ display: "grid", gap: 10 }}>
          <div>{error}</div>
          <button type="button" className="arcade-button" onClick={onRetry}>
            Retry
          </button>
        </div>
      ) : null}

      {stats ? (
        <div className="card" style={{ display: "grid", gap: 10 }}>
          {statRows.map(([label, value], index) => (
            <div key={label} style={{ display: "grid", gap: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                <span className="text-dim">{label}</span>
                <span className="label" style={{ color: "var(--gold)" }}>
                  {value}
                </span>
              </div>
              {index < statRows.length - 1 ? <NeonDivider /> : null}
            </div>
          ))}
        </div>
      ) : null}

      <div className="headline">Match History</div>
      {history.length === 0 ? (
        <div className="card">
          <div className="text-dim">No matches played yet. Complete an online match to populate history.</div>
        </div>
      ) : (
        <div style={{ display: "grid", gap: 10 }}>
          {history.map((match) => {
            const myPlayer = match.players.find((player) => player.playerId === stats?.playerId);
            const outcome =
              !stats || match.winnerPlayerId === null
                ? "DRAW"
                : match.winnerPlayerId === stats.playerId
                  ? "WIN"
                  : "LOSS";
            return (
              <div key={match.matchId} className="card" style={{ display: "grid", gap: 8 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                  <span className="text-dim">Match {match.matchId.slice(0, 8)}</span>
                  <span
                    className="label"
                    style={{ color: outcome === "WIN" ? "var(--green)" : outcome === "LOSS" ? "var(--red)" : "var(--gold)" }}
                  >
                    {outcome}
                  </span>
                </div>
                <div className="text-dim">Your score: {myPlayer?.score ?? 0}</div>
                <div className="text-dim">Mode: {isAuthenticated ? (match.mode ?? "casual").toUpperCase() : "CASUAL"}</div>
                {match.players.map((player) => (
                  <div key={`${match.matchId}-${player.playerId}`} className="text-dim">
                    {player.displayName}: {player.score}
                  </div>
                ))}
              </div>
            );
          })}
        </div>
      )}
    </ArcadeScaffold>
  );
};

