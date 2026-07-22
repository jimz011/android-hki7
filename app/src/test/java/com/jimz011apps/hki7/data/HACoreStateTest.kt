package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HACoreStateTest {
    @Test
    fun `parses every Home Assistant core lifecycle state`() {
        assertEquals(HACoreState.NOT_RUNNING, HACoreState.fromApiValue("NOT_RUNNING"))
        assertEquals(HACoreState.STARTING, HACoreState.fromApiValue("STARTING"))
        assertEquals(HACoreState.RUNNING, HACoreState.fromApiValue("RUNNING"))
        assertEquals(HACoreState.STOPPING, HACoreState.fromApiValue("STOPPING"))
        assertEquals(HACoreState.FINAL_WRITE, HACoreState.fromApiValue("FINAL_WRITE"))
        assertEquals(HACoreState.STOPPED, HACoreState.fromApiValue("STOPPED"))
    }

    @Test
    fun `unknown core state is safe and case insensitive`() {
        assertEquals(HACoreState.STARTING, HACoreState.fromApiValue(" starting "))
        assertEquals(HACoreState.UNKNOWN, HACoreState.fromApiValue("future_state"))
        assertEquals(HACoreState.UNKNOWN, HACoreState.fromApiValue(null))
    }
}
