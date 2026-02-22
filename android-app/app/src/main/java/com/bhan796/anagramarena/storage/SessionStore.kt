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

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) {
            prefs.edit().putString("access_token", value).apply()
        }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) {
            prefs.edit().putString("refresh_token", value).apply()
        }

    var authUserId: String?
        get() = prefs.getString("auth_user_id", null)
        set(value) {
            prefs.edit().putString("auth_user_id", value).apply()
        }

    var authEmail: String?
        get() = prefs.getString("auth_email", null)
        set(value) {
            prefs.edit().putString("auth_email", value).apply()
        }
}
