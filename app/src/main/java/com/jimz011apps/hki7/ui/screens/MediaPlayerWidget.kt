package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.data.HKIMediaPlayerWidget
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.mediaPlayerStatus
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon

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
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
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
    val status = mediaPlayerStatus(entity) ?: stringResource(R.string.widgets_unavailable)

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
                        contentDescription = entity.mediaTitle,
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
    var settingsPage by remember(widget) { mutableStateOf("content") }
    if (picking) {
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter { it.entity_id.startsWith("media_player.") },
            title = stringResource(R.string.ui_select_media_player_73f4f5b),
            singleSelect = true,
            preselectedIds = setOf(entityId),
            onDismiss = { picking = false },
            onEntitiesSelected = { ids -> ids.firstOrNull()?.let { entityId = it }; picking = false }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_media_player_title),
                stringResource(R.string.widgets_media_player_subtitle)
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
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_content_4f9be05), stringResource(R.string.ui_choose_the_player_and_dashboard_label_7fa88ba))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_player_e53407c), style = MaterialTheme.typography.labelLarge)
                        Text(
                            allEntities.find { it.entity_id == entityId }?.friendlyName ?: entityId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { picking = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_optional_932fc13)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_size_shape_and_artwork_4bb38bb))
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !square, onClick = { square = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                    FilterChip(selected = square, onClick = { square = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                }
                WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(entityId = entityId, title = title.ifBlank { null }, width = width, isSquare = square, cornerRadius = radius, backgroundUrl = backgroundUrl))
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}
