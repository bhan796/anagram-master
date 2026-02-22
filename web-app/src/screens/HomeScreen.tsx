import { useCallback, useEffect, useState } from "react";
import { ArcadeButton, ArcadeScaffold } from "../components/ArcadeComponents";
import { LogoParticleAnimation } from "../components/LogoParticleAnimation";
import * as SoundManager from "../sound/SoundManager";

interface HomeScreenProps {
  onPlayOnline: () => void;
  onPracticeMode: () => void;
  onProfile: () => void;
  onSettings: () => void;
  onHowToPlay: () => void;
  playersOnline: number;
}

export const HomeScreen = ({ onPlayOnline, onPracticeMode, onProfile, onSettings, onHowToPlay, playersOnline }: HomeScreenProps) => {
  const [logoComplete, setLogoComplete] = useState(false);
  const handleLogoComplete = useCallback(() => setLogoComplete(true), []);

  useEffect(() => {
    if (logoComplete) return;
    const fallback = window.setTimeout(() => setLogoComplete(true), 3200);
    return () => window.clearTimeout(fallback);
  }, [logoComplete]);

  const animatedButtonStyle = (delayMs: number) => ({
    opacity: logoComplete ? 1 : 0,
    transform: logoComplete ? "translateY(0)" : "translateY(12px)",
    transition: `opacity 400ms ease ${delayMs}ms, transform 400ms ease ${delayMs}ms`
  });

  return (
    <ArcadeScaffold>
      <div style={{ flex: 1 }} />
      <LogoParticleAnimation onComplete={handleLogoComplete} />
      <div style={{ height: 28 }} />

      <div style={animatedButtonStyle(0)}>
        <ArcadeButton
          text="Play Online"
          onClick={() => {
            void SoundManager.playClick();
            onPlayOnline();
          }}
        />
      </div>
      <div style={animatedButtonStyle(100)}>
        <ArcadeButton
          text="Practice Mode"
          onClick={() => {
            void SoundManager.playClick();
            onPracticeMode();
          }}
        />
      </div>
      <div style={animatedButtonStyle(200)}>
        <ArcadeButton
          text="How To Play"
          onClick={() => {
            void SoundManager.playClick();
            onHowToPlay();
          }}
        />
      </div>
      <div style={animatedButtonStyle(300)}>
        <ArcadeButton
          text="Profile / Stats"
          onClick={() => {
            void SoundManager.playClick();
            onProfile();
          }}
          accent="gold"
        />
      </div>
      <div style={animatedButtonStyle(400)}>
        <ArcadeButton
          text="Settings"
          onClick={() => {
            void SoundManager.playClick();
            onSettings();
          }}
          accent="magenta"
        />
      </div>

      <div
        style={{
          color: "var(--white)",
          textAlign: "center",
          marginTop: 4,
          fontFamily: "var(--font-pixel)",
          fontSize: "var(--text-label)",
          letterSpacing: "0.08em",
          textTransform: "uppercase"
        }}
      >
        Players Online: <span style={{ color: "var(--green)" }}>{playersOnline}</span>
      </div>
      <div style={{ flex: 1 }} />
    </ArcadeScaffold>
  );
};
