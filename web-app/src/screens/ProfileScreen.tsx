import { useMemo } from "react";
import { ArcadeBackButton, ArcadeScaffold, NeonDivider, NeonTitle } from "../components/ArcadeComponents";

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
  leaderboard: Array<{
    playerId: string;
    displayName: string;
    rating: number;
    rankTier: string;
    rankedGames: number;
    wins: number;
    losses: number;
    draws: number;
  }>;
  onBack: () => void;
  onRetry: () => void;
}

export const ProfileScreen = ({ isLoading, error, stats, history, leaderboard, onBack, onRetry }: ProfileScreenProps) => {
  const statRows = useMemo(
    () =>
      stats
        ? [
            ["Matches", stats.matchesPlayed],
            ["Wins", stats.wins],
            ["Losses", stats.losses],
            ["Draws", stats.draws],
            ["Total Score", stats.totalScore],
            ["Rank", stats.rankTier.toUpperCase()],
            ["Rating", stats.rating],
            ["Peak Rating", stats.peakRating],
            ["Ranked", `${stats.rankedWins}-${stats.rankedLosses}-${stats.rankedDraws}`]
          ]
        : [],
    [stats]
  );

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text={stats?.displayName ?? "Profile"} />

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
                <span className="label" style={{ color: "var(--cyan)" }}>
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
                <div className="text-dim">Mode: {(match.mode ?? "casual").toUpperCase()}</div>
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

      <div className="headline">Leaderboard</div>
      {leaderboard.length === 0 ? (
        <div className="card">
          <div className="text-dim">No ranked matches yet.</div>
        </div>
      ) : (
        <div className="card" style={{ display: "grid", gap: 8 }}>
          {leaderboard.slice(0, 20).map((entry, index) => (
            <div key={entry.playerId} style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
              <span className="text-dim">
                #{index + 1} {entry.displayName}
              </span>
              <span className="label" style={{ color: "var(--green)" }}>
                {entry.rating}
              </span>
            </div>
          ))}
        </div>
      )}
    </ArcadeScaffold>
  );
};
