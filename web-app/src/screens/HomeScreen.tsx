import { ArcadeButton, ArcadeScaffold, TileLogo } from "../components/ArcadeComponents";

interface HomeScreenProps {
  onPlayOnline: () => void;
  onPracticeMode: () => void;
  onProfile: () => void;
  onSettings: () => void;
}

export const HomeScreen = ({ onPlayOnline, onPracticeMode, onProfile, onSettings }: HomeScreenProps) => (
  <ArcadeScaffold>
    <div style={{ flex: 1 }} />
    <TileLogo />
    <div style={{ height: 28 }} />
    <ArcadeButton text="Play Online" onClick={onPlayOnline} />
    <ArcadeButton text="Practice Mode" onClick={onPracticeMode} />
    <ArcadeButton text="Profile / Stats" onClick={onProfile} accent="gold" />
    <ArcadeButton text="Settings" onClick={onSettings} accent="magenta" />
    <div style={{ flex: 1 }} />
  </ArcadeScaffold>
);
