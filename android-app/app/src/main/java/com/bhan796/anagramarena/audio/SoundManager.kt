package com.bhan796.anagramarena.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class MusicMode {
    NONE,
    MENU,
    MATCH
}

private data class MusicStep(val tone: Int, val durationMs: Int)

object SoundManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var soundEnabled = true
    private var masterMuted = false
    private var musicEnabled = true
    private var uiSfxEnabled = true
    private var gameSfxEnabled = true

    private var musicVolume = 0.5f
    private var uiSfxVolume = 0.8f
    private var gameSfxVolume = 0.85f

    private var activeMusicMode: MusicMode = MusicMode.NONE
    private var musicJob: Job? = null

    private fun canPlayUiSfx(): Boolean = soundEnabled && !masterMuted && uiSfxEnabled && uiSfxVolume > 0f
    private fun canPlayGameSfx(): Boolean = soundEnabled && !masterMuted && gameSfxEnabled && gameSfxVolume > 0f
    private fun canPlayMusic(): Boolean = !masterMuted && musicEnabled && musicVolume > 0f

    private fun playTone(tone: Int, durationMs: Int, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped <= 0f) return
        scope.launch {
            val generator = ToneGenerator(AudioManager.STREAM_MUSIC, (clamped * 100f).toInt().coerceIn(1, 100))
            try {
                generator.startTone(tone, durationMs)
                delay((durationMs + 24).toLong())
            } finally {
                generator.release()
            }
        }
    }

    private fun playSequence(volume: Float, steps: List<Pair<Int, Int>>) {
        scope.launch {
            for ((tone, duration) in steps) {
                playTone(tone, duration, volume)
                delay((duration + 20).toLong())
            }
        }
    }

    private fun startLoop(mode: MusicMode, bpm: Int, pattern: List<MusicStep>) {
        musicJob?.cancel()
        musicJob = scope.launch {
            val beatMs = (60_000f / bpm.toFloat()).toLong()
            while (isActive && activeMusicMode == mode && canPlayMusic()) {
                for (step in pattern) {
                    if (!isActive || activeMusicMode != mode || !canPlayMusic()) break
                    playTone(step.tone, step.durationMs, musicVolume * 0.6f)
                    delay(beatMs / 2)
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
        playTone(ToneGenerator.TONE_PROP_BEEP, 45, uiSfxVolume)
    }

    fun playTilePlace() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_7 to 70,
                ToneGenerator.TONE_DTMF_5 to 70
            )
        )
    }

    fun playWordSubmit() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_1 to 90,
                ToneGenerator.TONE_DTMF_3 to 90,
                ToneGenerator.TONE_DTMF_5 to 90
            )
        )
    }

    fun playWordValid() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_1 to 80,
                ToneGenerator.TONE_DTMF_3 to 80,
                ToneGenerator.TONE_DTMF_5 to 80,
                ToneGenerator.TONE_DTMF_7 to 80
            )
        )
    }

    fun playWordInvalid() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_SUP_ERROR to 140,
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 110
            )
        )
    }

    fun playTimerTick() {
        if (!canPlayGameSfx()) return
        playTone(ToneGenerator.TONE_PROP_BEEP, 30, gameSfxVolume * 0.8f)
    }

    fun playTimerUrgent() {
        if (!canPlayGameSfx()) return
        playTone(ToneGenerator.TONE_CDMA_HIGH_L, 70, gameSfxVolume)
    }

    fun playMatchFound() {
        if (!canPlayUiSfx()) return
        playSequence(
            uiSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_1 to 90,
                ToneGenerator.TONE_DTMF_3 to 90,
                ToneGenerator.TONE_DTMF_5 to 90,
                ToneGenerator.TONE_DTMF_7 to 90
            )
        )
    }

    fun playCountdownBeep() {
        if (!canPlayUiSfx()) return
        playTone(ToneGenerator.TONE_PROP_BEEP2, 120, uiSfxVolume)
    }

    fun playCountdownGo() {
        if (!canPlayUiSfx()) return
        playSequence(
            uiSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_A to 100,
                ToneGenerator.TONE_DTMF_B to 100,
                ToneGenerator.TONE_DTMF_C to 100
            )
        )
    }

    fun playRoundResult() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_6 to 100,
                ToneGenerator.TONE_DTMF_8 to 100
            )
        )
    }

    fun playWin() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_DTMF_1 to 90,
                ToneGenerator.TONE_DTMF_3 to 90,
                ToneGenerator.TONE_DTMF_5 to 90,
                ToneGenerator.TONE_DTMF_7 to 90,
                ToneGenerator.TONE_DTMF_9 to 90
            )
        )
    }

    fun playLose() {
        if (!canPlayGameSfx()) return
        playSequence(
            gameSfxVolume,
            listOf(
                ToneGenerator.TONE_CDMA_LOW_L to 120,
                ToneGenerator.TONE_CDMA_LOW_PBX_L to 120,
                ToneGenerator.TONE_SUP_ERROR to 140
            )
        )
    }

    fun startMenuMusic(forceRestart: Boolean = false) {
        if (!canPlayMusic()) return
        if (!forceRestart && activeMusicMode == MusicMode.MENU && musicJob?.isActive == true) return

        activeMusicMode = MusicMode.MENU
        startLoop(
            mode = MusicMode.MENU,
            bpm = 120,
            pattern = listOf(
                MusicStep(ToneGenerator.TONE_DTMF_1, 140),
                MusicStep(ToneGenerator.TONE_DTMF_3, 140),
                MusicStep(ToneGenerator.TONE_DTMF_5, 140),
                MusicStep(ToneGenerator.TONE_DTMF_8, 140),
                MusicStep(ToneGenerator.TONE_DTMF_3, 140),
                MusicStep(ToneGenerator.TONE_DTMF_5, 140),
                MusicStep(ToneGenerator.TONE_DTMF_2, 140),
                MusicStep(ToneGenerator.TONE_DTMF_4, 140)
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
                MusicStep(ToneGenerator.TONE_DTMF_7, 120),
                MusicStep(ToneGenerator.TONE_DTMF_5, 120),
                MusicStep(ToneGenerator.TONE_DTMF_3, 120),
                MusicStep(ToneGenerator.TONE_DTMF_1, 120),
                MusicStep(ToneGenerator.TONE_DTMF_6, 120),
                MusicStep(ToneGenerator.TONE_DTMF_4, 120),
                MusicStep(ToneGenerator.TONE_DTMF_2, 120),
                MusicStep(ToneGenerator.TONE_DTMF_0, 120)
            )
        )
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        activeMusicMode = MusicMode.NONE
    }
}
