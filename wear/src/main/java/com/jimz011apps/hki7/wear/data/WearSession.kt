package com.jimz011apps.hki7.wear.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the watch's access token valid, renewing it from the stored refresh token.
 *
 * The one place that knows a token can expire. [HomeAssistantRest] asks for a token per request
 * and retries once on a 401, so nothing else has to reason about token lifetime.
 */
class WearSession(private val prefs: WearPreferences) {

    /** One refresh at a time: a screen loading six entities at once would otherwise fire six. */
    private val refreshMutex = Mutex()

    class NotAuthenticated : Exception("The watch has no Home Assistant session")

    /** True once setup has produced a refresh token, whichever route it came from. */
    suspend fun isAuthenticated(): Boolean = prefs.refreshToken() != null

    /**
     * A usable access token, renewing first if the stored one has expired.
     *
     * Throws [NotAuthenticated] when there is nothing to renew from, and
     * [WearAuth.TokenException] with `invalidGrant` when the refresh token itself is dead — the
     * signal to send the user back through setup rather than retry.
     */
    suspend fun accessToken(): String {
        prefs.validAccessToken()?.let { return it }
        return refreshMutex.withLock {
            // Another caller may have renewed while this one waited for the lock.
            prefs.validAccessToken() ?: renew()
        }
    }

    /** Forces a renewal, for the 401 retry path where the token is stale earlier than expected. */
    suspend fun forceRenew(): String = refreshMutex.withLock { renew() }

    private suspend fun renew(): String {
        val serverUrl = prefs.serverUrlOnce() ?: throw NotAuthenticated()
        val refreshToken = prefs.refreshToken() ?: throw NotAuthenticated()
        val response = WearAuth.refresh(serverUrl, refreshToken)
        prefs.saveTokens(
            accessToken = response.accessToken,
            // Home Assistant does not rotate refresh tokens, so a response without one leaves the
            // stored token in place rather than clearing it.
            refreshToken = response.refreshToken,
            expiresAt = WearAuth.expiryFrom(response.expiresIn),
        )
        return response.accessToken
    }
}
