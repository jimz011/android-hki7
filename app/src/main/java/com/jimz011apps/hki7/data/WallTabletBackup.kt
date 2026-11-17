package com.jimz011apps.hki7.data

/**
 * Wall-tablet settings are stored per Home Assistant instance id. A reinstall creates a new id,
 * so a restored backup still holds the screensaver, camera popups and device camera under the id
 * from the previous install. When the active server has no copy and the backup holds exactly one
 * server's settings, keep those settings on the server the user just signed in to.
 */
fun <V> adoptWallTabletSettings(
    stored: Map<String, V>,
    activeInstanceId: String?,
    knownInstanceIds: Set<String>,
): Map<String, V> {
    if (activeInstanceId.isNullOrBlank() || stored.isEmpty() || stored.containsKey(activeInstanceId)) {
        return stored
    }
    val orphans = stored.filterKeys { it !in knownInstanceIds }
    val source = when {
        orphans.size == 1 -> orphans.values.first()
        stored.size == 1 -> stored.values.first()
        else -> return stored
    }
    return stored + (activeInstanceId to source)
}
