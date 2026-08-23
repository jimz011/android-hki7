package com.jimz011apps.hki7.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePanelTest {
    @Test
    fun streamPortIsClamped() {
        assertEquals(1024, DevicePanelSettings(streamPort = 80).clampedStreamPort())
        assertEquals(65535, DevicePanelSettings(streamPort = 99_999).clampedStreamPort())
        assertEquals(2971, DevicePanelSettings().clampedStreamPort())
    }

    @Test
    fun frontCameraIsTheDefault() {
        assertTrue(DevicePanelSettings().isFrontCamera())
        assertFalse(DevicePanelSettings(cameraFacing = DEVICE_CAMERA_BACK).isFrontCamera())
    }

    @Test
    fun cameraUrlUsesClampedPort() {
        assertEquals("http://10.0.0.8:2971/camera", cameraStreamUrl("10.0.0.8", 2971))
        assertEquals("http://10.0.0.8:1024/camera", cameraStreamUrl("10.0.0.8", 80))
        assertEquals("", DevicePanelSettings().cameraStreamUrlOrEmpty(""))
        assertEquals(
            "http://10.0.0.8:2971/camera",
            DevicePanelSettings().cameraStreamUrlOrEmpty("10.0.0.8"),
        )
    }

    @Test
    fun ssidQuotesAreStripped() {
        assertEquals("Home", sanitizeSsid("\"Home\""))
        assertEquals("unknown", sanitizeSsid("<unknown ssid>"))
        assertEquals("unknown", sanitizeSsid(null))
    }

    @Test
    fun registrationMarkerTracksToggles() {
        assertEquals(
            "abc@4",
            sensorsRegistrationMarker("abc", extraSensorsEnabled = false, cameraStreamEnabled = false, revision = 4),
        )
        assertEquals(
            "abc@4x",
            sensorsRegistrationMarker("abc", extraSensorsEnabled = true, cameraStreamEnabled = false, revision = 4),
        )
        assertEquals(
            "abc@4xc",
            sensorsRegistrationMarker("abc", extraSensorsEnabled = true, cameraStreamEnabled = true, revision = 4),
        )
    }
}
