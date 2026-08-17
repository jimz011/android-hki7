package com.jimz011apps.hki7.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import org.json.JSONObject

private const val TAG = "HaWebPage"
private const val EXTERNAL_APP = "externalApp"
private const val GET_AUTH_CALLBACK = "externalAuthSetToken"
private const val REVOKE_AUTH_CALLBACK = "externalAuthRevokeToken"

/**
 * A page of Home Assistant's own frontend — Settings, Developer Tools, HACS — full screen inside
 * HKI 7.
 *
 * Deliberately a plain composable rather than a [androidx.compose.ui.window.Dialog]. The settings
 * screen is itself a Dialog, and a WebView nested in a second one lives in its own window; the
 * frontend authenticated there and then rendered nothing but background. The onboarding login is
 * the one WebView in this app that has always worked, and it is an ordinary composable in the main
 * tree — so this is hosted the same way, with the same WebView settings. In particular it does not
 * set `useWideViewPort`/`loadWithOverviewMode`, which are for showing desktop pages zoomed out and
 * do a single-page app no favours.
 *
 * Home Assistant's frontend does not consume a companion app session from localStorage. With
 * `external_auth=1` it asks a native bridge for a short-lived access token while it boots. The
 * bridge is attached before the first navigation so the request cannot race WebView setup. Its
 * callback names are fixed and the current main document must still have the server's origin
 * before any token is returned.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaWebPage(
    title: String,
    baseUrl: String,
    path: String,
    accessToken: String?,
    accessTokenExpiry: Long?,
    onClose: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val composeView = LocalView.current
    val root = baseUrl.trim().removeSuffix("/")
    val target = remember(root, path) {
        "$root/${path.removePrefix("/")}".toUri().buildUpon()
            .appendQueryParameter("external_auth", "1")
            .build()
            .toString()
    }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Surfaced in the page rather than only logged. A frontend that loads its shell and then dies
    // on a script error looks identical to a blank screen, and "it went grey" is not something
    // anyone can act on.
    var failure by remember { mutableStateOf<String?>(null) }
    val sessionFailure = stringResource(R.string.ha_page_session_failed)

    val externalAuthJson = remember(accessToken, accessTokenExpiry) {
        if (accessToken.isNullOrBlank()) return@remember null
        val expiresIn = accessTokenExpiry
            ?.let { ((it - System.currentTimeMillis()) / 1_000L).coerceAtLeast(1L) }
            ?: 1_800L
        JSONObject().apply {
            put("access_token", accessToken)
            put("expires_in", expiresIn)
        }.toString()
    }
    // The Java bridge is created once with the WebView, while DataStore can replace the token
    // later after a proactive refresh. Keep its provider attached to the latest composition.
    val latestExternalAuthJson by rememberUpdatedState(externalAuthJson)

    // A searchable HA dropdown may focus an input and open the IME. Resizing this edge-to-edge
    // activity also resizes the WebView and its explicit HA viewport, pushing the entire page up
    // until none of the dropdown controls are reachable. Overlay the keyboard only for this page,
    // then restore the activity's previous behavior as soon as the WebView closes.
    DisposableEffect(activity) {
        val window = activity?.window
        val originalMode = window?.attributes?.softInputMode
        if (window != null && originalMode != null) {
            val state = originalMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE
            window.setSoftInputMode(
                state or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
        onDispose {
            if (window != null && originalMode != null) {
                window.setSoftInputMode(originalMode)
            }
        }
    }

    // These three pages draw HKI's own solid header behind the transparent status bar. Keep the
    // system icons tied to that local surface rather than to whichever dashboard hero image last
    // controlled them. Restore the prior appearance on exit; the rest of the app remains free to
    // choose its status-bar contrast per page.
    val useDarkStatusBarIcons = appColors.background.luminance() > 0.5f
    DisposableEffect(activity, composeView, useDarkStatusBarIcons) {
        val controller = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, composeView)
        }
        val previousAppearance = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = useDarkStatusBarIcons
        onDispose {
            if (controller != null && previousAppearance != null) {
                controller.isAppearanceLightStatusBars = previousAppearance
            }
        }
    }

    fun navigateBack() {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

    BackHandler(enabled = true) { navigateBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::navigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.ui_back_b52b36b),
                    tint = appColors.onSurface
                )
            }
            Text(
                title,
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { failure = null; webView?.reload() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.ha_page_reload),
                    tint = appColors.onSurface
                )
            }
            // An escape hatch that always works. Embedding someone else's single-page app is not
            // something this app can guarantee across every server version and WebView build, and
            // a dead end with no way out would be worse than not offering the page at all.
            IconButton(onClick = {
                val url = webView?.url ?: target
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.ha_page_open_browser),
                    tint = appColors.onSurface
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.ui_close_bbfa773),
                    tint = appColors.onSurface
                )
            }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        failure?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Box(Modifier.fillMaxSize()) {
            if (root.isBlank()) {
                Text(
                    stringResource(R.string.ha_page_no_server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.onMuted,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else if (externalAuthJson == null) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.5f))
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        // Debug builds can be inspected through ADB/Chrome DevTools. This makes
                        // WebView-only layout failures diagnosable without shipping any remote
                        // debugging capability in a release build.
                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                        WebView(ctx).apply {
                            setBackgroundColor(appColors.background.toArgb())
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // The user agent is deliberately left alone. Overriding it with a
                            // desktop-ish Chrome string — copied from the onboarding login, where
                            // it does no harm because that page is plain HTML — makes Home
                            // Assistant serve its modern frontend bundle whatever this device's
                            // WebView can actually run. The default string describes the real
                            // engine, so the server picks a build that works on it.
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            // Must be installed before loadUrl. The frontend calls this while its
                            // initial application shell is starting, before onPageFinished.
                            addJavascriptInterface(
                                HaExternalAuthBridge(
                                    webView = this,
                                    baseUrl = root,
                                    authJsonProvider = { latestExternalAuthJson },
                                    onAuthenticationFailed = { failure = sessionFailure }
                                ),
                                EXTERNAL_APP
                            )

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                    message ?: return false
                                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                        val text = message.message()
                                        // HA can reject a dialog's outstanding request after the
                                        // dialog has already been dismissed. Its connection is
                                        // still healthy, so this exact transient rejection must
                                        // not replace the embedded page with HKI's error banner.
                                        if (text.trim().equals("Uncaught (in promise) disconnected", ignoreCase = true)) {
                                            Log.d(TAG, "Ignored transient HA dialog disconnect")
                                            return false
                                        }
                                        Log.w(TAG, "console: $text (${message.sourceId()}:${message.lineNumber()})")
                                        if (failure == null) failure = text
                                    }
                                    return false
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    failure = null
                                    Log.d(TAG, "Page started: ${url?.toUri()?.encodedPath}")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    progress = 100
                                    Log.d(TAG, "Page finished: ${url?.toUri()?.encodedPath}")
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    // Only the main document: a page pulling in an optional asset
                                    // that 404s is not a failure worth shouting about.
                                    if (request?.isForMainFrame != true) return
                                    Log.w(TAG, "load error ${error?.errorCode}: ${error?.description}")
                                    failure = error?.description?.toString()
                                }
                            }
                            webView = this
                            loadUrl(target)
                        }
                    },
                    update = { view ->
                        webView = view
                        view.setBackgroundColor(appColors.background.toArgb())
                        if (view.url == null) view.loadUrl(target)
                    },
                    onRelease = { view ->
                        if (webView === view) webView = null
                        view.stopLoading()
                        view.removeJavascriptInterface(EXTERNAL_APP)
                        view.destroy()
                    }
                )
            }
        }
    }
}

/**
 * Legacy bridge used by HA versions that expose the external-app contract through
 * addJavascriptInterface. The official frontend still supports this fallback. Keeping the two
 * callback names allowlisted is essential: evaluating a callback supplied by the page would turn
 * this into arbitrary JavaScript execution with the access token as its argument.
 */
private class HaExternalAuthBridge(
    private val webView: WebView,
    private val baseUrl: String,
    private val authJsonProvider: () -> String?,
    private val onAuthenticationFailed: () -> Unit,
) {
    @JavascriptInterface
    fun getExternalAuth(payload: String) {
        if (authCallback(payload) != GET_AUTH_CALLBACK) {
            Log.w(TAG, "Rejected external auth request with an unexpected callback")
            return
        }
        webView.post {
            if (!sameWebOrigin(baseUrl, webView.url)) {
                Log.w(TAG, "Rejected external auth request outside the Home Assistant origin")
                return@post
            }
            val authJson = authJsonProvider()
            if (authJson == null) {
                onAuthenticationFailed()
                webView.evaluateJavascript("window.$GET_AUTH_CALLBACK(false);", null)
            } else {
                Log.d(TAG, "Supplying external auth to ${baseUrl.toUri().host}")
                webView.evaluateJavascript("window.$GET_AUTH_CALLBACK(true, $authJson);", null)
            }
        }
    }

    @JavascriptInterface
    fun revokeExternalAuth(payload: String) {
        if (authCallback(payload) != REVOKE_AUTH_CALLBACK) {
            Log.w(TAG, "Rejected external auth revoke request with an unexpected callback")
            return
        }
        webView.post {
            if (sameWebOrigin(baseUrl, webView.url)) {
                // Closing an embedded page must not silently sign the entire HKI app out.
                webView.evaluateJavascript("window.$REVOKE_AUTH_CALLBACK(false);", null)
            }
        }
    }

    @JavascriptInterface
    fun externalBus(message: String) {
        val request = runCatching { JSONObject(message) }.getOrNull() ?: return
        val type = request.optString("type")
        val id = if (request.has("id")) request.optInt("id") else null
        val event = request.optJSONObject("payload")?.optString("event")?.takeIf(String::isNotBlank)
        Log.d(TAG, "External bus: type=$type id=$id event=$event")
        if (type == "frontend/loaded") {
            webView.post {
                if (sameWebOrigin(baseUrl, webView.url)) {
                    webView.evaluateJavascript(FRONTEND_LAYOUT_FIX) { webView.invalidate() }
                }
            }
            if (BuildConfig.DEBUG) {
                logFrontendDom(250L)
                logFrontendDom(2_000L)
            }
            return
        }
        if (type != "config/get" || id == null) return

        // The frontend blocks its startup splash until the embedding app answers config/get.
        // Advertise only capabilities this small embedded host actually implements; Home
        // Assistant will keep its own UI for settings, Assist, media, tags and downloads.
        val response = JSONObject().apply {
            put("id", id)
            put("type", "result")
            put("success", true)
            put("result", JSONObject().apply {
                put("hasSettingsScreen", false)
                put("hasSidebar", false)
                put("canWriteTag", false)
                put("hasExoPlayer", false)
                put("canCommissionMatter", false)
                put("canImportThreadCredentials", false)
                put("hasAssist", false)
                put("hasBarCodeScanner", 0)
                put("canSetupImprov", false)
                put("downloadFileSupported", false)
                put("appVersion", BuildConfig.VERSION_NAME)
                put("hasEntityAddTo", false)
                put("hasAssistSettings", false)
                put("hasSplashscreen", false)
                put("hasMatterStatusReport", false)
            })
        }.toString()
        webView.post {
            if (sameWebOrigin(baseUrl, webView.url)) {
                // This WebView reports a valid viewport but HA's document/root custom elements
                // otherwise resolve to 0px tall. Establish the root height before allowing the
                // frontend past config/get, so its first panel is measured against real space.
                webView.evaluateJavascript(FRONTEND_LAYOUT_FIX) {
                    webView.evaluateJavascript("window.externalBus($response);", null)
                    Log.d(TAG, "External config supplied")
                }
            }
        }
    }

    /** Log element geometry/styles, never content, after HA says its first panel has rendered. */
    private fun logFrontendDom(delayMs: Long) {
        webView.postDelayed({
            if (!sameWebOrigin(baseUrl, webView.url)) return@postDelayed
            webView.evaluateJavascript(FRONTEND_DOM_DIAGNOSTIC) { result ->
                logLong("Frontend DOM after ${delayMs}ms", result)
            }
        }, delayMs)
    }

    /** Android truncates long log entries; preserve each bounded diagnostic snapshot in chunks. */
    private fun logLong(label: String, value: String) {
        value.chunked(3_000).forEachIndexed { index, chunk ->
            Log.d(TAG, "$label[$index]: $chunk")
        }
    }
}

private val FRONTEND_LAYOUT_FIX = """
    (function() {
        // Some HA frontend builds request the virtualizer from two lazy-loaded paths when the
        // Add Integration dialog first opens. Android WebView can execute both registration
        // wrappers, and the second `define` aborts the dialog even though a working virtualizer
        // is already registered. Ignore only that one exact duplicate; every other custom-element
        // registration error remains visible and actionable.
        if (!window.__hkiLitVirtualizerRegistryFix && window.CustomElementRegistry) {
            var registryPrototype = window.CustomElementRegistry.prototype;
            var originalDefine = registryPrototype && registryPrototype.define;
            if (typeof originalDefine === 'function') {
                registryPrototype.define = function(name, constructor, options) {
                    if (String(name).toLowerCase() === 'lit-virtualizer') {
                        var existing;
                        try { existing = this.get(name); } catch (_) { existing = null; }
                        if (existing) return;
                    }
                    return originalDefine.call(this, name, constructor, options);
                };
                window.__hkiLitVirtualizerRegistryFix = true;
            }
        }

        function fill(element, height) {
            if (!element) return;
            element.style.setProperty('display', 'block', 'important');
            element.style.setProperty('box-sizing', 'border-box', 'important');
            element.style.setProperty('width', '100%', 'important');
            element.style.setProperty('height', height, 'important');
            element.style.setProperty('min-height', height, 'important');
        }
        function apply() {
            // Percentage and viewport-unit heights resolve to zero in this AndroidView-hosted
            // WebView even though innerHeight is valid. Use that measured viewport explicitly.
            var viewportHeight = Math.max(
                window.innerHeight || 0,
                window.visualViewport ? window.visualViewport.height : 0,
                document.documentElement.clientHeight || 0,
                1
            );
            var height = Math.round(viewportHeight) + 'px';
            document.documentElement.style.setProperty('height', height, 'important');
            document.documentElement.style.setProperty('min-height', height, 'important');
            document.body.style.setProperty('height', height, 'important');
            document.body.style.setProperty('min-height', height, 'important');
            document.body.style.setProperty('margin', '0', 'important');
            var ha = document.querySelector('home-assistant');
            fill(ha, height);
            var main = ha && ha.shadowRoot && ha.shadowRoot.querySelector('home-assistant-main');
            fill(main, height);
            if (main && !main.__hkiSidebarDisabled) {
                main.__hkiSidebarDisabled = true;
                main.addEventListener('hass-toggle-menu', function(event) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                }, true);
            }
            var drawer = main && main.shadowRoot && main.shadowRoot.querySelector('ha-drawer');
            fill(drawer, height);
            var resolver = main && main.shadowRoot && main.shadowRoot.querySelector('partial-panel-resolver');
            fill(resolver, height);
            var panel = resolver && resolver.lastElementChild;
            fill(panel, height);

            // HA's lazy-loaded panel views are custom elements. In this embedded viewport their
            // root host can retain the browser's default `inline` display, making a 100%-high
            // app-bar inside its shadow root resolve to 0px and clip a fully rendered page. Fill
            // only the panel's root layout chain: wide, collapsed custom elements at this level
            // are structural containers, while controls deeper in the page are left untouched.
            function fillPanelLayout(container, depth) {
                if (!container || depth > 4) return;
                var root = container.shadowRoot || container;
                Array.from(root.children).forEach(function(child) {
                    var style = window.getComputedStyle(child);
                    if (style.display === 'none') return;
                    var tag = child.tagName.toLowerCase();
                    if (tag === 'style' || tag === 'script' || tag === 'card-mod') return;
                    var rect = child.getBoundingClientRect();
                    var customElement = tag.indexOf('-') !== -1;
                    var panelRoot = depth === 0 && customElement;
                    var wideCollapsed = customElement && rect.height < 1 && (
                        rect.width >= window.innerWidth * 0.75 ||
                        child.scrollWidth >= window.innerWidth * 0.75
                    );
                    if (panelRoot || wideCollapsed) fill(child, height);
                    if (child.shadowRoot && (panelRoot || wideCollapsed)) {
                        fillPanelLayout(child, depth + 1);
                    }
                });
            }
            fillPanelLayout(panel, 0);

            // HACS is not rendered directly in HA's panel tree. Its integration registers a
            // custom panel with embed_iframe=true; HA then gives that iframe a 100dvh height.
            // The same WebView viewport bug makes that resolve to 0px even though the iframe has
            // loaded. Size same-origin embedded panels and their direct app root explicitly.
            function fillEmbeddedPanelFrames(container) {
                if (!container) return;
                var frames = [];
                if (container.tagName && container.tagName.toLowerCase() === 'iframe') {
                    frames.push(container);
                }
                if (container.querySelectorAll) {
                    frames = frames.concat(Array.from(container.querySelectorAll('iframe')));
                }
                frames.forEach(function(frame) {
                    frame.style.setProperty('display', 'block', 'important');
                    frame.style.setProperty('box-sizing', 'border-box', 'important');
                    frame.style.setProperty('width', '100%', 'important');
                    frame.style.setProperty('height', height, 'important');
                    frame.style.setProperty('min-height', height, 'important');
                    frame.style.setProperty('max-height', height, 'important');
                    try {
                        var frameDocument = frame.contentDocument;
                        if (!frameDocument) return;
                        frameDocument.documentElement.style.setProperty('height', height, 'important');
                        frameDocument.documentElement.style.setProperty('min-height', height, 'important');
                        if (frameDocument.body) {
                            frameDocument.body.style.setProperty('display', 'block', 'important');
                            frameDocument.body.style.setProperty('height', height, 'important');
                            frameDocument.body.style.setProperty('min-height', height, 'important');
                            frameDocument.body.style.setProperty('margin', '0', 'important');
                            if (!frameDocument.body.__hkiSidebarDisabled) {
                                frameDocument.body.__hkiSidebarDisabled = true;
                                frameDocument.body.addEventListener('hass-toggle-menu', function(event) {
                                    event.preventDefault();
                                    event.stopImmediatePropagation();
                                }, true);
                            }
                            Array.from(frameDocument.body.children).forEach(function(child) {
                                var tag = child.tagName.toLowerCase();
                                if (tag.indexOf('-') === -1) return;
                                child.style.setProperty('display', 'block', 'important');
                                child.style.setProperty('box-sizing', 'border-box', 'important');
                                child.style.setProperty('width', '100%', 'important');
                                child.style.setProperty('height', height, 'important');
                                child.style.setProperty('min-height', height, 'important');
                            });
                            suppressHaSidebar(frameDocument.body);
                        }
                        if (!frame.__hkiEmbeddedFrameLoadFix) {
                            frame.__hkiEmbeddedFrameLoadFix = true;
                            frame.addEventListener('load', function() {
                                window.requestAnimationFrame(apply);
                            });
                        }
                        if (frame.contentWindow) {
                            frame.contentWindow.dispatchEvent(new Event('resize'));
                        }
                    } catch (_) {
                        // Cross-origin custom panels are left to the iframe itself. HACS and HA's
                        // built-in embedded panels are same-origin and take the branch above.
                    }
                });
            }
            fillEmbeddedPanelFrames(panel);

            // HKI supplies the navigation chrome for these embedded pages. Remove HA's own
            // hamburger and sidebar through their open shadow roots, and close the drawer if it
            // was carried across an in-page navigation. The traversal is bounded and touches no
            // page content or controls other than those two named navigation elements.
            function suppressHaSidebar(start) {
                var seen = new WeakSet();
                var visited = 0;
                function visit(element, depth) {
                    if (!element || seen.has(element) || depth > 18 || visited++ > 600) return;
                    seen.add(element);
                    var tag = element.tagName.toLowerCase();
                    if (tag === 'ha-menu-button' || tag === 'ha-sidebar') {
                        element.style.setProperty('display', 'none', 'important');
                    }
                    if (tag === 'ha-drawer' && element.open) element.open = false;

                    var children = element.shadowRoot
                        ? Array.from(element.shadowRoot.children)
                        : Array.from(element.children);
                    if (tag === 'slot' && element.assignedElements) {
                        children = element.assignedElements({flatten: true});
                    }
                    children.forEach(function(child) { visit(child, depth + 1); });
                }
                visit(start, 0);
            }
            suppressHaSidebar(main);

            // WA dialogs are placed in the browser's top layer. In this WebView their outer
            // element spans the viewport, but the native <dialog> and HA's lazy list can still
            // resolve to 0px tall. Give only open modal internals an explicit measured height;
            // keeping the HA dialog host untouched preserves its own header and close behavior.
            function fixHaDialogs() {
                if (!ha) return;
                var roots = Array.from(ha.children);
                if (ha.shadowRoot) {
                    Array.from(ha.shadowRoot.children).forEach(function(child) {
                        if (child !== main) roots.push(child);
                    });
                }
                var seen = new WeakSet();
                var visited = 0;

                function composedChildren(element) {
                    var tag = element.tagName.toLowerCase();
                    if (tag === 'slot' && element.assignedElements) {
                        var assigned = element.assignedElements({flatten: true});
                        if (assigned.length) return assigned;
                    }
                    var children = [];
                    if (element.shadowRoot) {
                        children = children.concat(Array.from(element.shadowRoot.children));
                    }
                    return children.concat(Array.from(element.children));
                }

                function repair(waDialog) {
                    var nativeDialog = waDialog.shadowRoot &&
                        waDialog.shadowRoot.querySelector('dialog');
                    if (!nativeDialog || !nativeDialog.hasAttribute('open')) return;

                    waDialog.style.setProperty('display', 'block', 'important');
                    waDialog.style.setProperty('box-sizing', 'border-box', 'important');
                    waDialog.style.setProperty('width', '100%', 'important');
                    waDialog.style.setProperty('height', height, 'important');
                    waDialog.style.setProperty('min-height', height, 'important');
                    waDialog.style.setProperty('max-height', height, 'important');

                    nativeDialog.style.setProperty('display', 'flex', 'important');
                    nativeDialog.style.setProperty('flex-direction', 'column', 'important');
                    nativeDialog.style.setProperty('box-sizing', 'border-box', 'important');
                    nativeDialog.style.setProperty('width', '100%', 'important');
                    nativeDialog.style.setProperty('height', height, 'important');
                    nativeDialog.style.setProperty('min-height', height, 'important');
                    nativeDialog.style.setProperty('max-height', height, 'important');

                    var content = nativeDialog.firstElementChild;
                    if (content) {
                        content.style.setProperty('display', 'flex', 'important');
                        content.style.setProperty('flex', '1 1 auto', 'important');
                        content.style.setProperty('height', 'auto', 'important');
                        content.style.setProperty('min-height', '0', 'important');
                        content.style.setProperty('max-height', '100%', 'important');
                    }

                    var dialogRect = nativeDialog.getBoundingClientRect();
                    var elements = [];
                    var localSeen = new WeakSet();
                    var localVisited = 0;
                    function collect(element, depth) {
                        if (!element || localSeen.has(element) || depth > 22 || localVisited++ > 900) return;
                        localSeen.add(element);
                        elements.push(element);
                        composedChildren(element).forEach(function(child) {
                            collect(child, depth + 1);
                        });
                    }
                    collect(waDialog, 0);

                    var footerHeight = 0;
                    elements.forEach(function(element) {
                        if (element.tagName.toLowerCase() === 'footer') {
                            footerHeight = Math.max(
                                footerHeight,
                                element.getBoundingClientRect().height || 0
                            );
                        }
                    });
                    elements.forEach(function(element) {
                        var tag = element.tagName.toLowerCase();
                        if (tag.indexOf('-') === -1 || tag === 'wa-dialog' ||
                            tag === 'ha-dialog' || tag.indexOf('dialog-') === 0) return;
                        var style = window.getComputedStyle(element);
                        if (style.display === 'none') return;
                        var rect = element.getBoundingClientRect();
                        var wide = rect.width >= window.innerWidth * 0.75 ||
                            element.scrollWidth >= window.innerWidth * 0.75;
                        if (rect.height >= 1 || !wide) return;
                        var available = Math.max(
                            36,
                            Math.round(dialogRect.bottom -
                                Math.max(dialogRect.top, rect.top) - footerHeight)
                        );
                        var availableHeight = available + 'px';
                        element.style.setProperty('display', 'block', 'important');
                        element.style.setProperty('box-sizing', 'border-box', 'important');
                        element.style.setProperty('width', '100%', 'important');
                        element.style.setProperty('height', availableHeight, 'important');
                        element.style.setProperty('min-height', availableHeight, 'important');
                        element.style.setProperty('max-height', availableHeight, 'important');
                    });
                }

                function visit(element, depth) {
                    if (!element || seen.has(element) || depth > 20 || visited++ > 700) return;
                    seen.add(element);
                    if (element.tagName.toLowerCase() === 'wa-dialog') repair(element);
                    composedChildren(element).forEach(function(child) {
                        visit(child, depth + 1);
                    });
                }
                roots.forEach(function(root) { visit(root, 0); });
            }
            fixHaDialogs();

            // At phone width HA renders searchable pickers as a bottom sheet rather than a
            // desktop popover. ha-bottom-sheet ultimately gives Web Awesome's native <dialog> a
            // max-height based on 100dvh. This WebView reports that unit as zero even though its
            // measured viewport is valid, leaving the sheet at the bottom edge with the focused
            // search input (and therefore the keyboard) but no visible options. Repair open sheets
            // at their native-dialog boundary. Content-sized sheets keep their natural height;
            // flex sheets such as generic pickers receive the intended viewport-filling height.
            function fixHaBottomSheets() {
                if (!ha) return;
                var seen = new WeakSet();
                var visited = 0;
                var sheetHeightValue = Math.max(120, viewportHeight - 48) + 'px';

                function composedChildren(element) {
                    var tag = element.tagName.toLowerCase();
                    if (tag === 'slot' && element.assignedElements) {
                        var assigned = element.assignedElements({flatten: true});
                        if (assigned.length) return assigned;
                    }
                    var children = [];
                    if (element.shadowRoot) {
                        children = children.concat(Array.from(element.shadowRoot.children));
                    }
                    return children.concat(Array.from(element.children));
                }

                function repair(sheet) {
                    if (!sheet.open || !sheet.shadowRoot) return;
                    var drawer = sheet.shadowRoot.querySelector('wa-drawer');
                    var nativeDialog = drawer && drawer.shadowRoot &&
                        drawer.shadowRoot.querySelector('dialog');
                    if (!nativeDialog || !nativeDialog.hasAttribute('open')) return;

                    sheet.style.setProperty(
                        '--ha-bottom-sheet-max-height', sheetHeightValue, 'important'
                    );
                    nativeDialog.style.setProperty(
                        'max-height', sheetHeightValue, 'important'
                    );

                    if (sheet.hasAttribute('flexcontent')) {
                        sheet.style.setProperty(
                            '--ha-bottom-sheet-height', sheetHeightValue, 'important'
                        );
                        nativeDialog.style.setProperty(
                            'height', sheetHeightValue, 'important'
                        );
                        nativeDialog.style.setProperty(
                            'min-height', sheetHeightValue, 'important'
                        );
                        var body = nativeDialog.querySelector('[part="body"]');
                        if (body) {
                            body.style.setProperty('height', '100%', 'important');
                            body.style.setProperty('min-height', '0', 'important');
                        }
                    }
                }

                function visit(element, depth) {
                    if (!element || seen.has(element) || depth > 24 || visited++ > 1000) return;
                    seen.add(element);
                    if (element.tagName.toLowerCase() === 'ha-bottom-sheet') repair(element);
                    composedChildren(element).forEach(function(child) {
                        visit(child, depth + 1);
                    });
                }
                visit(ha, 0);
            }
            fixHaBottomSheets();

            // partial-panel-resolver swaps its child when HA navigates without a full page load.
            // Observe child-list changes only; our own style updates therefore cannot loop.
            if (main && main.shadowRoot && !window.__hkiEmbeddedLayoutObserver) {
                window.__hkiEmbeddedLayoutObserver = new MutationObserver(function() {
                    window.requestAnimationFrame(apply);
                });
                window.__hkiEmbeddedLayoutObserver.observe(main.shadowRoot, {
                    childList: true,
                    subtree: true
                });
                // Observe the resolver explicitly as well. Its light-DOM panel child sits at a
                // shadow boundary, and WebView versions differ in whether a shadow-root subtree
                // observer reports mutations made beneath that host.
                if (resolver) {
                    window.__hkiEmbeddedLayoutObserver.observe(resolver, {
                        childList: true,
                        subtree: true
                    });
                }
                // HA appends dialogs as direct children of its root host. Watching that one
                // boundary catches late-opened editors without observing virtualized list rows.
                if (ha) {
                    window.__hkiEmbeddedLayoutObserver.observe(ha, {
                        childList: true,
                        subtree: false
                    });
                }
            }
        }
        apply();
        [50, 200, 1000, 2500].forEach(function(delay) { setTimeout(apply, delay); });
        if (!window.__hkiEmbeddedNavigationFix) {
            window.__hkiEmbeddedNavigationFix = true;
            var refreshAfterNavigation = function() {
                [0, 50, 200, 1000].forEach(function(delay) { setTimeout(apply, delay); });
            };
            window.addEventListener('popstate', refreshAfterNavigation);
            window.addEventListener('hashchange', refreshAfterNavigation);
            window.addEventListener('pageshow', refreshAfterNavigation);
            window.addEventListener('show-dialog', refreshAfterNavigation, true);
            window.addEventListener('picker-opened', refreshAfterNavigation, true);
            window.addEventListener('wa-show', refreshAfterNavigation, true);
            // The picker can stop its click before it bubbles. Capture the composed path and run
            // once its Lit update has appended the bottom sheet, before the opening animation
            // focuses the search input.
            window.addEventListener('click', function(event) {
                var opensPicker = event.composedPath().some(function(element) {
                    if (!element || !element.tagName) return false;
                    var tag = element.tagName.toLowerCase();
                    return tag === 'ha-picker-field' || tag === 'ha-generic-picker';
                });
                if (opensPicker) refreshAfterNavigation();
            }, true);
        }
        if (!window.__hkiEmbeddedResizeFix) {
            window.__hkiEmbeddedResizeFix = true;
            window.addEventListener('resize', apply);
            if (window.visualViewport) window.visualViewport.addEventListener('resize', apply);
        }
        return true;
    })();
""".trimIndent()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val FRONTEND_DOM_DIAGNOSTIC = """
    (function() {
        var output = [];
        var seen = new WeakSet();
        var limit = 160;

        function visit(element, path, depth) {
            if (!element || seen.has(element) || output.length >= limit || depth > 14) return;
            seen.add(element);
            var rect = element.getBoundingClientRect();
            var style = window.getComputedStyle(element);
            output.push({
                p: path,
                t: element.tagName,
                r: [Math.round(rect.x), Math.round(rect.y), Math.round(rect.width), Math.round(rect.height)],
                c: [element.clientWidth, element.clientHeight, element.scrollWidth, element.scrollHeight],
                d: style.display,
                v: style.visibility,
                o: style.opacity,
                pos: style.position,
                ov: style.overflow,
                sh: element.shadowRoot ? element.shadowRoot.children.length : 0
            });

            // Follow both ordinary DOM and open shadow roots. Slots are followed through their
            // assigned elements as well, because HA's drawer/panel layout crosses that boundary.
            var children = element.shadowRoot
                ? Array.from(element.shadowRoot.children)
                : Array.from(element.children);
            if (element.tagName === 'SLOT' && element.assignedElements) {
                children = element.assignedElements({flatten: true});
            }
            children.slice(0, 20).forEach(function(child, index) {
                visit(child, path + '/' + child.tagName.toLowerCase() + '[' + index + ']', depth + 1);
            });
        }

        var ha = document.querySelector('home-assistant');
        visit(ha, 'home-assistant', 0);
        return JSON.stringify({
            ready: document.readyState,
            hidden: document.hidden,
            viewport: [window.innerWidth, window.innerHeight],
            launch: !!document.getElementById('ha-launch-screen'),
            count: output.length,
            nodes: output
        });
    })();
""".trimIndent()

private fun authCallback(payload: String): String? =
    runCatching { JSONObject(payload).optString("callback").takeIf(String::isNotBlank) }.getOrNull()

/** Never return the app's access token to a document reached through an SSO redirect. */
private fun sameWebOrigin(baseUrl: String, candidateUrl: String?): Boolean {
    if (candidateUrl.isNullOrBlank()) return false
    val base = runCatching { baseUrl.toUri() }.getOrNull() ?: return false
    val candidate = runCatching { candidateUrl.toUri() }.getOrNull() ?: return false
    fun effectivePort(scheme: String?, port: Int): Int = when {
        port >= 0 -> port
        scheme.equals("https", ignoreCase = true) -> 443
        scheme.equals("http", ignoreCase = true) -> 80
        else -> -1
    }
    return base.scheme.equals(candidate.scheme, ignoreCase = true) &&
        base.host.equals(candidate.host, ignoreCase = true) &&
        effectivePort(base.scheme, base.port) == effectivePort(candidate.scheme, candidate.port)
}
