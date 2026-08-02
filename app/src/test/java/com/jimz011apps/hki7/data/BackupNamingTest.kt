package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BackupNamingTest {
    @Test
    fun includesAppVersionAndLocalTimestamp() {
        assertEquals(
            "HKI7-1.0.0-beta.11-2026-08-02_14-35-06.json",
            hki7BackupName(
                versionName = "1.0.0-beta.11",
                instant = Instant.parse("2026-08-02T12:35:06Z"),
                zoneId = ZoneId.of("Europe/Amsterdam"),
            ),
        )
    }

    @Test
    fun sanitizesVersionForStorageProviders() {
        assertEquals(
            "HKI7-beta-11-test-2026-01-01_00-00-00.json",
            hki7BackupName(
                versionName = "beta 11/test",
                instant = Instant.parse("2026-01-01T00:00:00Z"),
                zoneId = ZoneId.of("UTC"),
            ),
        )
    }
}
