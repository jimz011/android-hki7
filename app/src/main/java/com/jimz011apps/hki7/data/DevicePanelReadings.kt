package com.jimz011apps.hki7.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface

data class DevicePanelReadings(
    val interactive: Boolean,
    val brightness: Int,
    val wifiSsid: String,
    val localIp: String,
    val cameraUrl: String,
)

fun readDevicePanel(context: Context, settings: DevicePanelSettings): DevicePanelReadings {
    val ip = localIpv4(context)
    val cameraUrl = if (settings.cameraStreamEnabled) {
        settings.cameraStreamUrlOrEmpty(ip).ifBlank { "unavailable" }
    } else {
        "unavailable"
    }
    return DevicePanelReadings(
        interactive = isScreenInteractive(context),
        brightness = screenBrightness(context),
        wifiSsid = wifiSsid(context),
        localIp = ip.ifBlank { "unavailable" },
        cameraUrl = cameraUrl,
    )
}

fun localIpv4(context: Context): String {
    val fromLink = ipv4FromActiveNetwork(context)
    if (fromLink.isNotBlank()) return fromLink
    return ipv4FromInterfaces()
}

private fun isScreenInteractive(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isInteractive == true

private fun screenBrightness(context: Context): Int =
    runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(0).coerceIn(0, 255)

@Suppress("DEPRECATION")
private fun wifiSsid(context: Context): String {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return "unknown"
    return sanitizeSsid(wifi.connectionInfo?.ssid)
}

private fun ipv4FromActiveNetwork(context: Context): String {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return ""
    val network = cm.activeNetwork ?: return ""
    val properties = cm.getLinkProperties(network) ?: return ""
    return properties.linkAddresses
        .mapNotNull { it.address as? Inet4Address }
        .firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
        .orEmpty()
}

@SuppressLint("SoonBlockedPrivateApi")
private fun ipv4FromInterfaces(): String {
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
        .getOrDefault(emptyList())
    for (networkInterface in interfaces) {
        if (!networkInterface.isUp || networkInterface.isLoopback) continue
        val address = networkInterface.inetAddresses.toList()
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?: continue
        return address.hostAddress.orEmpty()
    }
    return ""
}

@Suppress("unused")
private val sdkGuard = Build.VERSION.SDK_INT
