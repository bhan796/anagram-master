import { ArcadeBackButton, ArcadeScaffold, NeonDivider, NeonTitle } from "../components/ArcadeComponents";

interface SettingsScreenProps {
  timerEnabled: boolean;
  masterMuted: boolean;
  sfxVolume: number;
  isAuthenticated: boolean;
  deletingAccount: boolean;
  onBack: () => void;
  onTimerToggle: (value: boolean) => void;
  onMasterMuteToggle: (value: boolean) => void;
  onSfxVolumeChange: (value: number) => void;
  onDeleteAccount: () => void;
}

export const SettingsScreen = ({
  timerEnabled,
  masterMuted,
  sfxVolume,
  isAuthenticated,
  deletingAccount,
  onBack,
  onTimerToggle,
  onMasterMuteToggle,
  onSfxVolumeChange,
  onDeleteAccount
}: SettingsScreenProps) => {
  const rows = [
    { label: "Practice Timer", value: timerEnabled, onToggle: onTimerToggle },
    { label: "Master Mute", value: !masterMuted, onToggle: (value: boolean) => onMasterMuteToggle(!value) }
  ];

  return (
    <ArcadeScaffold className="accent-magenta">
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Settings" />

      <div className="card" style={{ display: "grid", gap: 10 }}>
        {rows.map((row) => (
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
            <NeonDivider />
          </div>
        ))}

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span className="label">SFX Volume</span>
            <span className="label sound-pct">{Math.round(sfxVolume * 100)}%</span>
          </div>
          <input
            className="sound-slider"
            type="range"
            min={0}
            max={100}
            value={Math.round(sfxVolume * 100)}
            onChange={(event) => onSfxVolumeChange(Number(event.currentTarget.value) / 100)}
          />
        </div>

        {isAuthenticated ? (
          <>
            <NeonDivider />
            <button
              type="button"
              className="arcade-button"
              style={{ borderColor: "var(--red)", color: "var(--red)" }}
              onClick={onDeleteAccount}
              disabled={deletingAccount}
            >
              {deletingAccount ? "DELETING ACCOUNT..." : "DELETE ACCOUNT"}
            </button>
          </>
        ) : null}
      </div>
    </ArcadeScaffold>
  );
};
