package com.bhan796.anagramarena.storage

import android.content.Context

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var timerEnabled: Boolean
        get() = prefs.getBoolean("timer_enabled", true)
        set(value) {
            prefs.edit().putBoolean("timer_enabled", value).apply()
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }

    var masterMuted: Boolean
        get() = prefs.getBoolean("master_muted", false)
        set(value) {
            prefs.edit().putBoolean("master_muted", value).apply()
        }

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) {
            prefs.edit().putBoolean("music_enabled", value).apply()
        }

    var uiSfxEnabled: Boolean
        get() = prefs.getBoolean("ui_sfx_enabled", true)
        set(value) {
            prefs.edit().putBoolean("ui_sfx_enabled", value).apply()
        }

    var gameSfxEnabled: Boolean
        get() = prefs.getBoolean("game_sfx_enabled", true)
        set(value) {
            prefs.edit().putBoolean("game_sfx_enabled", value).apply()
        }

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 0.5f)
        set(value) {
            prefs.edit().putFloat("music_volume", value.coerceIn(0f, 1f)).apply()
        }

    var uiSfxVolume: Float
        get() = prefs.getFloat("ui_sfx_volume", 0.8f)
        set(value) {
            prefs.edit().putFloat("ui_sfx_volume", value.coerceIn(0f, 1f)).apply()
        }

    var gameSfxVolume: Float
        get() = prefs.getFloat("game_sfx_volume", 0.85f)
        set(value) {
            prefs.edit().putFloat("game_sfx_volume", value.coerceIn(0f, 1f)).apply()
        }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) {
            prefs.edit().putBoolean("vibration_enabled", value).apply()
        }
}
