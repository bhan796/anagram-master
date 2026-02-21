import { useEffect } from "react";
import type { OnlineUiState } from "../types/online";
import { ArcadeScaffold, NeonTitle, RankBadge } from "../components/ArcadeComponents";

interface MatchFoundScreenProps {
  state: OnlineUiState;
  onDone: () => void;
}

export const MatchFoundScreen = ({ state, onDone }: MatchFoundScreenProps) => {
  useEffect(() => {
    const timer = window.setTimeout(onDone, 10_000);
    return () => window.clearTimeout(timer);
  }, [onDone]);

  const me = state.myPlayer;
  const opponent = state.opponentPlayer;

  return (
    <ArcadeScaffold>
      <div className="card" style={{ display: "grid", gap: 14, textAlign: "center", animation: "pulse 900ms ease-in-out infinite alternate" }}>
        <NeonTitle text="Match Found" />
        <div className="headline" style={{ color: "var(--gold)" }}>
          {me?.displayName ?? "You"} vs {opponent?.displayName ?? "Opponent"}
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
          <div className="card" style={{ borderColor: "rgba(0,245,255,.55)", animation: "slideIn 500ms ease" }}>
            <div className="text-dim">You</div>
            <div className="headline" style={{ color: "var(--cyan)" }}>{me?.rating ?? 1000}</div>
            <RankBadge tier={me?.rankTier} />
          </div>
          <div className="card" style={{ borderColor: "rgba(255,215,0,.55)", animation: "slideIn 650ms ease" }}>
            <div className="text-dim">Opponent</div>
            <div className="headline" style={{ color: "var(--gold)" }}>{opponent?.rating ?? 1000}</div>
            <RankBadge tier={opponent?.rankTier} />
          </div>
        </div>
        <div className="text-dim">Entering arena in 10 seconds...</div>
      </div>
    </ArcadeScaffold>
  );
};
