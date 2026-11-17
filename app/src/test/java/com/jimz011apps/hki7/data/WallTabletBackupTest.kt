package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WallTabletBackupTest {
    @Test
    fun keepsSettingsAlreadyStoredForTheActiveServer() {
        val stored = mapOf("new" to "current", "old" to "backup")
        assertEquals(stored, adoptWallTabletSettings(stored, "new", setOf("new")))
    }

    @Test
    fun copiesTheOnlyBackupOntoTheNewInstall() {
        val stored = mapOf("old-install" to "screensaver")
        assertEquals(
            mapOf("old-install" to "screensaver", "new-install" to "screensaver"),
            adoptWallTabletSettings(stored, "new-install", setOf("new-install")),
        )
    }

    @Test
    fun doesNotGuessWhenSeveralServersWereBackedUp() {
        val stored = mapOf("home" to "a", "cabin" to "b")
        assertEquals(stored, adoptWallTabletSettings(stored, "new-install", setOf("new-install")))
    }

    @Test
    fun waitsUntilAServerExists() {
        val stored = mapOf("old-install" to "screensaver")
        assertEquals(stored, adoptWallTabletSettings(stored, null, emptySet()))
    }
}
