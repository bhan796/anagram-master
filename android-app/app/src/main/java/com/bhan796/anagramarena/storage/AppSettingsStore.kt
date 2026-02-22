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

    var sfxVolume: Float
        get() = prefs.getFloat("sfx_volume", 0.85f)
        set(value) {
            prefs.edit().putFloat("sfx_volume", value.coerceIn(0f, 1f)).apply()
        }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) {
            prefs.edit().putBoolean("vibration_enabled", value).apply()
        }
}
