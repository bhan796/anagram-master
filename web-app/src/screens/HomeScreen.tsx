import { ArcadeButton, ArcadeScaffold, TileLogo } from "../components/ArcadeComponents";
import * as SoundManager from "../sound/SoundManager";

interface HomeScreenProps {
  onPlayOnline: () => void;
  onPracticeMode: () => void;
  onProfile: () => void;
  onSettings: () => void;
  onHowToPlay: () => void;
  playersOnline: number;
}

export const HomeScreen = ({ onPlayOnline, onPracticeMode, onProfile, onSettings, onHowToPlay, playersOnline }: HomeScreenProps) => (
  <ArcadeScaffold>
    <div style={{ flex: 1 }} />
    <TileLogo />
    <div style={{ height: 28 }} />
    <ArcadeButton
      text="Play Online"
      onClick={() => {
        void SoundManager.playClick();
        onPlayOnline();
      }}
    />
    <ArcadeButton
      text="Practice Mode"
      onClick={() => {
        void SoundManager.playClick();
        onPracticeMode();
      }}
    />
    <ArcadeButton
      text="How To Play"
      onClick={() => {
        void SoundManager.playClick();
        onHowToPlay();
      }}
    />
    <ArcadeButton
      text="Profile / Stats"
      onClick={() => {
        void SoundManager.playClick();
        onProfile();
      }}
      accent="gold"
    />
    <ArcadeButton
      text="Settings"
      onClick={() => {
        void SoundManager.playClick();
        onSettings();
      }}
      accent="magenta"
    />
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
