import { useEffect, useRef, useState } from "react";
import type { OnlineUiState } from "../types/online";
import { RankBadge } from "../components/ArcadeComponents";
import * as SoundManager from "../sound/SoundManager";

interface MatchFoundScreenProps {
  state: OnlineUiState;
  onDone: () => void;
}

// -- Canvas particle system ----------------------------------------------------
type Particle = {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  color: string;
  alpha: number;
  decay: number;
};

const COLORS = ["#00f5ff", "#ffd700", "#ff00cc", "#39ff14"];

function spawnParticles(canvas: HTMLCanvasElement): Particle[] {
  const cx = canvas.width / 2;
  const cy = canvas.height / 2;
  return Array.from({ length: 40 }, () => {
    const angle = Math.random() * Math.PI * 2;
    const speed = 0.8 + Math.random() * 2.4;
    return {
      x: cx,
      y: cy,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      radius: 2 + Math.random() * 4,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
      alpha: 1,
      decay: 0.012 + Math.random() * 0.012
    };
  });
}

const ArcadeParticles = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = canvas.offsetWidth;
    canvas.height = canvas.offsetHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let particles = spawnParticles(canvas);
    let raf = 0;

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles = particles.filter((p) => p.alpha > 0.02);
      for (const p of particles) {
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.04;
        p.alpha -= p.decay;
        ctx.globalAlpha = Math.max(0, p.alpha);
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fill();
      }
      if (particles.length > 0) {
        raf = requestAnimationFrame(draw);
      }
    };

    raf = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(raf);
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: "fixed",
        inset: 0,
        width: "100%",
        height: "100%",
        pointerEvents: "none",
        zIndex: 0
      }}
    />
  );
};

// -- Animated rating counter ---------------------------------------------------
const AnimatedRating = ({ target, color }: { target: number; color: string }) => {
  const [displayed, setDisplayed] = useState(0);

  useEffect(() => {
    let current = 0;
    const step = Math.ceil(target / 40);
    const interval = window.setInterval(() => {
      current = Math.min(current + step, target);
      setDisplayed(current);
      if (current >= target) {
        window.clearInterval(interval);
      }
    }, 16);

    return () => window.clearInterval(interval);
  }, [target]);

  return (
    <div className="mf-rating" style={{ color }}>
      {displayed}
    </div>
  );
};

// -- Main screen ---------------------------------------------------------------
export const MatchFoundScreen = ({ state, onDone }: MatchFoundScreenProps) => {
  const onDoneRef = useRef(onDone);
  const didCompleteRef = useRef(false);
  const [secondsLeft, setSecondsLeft] = useState(10);
  const [countdownKey, setCountdownKey] = useState(10);
  const [showFight, setShowFight] = useState(false);

  useEffect(() => {
    onDoneRef.current = onDone;
  }, [onDone]);

  useEffect(() => {
    void SoundManager.playMatchFound();
  }, []);

  useEffect(() => {
    if (secondsLeft > 0) {
      void SoundManager.playCountdownBeep();
      return;
    }

    if (didCompleteRef.current) return;
    didCompleteRef.current = true;
    void SoundManager.playCountdownGo();
    window.setTimeout(() => onDoneRef.current(), 700);
  }, [secondsLeft]);

  useEffect(() => {
    const endAt = Date.now() + 10_000;
    const timer = window.setInterval(() => {
      const remainingMs = Math.max(0, endAt - Date.now());
      const remainingSecs = Math.ceil(remainingMs / 1000);
      setSecondsLeft((prev) => {
        if (remainingSecs !== prev) {
          setCountdownKey(remainingSecs);
        }
        return remainingSecs;
      });
      if (remainingMs <= 0) {
        window.clearInterval(timer);
        setShowFight(true);
      }
    }, 200);
    return () => window.clearInterval(timer);
  }, []);

  const me = state.myPlayer;
  const opp = state.opponentPlayer;

  return (
    <div className="mf-root">
      <div className="mf-flash" />
      <ArcadeParticles />

      <div
        style={{
          position: "relative",
          zIndex: 1,
          display: "grid",
          gap: 16,
          width: "100%",
          alignItems: "center",
          justifyItems: "center"
        }}
      >
        <div>
          <div className="mf-title-line mf-title-match">MATCH</div>
          <div className="mf-title-line mf-title-found">FOUND!</div>
        </div>

        <div className="mf-vs-row">
          <div className="mf-line" />
          <div className="mf-vs-text">VS</div>
          <div className="mf-line right" />
        </div>

        <div className="mf-players-row">
          <div className="mf-player-card you">
            <div className="text-dim">YOU</div>
            <div
              className="label"
              style={{
                color: "var(--white)",
                fontSize: "clamp(8px,1vw,10px)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap"
              }}
            >
              {me?.displayName ?? "You"}
            </div>
            <AnimatedRating target={me?.rating ?? 1000} color="var(--cyan)" />
            <RankBadge tier={me?.rankTier} />
          </div>

          <div className="mf-player-card opp">
            <div className="text-dim">OPP</div>
            <div
              className="label"
              style={{
                color: "var(--white)",
                fontSize: "clamp(8px,1vw,10px)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap"
              }}
            >
              {opp?.displayName ?? "Opponent"}
            </div>
            <AnimatedRating target={opp?.rating ?? 1000} color="var(--gold)" />
            <RankBadge tier={opp?.rankTier} />
          </div>
        </div>

        <div className="mf-countdown-wrap">
          {showFight ? (
            <div className="mf-fight">FIGHT!</div>
          ) : (
            <>
              <div className="text-dim" style={{ fontSize: "var(--text-label)" }}>
                ENTERING ARENA IN
              </div>
              <div
                className="mf-countdown-num"
                key={countdownKey}
                style={{ color: secondsLeft <= 3 ? "var(--red)" : "var(--cyan)" }}
              >
                {secondsLeft}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
