import { ArcadeBackButton, ArcadeButton, ArcadeScaffold, NeonDivider, NeonTitle } from "../components/ArcadeComponents";

interface PracticeMenuScreenProps {
  timerEnabled: boolean;
  onTimerToggle: (enabled: boolean) => void;
  onBack: () => void;
  onPracticeLetters: () => void;
  onPracticeConundrum: () => void;
}

export const PracticeMenuScreen = ({
  timerEnabled,
  onTimerToggle,
  onBack,
  onPracticeLetters,
  onPracticeConundrum
}: PracticeMenuScreenProps) => (
  <ArcadeScaffold>
    <ArcadeBackButton onClick={onBack} />
    <NeonTitle text="Practice" />

    <ArcadeButton text="Practice Letters Round" onClick={onPracticeLetters} />
    <ArcadeButton text="Practice Conundrum" onClick={onPracticeConundrum} />

    <div className="card" style={{ display: "grid", gap: 12 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
        <span className="label">Round Timer</span>
        <button type="button" className="arcade-button" style={{ width: 160 }} onClick={() => onTimerToggle(!timerEnabled)}>
          {timerEnabled ? "ON" : "OFF"}
        </button>
      </div>
      <NeonDivider />
      <div className="text-dim">Disable timer for manual testing and UI previews.</div>
    </div>
  </ArcadeScaffold>
);
