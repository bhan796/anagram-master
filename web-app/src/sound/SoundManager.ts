import * as Tone from "tone";

// -- Internal state ------------------------------------------------------------
let _soundEnabled = true;
let _masterMuted = false;

let _musicEnabled = true;
let _uiSfxEnabled = true;
let _gameSfxEnabled = true;

let _musicVolume = 0.5;
let _uiSfxVolume = 0.8;
let _gameSfxVolume = 0.85;

let _musicLoop: Tone.Part | null = null;
let _musicSynth: Tone.PolySynth | null = null;
let _musicReverb: Tone.Reverb | null = null;
let _started = false;

function clamp01(value: number): number {
  if (Number.isNaN(value)) return 0;
  return Math.max(0, Math.min(1, value));
}

function volumeToDb(volume: number): number {
  if (volume <= 0) return -72;
  return Tone.gainToDb(volume);
}

function canPlayUiSfx() {
  return _soundEnabled && !_masterMuted && _uiSfxEnabled && _uiSfxVolume > 0;
}

function canPlayGameSfx() {
  return _soundEnabled && !_masterMuted && _gameSfxEnabled && _gameSfxVolume > 0;
}

function canPlayMusic() {
  return !_masterMuted && _musicEnabled && _musicVolume > 0;
}

async function ensureStarted() {
  if (!_started) {
    await Tone.start();
    _started = true;
  }
}

// -- Settings ------------------------------------------------------------------
export function setSoundEnabled(enabled: boolean) {
  _soundEnabled = enabled;
}

export function setMasterMuted(muted: boolean) {
  _masterMuted = muted;
  if (muted) {
    stopMusic();
  }
}

export function setMusicEnabled(enabled: boolean) {
  _musicEnabled = enabled;
  if (!enabled) {
    stopMusic();
  }
}

export function setUiSfxEnabled(enabled: boolean) {
  _uiSfxEnabled = enabled;
}

export function setGameSfxEnabled(enabled: boolean) {
  _gameSfxEnabled = enabled;
}

export function setMusicVolume(volume: number) {
  _musicVolume = clamp01(volume);
  if (_musicSynth) {
    _musicSynth.volume.value = -18 + volumeToDb(_musicVolume);
  }
}

export function setUiSfxVolume(volume: number) {
  _uiSfxVolume = clamp01(volume);
}

export function setGameSfxVolume(volume: number) {
  _gameSfxVolume = clamp01(volume);
}

// -- Short SFX (fire-and-forget) -----------------------------------------------

export async function playClick() {
  if (!canPlayUiSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.04, sustain: 0, release: 0.01 },
    volume: -6 + volumeToDb(_uiSfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("C6", "32n");
  window.setTimeout(() => synth.dispose(), 300);
}

export async function playTilePlace() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.07, sustain: 0, release: 0.01 },
    volume: -8 + volumeToDb(_gameSfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("G4", "16n");
  window.setTimeout(() => {
    synth.triggerAttackRelease("E4", "16n");
  }, 80);
  window.setTimeout(() => synth.dispose(), 500);
}

export async function playWordSubmit() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "sine" },
    envelope: { attack: 0.01, decay: 0.1, sustain: 0.1, release: 0.2 },
    volume: -10 + volumeToDb(_gameSfxVolume)
  });
  const now = Tone.now();
  synth.triggerAttackRelease("C5", "8n", now);
  synth.triggerAttackRelease("E5", "8n", now + 0.08);
  synth.triggerAttackRelease("G5", "8n", now + 0.16);
  window.setTimeout(() => synth.dispose(), 1000);
}

export async function playWordValid() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "sine" },
    envelope: { attack: 0.01, decay: 0.12, sustain: 0.1, release: 0.3 },
    volume: -8 + volumeToDb(_gameSfxVolume)
  });
  const now = Tone.now();
  ["C5", "E5", "G5", "C6"].forEach((note, index) => {
    synth.triggerAttackRelease(note, "8n", now + index * 0.1);
  });
  window.setTimeout(() => synth.dispose(), 1500);
}

export async function playWordInvalid() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.01, decay: 0.2, sustain: 0, release: 0.05 },
    volume: -10 + volumeToDb(_gameSfxVolume)
  }).toDestination();
  const now = Tone.now();
  synth.triggerAttackRelease("A3", "8n", now);
  synth.triggerAttackRelease("F3", "8n", now + 0.15);
  window.setTimeout(() => synth.dispose(), 800);
}

export async function playTimerTick() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "sine" },
    envelope: { attack: 0.001, decay: 0.04, sustain: 0, release: 0.01 },
    volume: -12 + volumeToDb(_gameSfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("C6", "32n");
  window.setTimeout(() => synth.dispose(), 300);
}

export async function playTimerUrgent() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "sine" },
    envelope: { attack: 0.001, decay: 0.06, sustain: 0, release: 0.01 },
    volume: -8 + volumeToDb(_gameSfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("E6", "16n");
  window.setTimeout(() => synth.dispose(), 300);
}

export async function playMatchFound() {
  if (!canPlayUiSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.01, decay: 0.15, sustain: 0.1, release: 0.4 },
    volume: -4 + volumeToDb(_uiSfxVolume)
  });
  const now = Tone.now();
  ["C5", "E5", "G5", "C6", "E6"].forEach((note, index) => {
    synth.triggerAttackRelease(note, "8n", now + index * 0.12);
  });
  window.setTimeout(() => synth.dispose(), 2000);
}

export async function playLogoAssemble(): Promise<void> {
  if (!canPlayUiSfx()) return;
  await ensureStarted();

  const now = Tone.now();

  const noise = new Tone.Noise("pink").start(now).stop(now + 1.3);
  const filter = new Tone.Filter({ type: "bandpass", frequency: 120, Q: 0.8 });
  const gain = new Tone.Gain(0);
  noise.connect(filter);
  filter.connect(gain);
  gain.toDestination();

  gain.gain.setValueAtTime(0, now);
  gain.gain.linearRampToValueAtTime(0.18 * _uiSfxVolume, now + 0.08);
  gain.gain.linearRampToValueAtTime(0.22 * _uiSfxVolume, now + 1.0);
  gain.gain.linearRampToValueAtTime(0, now + 1.35);

  filter.frequency.setValueAtTime(120, now);
  filter.frequency.exponentialRampToValueAtTime(4800, now + 1.2);

  const snapSynth = new Tone.MetalSynth({
    envelope: { attack: 0.001, decay: 0.3, release: 0.1 },
    harmonicity: 5.1,
    modulationIndex: 32,
    resonance: 4000,
    octaves: 1.5
  }).toDestination();
  snapSynth.volume.value = -8 + volumeToDb(_uiSfxVolume);
  snapSynth.triggerAttackRelease("400", "8n", now + 1.4);

  window.setTimeout(() => {
    noise.dispose();
    filter.dispose();
    gain.dispose();
    snapSynth.dispose();
  }, 2500);
}

export async function playCountdownBeep() {
  if (!canPlayUiSfx()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "sine" },
    envelope: { attack: 0.001, decay: 0.12, sustain: 0, release: 0.05 },
    volume: -6 + volumeToDb(_uiSfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("A5", "16n");
  window.setTimeout(() => synth.dispose(), 500);
}

export async function playCountdownGo() {
  if (!canPlayUiSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.01, decay: 0.2, sustain: 0.1, release: 0.5 },
    volume: -3 + volumeToDb(_uiSfxVolume)
  });
  const now = Tone.now();
  ["A5", "D6", "G6"].forEach((note, index) => {
    synth.triggerAttackRelease(note, "8n", now + index * 0.1);
  });
  window.setTimeout(() => synth.dispose(), 2000);
}

export async function playRoundResult() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "sine" },
    envelope: { attack: 0.01, decay: 0.15, sustain: 0.05, release: 0.3 },
    volume: -10 + volumeToDb(_gameSfxVolume)
  });
  const now = Tone.now();
  synth.triggerAttackRelease("G5", "8n", now);
  synth.triggerAttackRelease("C6", "8n", now + 0.12);
  window.setTimeout(() => synth.dispose(), 1000);
}

export async function playWin() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.01, decay: 0.18, sustain: 0.1, release: 0.5 },
    volume: -4 + volumeToDb(_gameSfxVolume)
  });
  const now = Tone.now();
  ["C5", "E5", "G5", "C6", "E6", "G6"].forEach((note, index) => {
    synth.triggerAttackRelease(note, "8n", now + index * 0.1);
  });
  window.setTimeout(() => synth.dispose(), 2000);
}

export async function playLose() {
  if (!canPlayGameSfx()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "sawtooth" },
    envelope: { attack: 0.02, decay: 0.2, sustain: 0.05, release: 0.4 },
    volume: -6 + volumeToDb(_gameSfxVolume)
  });
  const now = Tone.now();
  ["G4", "F4", "Eb4", "C4"].forEach((note, index) => {
    synth.triggerAttackRelease(note, "8n", now + index * 0.13);
  });
  window.setTimeout(() => synth.dispose(), 2000);
}

// -- Background music (procedural chiptune loop) ------------------------------

export async function startMenuMusic() {
  if (!canPlayMusic()) return;
  await ensureStarted();
  stopMusic();

  Tone.getTransport().bpm.value = 120;
  _musicSynth = new Tone.PolySynth(Tone.Synth).toDestination();
  _musicSynth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.02, decay: 0.1, sustain: 0.4, release: 0.8 },
    volume: -18 + volumeToDb(_musicVolume)
  });

  _musicReverb = new Tone.Reverb({ decay: 2, wet: 0.25 }).toDestination();
  _musicSynth.connect(_musicReverb);

  const pattern = [
    { time: "0:0", note: "C4", dur: "8n" },
    { time: "0:0:2", note: "E4", dur: "8n" },
    { time: "0:1", note: "G4", dur: "8n" },
    { time: "0:1:2", note: "C5", dur: "8n" },
    { time: "0:2", note: "E5", dur: "8n" },
    { time: "0:2:2", note: "G4", dur: "8n" },
    { time: "0:3", note: "E4", dur: "8n" },
    { time: "0:3:2", note: "C4", dur: "8n" },
    { time: "1:0", note: "D4", dur: "8n" },
    { time: "1:0:2", note: "F4", dur: "8n" },
    { time: "1:1", note: "A4", dur: "8n" },
    { time: "1:1:2", note: "D5", dur: "8n" },
    { time: "1:2", note: "F5", dur: "8n" },
    { time: "1:2:2", note: "A4", dur: "8n" },
    { time: "1:3", note: "F4", dur: "8n" },
    { time: "1:3:2", note: "D4", dur: "8n" }
  ];

  const synth = _musicSynth;
  if (!synth) return;

  _musicLoop = new Tone.Part((time, value: { note: string; dur: string }) => {
    synth.triggerAttackRelease(value.note, value.dur, time);
  }, pattern);
  _musicLoop.loop = true;
  _musicLoop.loopEnd = "2m";
  _musicLoop.start(0);
  Tone.getTransport().start();
}

export async function startMatchMusic() {
  if (!canPlayMusic()) return;
  await ensureStarted();
  stopMusic();

  Tone.getTransport().bpm.value = 140;
  _musicSynth = new Tone.PolySynth(Tone.Synth).toDestination();
  _musicSynth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.01, decay: 0.08, sustain: 0.3, release: 0.5 },
    volume: -20 + volumeToDb(_musicVolume)
  });

  const pattern = [
    { time: "0:0", note: "A3", dur: "8n" },
    { time: "0:0:2", note: "C4", dur: "8n" },
    { time: "0:1", note: "E4", dur: "8n" },
    { time: "0:1:2", note: "A4", dur: "8n" },
    { time: "0:2", note: "G4", dur: "8n" },
    { time: "0:2:2", note: "E4", dur: "8n" },
    { time: "0:3", note: "C4", dur: "8n" },
    { time: "0:3:2", note: "A3", dur: "8n" },
    { time: "1:0", note: "F3", dur: "8n" },
    { time: "1:0:2", note: "A3", dur: "8n" },
    { time: "1:1", note: "C4", dur: "8n" },
    { time: "1:1:2", note: "F4", dur: "8n" },
    { time: "1:2", note: "E4", dur: "8n" },
    { time: "1:2:2", note: "C4", dur: "8n" },
    { time: "1:3", note: "A3", dur: "8n" },
    { time: "1:3:2", note: "F3", dur: "8n" }
  ];

  const synth = _musicSynth;
  if (!synth) return;

  _musicLoop = new Tone.Part((time, value: { note: string; dur: string }) => {
    synth.triggerAttackRelease(value.note, value.dur, time);
  }, pattern);
  _musicLoop.loop = true;
  _musicLoop.loopEnd = "2m";
  _musicLoop.start(0);
  Tone.getTransport().start();
}

export function stopMusic() {
  _musicLoop?.stop();
  _musicLoop?.dispose();
  _musicLoop = null;

  _musicSynth?.dispose();
  _musicSynth = null;

  _musicReverb?.dispose();
  _musicReverb = null;

  Tone.getTransport().stop();
}
