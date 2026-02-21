import { useEffect, useMemo, useState } from "react";
import type { OnlineUiState } from "../types/online";
import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";

interface MatchmakingScreenProps {
  state: OnlineUiState;
  onBack: () => void;
  onJoinQueue: (displayName?: string) => void;
  onCancelQueue: () => void;
  onRetryConnection: () => void;
  onMatchReady: () => void;
}

export const MatchmakingScreen = ({ state, onBack, onJoinQueue, onCancelQueue, onRetryConnection, onMatchReady }: MatchmakingScreenProps) => {
  const [displayName, setDisplayName] = useState(state.displayName ?? "");
  const [searchStarted, setSearchStarted] = useState(false);
  const [dots, setDots] = useState(1);
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

  return (
    <ArcadeScaffold>
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Search for opponents..." />

      <label className="text-dim" htmlFor="display-name">
        Display Name (optional)
      </label>
      <input
        id="display-name"
        className="input"
        value={displayName}
        onChange={(event) => setDisplayName(event.target.value)}
        placeholder="Guest name"
      />

      <ArcadeButton
        text={buttonText}
        onClick={() => {
          setSearchStarted(true);
          onJoinQueue(displayName || undefined);
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

      {state.connectionState === "failed" ? <ArcadeButton text="Retry Connection" onClick={onRetryConnection} /> : null}

      {state.localValidationMessage ? <div className="text-dim" style={{ color: "var(--red)" }}>{state.localValidationMessage}</div> : null}
    </ArcadeScaffold>
  );
};
