package com.example.hki7.data

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMonitorTest {
    @Test
    fun `recognizes local Home Assistant addresses`() {
        assertTrue(isLikelyLocalHomeAssistantUrl("http://homeassistant.local:8123"))
        assertTrue(isLikelyLocalHomeAssistantUrl("http://homeassistant:8123"))
        assertTrue(isLikelyLocalHomeAssistantUrl("http://192.168.1.20:8123"))
        assertTrue(isLikelyLocalHomeAssistantUrl("http://172.20.0.5:8123"))
        assertTrue(isLikelyLocalHomeAssistantUrl("http://[fd12::20]:8123"))
    }

    @Test
    fun `does not classify remote addresses as local`() {
        assertFalse(isLikelyLocalHomeAssistantUrl("https://example.ui.nabu.casa"))
        assertFalse(isLikelyLocalHomeAssistantUrl("https://ha.example.com"))
        assertFalse(isLikelyLocalHomeAssistantUrl("http://172.32.0.5:8123"))
    }

    @Test
    fun `local first setup works without an external URL or SSID permission`() {
        assertEquals(
            "http://homeassistant.local:8123",
            resolveHomeAssistantUrl(
                external = null,
                internal = "http://homeassistant.local:8123",
                homeSsids = emptyList(),
                ssid = null
            )
        )
    }

    @Test
    fun `onboarding stores local and remote endpoints in separate slots`() {
        assertEquals(
            HomeAssistantConnectionUrls(null, "http://192.168.1.20:8123"),
            splitHomeAssistantConnectionUrl("http://192.168.1.20:8123/")
        )
        assertEquals(
            HomeAssistantConnectionUrls("https://example.ui.nabu.casa", null),
            splitHomeAssistantConnectionUrl("https://example.ui.nabu.casa/")
        )
    }

    @Test
    fun `classifies selected and observed connection routes`() {
        assertEquals(
            HomeAssistantConnectionRoute.LOCAL,
            classifyHomeAssistantConnectionRoute(
                "http://192.168.1.20:8123",
                "http://192.168.1.20:8123/",
                connectedViaLocalAddress = true
            )
        )
        assertEquals(
            HomeAssistantConnectionRoute.NABU_CASA,
            classifyHomeAssistantConnectionRoute(
                "https://example.ui.nabu.casa",
                internalUrl = null,
                connectedViaLocalAddress = false
            )
        )
        assertEquals(
            HomeAssistantConnectionRoute.LOCAL,
            classifyHomeAssistantConnectionRoute(
                "https://ha.example.com",
                internalUrl = null,
                connectedViaLocalAddress = true
            )
        )
        assertEquals(
            HomeAssistantConnectionRoute.EXTERNAL,
            classifyHomeAssistantConnectionRoute(
                "https://ha.example.com",
                internalUrl = null,
                connectedViaLocalAddress = false
            )
        )
    }

    @Test
    fun `recognizes private socket addresses`() {
        assertTrue(isLocalNetworkAddress(InetAddress.getByName("192.168.1.20")))
        assertTrue(isLocalNetworkAddress(InetAddress.getByName("fd12::20")))
        assertFalse(isLocalNetworkAddress(InetAddress.getByName("8.8.8.8")))
    }
}
