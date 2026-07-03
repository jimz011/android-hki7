package com.example.hki7.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks the currently connected Wi-Fi SSID so the app can switch between an internal (local) and
 * external Home Assistant URL, like the official app. Reading the SSID requires location permission;
 * when it isn't available the SSID is null and the app falls back to the external URL.
 */
class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)

    private val _currentSsid = MutableStateFlow(readSsid())
    val currentSsid: StateFlow<String?> = _currentSsid

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _currentSsid.value = readSsid() }
        override fun onLost(network: Network) { _currentSsid.value = readSsid() }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _currentSsid.value = readSsid()
        }
    }

    init {
        runCatching {
            connectivityManager?.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                callback
            )
        }
    }

    private fun readSsid(): String? = currentWifiSsid(appContext)
}

/** Reads the currently connected Wi-Fi SSID, or null if unavailable (no permission / not on Wi-Fi). */
@Suppress("DEPRECATION")
fun currentWifiSsid(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val info = runCatching { wifiManager.connectionInfo }.getOrNull() ?: return null
    val ssid = info.ssid ?: return null
    return ssid.removePrefix("\"").removeSuffix("\"")
        .takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }
}

/** Picks the internal URL when on a configured home Wi-Fi, otherwise the external/primary URL. */
fun resolveHomeAssistantUrl(external: String?, internal: String?, homeSsids: List<String>, ssid: String?): String? {
    if (!internal.isNullOrBlank() && ssid != null && homeSsids.any { it.equals(ssid, ignoreCase = true) }) {
        return internal
    }
    return external
}
