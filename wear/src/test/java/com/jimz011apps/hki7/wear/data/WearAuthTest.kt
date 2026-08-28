package com.jimz011apps.hki7.wear.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearAuthTest {

    @Test
    fun `authorization url carries the client id and no redirect`() {
        val url = WearAuth.authorizationUrl("https://home.example.com/")
        assertTrue(url.startsWith("https://home.example.com/auth/authorize?"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=https%3A%2F%2Fhome-assistant.io%2Fandroid"))
        // RemoteAuthClient appends both of these itself; ours would collide with them.
        assertTrue("must not set redirect_uri", !url.contains("redirect_uri"))
        assertTrue("must not set code_challenge", !url.contains("code_challenge"))
    }

    @Test
    fun `trailing slashes do not produce a double slash`() {
        assertTrue(
            WearAuth.authorizationUrl("https://x.test///")
                .startsWith("https://x.test/auth/authorize"),
        )
    }

    @Test
    fun `expiry is in the future but undercuts the stated lifetime`() {
        val now = System.currentTimeMillis()
        val expiry = WearAuth.expiryFrom(1800)
        assertTrue("expiry must be ahead of now", expiry > now)
        // A margin is subtracted so a slow request cannot land after the token has died.
        assertTrue("expiry must undercut the raw lifetime", expiry < now + 1800 * 1000L)
    }

    @Test
    fun `a bare host is assumed to be https`() {
        assertEquals("https://abc.ui.nabu.casa", WearRemoteAuth.normaliseUrl("abc.ui.nabu.casa"))
    }

    @Test
    fun `an explicit scheme is preserved`() {
        assertEquals(
            "http://192.168.1.4:8123",
            WearRemoteAuth.normaliseUrl("http://192.168.1.4:8123"),
        )
    }

    @Test
    fun `trailing slashes and padding are trimmed`() {
        assertEquals("https://x.test", WearRemoteAuth.normaliseUrl("  https://x.test/  "))
    }

    @Test
    fun `blank input has no url`() {
        assertNull(WearRemoteAuth.normaliseUrl("   "))
    }
}
