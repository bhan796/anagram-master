package com.bhan796.anagramarena.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AuthSessionPayload(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
    val refreshExpiresInSeconds: Int
)

data class AuthResultPayload(
    val userId: String,
    val email: String,
    val playerId: String?,
    val session: AuthSessionPayload
)

class AuthApiService(private val baseUrl: String) {
    suspend fun register(email: String, password: String, playerId: String?): Result<AuthResultPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
            if (!playerId.isNullOrBlank()) payload.put("playerId", playerId)
            val response = postJson("/api/auth/register", payload)
            response.toAuthResult()
        }
    }

    suspend fun login(email: String, password: String, playerId: String?): Result<AuthResultPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
            if (!playerId.isNullOrBlank()) payload.put("playerId", playerId)
            val response = postJson("/api/auth/login", payload)
            response.toAuthResult()
        }
    }

    suspend fun oauth(provider: String, token: String, playerId: String?): Result<AuthResultPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("token", token)
            if (!playerId.isNullOrBlank()) payload.put("playerId", playerId)
            val response = postJson("/api/auth/oauth/$provider", payload)
            response.toAuthResult()
        }
    }

    suspend fun refresh(refreshToken: String): Result<AuthSessionPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val response = postJson("/api/auth/refresh", JSONObject().put("refreshToken", refreshToken))
            response.getJSONObject("session").toSession()
        }
    }

    suspend fun logout(refreshToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postJson("/api/auth/logout", JSONObject().put("refreshToken", refreshToken))
        }.map { Unit }
    }

    suspend fun me(accessToken: String): Result<Triple<String, String, String?>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getJson("/api/auth/me", accessToken)
            val playerIds = response.optJSONArray("playerIds")
            val playerId = if (playerIds != null && playerIds.length() > 0) playerIds.optString(0) else null
            Triple(response.getString("userId"), response.getString("email"), playerId)
        }
    }

    private fun getJson(path: String, accessToken: String?): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }
        return readJson(connection)
    }

    private fun postJson(path: String, payload: JSONObject): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(payload.toString())
        }

        return readJson(connection)
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val message = runCatching { JSONObject(body).optString("message") }.getOrDefault("")
            throw IllegalStateException(if (message.isNotBlank()) message else "HTTP ${connection.responseCode}: $body")
        }
        return if (body.isBlank()) JSONObject() else JSONObject(body)
    }

    private fun JSONObject.toAuthResult(): AuthResultPayload {
        return AuthResultPayload(
            userId = getString("userId"),
            email = getString("email"),
            playerId = if (has("playerId") && !isNull("playerId")) getString("playerId") else null,
            session = getJSONObject("session").toSession()
        )
    }

    private fun JSONObject.toSession(): AuthSessionPayload {
        return AuthSessionPayload(
            accessToken = getString("accessToken"),
            refreshToken = getString("refreshToken"),
            expiresInSeconds = optInt("expiresInSeconds", 900),
            refreshExpiresInSeconds = optInt("refreshExpiresInSeconds", 1209600)
        )
    }
}
