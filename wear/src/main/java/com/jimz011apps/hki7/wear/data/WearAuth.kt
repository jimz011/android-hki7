package com.jimz011apps.hki7.wear.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The watch's own Home Assistant OAuth.
 *
 * The watch holds a refresh token of its own rather than borrowing the phone's access token. An
 * access token expires in about half an hour, so a watch given one would work briefly and then
 * quietly stop until someone opened the phone app again — which is exactly the sort of failure
 * nobody diagnoses, they just decide the watch app is broken.
 *
 * Two ways in, and they produce the same result:
 *  - the phone hands over its refresh token over the Data Layer (no typing, needs HKI 7 on the phone)
 *  - [com.jimz011apps.hki7.wear.ui.ManualSetupScreen] runs the authorization on the phone's browser
 *    through `RemoteAuthClient` and exchanges the code here (needs no phone app at all)
 *
 * The client id is the same constant the phone app uses, which is also what Home Assistant's own
 * Android app uses. It is a fixed identifier, not a secret, and not derived from the package name.
 */
object WearAuth {

    const val CLIENT_ID = "https://home-assistant.io/android"

    private const val GRANT_TYPE_CODE = "authorization_code"
    private const val GRANT_TYPE_REFRESH = "refresh_token"

    /** Renew this many milliseconds before the token actually expires, to absorb a slow request. */
    private const val EXPIRY_MARGIN_MILLIS = 60_000L

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long = 1800,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    /**
     * The URL `RemoteAuthClient` should open on the phone.
     *
     * Deliberately carries no `redirect_uri` or `code_challenge`: `OAuthRequest.Builder` appends
     * those itself, including the Google-hosted callback the watch listens on. Adding our own
     * would collide with them.
     */
    fun authorizationUrl(serverUrl: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/auth/authorize?response_type=code&client_id=${encode(CLIENT_ID)}"
    }

    /** Exchanges an authorization code from the phone browser for this watch's own tokens. */
    suspend fun exchangeCode(serverUrl: String, code: String): TokenResponse =
        post(
            serverUrl,
            mapOf(
                "grant_type" to GRANT_TYPE_CODE,
                "code" to code,
                "client_id" to CLIENT_ID,
            ),
        )

    /** Trades the stored refresh token for a fresh access token. */
    suspend fun refresh(serverUrl: String, refreshToken: String): TokenResponse =
        post(
            serverUrl,
            mapOf(
                "grant_type" to GRANT_TYPE_REFRESH,
                "refresh_token" to refreshToken,
                "client_id" to CLIENT_ID,
            ),
        )

    /** Absolute expiry for a token that reports [expiresIn] seconds of life, minus the margin. */
    fun expiryFrom(expiresIn: Long): Long =
        System.currentTimeMillis() + (expiresIn * 1000L) - EXPIRY_MARGIN_MILLIS

    private suspend fun post(serverUrl: String, fields: Map<String, String>): TokenResponse =
        withContext(Dispatchers.IO) {
            val body = fields.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
            val connection = (URL("${serverUrl.trimEnd('/')}/auth/token").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray()) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    // A dead refresh token is the one failure that cannot be retried out of: the
                    // caller has to send the user back through setup rather than keep trying.
                    throw TokenException(
                        invalidGrant = code == HttpURLConnection.HTTP_BAD_REQUEST,
                        message = "HTTP $code",
                    )
                }
                wearJson.decodeFromString(
                    TokenResponse.serializer(),
                    connection.inputStream.bufferedReader().readText(),
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** [invalidGrant] means the refresh token itself is dead; anything else may be transient. */
    class TokenException(val invalidGrant: Boolean, message: String) : IOException(message)
}
