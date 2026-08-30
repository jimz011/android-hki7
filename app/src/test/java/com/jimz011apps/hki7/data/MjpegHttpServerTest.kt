package com.jimz011apps.hki7.data

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MjpegHttpServerTest {
    @Test
    fun busyPortFailsWithoutMarkingRunning() {
        val blocker = ServerSocket(0)
        try {
            val port = blocker.localPort
            val failed = MjpegHttpServer(port, "secret")
            val result = failed.start()
            assertTrue(result.isFailure)
            assertFalse(failed.isRunning)

            blocker.close()
            val retry = MjpegHttpServer(port, "secret")
            try {
                assertTrue(retry.start().isSuccess)
                assertTrue(retry.isRunning)
            } finally {
                retry.stop()
            }
        } finally {
            runCatching { blocker.close() }
        }
    }

    @Test
    fun blankTokenRefusesToStart() {
        val server = MjpegHttpServer(0, "")
        assertTrue(server.start().isFailure)
        assertFalse(server.isRunning)
    }

    @Test
    fun missingTokenIsRejected() {
        val probe = ServerSocket(0)
        val port = probe.localPort
        probe.close()
        val server = MjpegHttpServer(port, "secret")
        try {
            assertTrue(server.start(InetAddress.getByName("127.0.0.1")).isSuccess)
            val conn = URL("http://127.0.0.1:$port/camera").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            assertEquals(401, conn.responseCode)
            conn.disconnect()
        } finally {
            server.stop()
        }
    }
}
