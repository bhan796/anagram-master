import { useEffect, useMemo, useState } from "react";
import type { OnlineUiState } from "../types/online";
import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";

interface MatchmakingScreenProps {
  state: OnlineUiState;
  onBack: () => void;
  onJoinQueue: (mode: "casual" | "ranked") => void;
  onCancelQueue: () => void;
  onRetryConnection: () => void;
  onMatchReady: () => void;
}

export const MatchmakingScreen = ({ state, onBack, onJoinQueue, onCancelQueue, onRetryConnection, onMatchReady }: MatchmakingScreenProps) => {
  const [searchStarted, setSearchStarted] = useState(false);
  const [dots, setDots] = useState(1);
  const [selectedMode, setSelectedMode] = useState<"casual" | "ranked">("casual");
  const hasActiveMatch = Boolean(state.matchState && state.matchState.phase !== "finished");

  useEffect(() => {
    if (!searchStarted || !state.isInMatchmaking || hasActiveMatch) return;
    const timer = window.setInterval(() => setDots((value) => (value % 3) + 1), 350);
    return () => window.clearInterval(timer);
  }, [searchStarted, state.isInMatchmaking, hasActiveMatch]);

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
    if (searchStarted && hasActiveMatch) return "Match Found!";
    if (searchStarted && state.isInMatchmaking) return `Searching${".".repeat(dots)}`;
    return "Find Opponent";
  }, [searchStarted, hasActiveMatch, state.isInMatchmaking, dots]);

  const primaryEnabled =
    !searchStarted &&
    !state.isInMatchmaking &&
    (state.connectionState === "connected" || state.connectionState === "connecting");
  const showRetry = state.connectionState === "failed" || state.connectionState === "disconnected";

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Search for opponents..." />

      <div className="text-dim">Guest Alias</div>
      <div className="card" style={{ borderColor: "rgba(0,245,255,.55)", padding: "12px 14px" }}>
        <div className="headline" style={{ fontSize: "clamp(10px, 1.2vw, 12px)" }}>
          {state.displayName ?? "Assigned automatically"}
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
        <ArcadeButton
          text="Casual"
          onClick={() => setSelectedMode("casual")}
          disabled={searchStarted || state.isInMatchmaking || hasActiveMatch || selectedMode === "casual"}
          accent="gold"
        />
        <ArcadeButton
          text="Ranked"
          onClick={() => setSelectedMode("ranked")}
          disabled={searchStarted || state.isInMatchmaking || hasActiveMatch || selectedMode === "ranked"}
          accent="magenta"
        />
      </div>

      <ArcadeButton
        text={buttonText}
        onClick={() => {
          setSearchStarted(true);
          onJoinQueue(selectedMode);
        }}
        disabled={!primaryEnabled}
      />

      {searchStarted && state.isInMatchmaking && !hasActiveMatch ? (
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
