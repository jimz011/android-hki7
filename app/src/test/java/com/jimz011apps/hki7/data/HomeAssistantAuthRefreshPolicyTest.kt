package com.jimz011apps.hki7.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAssistantAuthRefreshPolicyTest {
    private val now = 1_000_000L

    @Test
    fun `expired and nearly expired sessions refresh before authenticated work`() {
        assertTrue(shouldRefreshBeforeAuthenticatedWork("refresh", now - 1L, now))
        assertTrue(shouldRefreshBeforeAuthenticatedWork("refresh", now + 60_000L, now))
    }

    @Test
    fun `healthy session connects without an unnecessary refresh`() {
        assertFalse(shouldRefreshBeforeAuthenticatedWork("refresh", now + 60_001L, now))
    }

    @Test
    fun `unknown expiry refreshes when a refresh token is available`() {
        assertTrue(shouldRefreshBeforeAuthenticatedWork("refresh", null, now))
        assertFalse(shouldRefreshBeforeAuthenticatedWork(null, null, now))
    }

    @Test
    fun `known expired access token without refresh requires login without connecting`() {
        assertTrue(isKnownExpiredWithoutRefreshToken("access", null, now, now))
        assertFalse(isKnownExpiredWithoutRefreshToken("access", "refresh", now, now))
        assertFalse(isKnownExpiredWithoutRefreshToken("access", null, now + 1L, now))
    }

    @Test
    fun `client token rejection is terminal but server failure remains retryable`() {
        assertTrue(TokenRefreshException(false, statusCode = 400).isClientRejection)
        assertTrue(TokenRefreshException(false, statusCode = 403).isClientRejection)
        assertFalse(TokenRefreshException(false, statusCode = 500).isClientRejection)
        assertFalse(TokenRefreshException(false).isClientRejection)
    }
}
