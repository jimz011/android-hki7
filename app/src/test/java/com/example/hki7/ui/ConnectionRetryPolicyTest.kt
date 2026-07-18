package com.example.hki7.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionRetryPolicyTest {
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
}
