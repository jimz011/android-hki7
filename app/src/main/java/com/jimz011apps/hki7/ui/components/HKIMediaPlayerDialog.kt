@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAMediaBrowseItem
import com.jimz011apps.hki7.data.HAServiceCall
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds

/** Extra scroll space needed while the overlay mini-player is visible. */
val LocalMediaPlayerBarInset = staticCompositionLocalOf { 0.dp }

// media_player supported_features bits (subset used here).
private const val MP_PAUSE = 1
private const val MP_SEEK = 2
private const val MP_VOLUME_SET = 4
private const val MP_VOLUME_MUTE = 8
private const val MP_PREVIOUS = 16
private const val MP_NEXT = 32
private const val MP_SELECT_SOURCE = 2048
private const val MP_PLAY = 16384
private const val MP_SHUFFLE = 32768
private const val MP_BROWSE_MEDIA = 131072
private const val MP_REPEAT = 262144

private fun HAEntity.supportsMedia(flag: Int) = supportedFeatures and flag != 0

private fun resolveMediaImage(url: String?, currentUrl: String): String? =
    url?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}$it" }

private fun formatMediaTime(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Live playback position: media_position plus wall time elapsed since HA reported it. */
private fun mediaProgressSeconds(entity: HAEntity, nowMillis: Long): Double? {
    val base = entity.mediaPosition ?: return null
    if (entity.state != "playing") return base
    val updatedAt = entity.mediaPositionUpdatedAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: return base
    val elapsed = (nowMillis - updatedAt) / 1000.0
    val duration = entity.mediaDuration ?: Double.MAX_VALUE
    return (base + elapsed).coerceIn(0.0, duration)
}

// ─────────────────────────────────────────────────────────────────────────────
// Full media player dialog (standard HKIDialog header: name/icon/last seen,
// close, history/activity and related device entities)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HKIMediaPlayerDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    currentUrl: String,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val isPlaying = entity.state == "playing"
    val artwork = resolveMediaImage(entity.entityPicture, currentUrl)
    val duration = entity.mediaDuration
    val accent = MaterialTheme.colorScheme.primary

    // 1s ticker so the progress bar advances between HA updates.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isPlaying) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1.seconds)
        }
    }
    val position = mediaProgressSeconds(entity, nowMillis)
    var seekDrag by remember(entity.entity_id) { mutableStateOf<Float?>(null) }
    val progressFraction = when {
        seekDrag != null -> seekDrag!!
        duration != null && duration > 0 && position != null -> (position / duration).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    var showSources by remember(entity.entity_id) { mutableStateOf(false) }
    var showBrowser by remember(entity.entity_id) { mutableStateOf(false) }

    fun service(name: String, call: HAServiceCall) = viewModel.callService("media_player", name, call)

    if (showBrowser) {
        MediaBrowseDialog(
            entityId = entity.entity_id,
            viewModel = viewModel,
            currentUrl = currentUrl,
            onPlay = { item ->
                service("play_media", HAServiceCall(
                    entity.entity_id,
                    media_content_id = item.media_content_id,
                    media_content_type = item.media_content_type
                ))
            },
            onShufflePlay = { item ->
                service("shuffle_set", HAServiceCall(entity.entity_id, shuffle = true))
                service("play_media", HAServiceCall(
                    entity.entity_id,
                    media_content_id = item.media_content_id,
                    media_content_type = item.media_content_type
                ))
            },
            onDismiss = { showBrowser = false }
        )
    }

    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.MusicNote,
        iconTint = accent,
        iconName = "speaker",
        statusText = entity.state.replace('_', ' ').uppercase()
    ) { _ ->
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Keep the artwork width-constrained. A height-first aspect ratio can grow wider
            // than the dialog on tall phones and visually touch the screen edges.
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(itemCornerShape()).background(appColors.subtleSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        AsyncImage(artwork, entity.mediaTitle, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = appColors.onMuted, modifier = Modifier.size(64.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                entity.mediaTitle ?: entity.friendlyName ?: entity.entity_id,
                color = appColors.onSurface, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val subtitle = listOfNotNull(
                entity.mediaArtist?.takeIf { it.isNotBlank() },
                entity.mediaAlbumName?.takeIf { it.isNotBlank() }
            ).joinToString(" - ").ifBlank { entity.appName.orEmpty() }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            // Progress
            if (duration != null && duration > 0) {
                Spacer(Modifier.height(2.dp))
                HKISlider(
                    value = progressFraction,
                    onValueChange = { if (entity.supportsMedia(MP_SEEK)) seekDrag = it },
                    onValueChangeFinished = {
                        seekDrag?.let { fraction ->
                            service("media_seek", HAServiceCall(entity.entity_id, seek_position = fraction * duration))
                        }
                        seekDrag = null
                    },
                    enabled = entity.supportsMedia(MP_SEEK),
                    colors = SliderDefaults.colors(disabledActiveTrackColor = accent),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMediaTime(((seekDrag?.toDouble()?.times(duration)) ?: position ?: 0.0).toLong()),
                        color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    Text(formatMediaTime(duration.toLong()), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Transport controls
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val shuffleOn = entity.mediaShuffle == true
                IconButton(
                    onClick = { service("shuffle_set", HAServiceCall(entity.entity_id, shuffle = !shuffleOn)) },
                    enabled = entity.supportsMedia(MP_SHUFFLE)
                ) {
                    Icon(Icons.Default.Shuffle, "Shuffle",
                        tint = if (shuffleOn) accent
                            else appColors.onMuted.copy(alpha = if (entity.supportsMedia(MP_SHUFFLE)) 1f else 0.35f))
                }
                IconButton(
                    onClick = { service("media_previous_track", HAServiceCall(entity.entity_id)) },
                    enabled = entity.supportsMedia(MP_PREVIOUS)
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = appColors.onSurface, modifier = Modifier.size(32.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .clickable(enabled = entity.supportsMedia(MP_PLAY) || entity.supportsMedia(MP_PAUSE)) {
                            service("media_play_pause", HAServiceCall(entity.entity_id))
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.Black, modifier = Modifier.size(34.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { service("media_next_track", HAServiceCall(entity.entity_id)) },
                    enabled = entity.supportsMedia(MP_NEXT)
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = appColors.onSurface, modifier = Modifier.size(32.dp))
                }
                val repeatMode = entity.mediaRepeat ?: "off"
                IconButton(
                    onClick = {
                        val next = when (repeatMode) { "off" -> "all"; "all" -> "one"; else -> "off" }
                        service("repeat_set", HAServiceCall(entity.entity_id, repeat = next))
                    },
                    enabled = entity.supportsMedia(MP_REPEAT)
                ) {
                    Icon(
                        if (repeatMode == "one") Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat",
                        tint = if (repeatMode != "off") accent
                            else appColors.onMuted.copy(alpha = if (entity.supportsMedia(MP_REPEAT)) 1f else 0.35f)
                    )
                }
            }

            // Bottom pill: only the capabilities this player actually has.
            val hasSources = entity.sourceList.isNotEmpty() && entity.supportsMedia(MP_SELECT_SOURCE)
            val hasBrowse = entity.supportsMedia(MP_BROWSE_MEDIA)
            val hasMute = entity.supportsMedia(MP_VOLUME_MUTE)
            val hasVolume = entity.supportsMedia(MP_VOLUME_SET)
            if (hasSources || hasBrowse || hasMute || hasVolume) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = itemCornerShape(), color = appColors.subtleSurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasBrowse) {
                            IconButton(onClick = { showBrowser = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, "Playlists", tint = appColors.onMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (hasSources) {
                            IconButton(onClick = { showSources = !showSources }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Speaker, "Source",
                                    tint = if (showSources) accent else appColors.onMuted,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                        if (hasMute) {
                            val muted = entity.isVolumeMuted == true
                            IconButton(
                                onClick = { service("volume_mute", HAServiceCall(entity.entity_id, is_volume_muted = !muted)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    "Mute", tint = appColors.onMuted, modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (hasVolume) {
                            var volumeDrag by remember(entity.entity_id) { mutableStateOf<Float?>(null) }
                            val volume = volumeDrag ?: (entity.volumeLevel ?: 0.0).toFloat()
                            HKISlider(
                                value = volume.coerceIn(0f, 1f),
                                onValueChange = { volumeDrag = it },
                                onValueChangeFinished = {
                                    volumeDrag?.let { service("volume_set", HAServiceCall(entity.entity_id, volume_level = it)) }
                                    volumeDrag = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (showSources && hasSources) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entity.sourceList.forEach { src ->
                        FilterChip(
                            selected = src == entity.mediaSource,
                            onClick = { service("select_source", HAServiceCall(entity.entity_id, source = src)) },
                            label = { Text(src, maxLines = 1) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Media library browser (playlists, favorites, …) via media_player/browse_media
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaBrowseDialog(
    entityId: String,
    viewModel: MainViewModel,
    currentUrl: String,
    onPlay: (HAMediaBrowseItem) -> Unit,
    onShufflePlay: (HAMediaBrowseItem) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var stack by remember { mutableStateOf<List<HAMediaBrowseItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(entityId) {
        loading = true
        val root = viewModel.browseMedia(entityId)
        if (root != null) stack = listOf(root) else failed = true
        loading = false
    }

    fun drillInto(item: HAMediaBrowseItem) {
        scope.launch {
            loading = true
            val child = viewModel.browseMedia(entityId, item.media_content_id, item.media_content_type)
            if (child != null) stack = stack + child
            loading = false
        }
    }

    val current = stack.lastOrNull()
    val children = current?.children.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false),
        title = {
            androidx.activity.compose.BackHandler {
                if (stack.size > 1) stack = stack.dropLast(1) else onDismiss()
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (stack.size > 1) {
                    IconButton(onClick = { stack = stack.dropLast(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(current?.title?.takeIf { it.isNotBlank() } ?: "Media library",
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        },
        text = {
            when {
                loading && current == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                failed -> Text("Media browsing isn't available for this player.", color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                else -> {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    Box {
                        LazyColumn(
                            Modifier.heightIn(max = 520.dp).fadingEdges(listState),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (current != null && (current.thumbnail != null || current.can_play)) {
                                item {
                                    val heroThumb = resolveMediaImage(current.thumbnail, currentUrl)
                                    Row(
                                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                                                .background(colors.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (heroThumb != null) {
                                                AsyncImage(heroThumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = colors.onSurfaceVariant,
                                                    modifier = Modifier.size(34.dp))
                                            }
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(
                                                current.title ?: "Media",
                                                color = colors.onSurface,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                listOfNotNull(
                                                    current.media_class?.replace('_', ' ')?.replaceFirstChar(Char::uppercase),
                                                    children.takeIf { it.isNotEmpty() }?.let { "${it.size} items" }
                                                ).joinToString(" • "),
                                                color = colors.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (current.can_play) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    listOf(
                                                        Triple(Icons.Default.PlayArrow, "Play", onPlay),
                                                        Triple(Icons.Default.Shuffle, "Shuffle", onShufflePlay)
                                                    ).forEach { (icon, label, action) ->
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = colors.primary,
                                                            modifier = Modifier.clickable(enabled = !loading) { action(current) }
                                                        ) {
                                                            Row(
                                                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                            ) {
                                                                Icon(icon, null, tint = colors.onPrimary, modifier = Modifier.size(17.dp))
                                                                Text(label, color = colors.onPrimary, style = MaterialTheme.typography.labelLarge)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (children.isEmpty() && !loading) {
                                item {
                                    Text("Nothing here.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                            itemsIndexed(children) { index, child ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(enabled = !loading) {
                                            if (child.can_expand) drillInto(child) else if (child.can_play) onPlay(child)
                                        }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val thumb = resolveMediaImage(child.thumbnail, currentUrl)
                                        Box(
                                            Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(colors.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (thumb != null) {
                                                AsyncImage(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            } else {
                                                Icon(
                                                    when {
                                                        child.can_expand -> Icons.Default.Folder
                                                        child.media_class == "playlist" -> Icons.AutoMirrored.Filled.PlaylistPlay
                                                        else -> Icons.Default.MusicNote
                                                    },
                                                    null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            (index + 1).toString(),
                                            color = colors.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                child.title?.takeIf { it.isNotBlank() } ?: child.media_content_id ?: "Untitled",
                                                color = colors.onSurface, style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            val metadata = listOfNotNull(
                                                child.artist?.takeIf { it.isNotBlank() },
                                                child.media_class
                                                    ?.takeUnless { child.can_play && it.equals("track", ignoreCase = true) }
                                                    ?.replace('_', ' ')
                                                    ?.replaceFirstChar(Char::uppercase)
                                            ).joinToString(" • ")
                                            if (metadata.isNotBlank()) Text(
                                                metadata, color = colors.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall, maxLines = 1
                                            )
                                        }
                                        child.duration?.takeIf { it > 0 }?.let { duration ->
                                            Text(
                                                formatMediaTime(duration.toLong()),
                                                color = colors.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Surface(shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(34.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (child.can_expand) Icons.Default.ChevronRight else Icons.Default.PlayArrow,
                                                    if (child.can_expand) "Open" else "Play",
                                                    tint = colors.onSurface, modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (loading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 3.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Mini player bar: shown above the navigation bar while media plays
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MediaPlayerMiniBar(
    players: List<HAEntity>,
    currentUrl: String,
    viewModel: MainViewModel,
    onOpen: (HAEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    // Match the notification banner: this transient overlay deliberately contrasts with the
    // active app theme (dark over light themes, light over dark themes).
    val backgroundIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val barBackground = if (backgroundIsLight) Color(0xFF211F24) else Color(0xFFF4F0F5)
    val barForeground = if (backgroundIsLight) Color(0xFFF7F2F8) else Color(0xFF211F24)
    val barMuted = barForeground.copy(alpha = 0.68f)
    val barSubtle = barForeground.copy(alpha = 0.10f)
    val barShape = itemCornerShape()
    val pagerState = rememberPagerState(pageCount = { players.size })

    // 1s ticker so the mini progress bar advances between HA updates.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1.seconds)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(80.dp).clip(barShape),
        shape = barShape,
        color = barBackground,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, barForeground.copy(alpha = 0.10f))
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(surfaceGradient(barBackground))
            )
            // One page per active player; swipe left/right to switch players.
            HorizontalPager(state = pagerState) { page ->
            val player = players.getOrNull(page) ?: return@HorizontalPager
            val artwork = resolveMediaImage(player.entityPicture, currentUrl)
            Column(Modifier.fillMaxSize().clickable { onOpen(player) }) {
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(barSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artwork != null) {
                            AsyncImage(artwork, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = barMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        // Player name on its own line so it's always clear which device is playing.
                        Text(
                            player.friendlyName ?: player.entity_id,
                            color = barMuted, style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            player.mediaTitle ?: player.state.replaceFirstChar(Char::uppercase),
                            color = barForeground, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        player.mediaArtist?.takeIf { it.isNotBlank() }?.let { artist ->
                            Text(
                                artist,
                                color = barMuted, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (players.size > 1) {
                        Text(
                            "${page + 1}/${players.size}",
                            color = barMuted, style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.callService("media_player", "media_previous_track", HAServiceCall(player.entity_id)) },
                            enabled = player.supportsMedia(MP_PREVIOUS),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                "Previous",
                                tint = barForeground.copy(alpha = if (player.supportsMedia(MP_PREVIOUS)) 1f else 0.35f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.callService("media_player", "media_play_pause", HAServiceCall(player.entity_id)) },
                            enabled = player.supportsMedia(MP_PLAY) || player.supportsMedia(MP_PAUSE),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (player.state == "playing") Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (player.state == "playing") "Pause" else "Play",
                                tint = barForeground.copy(
                                    alpha = if (player.supportsMedia(MP_PLAY) || player.supportsMedia(MP_PAUSE)) 1f else 0.35f
                                ),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.callService("media_player", "media_next_track", HAServiceCall(player.entity_id)) },
                            enabled = player.supportsMedia(MP_NEXT),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                "Next",
                                tint = barForeground.copy(alpha = if (player.supportsMedia(MP_NEXT)) 1f else 0.35f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                // Elapsed / progress / remaining, when the player reports a duration.
                val duration = player.mediaDuration
                val position = mediaProgressSeconds(player, nowMillis)
                if (duration != null && duration > 0 && position != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(formatMediaTime(position.toLong()), color = barMuted,
                            style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        val progress = (position / duration).toFloat().coerceIn(0f, 1f)
                        val trackColor = barForeground.copy(alpha = 0.18f)
                        val progressColor = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.weight(1f).height(3.dp)) {
                            val radius = size.height / 2f
                            drawRoundRect(
                                color = trackColor,
                                size = size,
                                cornerRadius = CornerRadius(radius, radius)
                            )
                            if (progress > 0f) {
                                drawRoundRect(
                                    color = progressColor,
                                    size = Size(size.width * progress, size.height),
                                    cornerRadius = CornerRadius(radius, radius)
                                )
                            }
                        }
                        Text("-${formatMediaTime((duration - position).toLong())}", color = barMuted,
                            style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    }
                }
            }
        }
        }
    }
}
