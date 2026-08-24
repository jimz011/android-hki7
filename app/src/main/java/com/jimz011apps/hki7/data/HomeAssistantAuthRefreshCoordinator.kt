package com.jimz011apps.hki7.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TOKEN_REFRESH_LEAD_MS = 60_000L

internal fun accessTokenNeedsRefresh(expiryMillis: Long?, nowMillis: Long): Boolean =
    expiryMillis == null || expiryMillis - nowMillis <= TOKEN_REFRESH_LEAD_MS

internal fun shouldRefreshBeforeAuthenticatedWork(
    refreshToken: String?,
    expiryMillis: Long?,
    nowMillis: Long
): Boolean = !refreshToken.isNullOrBlank() && accessTokenNeedsRefresh(expiryMillis, nowMillis)

internal fun isKnownExpiredWithoutRefreshToken(
    accessToken: String?,
    refreshToken: String?,
    expiryMillis: Long?,
    nowMillis: Long
): Boolean = !accessToken.isNullOrBlank() && refreshToken.isNullOrBlank() &&
    expiryMillis != null && expiryMillis <= nowMillis

internal sealed interface CoordinatedTokenRefreshResult {
    data class Success(
        val accessToken: String,
        val expiresInSeconds: Int?,
        val performedRefresh: Boolean
    ) : CoordinatedTokenRefreshResult

    data class LoginRequired(val cause: Throwable? = null) : CoordinatedTokenRefreshResult
    data class RetryableFailure(val cause: Throwable) : CoordinatedTokenRefreshResult
}

/**
 * Serializes refreshes across the foreground ViewModel and the optional background push service.
 * A waiting caller reuses a token saved by the first caller instead of submitting the same stale
 * refresh token again. This matters because Home Assistant counts rejected token requests toward
 * its IP-ban threshold.
 */
internal object HomeAssistantAuthRefreshCoordinator {
    private val mutex = Mutex()

    suspend fun refresh(
        serverUrl: String,
        prefs: PreferencesManager,
        expectedAccessToken: String?
    ): CoordinatedTokenRefreshResult = mutex.withLock {
        val storedAccessToken = prefs.accessToken.first()
        if (!storedAccessToken.isNullOrBlank() && storedAccessToken != expectedAccessToken) {
            val expiry = prefs.accessTokenExpiry.first()
            val remainingSeconds = expiry?.let {
                ((it - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
            }
            return@withLock CoordinatedTokenRefreshResult.Success(
                accessToken = storedAccessToken,
                expiresInSeconds = remainingSeconds,
                performedRefresh = false
            )
        }

        val refreshToken = prefs.refreshToken.first()
        if (refreshToken.isNullOrBlank()) {
            if (!expectedAccessToken.isNullOrBlank()) prefs.clearAuth()
            return@withLock CoordinatedTokenRefreshResult.LoginRequired()
        }

        try {
            val response = HomeAssistantClient.refreshAccessToken(serverUrl, refreshToken)
            prefs.saveAuthTokens(response.access_token, response.refresh_token, response.expires_in)
            CoordinatedTokenRefreshResult.Success(
                accessToken = response.access_token,
                expiresInSeconds = response.expires_in,
                performedRefresh = true
            )
        } catch (error: TokenRefreshException) {
            if (error.isClientRejection) {
                prefs.clearAuth()
                CoordinatedTokenRefreshResult.LoginRequired(error)
            } else {
                CoordinatedTokenRefreshResult.RetryableFailure(error)
            }
        } catch (error: Exception) {
            CoordinatedTokenRefreshResult.RetryableFailure(error)
        }
    }
}
