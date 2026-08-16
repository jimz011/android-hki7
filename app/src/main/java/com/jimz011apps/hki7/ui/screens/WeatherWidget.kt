@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.R

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource

import com.jimz011apps.hki7.ui.components.toVisibilitySpec
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAWeatherForecast
import com.jimz011apps.hki7.data.HKIButtonConfig
import com.jimz011apps.hki7.data.HKIButtonStack
import com.jimz011apps.hki7.data.HKIWeatherWidget
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.EditRemoveBadge
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.ForecastCard
import com.jimz011apps.hki7.ui.components.HorizonCard
import com.jimz011apps.hki7.ui.components.WeatherMainCard
import androidx.compose.runtime.CompositionLocalProvider
import com.jimz011apps.hki7.ui.components.WeatherStateIcon
import com.jimz011apps.hki7.ui.components.LocalWeatherHostSurface
import com.jimz011apps.hki7.ui.components.WeatherAnimationSurface
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.components.formatWeatherState
import com.jimz011apps.hki7.ui.components.weatherStateColor
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.MdiIconPickerDialog
import com.jimz011apps.hki7.ui.components.WidgetWidthSelector
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

val weatherWidgetStyleIds = listOf("current", "forecast", "hourly", "horizon", "wind", "rainmap")

@Composable
fun weatherStyleLabel(style: String): String = when (style) {
    "forecast" -> stringResource(R.string.widgets_weather_style_daily)
    "hourly" -> stringResource(R.string.widgets_weather_style_hourly)
    "horizon" -> stringResource(R.string.widgets_weather_style_horizon)
    "wind" -> stringResource(R.string.widgets_weather_style_wind)
    "rainmap" -> stringResource(R.string.widgets_weather_style_rain_map)
    else -> stringResource(R.string.widgets_weather_style_current)
}

@Composable
fun WeatherRoomWidget(
    widget: HKIWeatherWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val defaultWeatherEntity by viewModel.weather.collectAsState()
    val specificFlow = remember(viewModel, widget.entityId, isEditMode) {
        val ids = listOfNotNull(widget.entityId)
        if (isEditMode) viewModel.entitySnapshotFor(ids) else viewModel.entitiesFor(ids)
    }
    val specificEntities by specificFlow.collectAsState()
    val weatherEntity = widget.entityId?.let { specificEntities.firstOrNull() } ?: defaultWeatherEntity
    val weatherExtras by viewModel.weatherExtraEntities.collectAsState()
    val use24h by viewModel.use24hFormat.collectAsState()
    val sunEntityId = weatherExtras["sun"] ?: "sun.sun"
    val sunFlow = remember(viewModel, sunEntityId, isEditMode) {
        if (isEditMode) viewModel.entitySnapshotFor(listOf(sunEntityId)) else viewModel.entitiesFor(listOf(sunEntityId))
    }
    val sunEntities by sunFlow.collectAsState()
    val sunEntity = sunEntities.firstOrNull()

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional icon + name header, mirroring the button stacks.
            val showHeaderLabel = !widget.title.isNullOrBlank() || !widget.icon.isNullOrBlank()
            if (showHeaderLabel) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (!widget.icon.isNullOrBlank()) {
                            MdiIcon(widget.icon, tint = Color.Gray, size = 16.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        if (!widget.title.isNullOrBlank()) {
                            Text(
                                widget.title,
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (widget.style == "horizon") {
                HorizonCard(sun = sunEntity, use24h = use24h, cornerRadius = widget.cornerRadius)
            } else if (widget.style == "rainmap") {
                RainMapCard(widget.imageUrl, widget.cornerRadius)
            } else if (weatherEntity == null) {
                Surface(
                    shape = RoundedCornerShape(widget.cornerRadius.dp),
                    color = appColors.elevated.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ui_no_weather_entity_available_7476bcb), color = appColors.onMuted)
                    }
                }
            } else {
                // These cards are shared with the weather dialog, so say which host this is —
                // the dashboard widget and the dialog have separate animation switches.
                CompositionLocalProvider(
                    LocalWeatherHostSurface provides WeatherAnimationSurface.WIDGET
                ) {
                    when (widget.style) {
                        "forecast" -> {
                            val forecasts = rememberEntityForecast(weatherEntity, viewModel, "daily")
                            ForecastCard(forecasts, cornerRadius = widget.cornerRadius)
                        }
                        "hourly" -> {
                            val forecasts = rememberEntityForecast(weatherEntity, viewModel, "hourly")
                            HourlyForecastCard(forecasts, widget.cornerRadius)
                        }
                        "wind" -> WindCompassCard(weatherEntity, widget.cornerRadius)
                        else -> WeatherMainCard(weatherEntity, cornerRadius = widget.cornerRadius)
                    }
                }
            }
        }

        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun rememberEntityForecast(weatherEntity: HAEntity, viewModel: MainViewModel, type: String): List<HAWeatherForecast> {
    val cacheKey = "${weatherEntity.entity_id}:$type"
    val cacheFlow = remember(viewModel, cacheKey) { viewModel.weatherForecastFor(cacheKey) }
    val cachedForecast by cacheFlow.collectAsState()
    LaunchedEffect(weatherEntity.entity_id, type) {
        viewModel.fetchWeatherForecastFor(weatherEntity.entity_id, type)
    }
    return cachedForecast.takeUnless { it.isEmpty() }
        ?: weatherEntity.forecast.takeUnless { type != "daily" || it.isNullOrEmpty() }
        ?: emptyList()
}

@Composable
fun HourlyForecastCard(forecasts: List<HAWeatherForecast>, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    val accent = weatherStateColor(forecasts.firstOrNull()?.condition)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), appColors.elevated.copy(alpha = 0.96f))))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_hourly_forecast_50f6c9c), style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (forecasts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    forecasts.take(12).forEach { hour -> HourlyForecastItem(hour) }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.ui_no_hourly_forecast_available_9ef5a5f), color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastItem(forecast: HAWeatherForecast) {
    val appColors = LocalHKIAppColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val timePattern = android.text.format.DateFormat.getBestDateTimePattern(
        locale,
        if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm"
    )
    val timeLabel = try {
        LocalDateTime.parse(forecast.datetime, DateTimeFormatter.ISO_DATE_TIME)
            .format(DateTimeFormatter.ofPattern(timePattern, locale))
    } catch (_: Exception) {
        forecast.datetime.take(5)
    }
    Surface(
        modifier = Modifier.width(72.dp),
        color = appColors.subtleSurface,
        shape = itemCornerShape()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(timeLabel, color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
            WeatherStateIcon(
                state = forecast.condition,
                size = 34.dp,
                surface = WeatherAnimationSurface.FORECAST,
                contentDescription = forecast.condition?.let(::formatWeatherState),
                loop = false
            )
            Text(stringResource(R.string.ui_text_7f1f581, forecast.temperature?.toInt() ?: "--"), color = appColors.onSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WindCompassCard(weather: HAEntity, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    val bearing = weather.attributes?.get("wind_bearing")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val speed = weather.windSpeed
    val gust = weather.attributes?.get("wind_gust_speed")?.jsonPrimitive?.doubleOrNull
    val needleColor = Color(0xFF4A90E2)
    val compassDirections = stringArrayResource(R.array.widgets_compass_directions)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(needleColor.copy(alpha = 0.18f), appColors.elevated.copy(alpha = 0.96f))))
        ) {
            val compact = maxWidth < 220.dp
            val compassSize = if (compact) (maxWidth - 24.dp).coerceIn(72.dp, 128.dp) else 160.dp
            Column(Modifier.padding(if (compact) 12.dp else 20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ui_wind_142b575), style = MaterialTheme.typography.labelLarge, color = appColors.onMuted, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
                Box(modifier = Modifier.size(compassSize), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCompassDial(
                            face = appColors.elevated,
                            ink = appColors.onSurface,
                            muted = appColors.onMuted,
                            compact = compact
                        )
                        drawCompassNeedle(
                            bearing = bearing,
                            north = northColor,
                            south = appColors.onMuted,
                            pivot = appColors.onSurface
                        )
                    }
                    // The cardinals ride outside the dial's tick ring rather than on the face, so
                    // the needle never crosses them. Only the four majors are lettered — at 128dp
                    // the sixteen-point rose is drawn, not written, or nothing stays legible.
                    val cardinalPad = if (compact) 0.dp else 2.dp
                    CompassCardinal(compassDirections[0], northColor, FontWeight.Bold, Modifier.align(Alignment.TopCenter).padding(top = cardinalPad))
                    CompassCardinal(compassDirections[4], appColors.onMuted, FontWeight.Medium, Modifier.align(Alignment.CenterEnd).padding(end = cardinalPad))
                    CompassCardinal(compassDirections[8], appColors.onMuted, FontWeight.Medium, Modifier.align(Alignment.BottomCenter).padding(bottom = cardinalPad))
                    CompassCardinal(compassDirections[12], appColors.onMuted, FontWeight.Medium, Modifier.align(Alignment.CenterStart).padding(start = cardinalPad))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.ui_km_h_ede7178, speed?.toInt() ?: "--") + (gust?.takeUnless { compact }?.let { stringResource(R.string.ui_gust_53f5da4, it.toInt()) } ?: ""),
                    color = appColors.onSurface,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
                )
                Text(bearingToCompassLabel(bearing, compassDirections), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun bearingToCompassLabel(bearing: Double, dirs: Array<String>): String {
    val normalized = ((bearing % 360) + 360) % 360
    val idx = (normalized / 22.5).toInt().coerceIn(0, 15)
    return dirs[idx]
}

@Composable
private fun CompassCardinal(text: String, color: Color, weight: FontWeight, modifier: Modifier) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = weight,
        modifier = modifier
    )
}

/** North is red on every compass ever made; keeping that is most of what makes this one read as one. */
private val northColor = Color(0xFFE0503A)

/**
 * The dial an instrument compass has: a recessed face, a bezel, and a graduated ring reading
 * clockwise from north — minor ticks every 15°, longer ones every 45°, longest at the cardinals —
 * with an eight-point rose beneath the needle.
 *
 * Everything is proportional to the canvas rather than fixed in dp, because this same card renders
 * at 72dp inside a compact widget and at 160dp on the weather screen, and a rose drawn in absolute
 * units turns into a smudge at the small end.
 */
private fun DrawScope.drawCompassDial(face: Color, ink: Color, muted: Color, compact: Boolean) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = size.minDimension / 2f * 0.94f

    // Face, lit from the top left the way a physical dial catches the light.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(face.copy(alpha = 0.95f), face.copy(alpha = 0.55f)),
            center = Offset(center.x - outer * 0.35f, center.y - outer * 0.35f),
            radius = outer * 1.7f
        ),
        radius = outer,
        center = center
    )
    drawCircle(muted.copy(alpha = 0.30f), radius = outer, center = center, style = Stroke(width = outer * 0.035f))
    drawCircle(muted.copy(alpha = 0.16f), radius = outer * 0.88f, center = center, style = Stroke(width = outer * 0.014f))

    // Graduated ring. Three tick lengths give the eye something to measure against, which is the
    // difference between a dial and a plain circle with a line through it.
    for (deg in 0 until 360 step 15) {
        val cardinal = deg % 90 == 0
        val intercardinal = deg % 45 == 0
        val length = outer * when {
            cardinal -> 0.16f
            intercardinal -> 0.11f
            else -> 0.06f
        }
        val width = outer * if (cardinal) 0.035f else 0.018f
        val alpha = when {
            cardinal -> 0.75f
            intercardinal -> 0.5f
            else -> 0.28f
        }
        val rad = Math.toRadians(deg.toDouble() - 90)
        val from = Offset(
            center.x + (outer - length) * cos(rad).toFloat(),
            center.y + (outer - length) * sin(rad).toFloat()
        )
        val to = Offset(center.x + outer * 0.96f * cos(rad).toFloat(), center.y + outer * 0.96f * sin(rad).toFloat())
        drawLine(if (deg == 0) northColor.copy(alpha = 0.9f) else muted.copy(alpha = alpha), from, to, strokeWidth = width)
    }

    // Eight-point rose. Dropped when compact — below about 100dp the points collide with the
    // needle and the whole face turns to mud.
    if (compact) return
    val long = outer * 0.62f
    val short = outer * 0.34f
    for (step in 0 until 8) {
        val primary = step % 2 == 0
        rotate(degrees = step * 45f, pivot = center) {
            val reach = if (primary) long else short
            val waist = if (primary) outer * 0.11f else outer * 0.07f
            val point = Path().apply {
                moveTo(center.x, center.y - reach)
                lineTo(center.x + waist, center.y)
                lineTo(center.x, center.y + waist * 0.25f)
                lineTo(center.x - waist, center.y)
                close()
            }
            // Two tones per point, so each reads as a folded facet rather than a flat triangle.
            drawPath(point, ink.copy(alpha = if (primary) 0.13f else 0.08f))
            val facet = Path().apply {
                moveTo(center.x, center.y - reach)
                lineTo(center.x + waist, center.y)
                lineTo(center.x, center.y + waist * 0.25f)
                close()
            }
            drawPath(facet, ink.copy(alpha = if (primary) 0.07f else 0.04f))
        }
    }
}

/**
 * The needle, pointing the way the wind is coming from — which is what `wind_bearing` reports.
 * Two-tone and diamond-shaped rather than a plain line: the red half is north-seeking on a real
 * compass, and here it is the business end, so the card can be read at a glance from across a room.
 */
private fun DrawScope.drawCompassNeedle(bearing: Double, north: Color, south: Color, pivot: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = size.minDimension / 2f * 0.94f
    val reach = outer * 0.70f
    val waist = outer * 0.085f

    rotate(degrees = bearing.toFloat(), pivot = center) {
        val head = Path().apply {
            moveTo(center.x, center.y - reach)
            lineTo(center.x + waist, center.y)
            lineTo(center.x - waist, center.y)
            close()
        }
        val tail = Path().apply {
            moveTo(center.x, center.y + reach * 0.72f)
            lineTo(center.x + waist, center.y)
            lineTo(center.x - waist, center.y)
            close()
        }
        drawPath(tail, south.copy(alpha = 0.45f))
        drawPath(head, north)
    }
    // Cap, drawn unrotated so it stays a circle and hides where the two halves meet.
    drawCircle(pivot.copy(alpha = 0.85f), radius = outer * 0.075f, center = center)
    drawCircle(south.copy(alpha = 0.5f), radius = outer * 0.075f, center = center, style = Stroke(width = outer * 0.02f))
}

@Composable
fun RainMapCard(imageUrl: String?, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(weatherStateColor("rainy").copy(alpha = 0.16f), appColors.elevated.copy(alpha = 0.96f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WaterDrop, null, tint = weatherStateColor("rainy"), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_rain_map_7c1862a), style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.ui_rain_map_7c1862a),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.ui_set_a_rain_map_image_url_in_this_widget_03729b8),
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherWidgetSettingsDialog(
    widget: HKIWeatherWidget,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIWeatherWidget) -> Unit
) {
    var entityId by remember(widget) { mutableStateOf(widget.entityId) }
    var style by remember(widget) { mutableStateOf(widget.style) }
    var imageUrl by remember(widget) { mutableStateOf(widget.imageUrl ?: "") }
    var title by remember(widget) { mutableStateOf(widget.title ?: "") }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "") }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var settingsPage by remember(widget) { mutableStateOf("source") }
    var visSpec by remember(widget) {
        mutableStateOf(
            widget.toVisibilitySpec()
        )
    }

    val weatherEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("weather.") } }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = weatherEntities,
            title = stringResource(R.string.ui_select_weather_entity_d489986),
            singleSelect = true,
            preselectedIds = setOfNotNull(entityId),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids -> entityId = ids.firstOrNull(); showEntityPicker = false }
        )
    }

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName = it; showIconPicker = false }
        )
    }

    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = {
            com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle(
                stringResource(R.string.widgets_weather_title),
                stringResource(R.string.widgets_weather_subtitle)
            )
        },
        text = {
            val settingsScroll = rememberScrollState()
            Column(
                modifier = Modifier.heightIn(max = 480.dp).fadingEdges(settingsScroll).verticalScroll(settingsScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf(
                        "source" to stringResource(R.string.widgets_weather_title),
                        "appearance" to stringResource(R.string.widgets_tab_appearance),
                        "visibility" to stringResource(R.string.ui_visibility_7d9ff4f)
                    ),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "source") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_weather_source_778baeb), stringResource(R.string.ui_choose_the_entity_and_card_content_8bde89a))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_title_optional_932fc13)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_appearance_41def7a), stringResource(R.string.ui_card_width_icon_and_styling_de70fd8))
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text(stringResource(R.string.ui_icon_716f63b), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iconName.isNotEmpty()) MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) { Text(if (iconName.isEmpty()) stringResource(R.string.ui_choose_78b7c9f) else stringResource(R.string.ui_change_64fbd99)) }
                    if (iconName.isNotEmpty()) TextButton(onClick = { iconName = "" }) { Text(stringResource(R.string.ui_none_6eef664)) }
                }
                }
                if (settingsPage == "source") {
                Text(stringResource(R.string.ui_weather_entity_402cbd9), style = MaterialTheme.typography.labelLarge)
                val entityName = entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entityName ?: stringResource(R.string.ui_default_weather_entity_588d455),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entityName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    if (entityId != null) { TextButton(onClick = { entityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                }
                Text(stringResource(R.string.ui_style_99a0efc), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weatherWidgetStyleIds.forEach { value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(selected = style == value, onClick = { style = value })
                            Text(weatherStyleLabel(value), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (style == "rainmap") {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text(stringResource(R.string.ui_rain_map_image_url_0c47012)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (style == "horizon") {
                    Text(
                        stringResource(R.string.ui_uses_the_sun_entity_configured_in_weather_settings_e580a39),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (settingsPage == "visibility") {
                    com.jimz011apps.hki7.ui.components.SettingsSubcategory(stringResource(R.string.ui_visibility_7d9ff4f), stringResource(R.string.ui_hide_this_button_or_schedule_when_it_appears_a28bf66))
                    com.jimz011apps.hki7.ui.components.VisibilityEditor(visSpec) { visSpec = it }
                }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    widget.copy(
                        entityId = entityId,
                        width = width,
                        style = style,
                        imageUrl = imageUrl.ifBlank { null },
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        cornerRadius = cornerRadius,
                        isHidden = visSpec.hidden,
                        visibilityStart = visSpec.start,
                        visibilityEnd = visSpec.end,
                        visibilityRangeMode = visSpec.rangeMode,
                        visibilityRecurrence = visSpec.recurrence,
                        visibilityConditionEntityId = visSpec.conditionEntityId,
                        visibilityConditionState = visSpec.conditionState,
                        visibilityConditionNegate = visSpec.conditionNegate,
                        visibilityConditions = visSpec.conditions,
                        visibilityMatch = visSpec.match
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}

/**
 * Renders the cards of a weather stack (stackType == "weather"). Each item is stored as a
 * synthetic id in [HKIButtonStack.entityIds] with its style/entity/image in buttonConfigs.
 */
@Composable
fun WeatherStackContent(
    stack: HKIButtonStack,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onItemSettings: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    if (stack.entityIds.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(stack.cornerRadius.dp),
            color = appColors.subtleSurface
        ) {
            Box(Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (isEditMode) stringResource(R.string.ui_tap_to_add_a_weather_card_a3508a9) else stringResource(R.string.ui_no_weather_cards_ce32255),
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }
    // 3 on a dashboard page, 6 inside a custom popup (see LocalMaxStackColumns).
    val columns = stack.columns.coerceIn(1, com.jimz011apps.hki7.ui.components.LocalMaxStackColumns.current)
    if (isEditMode) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stack.entityIds.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { itemId ->
                        Box(modifier = Modifier.weight(1f)) {
                            WeatherStackCard(stack.buttonConfigs[itemId], allEntities, viewModel, stack.cornerRadius)
                            EditSettingsButton(
                                onClick = { onItemSettings(itemId) },
                                modifier = Modifier.align(Alignment.Center)
                            )
                            EditRemoveBadge(
                                onClick = { onRemoveItem(itemId) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }
                    repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stack.entityIds.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { itemId ->
                        Box(modifier = Modifier.weight(1f)) {
                            WeatherStackCard(stack.buttonConfigs[itemId], allEntities, viewModel, stack.cornerRadius)
                        }
                    }
                    repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun WeatherStackCard(
    config: HKIButtonConfig?,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    cornerRadius: Int
) {
    val appColors = LocalHKIAppColors.current
    val style = config?.weatherStyle ?: "current"
    if (style == "horizon") {
        val weatherExtras by viewModel.weatherExtraEntities.collectAsState()
        val use24h by viewModel.use24hFormat.collectAsState()
        val sunEntityId = weatherExtras["sun"] ?: "sun.sun"
        val sunFlow = remember(viewModel, sunEntityId) { viewModel.entitiesFor(listOf(sunEntityId)) }
        val observedSun by sunFlow.collectAsState()
        val sunEntity = observedSun.firstOrNull()
            ?: allEntities.find { it.entity_id == sunEntityId }
            ?: allEntities.find { it.entity_id == "sun.sun" }
        HorizonCard(sun = sunEntity, use24h = use24h, cornerRadius = cornerRadius)
        return
    }
    val weatherEntity = allEntities.find { it.entity_id == config?.weatherEntityId }
        ?: allEntities.find { it.entity_id.startsWith("weather.") }
    if (weatherEntity == null && style != "rainmap") {
        Surface(
            shape = RoundedCornerShape(cornerRadius.dp),
            color = appColors.elevated.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ui_no_weather_entity_available_7476bcb), color = appColors.onMuted)
            }
        }
        return
    }
    // A card inside a weather stack is still the dashboard widget, not the dialog.
    CompositionLocalProvider(LocalWeatherHostSurface provides WeatherAnimationSurface.WIDGET) {
        when (style) {
            "forecast" -> ForecastCard(rememberEntityForecast(weatherEntity!!, viewModel, "daily"), cornerRadius = cornerRadius)
            "hourly" -> HourlyForecastCard(rememberEntityForecast(weatherEntity!!, viewModel, "hourly"), cornerRadius)
            "wind" -> WindCompassCard(weatherEntity!!, cornerRadius)
            "rainmap" -> RainMapCard(config?.weatherImageUrl, cornerRadius)
            else -> WeatherMainCard(weatherEntity!!, cornerRadius = cornerRadius)
        }
    }
}

/** Add/edit dialog for a single weather card inside a weather stack. No name/icon — the stack owns those. */
@Composable
fun WeatherItemDialog(
    initial: HKIButtonConfig?,
    allEntities: List<HAEntity>,
    onDismiss: () -> Unit,
    onSave: (HKIButtonConfig) -> Unit
) {
    var style by remember(initial) { mutableStateOf(initial?.weatherStyle ?: "current") }
    var entityId by remember(initial) { mutableStateOf(initial?.weatherEntityId) }
    var imageUrl by remember(initial) { mutableStateOf(initial?.weatherImageUrl ?: "") }
    var showEntityPicker by remember { mutableStateOf(false) }
    val weatherEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("weather.") } }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = weatherEntities,
            title = stringResource(R.string.ui_select_weather_entity_d489986),
            singleSelect = true,
            preselectedIds = setOfNotNull(entityId),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids -> entityId = ids.firstOrNull(); showEntityPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_weather_card_0c6594c)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.ui_weather_entity_402cbd9), style = MaterialTheme.typography.labelLarge)
                val entityName = entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entityName ?: stringResource(R.string.ui_default_weather_entity_588d455),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entityName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text(stringResource(R.string.ui_change_64fbd99)) }
                    if (entityId != null) { TextButton(onClick = { entityId = null }) { Text(stringResource(R.string.ui_clear_719ea39)) } }
                }
                Text(stringResource(R.string.ui_style_99a0efc), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weatherWidgetStyleIds.forEach { value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(selected = style == value, onClick = { style = value })
                            Text(weatherStyleLabel(value), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (style == "rainmap") {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text(stringResource(R.string.ui_rain_map_image_url_0c47012)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (style == "horizon") {
                    Text(
                        stringResource(R.string.ui_uses_the_sun_entity_configured_in_weather_settings_e580a39),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    (initial ?: HKIButtonConfig()).copy(
                        weatherStyle = style,
                        weatherEntityId = entityId,
                        weatherImageUrl = imageUrl.ifBlank { null }
                    )
                )
            }) { Text(stringResource(R.string.ui_save_efc007a)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd21)) } }
    )
}
