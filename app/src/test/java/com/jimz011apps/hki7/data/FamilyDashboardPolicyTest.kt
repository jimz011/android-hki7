package com.jimz011apps.hki7.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyDashboardPolicyTest {
    @Test
    fun `existing policies default to unrestricted dashboard access`() {
        val policy = Hki7Policy()

        assertTrue(policy.allowDashboardSwitch)
        assertTrue(policy.allowDashboardCreate)
        assertTrue(policy.allowReimport)
        assertTrue(policy.isEmpty)
    }

    @Test
    fun `either dashboard restriction makes a policy non-empty`() {
        assertFalse(Hki7Policy(allowDashboardSwitch = false).isEmpty)
        assertFalse(Hki7Policy(allowDashboardCreate = false).isEmpty)
        assertFalse(Hki7Policy(allowReimport = false).isEmpty)
    }

    @Test
    fun `empty search lists allow every entity`() {
        val policy = Hki7Policy()

        assertTrue(policy.canSearchEntity("light.kitchen"))
        assertTrue(policy.canSearchEntity("sensor.temperature"))
    }

    @Test
    fun `visible search list acts as an allow-list`() {
        val policy = Hki7Policy(
            visibleSearchDomains = listOf("light"),
            visibleSearchEntityIds = listOf("sensor.outdoor_temperature"),
        )

        assertTrue(policy.canSearchEntity("light.kitchen"))
        assertTrue(policy.canSearchEntity("sensor.outdoor_temperature"))
        assertFalse(policy.canSearchEntity("sensor.indoor_temperature"))
    }

    @Test
    fun `invisible search selections override visible selections`() {
        val policy = Hki7Policy(
            visibleSearchDomains = listOf("light", "sensor"),
            hiddenSearchDomains = listOf("sensor"),
            hiddenSearchEntityIds = listOf("light.bedroom"),
        )

        assertTrue(policy.canSearchEntity("light.kitchen"))
        assertFalse(policy.canSearchEntity("light.bedroom"))
        assertFalse(policy.canSearchEntity("sensor.temperature"))
        assertFalse(policy.isEmpty)
    }
}
