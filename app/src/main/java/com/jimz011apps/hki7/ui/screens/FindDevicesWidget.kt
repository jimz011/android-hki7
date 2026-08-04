@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.HKIFindDevicesWidget
import com.jimz011apps.hki7.data.isButtonVisibleNow
import com.jimz011apps.hki7.data.isWidgetVisibleNow
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.MapTiles
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.components.WidgetBackground
import com.jimz011apps.hki7.ui.components.WidgetBackgroundSelector
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.components.WorldPoint
import com.jimz011apps.hki7.ui.components.clampWorldPoint
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.latLonToWorld
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.components.wrapTileX
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Model
// ─────────────────────────────────────────────────────────────────────────────

/** One plottable thing: an entity that reports coordinates, plus how to label and draw it. */
internal data class TrackedDevice(
    val entity: HAEntity,
    val name: String,
    val lat: Double?,
    val lon: Double?,
    /** `home`, `not_home`, or a zone name — shown under the name. */
    val state: String,
    val iconSlug: String,
    val pictureUrl: String?
) {
    val hasLocation: Boolean get() = lat != null && lon != null
}

/**
 * Entities worth offering in the picker: anything that can carry coordinates. `device_tracker` is
 * the obvious one, but a `person` aggregates trackers and is usually what a user means by "where is
 * X", and `sensor`s are excluded because a GPS-less tracker is still useful to list as "not home".
 */
fun findDeviceCandidates(allEntities: List<HAEntity>): List<HAEntity> =
    allEntities.filter {
        it.entity_id.startsWith("device_tracker.") || it.entity_id.startsWith("person.")
    }

private fun HAEntity.coordinate(key: String): Double? =
    attributes?.get(key)?.jsonPrimitive?.doubleOrNull

/** Picks an icon for a tracker from its own attributes, falling back on what the name suggests. */
private fun trackedDeviceIcon(entity: HAEntity): String {
    val name = (entity.friendlyName ?: entity.entity_id).lowercase()
    if (entity.entity_id.startsWith("person.")) return "account"
    return when {
        listOf("watch", "horloge").any(name::contains) -> "watch"
        listOf("tag", "airtag", "tracker").any(name::contains) -> "tag-outline"
        listOf("car", "auto", "tesla", "vehicle").any(name::contains) -> "car"
        listOf("tablet", "ipad").any(name::contains) -> "tablet"
        listOf("laptop", "macbook", "pc").any(name::contains) -> "laptop"
        listOf("bike", "fiets").any(name::contains) -> "bike"
        listOf("suitcase", "koffer", "luggage", "bag").any(name::contains) -> "bag-suitcase-outline"
        else -> "cellphone"
    }
}

@Composable
private fun resolveTrackedDevices(
    widget: HKIFindDevicesWidget,
    entities: List<HAEntity>,
    currentUrl: String
): List<TrackedDevice> = widget.entityIds
    .filter { isButtonVisibleNow(widget.itemConfigs[it] ?: HKIButtonConfig()) }
    .mapNotNull { id -> entities.find { it.entity_id == id } }
    .map { entity ->
        TrackedDevice(
            entity = entity,
            name = entity.friendlyName ?: entity.entity_id.substringAfter('.').replace('_', ' '),
            lat = entity.coordinate("latitude"),
            lon = entity.coordinate("longitude"),
            state = entity.state,
            iconSlug = trackedDeviceIcon(entity),
            pictureUrl = entity.entityPicture?.let {
                if (it.startsWith("http")) it else "${currentUrl.removeSuffix("/")}$it"
            }
        )
    }

@Composable
private fun deviceStateLabel(device: TrackedDevice): String = when (device.state.lowercase()) {
    "home" -> stringResource(R.string.widgets_find_state_home)
    "not_home" -> stringResource(R.string.widgets_find_state_away)
    "unknown", "unavailable", "" -> stringResource(R.string.widgets_find_state_unknown)
    else -> device.state.replaceFirstChar { it.uppercase() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Widget card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FindDevicesWidgetItem(
    widget: HKIFindDevicesWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onUpdate: (HKIFindDevicesWidget) -> Unit
) {
    if (!isWidgetVisibleNow(widget) && !isEditMode) return
    val entityFlow = remember(viewModel, widget.entityIds, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(widget.entityIds) else viewModel.entitiesFor(widget.entityIds)
    }
    val entities by entityFlow.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val devices = resolveTrackedDevices(widget, entities, currentUrl)
    var showDialog by remember(widget.id) { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        FindDevicesCard(
            widget = widget,
            devices = devices,
            currentUrl = currentUrl,
            modifier = Modifier.clickable(enabled = !isEditMode) { showDialog = true }
        )
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
    if (showDialog) {
        FindDevicesDialog(widget, devices, currentUrl) { showDialog = false }
    }
}

/** Same footprint, background handling and bottom-left label as the camera/vacuum/waste cards. */
@Composable
private fun FindDevicesCard(
    widget: HKIFindDevicesWidget,
    devices: List<TrackedDevice>,
    currentUrl: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val homeCount = devices.count { it.state.equals("home", ignoreCase = true) }
    val stateText = when {
        widget.entityIds.isEmpty() -> stringResource(R.string.widgets_find_no_devices_selected)
        devices.isEmpty() -> stringResource(R.string.widgets_find_no_devices_selected)
        else -> stringResource(R.string.widgets_find_summary, homeCount, devices.size)
    }

    Surface(
        modifier = modifier.fillMaxWidth()
            .aspectRatio(if (widget.isSquare) 1f else 16f / 9f)
            .clip(RoundedCornerShape(widget.cornerRadius.dp))
            .background(surfaceGradient(appColors.elevated)),
        shape = RoundedCornerShape(widget.cornerRadius.dp),
        color = Color.Transparent
    ) {
        Box {
            if (!widget.backgroundUrl.isNullOrBlank()) {
                WidgetBackground(widget.backgroundUrl, currentUrl)
            } else if (devices.size > 1) {
                // Several devices: overlap their badges, matching the waste/parcel cards.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-16).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        devices.take(4).forEach { device ->
                            DeviceBadge(device, accent, 68)
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(84.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape(21.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        MdiIcon(
                            devices.firstOrNull()?.iconSlug ?: widget.icon ?: "map-marker-radius-outline",
                            tint = accent, size = 44.dp
                        )
                    }
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, appColors.elevated.copy(alpha = 0.88f)))
                )
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = itemCornerShape()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        widget.title ?: stringResource(R.string.widgets_find_devices_title),
                        color = Color.White, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(5.dp).background(
                                if (homeCount > 0) accent else Color.White.copy(alpha = 0.5f), CircleShape
                            )
                        )
                        Text(
                            stateText, color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceBadge(device: TrackedDevice, accent: Color, size: Int) {
    val appColors = LocalHKIAppColors.current
    Box(
        Modifier.size(size.dp).background(appColors.elevated, RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center
    ) {
        if (device.pictureUrl != null) {
            AsyncImage(
                device.pictureUrl, device.name,
                Modifier.fillMaxSize().clip(RoundedCornerShape((size / 4).dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(accent.copy(alpha = 0.16f), RoundedCornerShape((size / 4).dp)),
                contentAlignment = Alignment.Center
            ) {
                MdiIcon(device.iconSlug, tint = accent, size = (size * 0.5).dp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog: map + device list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FindDevicesDialog(
    widget: HKIFindDevicesWidget,
    devices: List<TrackedDevice>,
    currentUrl: String,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    // null = "My devices", i.e. show every device at once. Otherwise the focused entity id.
    var selectedId by remember(widget.id) { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val located = devices.filter { it.hasLocation }
    val focused = selectedId?.let { id -> devices.find { it.entity.entity_id == id } }
    val shown = if (focused?.hasLocation == true) listOf(focused) else located

    AlertDialog(
        onDismissRequest = onDismiss,
        stableHeight = true,
        dismissOnTapOutside = true,
        title = { Text(widget.title ?: stringResource(R.string.widgets_find_devices_title)) },
        text = {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.fillMaxWidth().weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(appColors.subtleSurface)
                ) {
                    if (shown.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MdiIcon("map-marker-off-outline", tint = appColors.onMuted.copy(alpha = 0.4f), size = 40.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.widgets_find_no_location),
                                color = appColors.onMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        DeviceMap(devices = shown, refreshTick = refreshTick)
                    }
                }

                // Scope chip + refresh, mirroring the "My devices" control in the reference design.
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedId == null,
                        onClick = { selectedId = null },
                        leadingIcon = {
                            if (selectedId == null) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(stringResource(R.string.widgets_find_my_devices), fontSize = 12.sp) }
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            stringResource(R.string.widgets_find_refresh),
                            tint = appColors.onMuted
                        )
                    }
                }

                val listScroll = rememberScrollState()
                Column(
                    modifier = Modifier.weight(1f).fadingEdges(listScroll).verticalScroll(listScroll),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (devices.isEmpty()) {
                        Text(
                            stringResource(R.string.widgets_find_no_devices_selected),
                            color = appColors.onMuted, style = MaterialTheme.typography.bodySmall
                        )
                    }
                    devices.forEach { device ->
                        val isSelected = selectedId == device.entity.entity_id
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(itemCornerShape()).clickable {
                                // Tapping the focused device returns to "all", so the list doubles as
                                // a toggle rather than trapping the user on one pin.
                                selectedId = if (isSelected) null else device.entity.entity_id
                            },
                            shape = itemCornerShape(),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else appColors.subtleSurface
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DeviceBadge(device, MaterialTheme.colorScheme.primary, 38)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        device.name, color = appColors.onSurface,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (device.hasLocation) deviceStateLabel(device)
                                        else stringResource(R.string.widgets_find_no_gps),
                                        color = appColors.onMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * Tiled map showing one pin per device, auto-framed to fit them all.
 *
 * Shares the projection and basemap with the person map (see MapTiles). Pan and pinch are handled
 * directly rather than through a map SDK, which keeps this dependency-free and consistent with the
 * rest of the app's maps.
 */
@Composable
private fun DeviceMap(devices: List<TrackedDevice>, refreshTick: Int) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val darkTiles = MapTiles.useDarkTiles()
    val tileSizePx = with(density) { 256.dp.toPx() }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Framing key: recompute the fit when the set of pins or the viewport changes, but not on pan.
    val framingKey = remember(devices, viewportSize, refreshTick) {
        devices.joinToString(",") { "${it.entity.entity_id}:${it.lat},${it.lon}" } + "@$viewportSize"
    }

    var zoom by remember { mutableIntStateOf(15) }
    var center by remember { mutableStateOf(WorldPoint(0.0, 0.0)) }

    // Choose the deepest zoom at which every pin still fits, so one device is close-up and a
    // scattered set is pulled back far enough to see them all at once.
    androidx.compose.runtime.LaunchedEffect(framingKey) {
        if (devices.isEmpty() || viewportSize == IntSize.Zero) return@LaunchedEffect
        val padding = with(density) { 56.dp.toPx() }
        val fitZoom = (3..18).lastOrNull { candidate ->
            val points = devices.mapNotNull { d ->
                if (d.lat != null && d.lon != null) latLonToWorld(d.lat, d.lon, candidate, tileSizePx) else null
            }
            if (points.isEmpty()) return@lastOrNull false
            if (points.size == 1) return@lastOrNull candidate <= 16
            val spanX = points.maxOf { it.x } - points.minOf { it.x }
            val spanY = points.maxOf { it.y } - points.minOf { it.y }
            spanX + padding <= viewportSize.width && spanY + padding <= viewportSize.height
        } ?: 3
        zoom = fitZoom
        val points = devices.mapNotNull { d ->
            if (d.lat != null && d.lon != null) latLonToWorld(d.lat, d.lon, fitZoom, tileSizePx) else null
        }
        if (points.isNotEmpty()) {
            center = WorldPoint(
                (points.minOf { it.x } + points.maxOf { it.x }) / 2.0,
                (points.minOf { it.y } + points.maxOf { it.y }) / 2.0
            )
        }
    }

    fun setZoom(next: Int, focal: Offset? = null) {
        val clamped = next.coerceIn(3, 19)
        if (clamped == zoom) return
        val scale = 1 shl abs(clamped - zoom)
        val factor = if (clamped > zoom) scale.toDouble() else 1.0 / scale
        val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val focalPoint = focal ?: viewportCenter
        val focalOffset = focalPoint - viewportCenter
        val focalWorld = WorldPoint(center.x + focalOffset.x, center.y + focalOffset.y)
        center = clampWorldPoint(
            WorldPoint(focalWorld.x * factor - focalOffset.x, focalWorld.y * factor - focalOffset.y),
            clamped, tileSizePx
        )
        zoom = clamped
    }

    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(zoom, tileSizePx) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    if (gestureZoom > 1.15f) setZoom(zoom + 1, centroid)
                    else if (gestureZoom < 0.87f) setZoom(zoom - 1, centroid)
                    else center = clampWorldPoint(
                        WorldPoint(center.x - pan.x, center.y - pan.y), zoom, tileSizePx
                    )
                }
            }
            .pointerInput(zoom) {
                detectTapGestures(onDoubleTap = { setZoom(zoom + 1, it) })
            }
    ) {
        if (viewportSize != IntSize.Zero) {
            val leftWorld = center.x - viewportSize.width / 2.0
            val topWorld = center.y - viewportSize.height / 2.0
            val firstTileX = floor(leftWorld / tileSizePx).toInt() - 1
            val lastTileX = floor((leftWorld + viewportSize.width) / tileSizePx).toInt() + 1
            val firstTileY = floor(topWorld / tileSizePx).toInt() - 1
            val lastTileY = floor((topWorld + viewportSize.height) / tileSizePx).toInt() + 1
            val maxTile = (1 shl zoom) - 1

            for (tileY in firstTileY..lastTileY) {
                if (tileY !in 0..maxTile) continue
                for (tileX in firstTileX..lastTileX) {
                    val wrappedX = wrapTileX(tileX, zoom)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(MapTiles.url(zoom, wrappedX, tileY, darkTiles))
                            .httpHeaders(NetworkHeaders.Builder().add("User-Agent", "HKI7 Android").build())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(with(density) { tileSizePx.toDp() })
                            .offset {
                                IntOffset(
                                    x = (tileX * tileSizePx - leftWorld).roundToInt(),
                                    y = (tileY * tileSizePx - topWorld).roundToInt()
                                )
                            }
                    )
                }
            }

            devices.forEach { device ->
                val lat = device.lat ?: return@forEach
                val lon = device.lon ?: return@forEach
                val pin = latLonToWorld(lat, lon, zoom, tileSizePx)
                val markerSize = 44.dp
                val markerPx = with(density) { markerSize.toPx() }
                Box(
                    modifier = Modifier
                        .size(markerSize)
                        .offset {
                            IntOffset(
                                x = (pin.x - leftWorld - markerPx / 2).roundToInt(),
                                // Anchor the pin's point at the coordinate, not its centre.
                                y = (pin.y - topWorld - markerPx).roundToInt()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(markerSize),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (device.pictureUrl != null) {
                                AsyncImage(
                                    device.pictureUrl, device.name,
                                    Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                MdiIcon(
                                    device.iconSlug,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    size = 22.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapZoomControl(Icons.Default.Add) { setZoom(zoom + 1) }
            MapZoomControl(Icons.Default.Remove) { setZoom(zoom - 1) }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                MapTiles.MAP_ATTRIBUTION,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun MapZoomControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pickers & settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FindDevicesEntityPickerDialog(
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    AdvancedEntitySearchDialog(
        allEntities = findDeviceCandidates(allEntities),
        title = stringResource(R.string.widgets_find_select_devices),
        singleSelect = false,
        preselectedIds = emptySet(),
        onDismiss = onDismiss,
        onEntitiesSelected = onSelected
    )
}

@Composable
fun FindDevicesSettingsDialog(
    widget: HKIFindDevicesWidget,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIFindDevicesWidget) -> Unit
) {
    var entityIds by remember(widget) { mutableStateOf(widget.entityIds) }
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "map-marker-radius-outline") }
    var width by remember(widget) { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var isSquare by remember(widget) { mutableStateOf(widget.isSquare) }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("sources") }
    var itemConfigs by remember(widget) { mutableStateOf(widget.itemConfigs) }
    var editingItemVisibility by remember { mutableStateOf<String?>(null) }
    var visSpec by remember(widget) { mutableStateOf(widget.toVisibilitySpec()) }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = findDeviceCandidates(allEntities),
            title = stringResource(R.string.widgets_find_select_devices),
            singleSelect = false,
            preselectedIds = entityIds.toSet(),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { entityIds = it; showEntityPicker = false }
        )
    }
    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName = it; showIconPicker = false }
        )
    }

    val scroll = rememberScrollState()
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_find_devices_title),
                stringResource(R.string.widgets_find_devices_subtitle)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight().fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "sources" to stringResource(R.string.widgets_tab_data_sources),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "sources") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_data_sources_dadd6ac),
                        stringResource(R.string.widgets_find_sources_subtitle)
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.ui_title_768e0c1)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.widgets_find_devices_label), style = MaterialTheme.typography.labelLarge)
                    if (entityIds.isEmpty()) {
                        Text(
                            stringResource(R.string.ui_none_selected_5798946),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    entityIds.forEach { id ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                allEntities.find { it.entity_id == id }?.friendlyName ?: id,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                            IconButton(onClick = { editingItemVisibility = id }) {
                                Icon(
                                    if (isButtonVisibleNow(itemConfigs[id] ?: HKIButtonConfig())) Icons.Filled.Visibility
                                    else Icons.Filled.VisibilityOff,
                                    contentDescription = stringResource(R.string.ui_visibility_7d9ff4f)
                                )
                            }
                            IconButton(onClick = { entityIds = entityIds - id; itemConfigs = itemConfigs - id }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.action_remove))
                            }
                        }
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    Text(
                        stringResource(R.string.widgets_find_gps_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (settingsPage == "appearance") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_appearance_41def7a),
                        stringResource(R.string.ui_image_style_size_shape_and_background_40c17b6)
                    )
                    WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                    Text(stringResource(R.string.ui_shape_ea5c1a2), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isSquare, onClick = { isSquare = false }, label = { Text(stringResource(R.string.ui_standard_2dfa660)) })
                        FilterChip(selected = isSquare, onClick = { isSquare = true }, label = { Text(stringResource(R.string.ui_square_82810cb)) })
                    }
                    Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MdiIcon(iconName, size = 20.dp)
                        TextButton(onClick = { showIconPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    }
                    WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
                if (settingsPage == "visibility") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(
                        stringResource(R.string.ui_visibility_7d9ff4f),
                        stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66)
                    )
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        entityIds = entityIds,
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        width = width,
                        isSquare = isSquare,
                        cornerRadius = cornerRadius,
                        backgroundUrl = backgroundUrl,
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match,
                        itemConfigs = itemConfigs
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
    editingItemVisibility?.let { id ->
        com.jimz011apps.hki7.ui.components.ItemVisibilityDialog(
            label = allEntities.find { it.entity_id == id }?.friendlyName ?: id,
            config = itemConfigs[id] ?: HKIButtonConfig(),
            onDismiss = { editingItemVisibility = null },
            onSave = { itemConfigs = itemConfigs + (id to it) }
        )
    }
}
