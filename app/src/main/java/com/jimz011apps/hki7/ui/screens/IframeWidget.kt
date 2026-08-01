package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

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
import com.jimz011apps.hki7.data.isWidgetVisibleNow
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
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
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
                        stringResource(R.string.ui_empty_iframe_open_its_settings_in_edit_mode_and_c17ba2f),
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

/** Aspect-ratio values shared with the settings dialog. Labels are resolved per locale below. */
private val IFRAME_RATIOS = listOf(1f, 4f / 3f, 16f / 9f, 21f / 9f, 3f / 4f)

@Composable
private fun iframeRatioLabel(ratio: Float): String = when (ratio) {
    1f -> stringResource(R.string.widgets_ratio_square)
    4f / 3f -> "4:3"
    16f / 9f -> "16:9"
    21f / 9f -> stringResource(R.string.widgets_ratio_wide)
    else -> stringResource(R.string.widgets_ratio_tall)
}

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
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_iframe_title),
                stringResource(R.string.widgets_iframe_subtitle)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "content" to stringResource(R.string.widgets_tab_content),
                        "appearance" to stringResource(R.string.widgets_tab_appearance)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_content_4f9be05), stringResource(R.string.ui_the_web_page_shown_on_this_card_ce97bfd))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.ui_web_address_url_55052c9)) },
                        placeholder = { Text(stringResource(R.string.ui_example_com_or_https_8381ca6)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_title_optional_932fc13)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (settingsPage == "appearance") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_width_and_height_6ed6746))
                    Text(stringResource(R.string.ui_width_a58ddf5), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    WidgetWidthSelector(width = width, onWidthChange = { width = it })
                    Text(stringResource(R.string.ui_height_3f608b4), style = MaterialTheme.typography.labelLarge, color = appColors.onSurface)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IFRAME_RATIOS.forEach { ratio ->
                            androidx.compose.material3.FilterChip(
                                selected = kotlin.math.abs(aspect - ratio) < 0.001f,
                                onClick = { aspect = ratio },
                                label = { Text(iframeRatioLabel(ratio)) }
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
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) }
        }
    )
}
