@file:Suppress("UnusedBoxWithConstraintsScope", "SetJavaScriptEnabled")

package com.example.hki7.ui.components

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun HKICameraDialog(
    title: String,
    imageUrl: String?,
    refreshIntervalSeconds: Int = 0,
    liveWebUrl: String? = null,
    authToken: String? = null,
    statusText: String? = null,
    entity: HAEntity? = null,
    viewModel: MainViewModel? = null,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var refreshTick by remember(imageUrl, refreshIntervalSeconds) { mutableIntStateOf(0) }
    // Pause the live stream while backgrounded; destroyed via onRelease when the dialog closes.
    var streamWebView by remember { mutableStateOf<WebView?>(null) }
    WebViewLifecyclePause { streamWebView }
    LaunchedEffect(imageUrl, refreshIntervalSeconds, liveWebUrl) {
        refreshTick = 0
        if (!imageUrl.isNullOrBlank() && refreshIntervalSeconds > 0 && liveWebUrl.isNullOrBlank()) {
            while (true) {
                delay(refreshIntervalSeconds.seconds)
                refreshTick += 1
            }
        }
    }
    val resolvedModel = remember(imageUrl, refreshTick, refreshIntervalSeconds) {
        buildCameraRefreshModel(imageUrl, refreshIntervalSeconds, refreshTick)
    }
    if (entity != null && viewModel != null) {
        HKIDialog(
            entity = entity,
            viewModel = viewModel,
            onDismiss = onDismiss,
            icon = Icons.Default.CameraAlt,
            titleOverride = title,
            statusText = statusText ?: if (imageUrl.isNullOrBlank()) "No stream available" else "Live",
            showHistoryButton = true
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black
                ) {
                    CameraViewer(
                        imageUrl = resolvedModel,
                        liveWebUrl = liveWebUrl,
                        authToken = authToken,
                        title = title,
                        onWebViewChanged = { streamWebView = it }
                    )
                }
            }
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clip(RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            val isPhone = maxWidth < 600.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxSize(if (isPhone) 0.78f else 0.85f)
                    .windowInsetsPadding(WindowInsets.statusBars),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.elevated)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = appColors.elevated
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = appColors.onMuted)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleLarge, color = appColors.onSurface)
                                Text(statusText ?: if (imageUrl.isNullOrBlank()) "No stream available" else "Live", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .background(appColors.elevated.copy(alpha = 0.85f), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = appColors.onSurface)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black
                            ) {
                                CameraViewer(resolvedModel, liveWebUrl, authToken, title) { streamWebView = it }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraViewer(
    imageUrl: String?,
    liveWebUrl: String?,
    authToken: String?,
    title: String,
    onWebViewChanged: (WebView?) -> Unit
) {
    if (!liveWebUrl.isNullOrBlank()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_POINTER_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP -> { view.parent?.requestDisallowInterceptTouchEvent(false); view.performClick() }
                            MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                    webViewClient = WebViewClient()
                    loadUrl(liveWebUrl, cameraHeaders(authToken))
                    onWebViewChanged(this)
                }
            },
            update = { if (it.url != liveWebUrl) it.loadUrl(liveWebUrl, cameraHeaders(authToken)) },
            onRelease = {
                onWebViewChanged(null)
                it.teardownStream()
            }
        )
    } else {
        ZoomableCameraImage(imageUrl, title)
    }
}

private fun cameraHeaders(authToken: String?): Map<String, String> =
    if (authToken.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $authToken")

@Composable
fun ZoomableCameraImage(imageUrl: String?, contentDescription: String) {
    val hostView = LocalView.current
    // Refresh ticks only change the hki_refresh query param. Key zoom/pan and the last good frame
    // on the URL without it, so a snapshot refresh keeps showing the previous (memory-cached)
    // frame while the new one loads — a seamless swap instead of a flash — and zoom survives it.
    val stableKey = imageUrl?.substringBefore("hki_refresh=")?.trimEnd('?', '&')
    var scale by remember(stableKey) { mutableFloatStateOf(1f) }
    var offsetX by remember(stableKey) { mutableFloatStateOf(0f) }
    var offsetY by remember(stableKey) { mutableFloatStateOf(0f) }
    var lastSuccessfulModel by remember(stableKey) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_POINTER_DOWN -> hostView.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> hostView.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            .pointerInput(stableKey) {
                // Hand-rolled transform handling: only claim events for a pinch (2+ fingers) or a
                // pan while zoomed in. Unzoomed single-finger drags stay unconsumed so the list
                // around a snapshot camera card keeps scrolling (live cards use a WebView, which
                // never had this problem).
                awaitEachGesture {
                    var pinching = false
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) pinching = true
                        if (pinching || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            // Two persistent layers: the last good frame always stays visible underneath while the
            // next refresh tick loads invisibly on top and only covers it once fully decoded. This
            // avoids the blank recomposition frames a single reloading image shows on every tick.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                lastSuccessfulModel?.let { fallback ->
                    AsyncImage(
                        model = fallback,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { lastSuccessfulModel = imageUrl }
                )
            }
        } else {
            Text("No Stream Available", color = Color.Gray)
        }
    }
}
