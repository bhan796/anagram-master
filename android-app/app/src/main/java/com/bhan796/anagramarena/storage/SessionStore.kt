package com.bhan796.anagramarena.storage

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("online_session", Context.MODE_PRIVATE)

    var playerId: String?
        get() = prefs.getString("player_id", null)
        set(value) {
            prefs.edit().putString("player_id", value).apply()
        }

    var displayName: String?
        get() = prefs.getString("display_name", null)
        set(value) {
            prefs.edit().putString("display_name", value).apply()
        }

    var matchId: String?
        get() = prefs.getString("match_id", null)
        set(value) {
            prefs.edit().putString("match_id", value).apply()
        }
}
