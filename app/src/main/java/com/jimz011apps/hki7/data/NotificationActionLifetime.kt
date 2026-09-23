package com.jimz011apps.hki7.data

/** Retry briefly after a tap, never replay an old household action after a long offline stint. */
internal const val NOTIFICATION_ACTION_LIFETIME_MS = 5 * 60 * 1_000L

internal fun notificationActionRemainingMillis(requestedAtMillis: Long, nowMillis: Long): Long {
    // Old persisted jobs have no timestamp. A clock moved backwards also makes age unknowable.
    if (requestedAtMillis <= 0 || nowMillis < requestedAtMillis) return 0
    val age = nowMillis - requestedAtMillis
    return (NOTIFICATION_ACTION_LIFETIME_MS - age).coerceAtLeast(0)
}
