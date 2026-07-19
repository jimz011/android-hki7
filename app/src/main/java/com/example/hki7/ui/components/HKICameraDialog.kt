@file:Suppress("UnusedBoxWithConstraintsScope", "SetJavaScriptEnabled")

package com.example.hki7.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal data class CameraFullscreenRequest(
    val title: String,
    val imageUrl: String?,
    val liveWebUrl: String?,
    val authToken: String?,
    val onPrevious: (() -> Unit)?,
    val onNext: (() -> Unit)?,
    val positionText: String?,
    val onClosed: () -> Unit
)

internal val LocalCameraFullscreenLauncher =
    staticCompositionLocalOf<((CameraFullscreenRequest) -> Unit)?> { null }

@Composable
fun HKICameraDialog(
    title: String,
    imageUrl: String?,
    liveWebUrl: String? = null,
    authToken: String? = null,
    statusText: String? = null,
    entity: HAEntity? = null,
    viewModel: MainViewModel? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    positionText: String? = null,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    val fullscreenLauncher = LocalCameraFullscreenLauncher.current
    // Pause the live stream while backgrounded; destroyed via onRelease when the dialog closes.
    var streamWebView by remember { mutableStateOf<WebView?>(null) }
    WebViewLifecyclePause { streamWebView }
    // Refresh intervals belong to cards only. Dialog and fullscreen viewers always receive the
    // same live source and never run a snapshot refresh loop.
    val resolvedModel = imageUrl

    LaunchedEffect(
        isFullscreen,
        fullscreenLauncher,
        title,
        resolvedModel,
        liveWebUrl,
        authToken,
        onPrevious,
        onNext,
        positionText
    ) {
        if (isFullscreen && fullscreenLauncher != null) {
            fullscreenLauncher(
                CameraFullscreenRequest(
                    title = title,
                    imageUrl = resolvedModel,
                    liveWebUrl = liveWebUrl,
                    authToken = authToken,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    positionText = positionText,
                    onClosed = { isFullscreen = false }
                )
            )
        }
    }

    if (isFullscreen) {
        // MainApp owns the fullscreen window so a screen/dialog re-layout during rotation cannot
        // dispose it. Keep the local fallback for isolated previews and alternate hosts.
        if (fullscreenLauncher == null) {
            FullscreenCameraViewer(
                title = title,
                imageUrl = resolvedModel,
                liveWebUrl = liveWebUrl,
                authToken = authToken,
                onPrevious = onPrevious,
                onNext = onNext,
                positionText = positionText,
                onWebViewChanged = { streamWebView = it },
                onExitFullscreen = { isFullscreen = false }
            )
        }
        return
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
                    shape = itemCornerShape(),
                    color = Color.Black
                ) {
                    CameraViewerWithFullscreenButton(
                        imageUrl = resolvedModel,
                        liveWebUrl = liveWebUrl,
                        authToken = authToken,
                        title = title,
                        onWebViewChanged = { streamWebView = it },
                        onFullscreen = { isFullscreen = true }
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
                colors = CardDefaults.cardColors(
                    containerColor = appColors.elevated,
                    contentColor = appColors.onSurface
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                appColors.elevated,
                                appColors.elevated
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(17.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = appColors.onMuted)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                                Text(statusText ?: if (imageUrl.isNullOrBlank()) "No stream available" else "Live", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .background(appColors.subtleSurface, CircleShape)
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
                                shape = itemCornerShape(),
                                color = Color.Black
                            ) {
                                CameraViewerWithFullscreenButton(
                                    resolvedModel,
                                    liveWebUrl,
                                    authToken,
                                    title,
                                    onWebViewChanged = { streamWebView = it },
                                    onFullscreen = { isFullscreen = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("SourceLockedOrientationActivity")
internal fun CameraFullscreenHost(
    request: CameraFullscreenRequest?,
    onDismiss: () -> Unit
) {
    var streamWebView by remember { mutableStateOf<WebView?>(null) }
    WebViewLifecyclePause { streamWebView }
    if (request == null) return

    val activity = LocalContext.current.findActivity()
    // This host lives at MainApp level, above page dialogs and responsive screen content. Its
    // orientation permission therefore remains active while the fullscreen window is rebuilt.
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    FullscreenCameraViewer(
        title = request.title,
        imageUrl = request.imageUrl,
        liveWebUrl = request.liveWebUrl,
        authToken = request.authToken,
        onPrevious = request.onPrevious,
        onNext = request.onNext,
        positionText = request.positionText,
        onWebViewChanged = { streamWebView = it },
        onExitFullscreen = {
            request.onClosed()
            onDismiss()
        }
    )
}

@Composable
private fun CameraViewerWithFullscreenButton(
    imageUrl: String?,
    liveWebUrl: String?,
    authToken: String?,
    title: String,
    onWebViewChanged: (WebView?) -> Unit,
    onFullscreen: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        CameraViewer(imageUrl, liveWebUrl, authToken, title, onWebViewChanged)
        IconButton(
            onClick = onFullscreen,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.68f), CircleShape)
                .size(48.dp)
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Open camera full screen", tint = Color.White)
        }
    }
}

@Composable
private fun FullscreenCameraViewer(
    title: String,
    imageUrl: String?,
    liveWebUrl: String?,
    authToken: String?,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    positionText: String?,
    onWebViewChanged: (WebView?) -> Unit,
    onExitFullscreen: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var zoom by remember(imageUrl, liveWebUrl) { mutableFloatStateOf(1f) }
    val orientation = LocalConfiguration.current.orientation
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTimeoutKey by remember { mutableIntStateOf(0) }
    val isLive = !liveWebUrl.isNullOrBlank()

    fun revealControls() {
        controlsVisible = true
        controlsTimeoutKey += 1
    }

    LaunchedEffect(imageUrl, liveWebUrl) {
        isLoading = true
        revealControls()
        // Live MJPEG responses never finish by design, and some WebView builds never emit a
        // reliable commit callback for them. Never leave a permanent spinner over valid video.
        delay(2_000.milliseconds)
        isLoading = false
    }
    LaunchedEffect(orientation) {
        // A portrait zoom/pan must never carry into a differently shaped landscape viewport.
        zoom = 1f
    }
    LaunchedEffect(controlsVisible, controlsTimeoutKey, isLoading) {
        if (controlsVisible && !isLoading) {
            delay(3_500.milliseconds)
            controlsVisible = false
        }
    }
    Dialog(
        // Some Android builds send a dismiss request while rebuilding a Dialog for a rotation.
        // Fullscreen exits only through BackHandler or the explicit close button below.
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val hostView = LocalView.current
        val dialogWindow = (hostView.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            val insetsController = dialogWindow?.let { WindowCompat.getInsetsController(it, hostView) }
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
        }
        BackHandler(onBack = onExitFullscreen)

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            // Recreate only the media renderer after rotation. The surrounding fullscreen dialog
            // and controls remain alive, while WebView calculates a fresh viewport for the stream.
            key(orientation, imageUrl, liveWebUrl) {
                CameraViewer(
                    imageUrl = imageUrl,
                    liveWebUrl = liveWebUrl,
                    authToken = authToken,
                    title = title,
                    onWebViewChanged = onWebViewChanged,
                    fitToViewport = true,
                    controlledScale = if (isLive) null else zoom,
                    onScaleChanged = { zoom = it },
                    onLoadingChanged = { isLoading = it },
                    onTap = {
                        if (controlsVisible) controlsVisible = false else revealControls()
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(220)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(104.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                                )
                            )
                    ) {
                        Text(
                            title,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(horizontal = 22.dp, vertical = 20.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(if (isLive) 108.dp else 164.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (!isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ZoomOut,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = zoom,
                                    onValueChange = {
                                        zoom = it
                                        revealControls()
                                    },
                                    onValueChangeFinished = ::revealControls,
                                    valueRange = 1f..4f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                                )
                                Text(
                                    "${((zoom * 10).roundToInt() / 10f)}×",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(42.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onPrevious != null) {
                                IconButton(
                                    onClick = {
                                        onPrevious()
                                        revealControls()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous camera", tint = Color.White)
                                }
                            }
                            if (onNext != null) {
                                IconButton(
                                    onClick = {
                                        onNext()
                                        revealControls()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next camera", tint = Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLive) {
                                    Box(Modifier.size(7.dp).background(Color(0xFFEF5350), CircleShape))
                                    Spacer(Modifier.width(7.dp))
                                }
                                Text(
                                    if (isLive) "LIVE" else "Snapshot",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                positionText?.let {
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        it,
                                        color = Color.White.copy(alpha = 0.68f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            IconButton(
                                onClick = onExitFullscreen,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.FullscreenExit,
                                    contentDescription = "Exit full screen",
                                    tint = Color.White
                                )
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
    onWebViewChanged: (WebView?) -> Unit,
    fitToViewport: Boolean = false,
    controlledScale: Float? = null,
    onScaleChanged: ((Float) -> Unit)? = null,
    onLoadingChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLoadingChanged by rememberUpdatedState(onLoadingChanged)
    if (!liveWebUrl.isNullOrBlank()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                object : WebView(context) {
                    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                        super.onSizeChanged(w, h, oldw, oldh)
                        if (fitToViewport && w > 0 && h > 0 && (w != oldw || h != oldh)) {
                            post {
                                setInitialScale(0)
                                applyFullscreenCameraLayout()
                            }
                        }
                    }
                }.apply {
                    val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop
                    var downX = 0f
                    var downY = 0f
                    var tapCandidate = false
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(!fitToViewport)
                    settings.builtInZoomControls = !fitToViewport
                    settings.displayZoomControls = false
                    if (fitToViewport) {
                        setInitialScale(0)
                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = false
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER
                    }
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                tapCandidate = true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (kotlin.math.abs(event.x - downX) > touchSlop ||
                                    kotlin.math.abs(event.y - downY) > touchSlop
                                ) tapCandidate = false
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                tapCandidate = false
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                                view.performClick()
                                if (tapCandidate) currentOnTap()
                                tapCandidate = false
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                tapCandidate = false
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            currentOnLoadingChanged(true)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (fitToViewport) view?.applyFullscreenCameraLayout()
                            currentOnLoadingChanged(false)
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            if (fitToViewport) view?.applyFullscreenCameraLayout()
                            currentOnLoadingChanged(false)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame != false) currentOnLoadingChanged(false)
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 25) {
                                if (fitToViewport) view?.applyFullscreenCameraLayout()
                                currentOnLoadingChanged(false)
                            }
                        }
                    }
                    currentOnLoadingChanged(true)
                    loadUrl(liveWebUrl, cameraHeaders(authToken))
                    // Multipart MJPEG responses intentionally never finish loading. Hide the
                    // progress indicator even on WebView versions that provide no commit callback.
                    postDelayed({
                        if (isAttachedToWindow) {
                            if (fitToViewport) applyFullscreenCameraLayout()
                            currentOnLoadingChanged(false)
                        }
                    }, 1_500L)
                    onWebViewChanged(this)
                }
            },
            update = {
                if (it.url != liveWebUrl) {
                    currentOnLoadingChanged(true)
                    it.loadUrl(liveWebUrl, cameraHeaders(authToken))
                    it.postDelayed({
                        if (it.isAttachedToWindow) {
                            if (fitToViewport) it.applyFullscreenCameraLayout()
                            currentOnLoadingChanged(false)
                        }
                    }, 1_500L)
                } else if (fitToViewport) {
                    it.post { it.applyFullscreenCameraLayout() }
                }
            },
            onRelease = {
                currentOnLoadingChanged(false)
                onWebViewChanged(null)
                it.teardownStream()
            }
        )
    } else {
        ZoomableCameraImage(
            imageUrl = imageUrl,
            contentDescription = title,
            controlledScale = controlledScale,
            onScaleChanged = onScaleChanged,
            onLoadingChanged = onLoadingChanged,
            onTap = currentOnTap
        )
    }
}

/** Makes a directly-loaded image/MJPEG/video document behave like fullscreen media in a browser:
 * centered on both axes, preserving aspect ratio, and entirely visible in the current viewport. */
private fun WebView.applyFullscreenCameraLayout() {
    scrollTo(0, 0)
    evaluateJavascript(
        """
        (function() {
          var head = document.head || document.getElementsByTagName('head')[0];
          if (head && !document.querySelector('meta[name="viewport"]')) {
            var meta = document.createElement('meta');
            meta.name = 'viewport';
            meta.content = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no';
            head.appendChild(meta);
          }
          if (head && !document.getElementById('hki-camera-fit-style')) {
            var style = document.createElement('style');
            style.id = 'hki-camera-fit-style';
            style.textContent = 'html,body{position:fixed!important;inset:0!important;margin:0!important;width:100%!important;height:100%!important;overflow:hidden!important;background:#000!important}body{display:flex!important;align-items:center!important;justify-content:center!important}img,video{position:fixed!important;inset:0!important;display:block!important;box-sizing:border-box!important;width:100%!important;height:100%!important;max-width:100%!important;max-height:100%!important;object-fit:contain!important;object-position:center center!important;margin:0!important}';
            head.appendChild(style);
          }
          var root = document.documentElement;
          var body = document.body;
          if (!root || !body) return;
          root.style.cssText += ';position:fixed;inset:0;margin:0;width:100%;height:100%;overflow:hidden;background:#000;';
          body.style.cssText += ';position:fixed;inset:0;margin:0;width:100%;height:100%;overflow:hidden;background:#000;display:flex;align-items:center;justify-content:center;';
          var media = document.querySelectorAll('img, video');
          for (var i = 0; i < media.length; i++) {
            media[i].style.cssText += ';position:fixed;inset:0;display:block;box-sizing:border-box;width:100%;height:100%;max-width:100%;max-height:100%;object-fit:contain;object-position:center center;margin:0;';
          }
        })();
        """.trimIndent(),
        null
    )
}

private fun cameraHeaders(authToken: String?): Map<String, String> =
    if (authToken.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $authToken")

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ZoomableCameraImage(
    imageUrl: String?,
    contentDescription: String,
    controlledScale: Float? = null,
    onScaleChanged: ((Float) -> Unit)? = null,
    onLoadingChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val hostView = LocalView.current
    val currentOnTap by rememberUpdatedState(onTap)
    // Refresh ticks only change the hki_refresh query param. Key zoom/pan and the last good frame
    // on the URL without it, so a snapshot refresh keeps showing the previous (memory-cached)
    // frame while the new one loads — a seamless swap instead of a flash — and zoom survives it.
    val stableKey = imageUrl?.substringBefore("hki_refresh=")?.trimEnd('?', '&')
    var scale by remember(stableKey) { mutableFloatStateOf(1f) }
    var offsetX by remember(stableKey) { mutableFloatStateOf(0f) }
    var offsetY by remember(stableKey) { mutableFloatStateOf(0f) }
    var lastSuccessfulModel by remember(stableKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(controlledScale) {
        controlledScale?.let {
            scale = it.coerceIn(1f, 4f)
            if (scale == 1f) {
                offsetX = 0f
                offsetY = 0f
            }
        }
    }
    LaunchedEffect(imageUrl) {
        onLoadingChanged(!imageUrl.isNullOrBlank())
    }

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
                detectTapGestures(onTap = { currentOnTap() })
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
                            onScaleChanged?.invoke(scale)
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
                    onSuccess = {
                        lastSuccessfulModel = imageUrl
                        onLoadingChanged(false)
                    },
                    onError = { onLoadingChanged(false) }
                )
            }
        } else {
            Text("No Stream Available", color = Color.Gray)
        }
    }
}
