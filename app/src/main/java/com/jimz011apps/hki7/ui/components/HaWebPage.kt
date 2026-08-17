package com.jimz011apps.hki7.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import org.json.JSONObject

/** The client id this app authenticates with; the frontend keys its stored session on the same value. */
private const val HA_CLIENT_ID = "https://home-assistant.io/android"

/**
 * Pages of Home Assistant's own frontend — Developer Tools, Settings, HACS — hosted full screen
 * inside HKI 7.
 *
 * Full screen, and a [Dialog] rather than a settings section, for a reason beyond looks: the
 * settings body is a vertically scrolling column, and a WebView inside one is measured with
 * unbounded height, which is what left the page blank after signing in. It also matches the
 * onboarding login, which is the other place the app shows Home Assistant's own web UI.
 *
 * The frontend keeps its session in `localStorage.hassTokens` rather than in a cookie, so a
 * WebView pointed at the server lands on the login form even though this app is already
 * authenticated. Writing the tokens this app holds into that key hands it the session it expects —
 * the same mechanism the official companion app uses. localStorage is per-origin, so the first
 * load has to be allowed to start before the write can happen; the page is then reloaded once,
 * guarded by [seeded] so it cannot loop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaWebPageDialog(
    title: String,
    baseUrl: String,
    path: String,
    accessToken: String?,
    refreshToken: String?,
    accessTokenExpiry: Long?,
    onDismiss: () -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    val root = baseUrl.trim().removeSuffix("/")
    val target = remember(root, path) { "$root/${path.removePrefix("/")}" }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        // Back walks the frontend's own history first — Settings and Developer Tools are deep
        // trees, and leaving from three levels in would be its own bug report.
        BackHandler(enabled = true) {
            val view = webView
            if (view != null && view.canGoBack()) view.goBack() else onDismiss()
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
                IconButton(onClick = onDismiss) {
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
                IconButton(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ha_page_reload))
                }
            }
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
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
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = false
                                settings.mediaPlaybackRequiresUserGesture = false
                                // The frontend serves a different bundle to browsers it does not
                                // recognise; the onboarding login sends the same string.
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
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
}
