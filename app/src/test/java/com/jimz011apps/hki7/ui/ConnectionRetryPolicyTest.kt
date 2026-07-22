package com.jimz011apps.hki7.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionRetryPolicyTest {
    @Test
    fun `dashboard generation gets a ten second reconnect screen grace period`() {
        assertEquals(10_000L, connectionIssueGraceMillis(isAutoGenerating = true, hasConnectedOnce = false))
        assertEquals(10_000L, connectionIssueGraceMillis(isAutoGenerating = true, hasConnectedOnce = true))
        assertEquals(3_000L, connectionIssueGraceMillis(isAutoGenerating = false, hasConnectedOnce = true))
        assertEquals(0L, connectionIssueGraceMillis(isAutoGenerating = false, hasConnectedOnce = false))
    }

    @Test
    fun `first two failures remain in reconnecting banner state`() {
        assertEquals(ConnectionStatus.CONNECTING, connectionStatusAfterFailures(1))
        assertEquals(ConnectionStatus.CONNECTING, connectionStatusAfterFailures(2))
    }

    @Test
    fun `third consecutive failure shows actionable error state`() {
        assertEquals(ConnectionStatus.ERROR, connectionStatusAfterFailures(3))
        assertEquals(ConnectionStatus.ERROR, connectionStatusAfterFailures(4))
    }

    @Test
    fun `silent safety refresh does not replace a healthy connected ui`() {
        assertEquals(true, preserveConnectedUiAfterRefreshFailure(true, ConnectionStatus.CONNECTED))
        assertEquals(false, preserveConnectedUiAfterRefreshFailure(false, ConnectionStatus.CONNECTED))
        assertEquals(false, preserveConnectedUiAfterRefreshFailure(true, ConnectionStatus.CONNECTING))
    }

    @Test
    fun `expected restart has a dedicated status instead of generic reconnecting`() {
        assertEquals(
            "Home Assistant is restarting…",
            homeAssistantConnectionStatusLabel(
                ConnectionStatus.CONNECTING,
                HomeAssistantRestartPhase.RESTARTING,
                isAutoGenerating = false
            )
        )
        assertEquals(
            "Reconnecting to Home Assistant…",
            homeAssistantConnectionStatusLabel(
                ConnectionStatus.CONNECTING,
                HomeAssistantRestartPhase.NONE,
                isAutoGenerating = false
            )
        )
    }

    @Test
    fun `restart phase reports stopping starting and restoring`() {
        assertEquals(
            "Stopping for restart…",
            homeAssistantConnectionStatusLabel(ConnectionStatus.CONNECTING, HomeAssistantRestartPhase.STOPPING, false)
        )
        assertEquals(
            "Home Assistant is starting…",
            homeAssistantConnectionStatusLabel(ConnectionStatus.CONNECTING, HomeAssistantRestartPhase.STARTING, false)
        )
        assertEquals(
            "Restoring your dashboard…",
            homeAssistantConnectionStatusLabel(ConnectionStatus.CONNECTED, HomeAssistantRestartPhase.RESTORING, false)
        )
    }
}
