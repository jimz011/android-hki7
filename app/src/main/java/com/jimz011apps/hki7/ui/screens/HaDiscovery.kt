package com.jimz011apps.hki7.ui.screens

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/** A Home Assistant instance found on the local network via mDNS/Zeroconf. */
data class DiscoveredHa(val name: String, val baseUrl: String)

/** HA advertises this Bonjour service type on the LAN (the official app discovers the same way). */
private const val HA_SERVICE_TYPE = "_home-assistant._tcp"

/**
 * Discovers Home Assistant instances on the local network using Android's NSD (mDNS) while [active]
 * is true, mirroring how the official companion app auto-finds servers during onboarding. Returns a
 * live, de-duplicated list. Discovery + multicast lock are torn down automatically when this leaves
 * composition or [active] becomes false.
 */
@Composable
fun rememberHaDiscovery(active: Boolean): SnapshotStateList<DiscoveredHa> {
    val context = LocalContext.current.applicationContext
    val results = remember { mutableStateListOf<DiscoveredHa>() }

    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose { }

        results.clear()
        val nsd = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        // Some devices drop multicast (mDNS) packets unless a multicast lock is held.
        val multicastLock = runCatching {
            wifi?.createMulticastLock("hki7-mdns")?.apply { setReferenceCounted(true); acquire() }
        }.getOrNull()

        val mainHandler = Handler(Looper.getMainLooper())
        val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
        val resolving = AtomicBoolean(false)

        fun publish(info: NsdServiceInfo) {
            val attrs = info.attributes ?: emptyMap()
            fun attr(key: String) = attrs[key]?.let { runCatching { String(it) }.getOrNull() }?.takeIf { it.isNotBlank() }
            val base = attr("base_url")
                ?: attr("internal_url")
                ?: runCatching { info.hostAddresses.firstOrNull()?.hostAddress }.getOrNull()?.let { "http://$it:${info.port}" }
                ?: return
            val name = info.serviceName?.takeIf { it.isNotBlank() } ?: "Home Assistant"
            mainHandler.post {
                if (results.none { it.baseUrl.equals(base, ignoreCase = true) }) {
                    results.add(DiscoveredHa(name, base.removeSuffix("/")))
                }
            }
        }

        fun resolveNext() {
            if (nsd == null) return
            if (!resolving.compareAndSet(false, true)) return
            val next = resolveQueue.poll()
            if (next == null) { resolving.set(false); return }
            @Suppress("DEPRECATION")
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    resolving.set(false); resolveNext()
                }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    publish(serviceInfo)
                    resolving.set(false); resolveNext()
                }
            })
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolveQueue.add(serviceInfo)
                resolveNext()
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        runCatching { nsd?.discoverServices(HA_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener) }

        onDispose {
            runCatching { nsd?.stopServiceDiscovery(discoveryListener) }
            runCatching { multicastLock?.release() }
        }
    }

    return results
}
