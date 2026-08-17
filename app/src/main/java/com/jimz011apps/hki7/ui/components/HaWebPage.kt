package com.jimz011apps.hki7.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import org.json.JSONObject

/** The client id this app authenticates with; the frontend keys its stored session on the same value. */
private const val HA_CLIENT_ID = "https://home-assistant.io/android"
private const val TAG = "HaWebPage"

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
 * The frontend keeps its session in `localStorage.hassTokens` rather than in a cookie, so a
 * WebView pointed at the server would otherwise land on the login form even though this app is
 * already authenticated. localStorage is per-origin, so the first load has to be allowed to start
 * before the tokens can be written; the page is then reloaded once, guarded so it cannot loop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaWebPage(
    title: String,
    baseUrl: String,
    path: String,
    accessToken: String?,
    refreshToken: String?,
    accessTokenExpiry: Long?,
    onClose: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val root = baseUrl.trim().removeSuffix("/")
    val target = remember(root, path) { "$root/${path.removePrefix("/")}" }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Surfaced in the page rather than only logged. A frontend that loads its shell and then dies
    // on a script error looks identical to a blank screen, and "it went grey" is not something
    // anyone can act on.
    var failure by remember { mutableStateOf<String?>(null) }

    val seedScript = remember(root, accessToken, refreshToken, accessTokenExpiry) {
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) return@remember null
        // Built with JSONObject rather than string templating: a token is opaque and could
        // otherwise carry a character that breaks out of the literal.
        val tokens = JSONObject().apply {
            put("access_token", accessToken)
            put("token_type", "Bearer")
            put("refresh_token", refreshToken)
            put("hassUrl", root)
            put("clientId", HA_CLIENT_ID)
            put("expires_in", 1800)
            put("expires", accessTokenExpiry ?: (System.currentTimeMillis() + 1_800_000L))
        }
        "window.localStorage.setItem('hassTokens', ${JSONObject.quote(tokens.toString())});"
    }

    BackHandler(enabled = true) {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

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
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ui_close_bbfa773))
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
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ha_page_reload))
            }
            // An escape hatch that always works. Embedding someone else's single-page app is not
            // something this app can guarantee across every server version and WebView build, and
            // a dead end with no way out would be worse than not offering the page at all.
            val context = LocalContext.current
            IconButton(onClick = {
                val url = webView?.url ?: target
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.ha_page_open_browser))
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
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            var seeded = false
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            // The user agent is deliberately left alone. Overriding it with a
                            // desktop-ish Chrome string — copied from the onboarding login, where
                            // it does no harm because that page is plain HTML — makes Home
                            // Assistant serve its modern frontend bundle whatever this device's
                            // WebView can actually run. The default string describes the real
                            // engine, so the server picks a build that works on it.
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                    message ?: return false
                                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                        Log.w(TAG, "console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                                        if (failure == null) failure = message.message()
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
                                    if (seedScript != null && !seeded) {
                                        seeded = true
                                        view?.evaluateJavascript(seedScript) {
                                            view.post { view.reload() }
                                        }
                                    }
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
                        if (view.url == null) view.loadUrl(target)
                    }
                )
            }
        }
    }
}
