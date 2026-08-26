package com.jimz011apps.hki7.data

import java.net.ServerSocket
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MjpegHttpServerTest {
    @Test
    fun busyPortFailsWithoutMarkingRunning() {
        val blocker = ServerSocket(0)
        try {
            val port = blocker.localPort
            val failed = MjpegHttpServer(port)
            val result = failed.start()
            assertTrue(result.isFailure)
            assertFalse(failed.isRunning)

            blocker.close()
            val retry = MjpegHttpServer(port)
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
}
