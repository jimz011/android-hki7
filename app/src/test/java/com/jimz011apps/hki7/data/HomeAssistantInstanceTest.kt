package com.jimz011apps.hki7.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAssistantInstanceTest {
    @Test
    fun primaryUrlPrefersExternalAndAuthenticationAcceptsRefreshOnly() {
        val instance = HomeAssistantInstance(
            id = "home-1",
            name = "Home",
            externalUrl = "https://example.ui.nabu.casa",
            internalUrl = "http://homeassistant.local:8123",
            refreshToken = "refresh"
        )

        assertEquals("https://example.ui.nabu.casa", instance.primaryUrl)
        assertTrue(instance.isAuthenticated)
        assertFalse(instance.copy(accessToken = null, refreshToken = null).isAuthenticated)
    }

    @Test
    fun oldNotificationHistoryDecodesWithoutInstanceMetadata() {
        val oldEntry = """{"id":"1","message":"Door opened","timestamp":42}"""
        val decoded = Json.decodeFromString<HKINotification>(oldEntry)

        assertEquals("Door opened", decoded.message)
        assertNull(decoded.instanceId)
        assertNull(decoded.instanceName)
    }
}
