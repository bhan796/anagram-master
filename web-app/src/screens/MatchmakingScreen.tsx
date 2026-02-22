import { useEffect, useMemo, useState } from "react";
import type { OnlineUiState } from "../types/online";
import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonTitle, RankBadge } from "../components/ArcadeComponents";

interface MatchmakingScreenProps {
  state: OnlineUiState;
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
  onJoinQueue: (mode: "casual" | "ranked") => void;
  onCancelQueue: () => void;
  onRetryConnection: () => void;
  onMatchReady: () => void;
}

export const MatchmakingScreen = ({
  state,
  leaderboard,
  onBack,
  onJoinQueue,
  onCancelQueue,
  onRetryConnection,
  onMatchReady
}: MatchmakingScreenProps) => {
  const [searchStarted, setSearchStarted] = useState(false);
  const [dots, setDots] = useState(1);
  const [selectedMode, setSelectedMode] = useState<"casual" | "ranked">("casual");
  const hasActiveMatch = Boolean(state.matchState && state.matchState.phase !== "finished");
  const isSearching = state.isInMatchmaking || searchStarted;

  useEffect(() => {
    if (state.isInMatchmaking) {
      setSearchStarted(true);
    } else if (!hasActiveMatch) {
      setSearchStarted(false);
    }
  }, [state.isInMatchmaking, hasActiveMatch]);

  useEffect(() => {
    if (!isSearching || hasActiveMatch) return;
    const timer = window.setInterval(() => setDots((value) => (value % 3) + 1), 350);
    return () => window.clearInterval(timer);
  }, [isSearching, hasActiveMatch]);

  useEffect(() => {
    if (searchStarted && hasActiveMatch) {
      const timer = window.setTimeout(() => {
        onMatchReady();
        setSearchStarted(false);
      }, 900);
      return () => window.clearTimeout(timer);
    }
  }, [searchStarted, hasActiveMatch, onMatchReady]);

  const buttonText = useMemo(() => {
    if (isSearching && hasActiveMatch) return "Match Found!";
    if (isSearching && state.isInMatchmaking) return `Searching${".".repeat(dots)}`;
    return "Find Opponent";
  }, [isSearching, hasActiveMatch, state.isInMatchmaking, dots]);

  const primaryEnabled =
    !state.isInMatchmaking &&
    (state.connectionState === "connected" || state.connectionState === "connecting");
  const showRetry = state.connectionState === "failed" || state.connectionState === "disconnected";

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Search for opponents..." />

      <div className="card" style={{ borderColor: "rgba(0,245,255,.55)", padding: "12px 14px", display: "grid", gap: 8 }}>
        <div className="text-dim">Username</div>
        <div className="headline" style={{ fontSize: "clamp(10px, 1.2vw, 12px)" }}>
          {state.displayName ?? "Guest"}
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <RankBadge tier={state.playerRankTier} />
          <span className="label" style={{ color: "var(--green)" }}>
            {state.playerRating} ELO
          </span>
        </div>
      </div>

      <div className="mode-select-row">
        <button
          type="button"
          className={`mode-select-btn ${selectedMode === "casual" ? "selected" : "unselected"}`}
          onClick={() => setSelectedMode("casual")}
          disabled={isSearching || state.isInMatchmaking || hasActiveMatch}
        >
          Casual
        </button>
        <button
          type="button"
          className={`mode-select-btn ${selectedMode === "ranked" ? "selected" : "unselected"}`}
          onClick={() => setSelectedMode("ranked")}
          disabled={isSearching || state.isInMatchmaking || hasActiveMatch}
        >
          Ranked
        </button>
      </div>

      <ArcadeButton
        text={buttonText}
        onClick={() => {
          setSearchStarted(true);
          onJoinQueue(selectedMode);
        }}
        disabled={!primaryEnabled}
      />

      <div className="card" style={{ display: "grid", gap: 8 }}>
        <div className="headline" style={{ fontSize: "clamp(10px, 1.2vw, 12px)" }}>
          Leaderboard
        </div>
        {leaderboard.length === 0 ? (
          <div className="text-dim">No ranked matches yet.</div>
        ) : (
          leaderboard.slice(0, 8).map((entry, index) => (
            <div key={entry.playerId} style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
              <span className="text-dim">
                #{index + 1} {entry.displayName}
              </span>
              <span className="label" style={{ color: "var(--green)" }}>
                {entry.rating}
              </span>
            </div>
          ))
        )}
      </div>

      {isSearching && state.isInMatchmaking && !hasActiveMatch ? (
        <ArcadeButton
          text="Cancel Search"
          onClick={() => {
            setSearchStarted(false);
            onCancelQueue();
          }}
        />
      ) : null}

      {showRetry ? <ArcadeButton text="Retry Connection" onClick={onRetryConnection} /> : null}

      {state.localValidationMessage ? <div className="text-dim" style={{ color: "var(--red)" }}>{state.localValidationMessage}</div> : null}
    </ArcadeScaffold>
  );
};
