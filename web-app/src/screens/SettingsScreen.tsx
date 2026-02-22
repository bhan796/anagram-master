import { ArcadeBackButton, ArcadeScaffold, NeonDivider, NeonTitle } from "../components/ArcadeComponents";

interface SettingsScreenProps {
  timerEnabled: boolean;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  masterMuted: boolean;
  musicEnabled: boolean;
  uiSfxEnabled: boolean;
  gameSfxEnabled: boolean;
  musicVolume: number;
  uiSfxVolume: number;
  gameSfxVolume: number;
  onBack: () => void;
  onTimerToggle: (value: boolean) => void;
  onSoundToggle: (value: boolean) => void;
  onVibrationToggle: (value: boolean) => void;
  onMasterMuteToggle: (value: boolean) => void;
  onMusicToggle: (value: boolean) => void;
  onUiSfxToggle: (value: boolean) => void;
  onGameSfxToggle: (value: boolean) => void;
  onMusicVolumeChange: (value: number) => void;
  onUiSfxVolumeChange: (value: number) => void;
  onGameSfxVolumeChange: (value: number) => void;
}

const pct = (value: number) => `${Math.round(value * 100)}%`;

export const SettingsScreen = ({
  timerEnabled,
  soundEnabled,
  vibrationEnabled,
  masterMuted,
  musicEnabled,
  uiSfxEnabled,
  gameSfxEnabled,
  musicVolume,
  uiSfxVolume,
  gameSfxVolume,
  onBack,
  onTimerToggle,
  onSoundToggle,
  onVibrationToggle,
  onMasterMuteToggle,
  onMusicToggle,
  onUiSfxToggle,
  onGameSfxToggle,
  onMusicVolumeChange,
  onUiSfxVolumeChange,
  onGameSfxVolumeChange
}: SettingsScreenProps) => {
  const baseRows = [
    { label: "Practice Timer", value: timerEnabled, onToggle: onTimerToggle },
    { label: "Master Mute", value: !masterMuted, onToggle: (value: boolean) => onMasterMuteToggle(!value) },
    { label: "SFX Master", value: soundEnabled, onToggle: onSoundToggle },
    { label: "Vibration", value: vibrationEnabled, onToggle: onVibrationToggle }
  ];

  return (
    <ArcadeScaffold className="accent-magenta">
      <ArcadeBackButton onClick={onBack} />
      <NeonTitle text="Settings" />

      <div className="card" style={{ display: "grid", gap: 10 }}>
        {baseRows.map((row, index) => (
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
            {index < baseRows.length - 1 ? <NeonDivider /> : null}
          </div>
        ))}
      </div>

      <div className="card" style={{ display: "grid", gap: 14 }}>
        <div className="headline" style={{ color: "var(--cyan)" }}>
          Sound Mix
        </div>

        <div className="sound-setting-block">
          <div className="sound-setting-header">
            <span className="label">Music</span>
            <button type="button" className="arcade-button" style={{ width: 92 }} onClick={() => onMusicToggle(!musicEnabled)}>
              {musicEnabled ? "ON" : "OFF"}
            </button>
          </div>
          <div className="sound-slider-row">
            <input
              className="sound-slider"
              type="range"
              min={0}
              max={100}
              value={Math.round(musicVolume * 100)}
              onChange={(event) => onMusicVolumeChange(Number(event.currentTarget.value) / 100)}
            />
            <span className="label sound-pct">{pct(musicVolume)}</span>
          </div>
        </div>

        <NeonDivider />

        <div className="sound-setting-block">
          <div className="sound-setting-header">
            <span className="label">UI SFX</span>
            <button type="button" className="arcade-button" style={{ width: 92 }} onClick={() => onUiSfxToggle(!uiSfxEnabled)}>
              {uiSfxEnabled ? "ON" : "OFF"}
            </button>
          </div>
          <div className="sound-slider-row">
            <input
              className="sound-slider"
              type="range"
              min={0}
              max={100}
              value={Math.round(uiSfxVolume * 100)}
              onChange={(event) => onUiSfxVolumeChange(Number(event.currentTarget.value) / 100)}
            />
            <span className="label sound-pct">{pct(uiSfxVolume)}</span>
          </div>
        </div>

        <NeonDivider />

        <div className="sound-setting-block">
          <div className="sound-setting-header">
            <span className="label">Gameplay SFX</span>
            <button type="button" className="arcade-button" style={{ width: 92 }} onClick={() => onGameSfxToggle(!gameSfxEnabled)}>
              {gameSfxEnabled ? "ON" : "OFF"}
            </button>
          </div>
          <div className="sound-slider-row">
            <input
              className="sound-slider"
              type="range"
              min={0}
              max={100}
              value={Math.round(gameSfxVolume * 100)}
              onChange={(event) => onGameSfxVolumeChange(Number(event.currentTarget.value) / 100)}
            />
            <span className="label sound-pct">{pct(gameSfxVolume)}</span>
          </div>
        </div>
      </div>
    </ArcadeScaffold>
  );
};
