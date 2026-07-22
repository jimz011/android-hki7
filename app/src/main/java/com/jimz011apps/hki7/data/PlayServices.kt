package com.jimz011apps.hki7.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Awaits a Play Services [Task] for its result, resolving to null on failure/cancellation. */
suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
    addOnCanceledListener { if (cont.isActive) cont.resume(null) }
}

/** Awaits a Play Services [Task] for completion, returning whether it succeeded. Use for `Task<Void>`
 *  (e.g. geofence add/remove) where the result value is null even on success. */
suspend fun Task<*>.awaitSuccess(): Boolean = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(true) }
    addOnFailureListener { if (cont.isActive) cont.resume(false) }
    addOnCanceledListener { if (cont.isActive) cont.resume(false) }
}
