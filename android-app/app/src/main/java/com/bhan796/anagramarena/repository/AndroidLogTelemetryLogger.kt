package com.bhan796.anagramarena.repository

import android.util.Log

class AndroidLogTelemetryLogger : TelemetryLogger {
    override fun log(event: String, fields: Map<String, String>) {
        if (fields.isEmpty()) {
            Log.d("AnagramTelemetry", event)
            return
        }

        val joined = fields.entries.joinToString(separator = " ") { "${it.key}=${it.value}" }
        Log.d("AnagramTelemetry", "$event $joined")
    }
}
