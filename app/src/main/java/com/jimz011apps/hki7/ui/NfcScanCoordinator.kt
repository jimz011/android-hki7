package com.jimz011apps.hki7.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal data class NfcConnectionSnapshot(
    val activeInstanceId: String?,
    val connectedInstanceId: String?,
    val connected: Boolean,
)

internal enum class NfcConnectionWaitResult { READY, INSTANCE_CHANGED, TIMED_OUT }

/** Waits for the client belonging to the instance that owned a tag tap. A later instance switch
 * cancels the request instead of allowing a queued scan to fire in a different Home Assistant. */
internal suspend fun awaitNfcConnection(
    targetInstanceId: String,
    snapshots: Flow<NfcConnectionSnapshot>,
    timeoutMillis: Long,
): NfcConnectionWaitResult = withTimeoutOrNull(timeoutMillis) {
    snapshots.first { snapshot ->
        snapshot.activeInstanceId != targetInstanceId ||
            (snapshot.connected && snapshot.connectedInstanceId == targetInstanceId)
    }.let { snapshot ->
        if (snapshot.activeInstanceId == targetInstanceId) {
            NfcConnectionWaitResult.READY
        } else {
            NfcConnectionWaitResult.INSTANCE_CHANGED
        }
    }
} ?: NfcConnectionWaitResult.TIMED_OUT
