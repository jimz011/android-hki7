@file:Suppress("UnusedBoxWithConstraintsScope", "SetJavaScriptEnabled")

package com.example.hki7.ui.components

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.hki7.ui.theme.LocalHKIAppColors
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.pointer.pointerInput
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
                                                @Suppress("DEPRECATION")
                                                settings.setSupportZoom(true)
                                                settings.builtInZoomControls = true
                                                settings.displayZoomControls = false
                                                webViewClient = WebViewClient()
                                                val headers = if (!authToken.isNullOrBlank()) {
                                                    mapOf("Authorization" to "Bearer $authToken")
                                                } else {
                                                    emptyMap()
                                                }
                                                loadUrl(liveWebUrl, headers)
                                                streamWebView = this
                                            }
                                        },
                                        update = { webView ->
                                            if (webView.url != liveWebUrl) {
                                                val headers = if (!authToken.isNullOrBlank()) {
                                                    mapOf("Authorization" to "Bearer $authToken")
                                                } else {
                                                    emptyMap()
                                                }
                                                webView.loadUrl(liveWebUrl, headers)
                                            }
                                        },
                                        // Stop decoding/streaming when the dialog is dismissed.
                                        onRelease = { webView ->
                                            streamWebView = null
                                            webView.teardownStream()
                                        }
                                    )
                                } else {
                                    ZoomableCameraImage(imageUrl = resolvedModel, contentDescription = title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableCameraImage(imageUrl: String?, contentDescription: String) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offsetX by remember(imageUrl) { mutableFloatStateOf(0f) }
    var offsetY by remember(imageUrl) { mutableFloatStateOf(0f) }
    var lastSuccessfulModel by remember(imageUrl) { mutableStateOf(imageUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUrl) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.Fit,
                loading = {
                    val fallback = lastSuccessfulModel
                    if (!fallback.isNullOrBlank()) {
                        AsyncImage(
                            model = fallback,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                success = {
                    lastSuccessfulModel = imageUrl
                    SubcomposeAsyncImageContent()
                }
            )
        } else {
            Text("No Stream Available", color = Color.Gray)
        }
    }
}
