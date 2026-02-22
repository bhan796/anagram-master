import { ArcadeBackButton, ArcadeScaffold, NeonTitle } from "../components/ArcadeComponents";
import { HowToPlayTabs } from "../components/HowToPlayTabs";

interface HowToPlayScreenProps {
  onBack: () => void;
}

export const HowToPlayScreen = ({ onBack }: HowToPlayScreenProps) => (
  <ArcadeScaffold>
    <ArcadeBackButton onClick={onBack} />
    <NeonTitle text="HOW TO PLAY" />
    <HowToPlayTabs />
  </ArcadeScaffold>
);
