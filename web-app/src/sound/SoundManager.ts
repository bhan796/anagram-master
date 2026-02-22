import * as Tone from "tone";

let _soundEnabled = true;
let _masterMuted = false;
let _sfxVolume = 0.85;
let _started = false;

function volumeToDb(volume: number): number {
  if (volume <= 0) return -72;
  return Tone.gainToDb(volume);
}

function canPlay() {
  return _soundEnabled && !_masterMuted && _sfxVolume > 0;
}

async function ensureStarted() {
  if (!_started) {
    await Tone.start();
    _started = true;
  }
}

export function setSoundEnabled(enabled: boolean) {
  _soundEnabled = enabled;
}

export function setMasterMuted(muted: boolean) {
  _masterMuted = muted;
}

export function setSfxVolume(volume: number) {
  if (Number.isNaN(volume)) {
    _sfxVolume = 0;
    return;
  }
  _sfxVolume = Math.max(0, Math.min(1, volume));
}

export async function playClick() {
  if (!canPlay()) return;
  await ensureStarted();
  const now = Tone.now();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.035, sustain: 0, release: 0.005 },
    volume: -4 + volumeToDb(_sfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("C7", "64n", now);
  window.setTimeout(() => synth.dispose(), 200);
}

export async function playTilePlace() {
  if (!canPlay()) return;
  await ensureStarted();
  const notes = ["G5", "A5", "B5", "G5", "F5"] as const;
  const note = notes[Math.floor(Math.random() * notes.length)];
  const now = Tone.now();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.055, sustain: 0.02, release: 0.02 },
    volume: -7 + volumeToDb(_sfxVolume)
  }).toDestination();
  synth.triggerAttackRelease(note, "32n", now);

  const thunk = new Tone.Synth({
    oscillator: { type: "sine" },
    envelope: { attack: 0.001, decay: 0.08, sustain: 0, release: 0.01 },
    volume: -12 + volumeToDb(_sfxVolume)
  }).toDestination();
  thunk.triggerAttackRelease("D3", "32n", now + 0.01);

  window.setTimeout(() => {
    synth.dispose();
    thunk.dispose();
  }, 400);
}

export async function playWordSubmit() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "triangle" },
    envelope: { attack: 0.005, decay: 0.08, sustain: 0, release: 0.05 },
    volume: -7 + volumeToDb(_sfxVolume)
  });
  const now = Tone.now();
  const run = ["C5", "E5", "G5", "B5", "D6", "G6"];
  run.forEach((note, i) => synth.triggerAttackRelease(note, "32n", now + i * 0.045));
  window.setTimeout(() => synth.dispose(), 1000);
}

export async function playWordValid() {
  if (!canPlay()) return;
  await ensureStarted();
  const now = Tone.now();

  const run = new Tone.PolySynth(Tone.Synth).toDestination();
  run.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.005, decay: 0.1, sustain: 0.1, release: 0.15 },
    volume: -5 + volumeToDb(_sfxVolume)
  });
  ["C5", "E5", "G5", "C6", "E6"].forEach((note, i) => run.triggerAttackRelease(note, "16n", now + i * 0.07));

  const chord = new Tone.PolySynth(Tone.Synth).toDestination();
  chord.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.005, decay: 0.08, sustain: 0.15, release: 0.4 },
    volume: -4 + volumeToDb(_sfxVolume)
  });
  chord.triggerAttackRelease(["C6", "E6", "G6"], "8n", now + 0.38);

  window.setTimeout(() => {
    run.dispose();
    chord.dispose();
  }, 1500);
}

export async function playWordInvalid() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "sawtooth" },
    envelope: { attack: 0.01, decay: 0.18, sustain: 0.05, release: 0.12 },
    volume: -8 + volumeToDb(_sfxVolume)
  }).toDestination();
  const now = Tone.now();
  ["Bb4", "Ab4", "F4", "D4"].forEach((note, i) => synth.triggerAttackRelease(note, "8n", now + i * 0.12));
  window.setTimeout(() => synth.dispose(), 1000);
}

export async function playTimerTick() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.022, sustain: 0, release: 0.003 },
    volume: -14 + volumeToDb(_sfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("C7", "64n");
  window.setTimeout(() => synth.dispose(), 150);
}

export async function playTimerUrgent() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.05, sustain: 0.05, release: 0.02 },
    volume: -6 + volumeToDb(_sfxVolume)
  });
  const now = Tone.now();
  synth.triggerAttackRelease("E7", "32n", now);
  synth.triggerAttackRelease("G7", "32n", now + 0.06);
  window.setTimeout(() => synth.dispose(), 300);
}

export async function playMatchFound() {
  if (!canPlay()) return;
  await ensureStarted();
  const now = Tone.now();

  const sweep = new Tone.Synth({
    oscillator: { type: "sawtooth" },
    envelope: { attack: 0.01, decay: 0.0, sustain: 1.0, release: 0.2 },
    volume: -10 + volumeToDb(_sfxVolume)
  }).toDestination();
  sweep.frequency.setValueAtTime(200, now);
  sweep.frequency.exponentialRampToValueAtTime(1600, now + 0.55);
  sweep.triggerAttack(now);
  sweep.triggerRelease(now + 0.55);

  const crash = new Tone.PolySynth(Tone.Synth).toDestination();
  crash.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.005, decay: 0.2, sustain: 0.2, release: 0.6 },
    volume: -3 + volumeToDb(_sfxVolume)
  });
  crash.triggerAttackRelease(["C5", "E5", "G5", "C6"], "4n", now + 0.58);

  window.setTimeout(() => {
    sweep.dispose();
    crash.dispose();
  }, 2500);
}

export async function playLogoAssemble(): Promise<void> {
  if (!canPlay()) return;
  await ensureStarted();

  const now = Tone.now();

  const noise = new Tone.Noise("pink").start(now).stop(now + 1.3);
  const filter = new Tone.Filter({ type: "bandpass", frequency: 120, Q: 0.8 });
  const gain = new Tone.Gain(0);
  noise.connect(filter);
  filter.connect(gain);
  gain.toDestination();

  gain.gain.setValueAtTime(0, now);
  gain.gain.linearRampToValueAtTime(0.18 * _sfxVolume, now + 0.08);
  gain.gain.linearRampToValueAtTime(0.22 * _sfxVolume, now + 1.0);
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
  snapSynth.volume.value = -8 + volumeToDb(_sfxVolume);
  snapSynth.triggerAttackRelease("400", "8n", now + 1.4);

  window.setTimeout(() => {
    noise.dispose();
    filter.dispose();
    gain.dispose();
    snapSynth.dispose();
  }, 2500);
}

export async function playCountdownBeep() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "square" },
    envelope: { attack: 0.001, decay: 0.09, sustain: 0, release: 0.02 },
    volume: -5 + volumeToDb(_sfxVolume)
  }).toDestination();
  synth.triggerAttackRelease("A6", "16n");
  window.setTimeout(() => synth.dispose(), 400);
}

export async function playCountdownGo() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.005, decay: 0.12, sustain: 0.15, release: 0.35 },
    volume: -2 + volumeToDb(_sfxVolume)
  });
  const now = Tone.now();
  synth.triggerAttackRelease(["G5", "B5", "D6"], "8n", now);
  synth.triggerAttackRelease(["A5", "C6", "E6"], "8n", now + 0.1);
  synth.triggerAttackRelease(["C6", "E6", "G6"], "4n", now + 0.2);
  window.setTimeout(() => synth.dispose(), 2000);
}

export async function playRoundResult() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "sine" },
    envelope: { attack: 0.005, decay: 0.3, sustain: 0.2, release: 0.8 },
    volume: -8 + volumeToDb(_sfxVolume)
  });
  const now = Tone.now();
  synth.triggerAttackRelease(["G5", "B5"], "4n", now);
  synth.triggerAttackRelease(["C6", "E6"], "4n", now + 0.38);
  window.setTimeout(() => synth.dispose(), 2000);
}

export async function playWin() {
  if (!canPlay()) return;
  await ensureStarted();
  const now = Tone.now();
  const synth = new Tone.PolySynth(Tone.Synth).toDestination();
  synth.set({
    oscillator: { type: "square" },
    envelope: { attack: 0.005, decay: 0.12, sustain: 0.15, release: 0.4 },
    volume: -3 + volumeToDb(_sfxVolume)
  });
  ["C5", "E5", "G5", "C6", "E6", "G6"].forEach((note, i) => synth.triggerAttackRelease(note, "8n", now + i * 0.09));
  synth.triggerAttackRelease(["C5", "E5", "G5", "C6"], "4n", now + 0.65);
  synth.triggerAttackRelease(["G6", "E6", "C6"], "2n", now + 0.95);
  window.setTimeout(() => synth.dispose(), 3500);
}

export async function playLose() {
  if (!canPlay()) return;
  await ensureStarted();
  const synth = new Tone.Synth({
    oscillator: { type: "sawtooth" },
    envelope: { attack: 0.02, decay: 0.22, sustain: 0.08, release: 0.3 },
    volume: -7 + volumeToDb(_sfxVolume)
  }).toDestination();
  const now = Tone.now();
  ["G4", "F4", "Eb4", "Db4", "Bb3"].forEach((note, i) => synth.triggerAttackRelease(note, "8n", now + i * 0.14));
  window.setTimeout(() => synth.dispose(), 2000);
}
