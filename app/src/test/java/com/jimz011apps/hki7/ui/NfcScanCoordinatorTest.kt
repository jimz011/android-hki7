package com.jimz011apps.hki7.ui

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NfcScanCoordinatorTest {
    @Test
    fun `cold-start scan waits for its instance to connect`() = runBlocking {
        val snapshots = flowOf(
            NfcConnectionSnapshot("home-a", null, false),
            NfcConnectionSnapshot("home-a", "home-a", true),
        )

        assertEquals(
            NfcConnectionWaitResult.READY,
            awaitNfcConnection("home-a", snapshots, 1_000),
        )
    }

    @Test
    fun `scan is cancelled instead of sent after an instance switch`() = runBlocking {
        val snapshots = flowOf(
            NfcConnectionSnapshot("home-a", null, false),
            NfcConnectionSnapshot("home-b", "home-b", true),
        )

        assertEquals(
            NfcConnectionWaitResult.INSTANCE_CHANGED,
            awaitNfcConnection("home-a", snapshots, 1_000),
        )
    }

    @Test
    fun `scan expires when connection never becomes ready`() = runBlocking {
        val snapshots = flow<NfcConnectionSnapshot> {
            emit(NfcConnectionSnapshot("home-a", null, false))
            kotlinx.coroutines.awaitCancellation()
        }

        assertEquals(
            NfcConnectionWaitResult.TIMED_OUT,
            awaitNfcConnection("home-a", snapshots, 10),
        )
    }
}
