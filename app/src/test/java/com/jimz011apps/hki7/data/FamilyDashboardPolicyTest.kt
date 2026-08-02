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
        assertTrue(policy.isEmpty)
    }

    @Test
    fun `either dashboard restriction makes a policy non-empty`() {
        assertFalse(Hki7Policy(allowDashboardSwitch = false).isEmpty)
        assertFalse(Hki7Policy(allowDashboardCreate = false).isEmpty)
    }
}
