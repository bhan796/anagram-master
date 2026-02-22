package com.bhan796.anagramarena.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private enum class MusicMode {
    NONE,
    MENU,
    MATCH
}

private enum class Waveform {
    SINE,
    SQUARE,
    SAW
}

private data class ToneStep(
    val frequencyHz: Double,
    val beats: Double,
    val waveform: Waveform
)

object SoundManager {
    private const val SAMPLE_RATE = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var soundEnabled = true
    private var masterMuted = false
    private var musicEnabled = true
    private var uiSfxEnabled = true
    private var gameSfxEnabled = true

    private var musicVolume = 0.5f
    private var uiSfxVolume = 0.8f
    private var gameSfxVolume = 0.85f

    private var activeMusicMode = MusicMode.NONE
    private var musicJob: Job? = null

    private fun canPlayUiSfx(): Boolean = soundEnabled && !masterMuted && uiSfxEnabled && uiSfxVolume > 0f
    private fun canPlayGameSfx(): Boolean = soundEnabled && !masterMuted && gameSfxEnabled && gameSfxVolume > 0f
    private fun canPlayMusic(): Boolean = !masterMuted && musicEnabled && musicVolume > 0f

    private fun playTone(frequencyHz: Double, durationMs: Int, volume: Float, waveform: Waveform = Waveform.SQUARE) {
        val level = volume.coerceIn(0f, 1f)
        if (level <= 0f) return
        scope.launch {
            playBuffer(generateBuffer(frequencyHz, durationMs, level, waveform))
        }
    }

    private fun playChord(tones: List<Double>, durationMs: Int, volume: Float, waveform: Waveform = Waveform.SQUARE) {
        val level = volume.coerceIn(0f, 1f)
        if (level <= 0f || tones.isEmpty()) return
        scope.launch {
            playBuffer(generateMixedBuffer(tones, durationMs, level, waveform))
        }
    }

    private fun playSequence(steps: List<Pair<Double, Int>>, volume: Float, waveform: Waveform = Waveform.SQUARE) {
        val level = volume.coerceIn(0f, 1f)
        if (level <= 0f || steps.isEmpty()) return
        scope.launch {
            for ((freq, duration) in steps) {
                playBuffer(generateBuffer(freq, duration, level, waveform))
                if (!isActive) break
            }
        }
    }

    private fun generateBuffer(frequencyHz: Double, durationMs: Int, volume: Float, waveform: Waveform): ShortArray {
        val sampleCount = ((SAMPLE_RATE * durationMs) / 1000.0).toInt().coerceAtLeast(1)
        val attackSamples = (sampleCount * 0.07).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleCount * 0.15).toInt().coerceAtLeast(1)
        val data = ShortArray(sampleCount)

        for (i in 0 until sampleCount) {
            val phase = (2.0 * PI * frequencyHz * i.toDouble()) / SAMPLE_RATE.toDouble()
            val raw = when (waveform) {
                Waveform.SINE -> sin(phase)
                Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                Waveform.SAW -> ((i.toDouble() * frequencyHz / SAMPLE_RATE.toDouble()) % 1.0) * 2.0 - 1.0
            }

            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples.toDouble()
                i > sampleCount - releaseSamples -> (sampleCount - i).toDouble() / releaseSamples.toDouble()
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            val value = raw * envelope * volume
            data[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return data
    }

    private fun generateMixedBuffer(tones: List<Double>, durationMs: Int, volume: Float, waveform: Waveform): ShortArray {
        val sampleCount = ((SAMPLE_RATE * durationMs) / 1000.0).toInt().coerceAtLeast(1)
        val attackSamples = (sampleCount * 0.07).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleCount * 0.15).toInt().coerceAtLeast(1)
        val data = ShortArray(sampleCount)
        val toneCount = tones.size.coerceAtLeast(1)

        for (i in 0 until sampleCount) {
            var mixed = 0.0
            for (frequencyHz in tones) {
                val phase = (2.0 * PI * frequencyHz * i.toDouble()) / SAMPLE_RATE.toDouble()
                val raw = when (waveform) {
                    Waveform.SINE -> sin(phase)
                    Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                    Waveform.SAW -> ((i.toDouble() * frequencyHz / SAMPLE_RATE.toDouble()) % 1.0) * 2.0 - 1.0
                }
                mixed += raw / toneCount.toDouble()
            }

            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples.toDouble()
                i > sampleCount - releaseSamples -> (sampleCount - i).toDouble() / releaseSamples.toDouble()
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            val value = mixed * envelope * volume
            data[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return data
    }

    private fun playBuffer(buffer: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * 2)
            .build()

        try {
            audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            audioTrack.play()
            while (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING && audioTrack.playbackHeadPosition < buffer.size) {
                Thread.sleep(4)
            }
        } finally {
            audioTrack.release()
        }
    }

    private fun startLoop(mode: MusicMode, bpm: Int, pattern: List<ToneStep>) {
        musicJob?.cancel()
        musicJob = scope.launch {
            val beatMs = 60_000.0 / bpm.toDouble()
            while (isActive && activeMusicMode == mode && canPlayMusic()) {
                for (step in pattern) {
                    if (!isActive || activeMusicMode != mode || !canPlayMusic()) break
                    val duration = (beatMs * step.beats).toInt().coerceAtLeast(30)
                    playBuffer(generateBuffer(step.frequencyHz, duration, (musicVolume * 0.55f).coerceIn(0f, 1f), step.waveform))
                }
            }
        }
    }

    private fun updateMusicState(targetMode: MusicMode = activeMusicMode) {
        if (!canPlayMusic()) {
            stopMusic()
            return
        }

        when (targetMode) {
            MusicMode.NONE -> stopMusic()
            MusicMode.MENU -> startMenuMusic(forceRestart = true)
            MusicMode.MATCH -> startMatchMusic(forceRestart = true)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun setMasterMuted(muted: Boolean) {
        masterMuted = muted
        if (muted) {
            stopMusic()
        } else {
            updateMusicState()
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            stopMusic()
        } else {
            updateMusicState()
        }
    }

    fun setUiSfxEnabled(enabled: Boolean) {
        uiSfxEnabled = enabled
    }

    fun setGameSfxEnabled(enabled: Boolean) {
        gameSfxEnabled = enabled
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        updateMusicState()
    }

    fun setUiSfxVolume(volume: Float) {
        uiSfxVolume = volume.coerceIn(0f, 1f)
    }

    fun setGameSfxVolume(volume: Float) {
        gameSfxVolume = volume.coerceIn(0f, 1f)
    }

    fun playClick() {
        if (!canPlayUiSfx()) return
        playTone(1046.50, 52, uiSfxVolume, Waveform.SQUARE)
    }

    fun playTilePlace() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(392.0 to 90, 329.63 to 90), gameSfxVolume, Waveform.SQUARE)
    }

    fun playWordSubmit() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(523.25 to 90, 659.25 to 90, 783.99 to 90), gameSfxVolume, Waveform.SINE)
    }

    fun playWordValid() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(523.25 to 80, 659.25 to 80, 783.99 to 80, 1046.50 to 80), gameSfxVolume, Waveform.SINE)
    }

    fun playWordInvalid() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(220.0 to 140, 174.61 to 120), gameSfxVolume, Waveform.SAW)
    }

    fun playTimerTick() {
        if (!canPlayGameSfx()) return
        playTone(1046.50, 42, gameSfxVolume * 0.8f, Waveform.SINE)
    }

    fun playTimerUrgent() {
        if (!canPlayGameSfx()) return
        playTone(1318.51, 70, gameSfxVolume, Waveform.SINE)
    }

    fun playMatchFound() {
        if (!canPlayUiSfx()) return
        playSequence(listOf(523.25 to 90, 659.25 to 90, 783.99 to 90, 1046.50 to 90), uiSfxVolume, Waveform.SQUARE)
    }

    fun playCountdownBeep() {
        if (!canPlayUiSfx()) return
        playTone(880.0, 120, uiSfxVolume, Waveform.SINE)
    }

    fun playCountdownGo() {
        if (!canPlayUiSfx()) return
        playChord(listOf(880.0, 1174.66, 1567.98), 260, uiSfxVolume, Waveform.SQUARE)
    }

    fun playRoundResult() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(783.99 to 100, 1046.50 to 100), gameSfxVolume, Waveform.SINE)
    }

    fun playWin() {
        if (!canPlayGameSfx()) return
        playSequence(
            listOf(523.25 to 90, 659.25 to 90, 783.99 to 90, 1046.50 to 90, 1318.51 to 90),
            gameSfxVolume,
            Waveform.SQUARE
        )
    }

    fun playLose() {
        if (!canPlayGameSfx()) return
        playSequence(listOf(392.0 to 120, 349.23 to 120, 311.13 to 120, 261.63 to 140), gameSfxVolume, Waveform.SAW)
    }

    fun startMenuMusic(forceRestart: Boolean = false) {
        if (!canPlayMusic()) return
        if (!forceRestart && activeMusicMode == MusicMode.MENU && musicJob?.isActive == true) return

        activeMusicMode = MusicMode.MENU
        startLoop(
            mode = MusicMode.MENU,
            bpm = 120,
            pattern = listOf(
                ToneStep(261.63, 0.5, Waveform.SQUARE),
                ToneStep(329.63, 0.5, Waveform.SQUARE),
                ToneStep(392.0, 0.5, Waveform.SQUARE),
                ToneStep(523.25, 0.5, Waveform.SQUARE),
                ToneStep(329.63, 0.5, Waveform.SQUARE),
                ToneStep(392.0, 0.5, Waveform.SQUARE),
                ToneStep(293.66, 0.5, Waveform.SQUARE),
                ToneStep(349.23, 0.5, Waveform.SQUARE)
            )
        )
    }

    fun startMatchMusic(forceRestart: Boolean = false) {
        if (!canPlayMusic()) return
        if (!forceRestart && activeMusicMode == MusicMode.MATCH && musicJob?.isActive == true) return

        activeMusicMode = MusicMode.MATCH
        startLoop(
            mode = MusicMode.MATCH,
            bpm = 140,
            pattern = listOf(
                ToneStep(220.0, 0.5, Waveform.SQUARE),
                ToneStep(261.63, 0.5, Waveform.SQUARE),
                ToneStep(329.63, 0.5, Waveform.SQUARE),
                ToneStep(440.0, 0.5, Waveform.SQUARE),
                ToneStep(392.0, 0.5, Waveform.SQUARE),
                ToneStep(329.63, 0.5, Waveform.SQUARE),
                ToneStep(261.63, 0.5, Waveform.SQUARE),
                ToneStep(220.0, 0.5, Waveform.SQUARE)
            )
        )
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        activeMusicMode = MusicMode.NONE
    }
}
