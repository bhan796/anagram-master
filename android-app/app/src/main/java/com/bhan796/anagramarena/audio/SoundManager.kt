package com.bhan796.anagramarena.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private enum class Waveform {
    SINE,
    SQUARE,
    SAW
}

object SoundManager {
    private const val SAMPLE_RATE = 22_050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var soundEnabled = true
    private var masterMuted = false
    private var sfxVolume = 0.85f

    private fun canPlay(): Boolean = soundEnabled && !masterMuted && sfxVolume > 0f

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

    private fun generateDualSweepBuffer(
        startHzA: Double,
        endHzA: Double,
        startHzB: Double,
        endHzB: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val sampleCount = ((SAMPLE_RATE * durationMs) / 1000.0).toInt().coerceAtLeast(1)
        val attackSamples = (sampleCount * 0.06).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleCount * 0.18).toInt().coerceAtLeast(1)
        val data = ShortArray(sampleCount)

        var phaseA = 0.0
        var phaseB = 0.0
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleCount.toDouble()
            val freqA = startHzA + (endHzA - startHzA) * t
            val freqB = startHzB + (endHzB - startHzB) * t

            phaseA += (2.0 * PI * freqA) / SAMPLE_RATE.toDouble()
            phaseB += (2.0 * PI * freqB) / SAMPLE_RATE.toDouble()

            val mixed = (sin(phaseA) * 0.6) + (sin(phaseB) * 0.4)
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

    private fun generateSnapBurstBuffer(durationMs: Int, volume: Float): ShortArray {
        val sampleCount = ((SAMPLE_RATE * durationMs) / 1000.0).toInt().coerceAtLeast(1)
        val data = ShortArray(sampleCount)
        var phaseA = 0.0
        var phaseB = 0.0

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleCount.toDouble()
            val freqA = 1400.0 + 500.0 * t
            val freqB = 2200.0 - 700.0 * t
            phaseA += (2.0 * PI * freqA) / SAMPLE_RATE.toDouble()
            phaseB += (2.0 * PI * freqB) / SAMPLE_RATE.toDouble()

            val transient = (sin(phaseA) * 0.7) + (sin(phaseB) * 0.3)
            val envelope = (1.0 - t).coerceIn(0.0, 1.0)
            val value = transient * envelope * volume
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

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun setMasterMuted(muted: Boolean) {
        masterMuted = muted
    }

    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
    }

    fun playClick() {
        if (!canPlay()) return
        playTone(2093.0, 38, sfxVolume * 0.85f, Waveform.SQUARE)
    }

    fun playTilePlace() {
        if (!canPlay()) return
        val topNotes = listOf(783.99, 880.0, 987.77, 783.99, 698.46)
        val top = topNotes.random()
        scope.launch {
            playBuffer(generateBuffer(top, 55, sfxVolume, Waveform.SQUARE))
            playBuffer(generateBuffer(146.83, 75, sfxVolume * 0.85f, Waveform.SINE))
        }
    }

    fun playWordSubmit() {
        if (!canPlay()) return
        playSequence(
            listOf(523.25 to 45, 659.25 to 45, 783.99 to 45, 987.77 to 45, 1174.66 to 45, 1567.98 to 45),
            sfxVolume,
            Waveform.SINE
        )
    }

    fun playWordValid() {
        if (!canPlay()) return
        scope.launch {
            for ((freq, dur) in listOf(523.25 to 65, 659.25 to 65, 783.99 to 65, 1046.50 to 65, 1318.51 to 65)) {
                playBuffer(generateBuffer(freq, dur, sfxVolume, Waveform.SQUARE))
            }
            delay(20)
            playBuffer(generateMixedBuffer(listOf(1046.50, 1318.51, 1567.98), 220, sfxVolume * 0.9f, Waveform.SQUARE))
        }
    }

    fun playWordInvalid() {
        if (!canPlay()) return
        playSequence(
            listOf(466.16 to 110, 415.30 to 110, 311.13 to 110, 277.18 to 120, 233.08 to 130),
            sfxVolume * 0.85f,
            Waveform.SAW
        )
    }

    fun playTimerTick() {
        if (!canPlay()) return
        playTone(2093.0, 22, sfxVolume * 0.55f, Waveform.SQUARE)
    }

    fun playTimerUrgent() {
        if (!canPlay()) return
        playSequence(listOf(2637.02 to 50, 3135.96 to 50), sfxVolume * 0.9f, Waveform.SQUARE)
    }

    fun playMatchFound() {
        if (!canPlay()) return
        scope.launch {
            val sweepSteps = listOf(220.0 to 45, 330.0 to 45, 495.0 to 45, 660.0 to 45, 880.0 to 45, 1100.0 to 45, 1320.0 to 45)
            for ((freq, dur) in sweepSteps) {
                playBuffer(generateBuffer(freq, dur, sfxVolume * 0.6f, Waveform.SAW))
            }
            playBuffer(generateMixedBuffer(listOf(523.25, 659.25, 783.99, 1046.50), 450, sfxVolume, Waveform.SQUARE))
        }
    }

    fun playLogoAssemble() {
        if (!canPlay()) return
        val level = (sfxVolume * 0.7f).coerceIn(0f, 1f)
        scope.launch {
            playBuffer(
                generateDualSweepBuffer(
                    startHzA = 80.0,
                    endHzA = 800.0,
                    startHzB = 120.0,
                    endHzB = 1200.0,
                    durationMs = 1200,
                    volume = level
                )
            )
            delay(200)
            playBuffer(generateSnapBurstBuffer(durationMs = 90, volume = level * 0.9f))
        }
    }

    fun playCountdownBeep() {
        if (!canPlay()) return
        playTone(1760.0, 105, sfxVolume, Waveform.SQUARE)
    }

    fun playCountdownGo() {
        if (!canPlay()) return
        scope.launch {
            playBuffer(generateMixedBuffer(listOf(783.99, 987.77, 1174.66), 110, sfxVolume, Waveform.SQUARE))
            delay(10)
            playBuffer(generateMixedBuffer(listOf(880.0, 1046.50, 1318.51), 110, sfxVolume, Waveform.SQUARE))
            delay(10)
            playBuffer(generateMixedBuffer(listOf(1046.50, 1318.51, 1567.98), 260, sfxVolume, Waveform.SQUARE))
        }
    }

    fun playRoundResult() {
        if (!canPlay()) return
        scope.launch {
            playBuffer(generateMixedBuffer(listOf(783.99, 987.77), 300, sfxVolume * 0.8f, Waveform.SINE))
            delay(350)
            playBuffer(generateMixedBuffer(listOf(1046.50, 1318.51), 380, sfxVolume * 0.8f, Waveform.SINE))
        }
    }

    fun playWin() {
        if (!canPlay()) return
        scope.launch {
            for ((freq, dur) in listOf(523.25 to 80, 659.25 to 80, 783.99 to 80, 1046.50 to 80, 1318.51 to 80, 1567.98 to 80)) {
                playBuffer(generateBuffer(freq, dur, sfxVolume, Waveform.SQUARE))
            }
            delay(30)
            playBuffer(generateMixedBuffer(listOf(523.25, 659.25, 783.99, 1046.50), 200, sfxVolume, Waveform.SQUARE))
            delay(220)
            playBuffer(generateMixedBuffer(listOf(1567.98, 1318.51, 1046.50), 600, sfxVolume * 0.85f, Waveform.SQUARE))
        }
    }

    fun playLose() {
        if (!canPlay()) return
        playSequence(
            listOf(392.0 to 130, 349.23 to 130, 311.13 to 130, 277.18 to 130, 233.08 to 160),
            sfxVolume,
            Waveform.SAW
        )
    }
}
