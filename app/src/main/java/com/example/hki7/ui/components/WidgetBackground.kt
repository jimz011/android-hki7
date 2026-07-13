package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Draws a widget's custom background image (a full URL or an HA path, resolved via
 * [resolveCameraUrl]) plus a subtle scrim for legibility. Emit it as the FIRST child of a card's
 * `Box` so the widget content draws on top. Uses `matchParentSize` so it fills the card regardless
 * of whether the card's height is fixed (aspect-ratio) or wraps its content. No-op when
 * [backgroundUrl] is blank/unresolvable.
 */
@Composable
fun BoxScope.WidgetBackground(backgroundUrl: String?, currentUrl: String) {
    val resolved = remember(backgroundUrl, currentUrl) { resolveCameraUrl(backgroundUrl, currentUrl) }
    if (resolved.isNullOrBlank()) return
    AsyncImage(
        model = resolved,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize()
    )
    Box(
        Modifier.matchParentSize().background(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.40f))
            )
        )
    )
}

/** Settings-dialog field for a widget's background image (URL or HA path). Passes null when blank. */
@Composable
fun WidgetBackgroundSelector(url: String?, onUrlChange: (String?) -> Unit) {
    OutlinedTextField(
        value = url ?: "",
        onValueChange = { onUrlChange(it.ifBlank { null }) },
        label = { Text("Background image (URL or path)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
