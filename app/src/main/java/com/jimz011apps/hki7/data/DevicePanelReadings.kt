package com.jimz011apps.hki7.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

/** Site-local IPv4 on a real LAN interface, skipping VPN/tunnels so the MJPEG URL is reachable. */
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

private fun wifiSsid(context: Context): String =
    currentWifiSsid(context) ?: "unavailable"

private fun ipv4FromActiveNetwork(context: Context): String {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return ""
    val network = cm.activeNetwork ?: return ""
    val capabilities = cm.getNetworkCapabilities(network) ?: return ""
    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return ""
    val properties = cm.getLinkProperties(network) ?: return ""
    return properties.linkAddresses
        .mapNotNull { it.address as? Inet4Address }
        .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress
        .orEmpty()
}

@SuppressLint("SoonBlockedPrivateApi")
private fun ipv4FromInterfaces(): String {
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
        .getOrDefault(emptyList())
        .sortedBy { vpnInterfaceSort(it.name) }
    for (networkInterface in interfaces) {
        if (!networkInterface.isUp || networkInterface.isLoopback) continue
        if (isTunnelInterface(networkInterface.name)) continue
        val address = networkInterface.inetAddresses.toList()
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?: continue
        return address.hostAddress.orEmpty()
    }
    return ""
}

private fun isTunnelInterface(name: String): Boolean {
    val n = name.lowercase()
    return n.startsWith("tun") || n.startsWith("ppp") || n.startsWith("wg") ||
        n.startsWith("rmnet") || n.contains("vpn")
}

private fun vpnInterfaceSort(name: String): Int {
    val n = name.lowercase()
    if (n.startsWith("wlan") || n.startsWith("eth") || n.startsWith("ap")) return 0
    if (isTunnelInterface(n)) return 2
    return 1
}

@Suppress("unused")
private val sdkGuard = Build.VERSION.SDK_INT
