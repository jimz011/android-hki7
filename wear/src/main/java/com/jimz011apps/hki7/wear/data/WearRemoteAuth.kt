package com.jimz011apps.hki7.wear.data

import android.content.Context
import androidx.core.net.toUri
import androidx.wear.phone.interactions.authentication.CodeChallenge
import androidx.wear.phone.interactions.authentication.CodeVerifier
import androidx.wear.phone.interactions.authentication.OAuthRequest
import androidx.wear.phone.interactions.authentication.OAuthResponse
import androidx.wear.phone.interactions.authentication.RemoteAuthClient
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.Executors

/**
 * Signs the watch in by running Home Assistant's login page on the paired phone's browser.
 *
 * This is what makes the watch standalone. The Data Layer handover is nicer when it applies — no
 * typing at all — but it needs HKI 7 installed on the phone. This path needs only a phone with a
 * browser, so it still works after the phone app is removed, after a watch factory reset, or when
 * the handover simply never arrived.
 *
 * Nothing sensitive is typed on the watch: only the server address. The password, and any
 * two-factor step, happen on a phone-sized screen with a real keyboard and a password manager.
 *
 * `RemoteAuthClient` appends its own `redirect_uri` (a Google-hosted callback the watch listens
 * on) and the PKCE `code_challenge`, which is why [WearAuth.authorizationUrl] must not add either.
 */
object WearRemoteAuth {

    sealed interface Result {
        data class Success(val refreshToken: String) : Result

        /** No paired phone, or the phone could not open the page. */
        data object PhoneUnavailable : Result

        /** This watch or phone cannot run the remote authorization flow at all. */
        data object Unsupported : Result

        data class Failed(val message: String?) : Result
    }

    /**
     * Runs the whole flow: authorize on the phone, exchange the code here, store the session.
     *
     * [serverUrl] should already be normalised to an absolute http(s) URL.
     */
    suspend fun authenticate(
        context: Context,
        serverUrl: String,
        prefs: WearPreferences,
    ): Result {
        val verifier = CodeVerifier()
        val request = runCatching {
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(WearAuth.authorizationUrl(serverUrl).toUri())
                .setCodeChallenge(CodeChallenge(verifier))
                .build()
        }.getOrElse { return Result.Unsupported }

        val client = RemoteAuthClient.create(context)
        val code = try {
            awaitAuthorizationCode(client, request)
        } finally {
            client.close()
        }

        return when (code) {
            is CodeResult.Error -> code.result
            is CodeResult.Code -> runCatching {
                val tokens = WearAuth.exchangeCode(serverUrl, code.value)
                val refreshToken = tokens.refreshToken
                    ?: return Result.Failed("No refresh token returned")
                prefs.saveSession(serverUrl, refreshToken)
                prefs.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = refreshToken,
                    expiresAt = WearAuth.expiryFrom(tokens.expiresIn),
                )
                Result.Success(refreshToken)
            }.getOrElse { Result.Failed(it.message) }
        }
    }

    private sealed interface CodeResult {
        data class Code(val value: String) : CodeResult
        data class Error(val result: Result) : CodeResult
    }

    private suspend fun awaitAuthorizationCode(
        client: RemoteAuthClient,
        request: OAuthRequest,
    ): CodeResult = suspendCoroutine { continuation ->
        client.sendAuthorizationRequest(
            request,
            Executors.newSingleThreadExecutor(),
            object : RemoteAuthClient.Callback() {
                override fun onAuthorizationError(request: OAuthRequest, errorCode: Int) {
                    continuation.resume(
                        CodeResult.Error(
                            when (errorCode) {
                                RemoteAuthClient.ERROR_UNSUPPORTED -> Result.Unsupported
                                RemoteAuthClient.ERROR_PHONE_UNAVAILABLE -> Result.PhoneUnavailable
                                else -> Result.Failed("Authorization error $errorCode")
                            },
                        ),
                    )
                }

                override fun onAuthorizationResponse(request: OAuthRequest, response: OAuthResponse) {
                    val code = response.responseUrl?.getQueryParameter("code")
                    continuation.resume(
                        if (code.isNullOrBlank()) {
                            CodeResult.Error(Result.Failed("No authorization code returned"))
                        } else {
                            CodeResult.Code(code)
                        },
                    )
                }
            },
        )
    }

    /** Accepts what someone can realistically type on a watch and makes a URL of it. */
    fun normaliseUrl(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            // A bare host is almost always a Nabu Casa address, which is https-only. A local
            // address typed without a scheme is the rarer case and can be typed in full.
            "https://$trimmed"
        }
        return withScheme.takeIf { runCatching { java.net.URL(it) }.isSuccess }
    }
}
