package com.jimz011apps.hki7.data

import com.jimz011apps.hki7.BuildConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val backupTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

/** One readable name shared by manual, Google Drive, and Home Assistant backups. */
fun hki7BackupName(
    versionName: String = BuildConfig.VERSION_NAME,
    instant: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val safeVersion = versionName.trim().ifBlank { "unknown" }
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
    val timestamp = backupTimestampFormatter.withZone(zoneId).format(instant)
    return "HKI7-$safeVersion-$timestamp.json"
}
