@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fully stops a streaming camera WebView so it no longer decodes video or holds the network once it
 * leaves the screen. Without this, a live-camera WebView keeps pulling its feed after the card
 * scrolls off or the dialog closes (until GC) — a real battery drain. Call from `AndroidView`'s
 * `onRelease`.
 */
fun WebView.teardownStream() {
    runCatching {
        stopLoading()
        loadUrl("about:blank")
        onPause()
        destroy()
    }
}

/**
 * Pauses a streaming camera WebView while the app is backgrounded and resumes it on return, so the
 * camera feed isn't decoded/downloaded when no one's looking. [webView] is read lazily so it can be
 * supplied after the [androidx.compose.ui.viewinterop.AndroidView] factory creates it.
 */
@Composable
fun WebViewLifecyclePause(webView: () -> WebView?) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView()?.onPause()
                Lifecycle.Event.ON_RESUME -> webView()?.onResume()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

fun resolveCameraUrl(url: String?, currentUrl: String): String? {
    val clean = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val base = currentUrl.removeSuffix("/")
    val normalized = when {
        clean.startsWith("url:", ignoreCase = true) -> clean.substringAfter(":").trim()
        clean.startsWith("webrtc:", ignoreCase = true) -> clean.substringAfter(":").trim()
        else -> clean
    }.takeIf { it.isNotBlank() } ?: return null

    return when {
        normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true) -> normalized
        normalized.startsWith("/") -> "$base$normalized"
        normalized.startsWith("camera.", ignoreCase = true) -> "$base/api/camera_proxy/$normalized"
        normalized.contains("/") -> "$base/$normalized"
        normalized.contains(".") && !normalized.contains(":") -> "$base/$normalized"
        // Treat plain aliases (e.g. poort_hd or url: poort_hd) as camera entity ids.
        else -> "$base/api/camera_proxy/camera.$normalized"
    }
}

/** Reserved icon-slug sentinel: render the entity's Home Assistant `entity_picture` instead of an
 *  MDI icon. Stored in the usual icon fields (HKIButtonConfig.icon, HKIBadge.customIcon, …). */
const val ENTITY_PICTURE_ICON = "entity_picture"

/** Absolute URL for an entity's `entity_picture` (relative HA paths are prefixed with the base URL),
 *  or null if the entity has no picture. */
fun resolveEntityPictureUrl(entity: HAEntity?, currentUrl: String): String? =
    resolveCameraUrl(entity?.entityPicture, currentUrl)

fun buildCameraRefreshModel(imageUrl: String?, refreshIntervalSeconds: Int, refreshTick: Int): String? {
    val baseUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (refreshIntervalSeconds <= 0) return baseUrl
    val separator = if (baseUrl.contains("?")) "&" else "?"
    return "${baseUrl}${separator}hki_refresh=$refreshTick"
}

fun resolveEntityCameraUrl(entity: HAEntity?, currentUrl: String, preferLive: Boolean): String? {
    val cameraEntity = entity ?: return null
    val base = currentUrl.removeSuffix("/")
    val token = cameraEntity.attributes?.get("access_token")?.jsonPrimitive?.contentOrNull
    if (preferLive) {
        val streamBase = "$base/api/camera_proxy_stream/${cameraEntity.entity_id}"
        return if (!token.isNullOrBlank()) "$streamBase?token=$token" else streamBase
    }

    val snapshot = cameraEntity.attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull
    return resolveCameraUrl(snapshot, currentUrl)
}

fun buildWebRtcApiUrl(source: String?, currentUrl: String): String? {
    val clean = source?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val base = currentUrl.removeSuffix("/")
    val normalized = when {
        clean.startsWith("url:", ignoreCase = true) -> clean.substringAfter(":").trim()
        clean.startsWith("webrtc:", ignoreCase = true) -> clean.substringAfter(":").trim()
        else -> clean
    }.takeIf { it.isNotBlank() } ?: return null

    if (normalized.startsWith("http://", ignoreCase = true) ||
        normalized.startsWith("https://", ignoreCase = true) ||
        normalized.startsWith("/")) {
        return resolveCameraUrl(normalized, currentUrl)
    }

    val entityId = when {
        normalized.startsWith("camera.", ignoreCase = true) -> normalized
        normalized.contains(".") && !normalized.contains(":") -> normalized
        else -> "camera.$normalized"
    }

    return "$base/api/camera_proxy_stream/$entityId"
}
