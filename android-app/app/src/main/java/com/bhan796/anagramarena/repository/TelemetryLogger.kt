package com.bhan796.anagramarena.repository

interface TelemetryLogger {
    fun log(event: String, fields: Map<String, String> = emptyMap())
}

class NoOpTelemetryLogger : TelemetryLogger {
    override fun log(event: String, fields: Map<String, String>) = Unit
}
