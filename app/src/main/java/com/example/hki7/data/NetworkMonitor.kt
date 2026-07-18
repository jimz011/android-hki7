package com.example.hki7.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.URI
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

    // An SSID value cannot distinguish a live connection from disconnecting and reconnecting to
    // that same Wi-Fi. Track the actual Network object so stale sockets can be replaced immediately.
    private val _networkGeneration = MutableStateFlow(0L)
    val networkGeneration: StateFlow<Long> = _networkGeneration
    private val _networkChangeGeneration = MutableStateFlow(0L)
    val networkChangeGeneration: StateFlow<Long> = _networkChangeGeneration
    private var activeWifiNetwork: Network? = connectivityManager?.activeNetwork?.takeIf { network ->
        connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    @Synchronized
    private fun updateWifiNetwork(network: Network?) {
        if (activeWifiNetwork == network) return
        activeWifiNetwork = network
        _networkGeneration.value += 1
    }

    @Synchronized
    private fun signalNetworkChange() {
        _networkChangeGeneration.value += 1
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _currentSsid.value = readSsid()
            updateWifiNetwork(network)
            signalNetworkChange()
        }
        override fun onLost(network: Network) {
            if (network == activeWifiNetwork) {
                _currentSsid.value = null
                updateWifiNetwork(null)
            } else {
                _currentSsid.value = readSsid()
            }
            signalNetworkChange()
        }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _currentSsid.value = readSsid()
            signalNetworkChange()
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

enum class HomeAssistantConnectionRoute(val displayName: String) {
    LOCAL("Local"),
    NABU_CASA("Nabu Casa"),
    EXTERNAL("External")
}

/** Classifies the endpoint that actually completed an authenticated Home Assistant request. */
internal fun classifyHomeAssistantConnectionRoute(
    activeUrl: String,
    internalUrl: String?,
    connectedViaLocalAddress: Boolean
): HomeAssistantConnectionRoute {
    if (internalUrl?.trim()?.trimEnd('/')
            ?.equals(activeUrl.trim().trimEnd('/'), ignoreCase = true) == true
    ) return HomeAssistantConnectionRoute.LOCAL

    val host = runCatching { URI(activeUrl).host.orEmpty().lowercase() }.getOrDefault("")
    if (host == "nabu.casa" || host.endsWith(".nabu.casa")) {
        return HomeAssistantConnectionRoute.NABU_CASA
    }
    return if (connectedViaLocalAddress || isLikelyLocalHomeAssistantUrl(activeUrl)) {
        HomeAssistantConnectionRoute.LOCAL
    } else {
        HomeAssistantConnectionRoute.EXTERNAL
    }
}

internal fun isLocalNetworkAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress ||
        address.isLinkLocalAddress || address.isSiteLocalAddress
    ) return true
    val host = address.hostAddress.orEmpty().substringBefore('%').lowercase()
    return host.startsWith("fc") || host.startsWith("fd")
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

/** True for addresses that are normally reachable only from the user's LAN. */
internal fun isLikelyLocalHomeAssistantUrl(url: String): Boolean {
    val host = runCatching { URI(url).host?.lowercase()?.removeSurrounding("[", "]") }.getOrNull() ?: return false
    if (host == "localhost" || host.endsWith(".local")) return true
    if (host == "::1" || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")) return true

    val octets = host.split('.').mapNotNull { it.toIntOrNull() }
    if (octets.size != 4) return false
    return octets[0] == 10 ||
        (octets[0] == 172 && octets[1] in 16..31) ||
        (octets[0] == 192 && octets[1] == 168) ||
        octets[0] == 127
}
