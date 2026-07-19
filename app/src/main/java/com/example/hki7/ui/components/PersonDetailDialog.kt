package com.example.hki7.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HKIActionButton
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

@Composable
fun PersonDetailDialog(
    person: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    // Settings needs the full catalog for the entity pickers; normal view only the person.
    val personEntityFlow = remember(viewModel, person.entity_id, isEditMode) {
        if (isEditMode) viewModel.entitiesMatching { true }
        else viewModel.entitiesFor(listOf(person.entity_id))
    }
    val allEntities by personEntityFlow.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val livePerson = allEntities.find { it.entity_id == person.entity_id } ?: person
    val imageUrl = livePerson.entityPicture?.let {
        if (it.startsWith("http") || it.startsWith("content:") || it.startsWith("file:")) it else "$currentUrl$it"
    }

    val pageConfigs by viewModel.pageConfigsMapping.collectAsState()
    val homeConfig = pageConfigs["home"] ?: HKIPageConfig()
    val personButtons = homeConfig.personButtons[person.entity_id] ?: emptyList()

    // Edit mode: tapping a person opens its individual settings (the entity dialog auto-dismisses in edit
    // mode, so the settings live in their own dialog here rather than inside the person dialog).
    if (isEditMode) {
        PersonSettingsDialog(
            person = livePerson,
            viewModel = viewModel,
            allEntities = allEntities,
            areas = areas,
            homeConfig = homeConfig,
            personButtons = personButtons,
            onDismiss = onDismiss
        )
        return
    }

    val lat = livePerson.attributes?.get("latitude")?.jsonPrimitive?.doubleOrNull
    val lon = livePerson.attributes?.get("longitude")?.jsonPrimitive?.doubleOrNull

    // Reverse-geocode the coordinates to a human-readable address, shown under the map.
    var address by remember(lat, lon) { mutableStateOf<String?>(null) }
    LaunchedEffect(lat, lon) {
        address = if (lat != null && lon != null) reverseGeocode(lat, lon) else null
    }

    HKIDialog(
        entity = livePerson,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Person,
        headerImageUrl = imageUrl,
        statusText = livePerson.state.uppercase(),
        customButtons = personButtons
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Map
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
                    .clip(itemCornerShape()),
                color = if (lat != null && lon != null) Color.Black else appColors.elevated
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (lat != null && lon != null) {
                        OpenStreetMapPreview(lat = lat, lon = lon, imageUrl = imageUrl)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Location Unavailable", color = appColors.onMuted)
                        }
                    }
                }
            }

            // Geocoded location under the map: centered pill, styled like the battery widget's state chip.
            if (lat != null && lon != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = appColors.subtleSurface,
                        border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                address ?: "Locating…",
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Standalone person settings dialog shown when a person is tapped in edit mode. */
@Composable
private fun PersonSettingsDialog(
    person: HAEntity,
    viewModel: MainViewModel,
    allEntities: List<HAEntity>,
    areas: List<com.example.hki7.data.HAArea>,
    homeConfig: HKIPageConfig,
    personButtons: List<HKIActionButton>,
    onDismiss: () -> Unit
) {
    ModernSettingsDialogFrame(
        title = "Person settings",
        subtitle = person.friendlyName ?: person.entity_id,
        icon = Icons.Default.Person,
        onDismiss = onDismiss,
        content = {
            PersonSettingsView(
                person = person,
                viewModel = viewModel,
                allEntities = allEntities,
                areas = areas,
                homeConfig = homeConfig,
                personButtons = personButtons,
                onBack = onDismiss,
                showHeader = false
            )
        },
        footer = { Button(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun PersonSettingsView(
    person: HAEntity,
    viewModel: MainViewModel,
    allEntities: List<HAEntity>,
    areas: List<com.example.hki7.data.HAArea>,
    homeConfig: HKIPageConfig,
    personButtons: List<HKIActionButton>,
    onBack: () -> Unit,
    showHeader: Boolean = true
) {
    val appColors = LocalHKIAppColors.current
    val isVisible = person.entity_id !in homeConfig.hiddenPeople

    Column(
        modifier = Modifier
            .padding(if (showHeader) 24.dp else 0.dp)
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (showHeader) {
            ModernSettingsHeader(
                title = "Person settings",
                subtitle = person.friendlyName ?: person.entity_id,
                icon = Icons.Default.Person,
                onClose = onBack
            )
            Spacer(Modifier.height(16.dp))
        }

        SettingsSubcategory("Visibility", "Choose whether this person appears in the Home header")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isVisible,
                onCheckedChange = { checked ->
                    viewModel.updatePageConfig(
                        "home",
                        homeConfig.copy(
                            hiddenPeople = if (checked) {
                                homeConfig.hiddenPeople - person.entity_id
                            } else {
                                (homeConfig.hiddenPeople + person.entity_id).distinct()
                            }
                        )
                    )
                }
            )
            Text("Show in header", color = appColors.onSurface)
        }

        Spacer(Modifier.height(16.dp))

        SettingsSubcategory("Quick actions", "Buttons available from this person's detail dialog")
        Spacer(Modifier.height(8.dp))
        CustomButtonsEditor(
            buttons = personButtons,
            allEntities = allEntities,
            areas = areas,
            onChange = { updated ->
                viewModel.updatePageConfig(
                    "home",
                    homeConfig.copy(personButtons = homeConfig.personButtons + (person.entity_id to updated))
                )
            }
        )
    }
}

/** Reverse-geocodes coordinates to a display address via OpenStreetMap's Nominatim service.
 *  Uses the same "HKI7 Android" User-Agent as the map tiles; returns null on any failure. */
private val geocodeJson = Json { ignoreUnknownKeys = true }

private suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=16&addressdetails=0")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "HKI7 Android")
            connectTimeout = 8000
            readTimeout = 8000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        geocodeJson.parseToJsonElement(body)
            .jsonObject["display_name"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}

@Composable
private fun OpenStreetMapPreview(lat: Double, lon: Double, imageUrl: String?) {
    var zoom by remember(lat, lon) { mutableIntStateOf(15) }
    val context = LocalContext.current
    val density = LocalDensity.current
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val tileSizePx = with(density) { 256.dp.toPx() }
    var center by remember(lat, lon, tileSizePx) {
        mutableStateOf(latLonToWorld(lat, lon, zoom, tileSizePx))
    }
    val markerSize = 64.dp
    val markerSizePx = with(density) { markerSize.toPx() }

    fun setZoom(nextZoom: Int, focal: Offset? = null) {
        val clampedZoom = nextZoom.coerceIn(3, 19)
        if (clampedZoom == zoom) return

        val scale = 1 shl kotlin.math.abs(clampedZoom - zoom)
        val zoomFactor = if (clampedZoom > zoom) scale.toDouble() else 1.0 / scale.toDouble()
        val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val focalPoint = focal ?: viewportCenter
        val focalOffset = focalPoint - viewportCenter
        val focalWorld = WorldPoint(center.x + focalOffset.x, center.y + focalOffset.y)

        center = clampWorldPoint(
            WorldPoint(
                x = focalWorld.x * zoomFactor - focalOffset.x,
                y = focalWorld.y * zoomFactor - focalOffset.y
            ),
            clampedZoom,
            tileSizePx
        )
        zoom = clampedZoom
    }

    LaunchedEffect(lat, lon, tileSizePx) {
        zoom = 15
        center = latLonToWorld(lat, lon, 15, tileSizePx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(itemCornerShape())
            .background(Color(0xFF151515))
            .onSizeChanged { viewportSize = it }
            .pointerInput(lat, lon, zoom, viewportSize, tileSizePx) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    center = clampWorldPoint(
                        WorldPoint(center.x - pan.x, center.y - pan.y),
                        zoom,
                        tileSizePx
                    )

                    if (gestureZoom > 1.18f) {
                        setZoom(zoom + 1, centroid)
                    } else if (gestureZoom < 0.85f) {
                        setZoom(zoom - 1, centroid)
                    }
                }
            }
    ) {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
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
                            .data("https://tile.openstreetmap.org/$zoom/$wrappedX/$tileY.png")
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

            val markerWorld = latLonToWorld(lat, lon, zoom, tileSizePx)
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (markerWorld.x - leftWorld - markerSizePx / 2f).roundToInt(),
                            y = (markerWorld.y - topWorld - markerSizePx / 2f).roundToInt()
                        )
                    }
                    .size(markerSize),
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White),
                color = Color.DarkGray,
                shadowElevation = 8.dp
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = Color.LightGray, modifier = Modifier.padding(12.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapZoomButton(icon = Icons.Default.Add, onClick = { setZoom(zoom + 1) })
            MapZoomButton(icon = Icons.Default.Remove, onClick = { setZoom(zoom - 1) })
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "OpenStreetMap",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun MapZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp).clickable { onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

private data class WorldPoint(val x: Double, val y: Double)

private fun latLonToWorld(lat: Double, lon: Double, zoom: Int, tileSizePx: Float): WorldPoint {
    val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
    val latRad = Math.toRadians(clampedLat)
    val tileCount = 1 shl zoom
    val x = (lon + 180.0) / 360.0 * tileCount
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * tileCount
    return WorldPoint(x * tileSizePx, y.coerceIn(0.0, tileCount - 1.0) * tileSizePx)
}

private fun clampWorldPoint(point: WorldPoint, zoom: Int, tileSizePx: Float): WorldPoint {
    val worldSize = (1 shl zoom) * tileSizePx
    return WorldPoint(
        x = ((point.x % worldSize) + worldSize) % worldSize,
        y = point.y.coerceIn(0.0, worldSize.toDouble())
    )
}

private fun wrapTileX(x: Int, zoom: Int): Int {
    val tileCount = 1 shl zoom
    return ((x % tileCount) + tileCount) % tileCount
}
