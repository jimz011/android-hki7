package com.example.hki7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIMediaPlayerWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.WidgetBackground
import com.example.hki7.ui.components.WidgetBackgroundSelector
import com.example.hki7.ui.components.mediaPlayerStatus
import com.example.hki7.ui.components.surfaceGradient
import com.example.hki7.ui.components.itemCornerShape
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon

/** Media player card that uses the current album art (entity_picture) as its background.
 *  Tapping opens the full media player dialog via [onOpen]. */
@Composable
fun MediaPlayerWidgetItem(
    widget: HKIMediaPlayerWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onOpen: (String) -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val entityFlow = remember(viewModel, widget.entityId) {
        viewModel.entitiesMatching("id:${widget.entityId}") { it.entity_id == widget.entityId }
    }
    val entities by entityFlow.collectAsState()
    val entity: HAEntity? = entities.firstOrNull()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val artwork = entity?.entityPicture?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}$it"
    }
    val name = widget.title ?: entity?.friendlyName ?: widget.entityId
    val status = mediaPlayerStatus(entity) ?: "Unavailable"

    Box {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
                .clip(RoundedCornerShape(widget.cornerRadius.dp))
                .background(surfaceGradient(appColors.elevated))
                .clickable(enabled = !isEditMode) { onOpen(widget.entityId) },
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = Color.Transparent
        ) {
            Box {
                if (!widget.backgroundUrl.isNullOrBlank()) {
                    // A configured background image overrides the album art.
                    WidgetBackground(widget.backgroundUrl, currentUrl)
                } else if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = entity?.mediaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Scrim so the overlay text stays readable on bright artwork.
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)))
                    ))
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(84.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MdiIcon(widget.icon ?: "speaker", tint = MaterialTheme.colorScheme.primary, size = 44.dp)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = itemCornerShape()
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(name, color = Color.White, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(status, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
fun MediaPlayerWidgetSettingsDialog(
    widget: HKIMediaPlayerWidget,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIMediaPlayerWidget) -> Unit
) {
    var entityId by remember(widget) { mutableStateOf(widget.entityId) }
    var title by remember(widget) { mutableStateOf(widget.title.orEmpty()) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var square by remember(widget) { mutableStateOf(widget.isSquare) }
    var radius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("media_player.") },
            title = "Select Media Player",
            singleSelect = true,
            preselectedIds = setOf(entityId),
            onDismiss = { picking = false },
            onEntitiesSelected = { ids -> ids.firstOrNull()?.let { entityId = it }; picking = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media Player Widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Player", style = MaterialTheme.typography.labelLarge)
                        Text(
                            allEntities.find { it.entity_id == entityId }?.friendlyName ?: entityId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { picking = true }) { Text("Change") }
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !square, onClick = { square = false }, label = { Text("Standard") })
                    FilterChip(selected = square, onClick = { square = true }, label = { Text("Square") })
                }
                WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(entityId = entityId, title = title.ifBlank { null }, width = width, isSquare = square, cornerRadius = radius, backgroundUrl = backgroundUrl))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
