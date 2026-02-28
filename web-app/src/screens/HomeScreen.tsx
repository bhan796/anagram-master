import { useCallback, useEffect, useState } from "react";
import { ArcadeButton, ArcadeScaffold, RuneIcon, TileLogo } from "../components/ArcadeComponents";
import { LogoParticleAnimation } from "../components/LogoParticleAnimation";
import * as SoundManager from "../sound/SoundManager";

interface HomeScreenProps {
  onPlayOnline: () => void;
  onPracticeMode: () => void;
  onProfile: () => void;
  onSettings: () => void;
  onHowToPlay: () => void;
  onShop: () => void;
  onAchievements: () => void;
  isAuthenticated: boolean;
  authEmail: string | null;
  runes: number;
  onAuthAction: () => void;
  playIntro: boolean;
  onIntroComplete: () => void;
}

export const HomeScreen = ({
  onPlayOnline,
  onPracticeMode,
  onProfile,
  onSettings,
  onHowToPlay,
  onShop,
  onAchievements,
  isAuthenticated,
  authEmail,
  runes,
  onAuthAction,
  playIntro,
  onIntroComplete
}: HomeScreenProps) => {
  const [logoComplete, setLogoComplete] = useState(!playIntro);
  const handleLogoComplete = useCallback(() => {
    setLogoComplete(true);
    onIntroComplete();
  }, [onIntroComplete]);

  useEffect(() => {
    if (!playIntro) {
      setLogoComplete(true);
      return;
    }
    if (logoComplete) return;
    const fallback = window.setTimeout(() => {
      setLogoComplete(true);
      onIntroComplete();
    }, 3200);
    return () => window.clearTimeout(fallback);
  }, [logoComplete, onIntroComplete, playIntro]);

  const animatedButtonStyle = (delayMs: number) => ({
    opacity: logoComplete ? 1 : 0,
    transform: logoComplete ? "translateY(0)" : "translateY(12px)",
    transition: `opacity 400ms ease ${delayMs}ms, transform 400ms ease ${delayMs}ms`
  });

  return (
    <ArcadeScaffold>
      <div style={{ flex: 1 }} />
      {playIntro ? (
        <LogoParticleAnimation onComplete={handleLogoComplete} />
      ) : (
        <div style={{ width: "100%", minHeight: 120, display: "grid", alignContent: "center", justifyItems: "center" }}>
          <TileLogo />
        </div>
      )}
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
      {isAuthenticated ? (
        <>
          <div style={animatedButtonStyle(350)}>
            <ArcadeButton
              text="Achievements"
              onClick={() => {
                void SoundManager.playClick();
                onAchievements();
              }}
              accent="gold"
            />
          </div>
          <div style={animatedButtonStyle(400)}>
            <ArcadeButton
              text="Shop"
              onClick={() => {
                void SoundManager.playClick();
                onShop();
              }}
            />
          </div>
        </>
      ) : null}
      <div style={animatedButtonStyle(550)}>
        <ArcadeButton
          text="Settings"
          onClick={() => {
            void SoundManager.playClick();
            onSettings();
          }}
          accent="magenta"
        />
      </div>
      <div style={animatedButtonStyle(600)}>
        <ArcadeButton
          text={isAuthenticated ? "Log Out" : "Sign In"}
          onClick={() => {
            void SoundManager.playClick();
            onAuthAction();
          }}
          accent={isAuthenticated ? "red" : "green"}
        />
      </div>

      <div
        style={{
          color: "var(--white)",
          textAlign: "center",
          marginTop: 2,
          fontFamily: "var(--font-pixel)",
          fontSize: "var(--text-label)",
          letterSpacing: "0.08em",
          textTransform: "uppercase"
        }}
      >
        Account: <span style={{ color: isAuthenticated ? "var(--cyan)" : "var(--gold)" }}>{isAuthenticated ? authEmail ?? "Signed In" : "Guest"}</span>
      </div>
      {isAuthenticated ? (
        <div
          style={{
            color: "var(--gold)",
            textAlign: "center",
            marginTop: 2,
            fontFamily: "var(--font-pixel)",
            fontSize: "var(--text-label)",
            letterSpacing: "0.08em"
          }}
        >
          <RuneIcon /> {runes.toLocaleString()} RUNES
        </div>
      ) : null}
      <div style={{ flex: 1 }} />
    </ArcadeScaffold>
  );
};

