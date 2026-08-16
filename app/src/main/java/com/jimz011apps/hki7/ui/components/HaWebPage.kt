package com.jimz011apps.hki7.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jimz011apps.hki7.R
import androidx.compose.ui.res.stringResource
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import org.json.JSONObject

/** The client id this app authenticates with; the frontend keys its stored session on the same value. */
private const val HA_CLIENT_ID = "https://home-assistant.io/android"

/**
 * Pages of Home Assistant's own frontend — Developer Tools, Settings, HACS — hosted inside HKI 7.
 *
 * The frontend keeps its session in `localStorage.hassTokens` rather than in a cookie, so a
 * WebView pointed at the server lands on the login form even though this app is already
 * authenticated. Writing the tokens this app holds into that key hands the frontend the session it
 * expects, which is the same mechanism the official companion app uses.
 *
 * The write has to happen on the server's own origin (localStorage is per-origin), so the first
 * load is allowed to start — whatever it renders — and the page is reloaded once immediately
 * after the tokens are in place. That reload happens exactly once per WebView, guarded by
 * [seeded]; reloading on every page start would put the view in a loop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaWebPage(
    baseUrl: String,
    path: String,
    accessToken: String?,
    refreshToken: String?,
    accessTokenExpiry: Long?,
    onBack: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val root = baseUrl.trim().removeSuffix("/")
    val target = remember(root, path) { "$root/${path.removePrefix("/")}" }
    var loading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Back walks the frontend's own history first — Developer Tools and Settings are deep trees,
    // and leaving the page from three levels in would be its own bug report.
    BackHandler(enabled = true) {
        val view = webView
        if (canGoBack && view != null && view.canGoBack()) view.goBack() else onBack()
    }

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
            // The frontend refreshes on its own once this passes, so a stale value costs one
            // round trip rather than a failed load.
            put("expires_in", 1800)
            put("expires", accessTokenExpiry ?: (System.currentTimeMillis() + 1_800_000L))
        }
        "window.localStorage.setItem('hassTokens', ${JSONObject.quote(tokens.toString())});"
    }

    Column(Modifier.fillMaxSize()) {
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
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
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.mediaPlaybackRequiresUserGesture = false
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    loading = true
                                    if (seedScript != null && !seeded) {
                                        seeded = true
                                        view?.evaluateJavascript(seedScript) {
                                            // Boot again now the session is there to be found.
                                            view.post { view.reload() }
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    canGoBack = view?.canGoBack() == true
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
