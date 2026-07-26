package com.jimz011apps.hki7.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jimz011apps.hki7.data.HKIIframeWidget
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/** Normalises user input into a loadable URL (adds https:// when no scheme is given). */
private fun normalizeUrl(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    return if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(t)) t else "https://$t"
}

/** Embeds a web page (iFrame-style) inside the dashboard using a WebView. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IframeWidgetItem(
    widget: HKIIframeWidget,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val url = remember(widget.url) { normalizeUrl(widget.url) }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(widget.aspectRatio.coerceAtLeast(0.2f)),
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = appColors.elevated
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(widget.cornerRadius.dp))) {
                if (url.isBlank()) {
                    Text(
                        "Empty iFrame — open its settings in edit mode and enter a web address.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = false
                                loadUrl(url)
                            }
                        },
                        update = { web -> if (web.url != url) web.loadUrl(url) }
                    )
                    // In edit mode, block the WebView from swallowing taps so the edit controls work.
                    if (isEditMode) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.03f))
                                .clickable { onSettings() }
                        )
                    }
                }
            }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** Aspect-ratio presets shared with the settings dialog. */
private val IFRAME_RATIOS: List<Pair<String, Float>> = listOf(
    "Square" to 1f,
    "4:3" to 4f / 3f,
    "16:9" to 16f / 9f,
    "Wide" to 21f / 9f,
    "Tall" to 3f / 4f,
)

@Composable
fun IframeWidgetSettingsDialog(
    widget: HKIIframeWidget,
    onDismiss: () -> Unit,
    onSave: (HKIIframeWidget) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var url by remember(widget) { mutableStateOf(widget.url) }
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var aspect by remember(widget) { androidx.compose.runtime.mutableFloatStateOf(widget.aspectRatio) }
    var settingsPage by remember(widget) { mutableStateOf("content") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle("iFrame", "Embed a web page on your dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf("content" to "Content", "appearance" to "Appearance"),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory("Content", "The web page shown on this card")
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Web address (URL)") },
                        placeholder = { Text("example.com or https://…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (settingsPage == "appearance") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory("Appearance", "Width and height")
                    Text("Width", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text("Height", style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IFRAME_RATIOS.forEach { (label, ratio) ->
                            androidx.compose.material3.FilterChip(
                                selected = kotlin.math.abs(aspect - ratio) < 0.001f,
                                onClick = { aspect = ratio },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = {
                onSave(
                    widget.copy(
                        url = url.trim(),
                        title = title.trim().ifBlank { null },
                        width = width,
                        aspectRatio = aspect,
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
