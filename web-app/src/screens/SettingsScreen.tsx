import { ArcadeBackButton, ArcadeScaffold, NeonDivider, NeonTitle } from "../components/ArcadeComponents";

interface SettingsScreenProps {
  timerEnabled: boolean;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  onBack: () => void;
  onTimerToggle: (value: boolean) => void;
  onSoundToggle: (value: boolean) => void;
  onVibrationToggle: (value: boolean) => void;
}

export const SettingsScreen = ({
  timerEnabled,
  soundEnabled,
  vibrationEnabled,
  onBack,
  onTimerToggle,
  onSoundToggle,
  onVibrationToggle
}: SettingsScreenProps) => {
  const rows = [
    { label: "Practice Timer", value: timerEnabled, onToggle: onTimerToggle },
    { label: "Sound", value: soundEnabled, onToggle: onSoundToggle },
    { label: "Vibration", value: vibrationEnabled, onToggle: onVibrationToggle }
  ];

  return (
    <ArcadeScaffold className="accent-magenta">
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Settings" />

      <div className="card" style={{ display: "grid", gap: 10 }}>
        {rows.map((row, index) => (
          <div key={row.label} style={{ display: "grid", gap: 10 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
              <span className="label">{row.label}</span>
              <button
                type="button"
                className="arcade-button"
                style={{ width: 140, borderColor: "var(--magenta)", color: "var(--magenta)" }}
                onClick={() => row.onToggle(!row.value)}
              >
                {row.value ? "ON" : "OFF"}
              </button>
            </div>
            {index < rows.length - 1 ? <NeonDivider /> : null}
          </div>
        ))}
      </div>
    </ArcadeScaffold>
  );
};
