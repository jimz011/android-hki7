package com.jimz011apps.hki7.data

import kotlinx.serialization.Serializable

const val DEVICE_CAMERA_FRONT = "front"
const val DEVICE_CAMERA_BACK = "back"
const val DEFAULT_DEVICE_CAMERA_STREAM_PORT = 2971
const val MIN_DEVICE_CAMERA_STREAM_PORT = 1024
const val MAX_DEVICE_CAMERA_STREAM_PORT = 65535

@Serializable
data class DevicePanelSettings(
    val extraSensorsEnabled: Boolean = true,
    val cameraStreamEnabled: Boolean = false,
    val cameraFacing: String = DEVICE_CAMERA_FRONT,
    val streamPort: Int = DEFAULT_DEVICE_CAMERA_STREAM_PORT,
)

@Serializable
data class DevicePanelStore(
    val byInstanceId: Map<String, DevicePanelSettings> = emptyMap(),
)

fun DevicePanelSettings.clampedStreamPort(): Int =
    streamPort.coerceIn(MIN_DEVICE_CAMERA_STREAM_PORT, MAX_DEVICE_CAMERA_STREAM_PORT)

fun DevicePanelSettings.isFrontCamera(): Boolean = cameraFacing != DEVICE_CAMERA_BACK

fun sanitizeSsid(raw: String?): String {
    val trimmed = raw?.trim()?.removeSurrounding("\"")?.trim().orEmpty()
    if (trimmed.isEmpty() ||
        trimmed.equals("<unknown ssid>", ignoreCase = true) ||
        trimmed.equals("unknown ssid", ignoreCase = true)
    ) {
        return "unknown"
    }
    return trimmed
}

fun cameraStreamUrl(ip: String, port: Int): String =
    "http://$ip:${port.coerceIn(MIN_DEVICE_CAMERA_STREAM_PORT, MAX_DEVICE_CAMERA_STREAM_PORT)}/camera"

fun DevicePanelSettings.cameraStreamUrlOrEmpty(ip: String): String {
    if (ip.isBlank()) return ""
    return cameraStreamUrl(ip, clampedStreamPort())
}

internal fun sensorsRegistrationMarker(
    webhookId: String,
    extraSensorsEnabled: Boolean,
    cameraStreamEnabled: Boolean,
    revision: Int,
): String {
    val extra = if (extraSensorsEnabled) "x" else ""
    val camera = if (cameraStreamEnabled) "c" else ""
    return "$webhookId@$revision$extra$camera"
}
