package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAWeatherForecast
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HKIWeatherDialog(
    weather: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    settingsTitle: String = "Header Pill",
    displayType: String? = null,
    alarmEntityIds: List<String> = emptyList(),
    onDisplayTypeSelected: ((String) -> Unit)? = null,
    onAlarmEntitiesSelected: ((List<String>) -> Unit)? = null
) {
    val allEntities by viewModel.entities.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val use24h by viewModel.use24hFormat.collectAsState()
    val extraEntities by viewModel.weatherExtraEntities.collectAsState()
    val fetchedForecast by viewModel.weatherForecast.collectAsState()
    val forecastCache by viewModel.weatherForecastCache.collectAsState()
    val currentDisplayType = if (isEditMode) displayType ?: "Weather" else "Weather"

    // Modern HA no longer puts forecasts in the entity attributes; fetch (TTL-cached) on open.
    LaunchedEffect(weather.entity_id) { viewModel.fetchWeatherForecastFor(weather.entity_id, "daily") }
    val dialogForecast = weather.forecast.takeUnless { it.isNullOrEmpty() }
        ?: forecastCache["${weather.entity_id}:daily"].takeUnless { it.isNullOrEmpty() }
        ?: fetchedForecast

    // One id->entity map + role resolution per state batch; the previous five full-list scans on
    // every websocket update were a big part of why this dialog felt sluggish.
    val roleEntities = remember(allEntities, extraEntities) {
        val byId = allEntities.associateBy { it.entity_id }
        fun pick(role: String, fallback: (HAEntity) -> Boolean): HAEntity? =
            extraEntities[role]?.let { byId[it] } ?: allEntities.find(fallback)
        listOf(
            pick("sun") { it.entity_id == "sun.sun" },
            pick("moon") { it.entity_id == "sensor.moon" },
            pick("aqi") { it.entity_id.contains("aqi", ignoreCase = true) },
            pick("season") { it.entity_id == "sensor.season" },
            pick("rain") {
                it.entity_id.contains("rain", ignoreCase = true) ||
                    it.entity_id.contains("precipitation", ignoreCase = true)
            }
        )
    }
    val (sun, moon, aqi, season, rain) = roleEntities

    HKIDialog(
        entity = weather,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = headerDisplayIcon(currentDisplayType, weather.state),
        titleOverride = if (isEditMode) settingsTitle else "Weather",
        statusText = if (isEditMode) "SETTINGS" else "${formatWeatherState(weather.state)} - ${season?.state ?: "Season"}"
    ) {
        if (isEditMode) {
            WeatherConfigView(
                allEntities = allEntities,
                viewModel = viewModel,
                title = settingsTitle,
                displayType = displayType,
                alarmEntityIds = alarmEntityIds,
                onDisplayTypeSelected = onDisplayTypeSelected,
                onAlarmEntitiesSelected = onAlarmEntitiesSelected,
                onEntitySelected = { id -> viewModel.saveWeatherEntity(id) }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                item(span = { GridItemSpan(2) }) { WeatherMainCard(weather) }
                item(span = { GridItemSpan(2) }) { ForecastCard(dialogForecast) }
                item { SunCard(sun, use24h) }
                item { MoonCard(moon) }
                item { AqiCard(aqi) }
                item { RainCard(rain) }
                item { StatsCard(weather) }
            }
        }
    }
}

@Suppress("SpellCheckingInspection")
fun weatherIcon(state: String): ImageVector {
    return when (state.lowercase()) {
        "cloudy", "partlycloudy", "partly_cloudy" -> Icons.Default.Cloud
        "rainy", "pouring" -> Icons.Default.CloudQueue
        "sunny", "clear-night" -> Icons.Default.WbSunny
        else -> Icons.Default.Cloud
    }
}

@Suppress("SpellCheckingInspection")
fun formatWeatherState(state: String): String {
    return state
        .replace("partlycloudy", "partly cloudy", ignoreCase = true)
        .replace("_", " ")
        .replace("-", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

@Composable
fun WeatherMainCard(weather: HAEntity, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    Card(
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${weather.temperature?.toInt() ?: "--"}\u00B0",
                    style = MaterialTheme.typography.displayLarge,
                    color = appColors.onSurface,
                    fontWeight = FontWeight.Light
                )
                Text(formatWeatherState(weather.state), color = appColors.onMuted)
            }
            Icon(
                imageVector = weatherIcon(weather.state),
                contentDescription = null,
                tint = appColors.onSurface,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
fun ForecastCard(forecasts: List<HAWeatherForecast>?, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Forecast", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(12.dp))
            if (!forecasts.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    forecasts.take(5).forEach { day -> ForecastItem(day) }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    Text("No forecast available", color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SunCard(sun: HAEntity?, use24h: Boolean) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Sun", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(8.dp))

            val sunset = sun?.attributes?.get("next_setting")?.jsonPrimitive?.contentOrNull
            val sunrise = sun?.attributes?.get("next_rising")?.jsonPrimitive?.contentOrNull
            val pattern = if (use24h) "HH:mm" else "hh:mm a"
            val sunsetStr = sunset?.let {
                runCatching { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern(pattern)) }.getOrDefault("--")
            } ?: "--"
            val sunriseStr = sunrise?.let {
                runCatching { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern(pattern)) }.getOrDefault("--")
            } ?: "--"

            val sunProgress = remember(sun?.state, sunrise, sunset) { calculateSunProgress(sun?.state, sunrise, sunset) }
            val riseTime = parseSunTime(sunrise)
            val setTime = parseSunTime(sunset)
            val dawnTime = sun?.attributes?.get("next_dawn")?.jsonPrimitive?.contentOrNull?.let { parseSunTime(it) }
                ?: runCatching {
                    if (riseTime != null && setTime != null) riseTime.minusMinutes(((java.time.Duration.between(riseTime, setTime).toMinutes()) * 7L / 100L)) else null
                }.getOrNull()
            val duskTime = sun?.attributes?.get("next_dusk")?.jsonPrimitive?.contentOrNull?.let { parseSunTime(it) }
                ?: runCatching {
                    if (riseTime != null && setTime != null) setTime.plusMinutes(((java.time.Duration.between(riseTime, setTime).toMinutes()) * 7L / 100L)) else null
                }.getOrNull()

            fun timeToProgress(t: OffsetDateTime?): Float {
                if (riseTime == null || setTime == null || t == null) return 0f
                val total = java.time.Duration.between(riseTime, setTime).toMillis().coerceAtLeast(1)
                val elapsed = java.time.Duration.between(riseTime, t).toMillis()
                return (elapsed.toFloat() / total).coerceIn(0f, 1f)
            }

            val dawnProgress = timeToProgress(dawnTime)
            val noonProgress = 0.5f
            val duskProgress = timeToProgress(duskTime)

            // Top row with sunrise/sunset times
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(sunriseStr, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text("Sunrise", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(sunsetStr, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text("Sunset", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxWidth(0.96f).height(84.dp)) {
                    val w = size.width
                    val h = size.height
                    val horizon = h * 0.7f
                    val amplitude = h * 0.6f
                    val samples = 120

                    // Build full arc path
                    val fullPath = Path()
                    for (i in 0..samples) {
                        val t = i / samples.toFloat()
                        val x = t * w
                        val y = horizon - sin(t * PI).toFloat() * amplitude
                        if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
                    }

                    // Night area before dawn
                    if (dawnProgress > 0f) {
                        val endX = dawnProgress * w
                        val nightPath = Path()
                        nightPath.moveTo(0f, horizon)
                        for (i in 0..(samples * dawnProgress).toInt().coerceAtLeast(1)) {
                            val t = i / samples.toFloat()
                            val x = t * w
                            val y = horizon - sin(t * PI).toFloat() * amplitude
                            nightPath.lineTo(x, y)
                        }
                        nightPath.lineTo(endX, horizon)
                        nightPath.close()
                        drawPath(nightPath, Color(0xFF1E2A4A))
                    }

                    // Filled day under arc up to sun position
                    val sunFillPath = Path()
                    val sunIndex = (samples * sunProgress).toInt().coerceIn(0, samples)
                    sunFillPath.moveTo(0f, horizon)
                    for (i in 0..sunIndex) {
                        val t = i / samples.toFloat()
                        val x = t * w
                        val y = horizon - sin(t * PI).toFloat() * amplitude
                        sunFillPath.lineTo(x, y)
                    }
                    sunFillPath.lineTo(sunIndex / samples.toFloat() * w, horizon)
                    sunFillPath.close()
                    drawPath(sunFillPath, Color(0xFFBFE0FF))

                    // Arc outline
                    drawPath(fullPath, Color(0xFF9FBFE0), style = Stroke(width = 2.dp.toPx()))

                    // Sun marker
                    val tSun = sunProgress.coerceIn(0f, 1f)
                    val sunX = tSun * w
                    val sunY = horizon - sin(tSun * PI).toFloat() * amplitude
                    drawCircle(Color(0xFFFFD45A), radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(sunX, sunY))
                    drawCircle(Color.White.copy(alpha = 0.22f), radius = 16.dp.toPx(), center = androidx.compose.ui.geometry.Offset(sunX, sunY), style = Stroke(width = 1.2.dp.toPx()))

                    // Markers: dawn, noon, dusk
                    fun drawMarker(progress: Float, color: Color) {
                        val x = progress * w
                        val yTop = horizon - sin(progress * PI).toFloat() * amplitude - 6.dp.toPx()
                        drawLine(color, androidx.compose.ui.geometry.Offset(x, yTop), androidx.compose.ui.geometry.Offset(x, horizon + 6.dp.toPx()), strokeWidth = 1.dp.toPx())
                        drawCircle(color, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, yTop - 6.dp.toPx()))
                    }

                    if (dawnProgress in 0f..1f) drawMarker(dawnProgress, Color(0xFF6978A8))
                    drawMarker(noonProgress, Color(0xFF4F6FA0))
                    if (duskProgress in 0f..1f) drawMarker(duskProgress, Color(0xFF6978A8))
                }
            }

            Spacer(Modifier.height(6.dp))

            // Bottom labels: Dawn | Solar noon | Dusk
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dawn", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    Text(dawnTime?.toLocalTime()?.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "hh:mm a")) ?: "--", color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Solar noon", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    Text(
                        runCatching {
                            if (riseTime != null && setTime != null) {
                                val noon = riseTime.plusSeconds(java.time.Duration.between(riseTime, setTime).toSeconds() / 2)
                                noon.toLocalTime().format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "hh:mm a"))
                            } else "--"
                        }.getOrDefault("--"),
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dusk", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                    Text(duskTime?.toLocalTime()?.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "hh:mm a")) ?: "--", color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun calculateSunProgress(state: String?, sunrise: String?, sunset: String?): Float {
    val now = OffsetDateTime.now()
    val nextRise = parseSunTime(sunrise)
    val nextSet = parseSunTime(sunset)
    return when (state) {
        "above_horizon" -> {
            val set = nextSet ?: return 0.65f
            val rise = set.minusHours(12)
            val total = java.time.Duration.between(rise, set).toMillis().coerceAtLeast(1)
            val elapsed = java.time.Duration.between(rise, now).toMillis()
            (elapsed.toFloat() / total).coerceIn(0.05f, 0.95f)
        }
        else -> {
            val rise = nextRise ?: return 0.02f
            if (rise.toLocalDate() == now.toLocalDate()) 0.02f else 0.98f
        }
    }
}

private fun parseSunTime(value: String?): OffsetDateTime? {
    return value?.let {
        runCatching { OffsetDateTime.parse(it).withOffsetSameInstant(OffsetDateTime.now().offset) }
            .recoverCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.systemDefault()).toOffsetDateTime() }
            .getOrNull()
    }
}

@Composable
fun MoonCard(moon: HAEntity?) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Moon", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Icon(Icons.Default.Brightness2, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                moon?.state?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                color = appColors.onSurface,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun AqiCard(aqi: HAEntity?) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("AQI", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(8.dp))
            Text(aqi?.state ?: "--", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFAF7AC5))
            Text("Air Quality", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun RainCard(rain: HAEntity?) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            val unit = rain?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: "mm"
            Text("Rain", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(8.dp))
            Text("${rain?.state ?: "0"}$unit", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFCCE6FF))
            Text("Precipitation", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StatsCard(weather: HAEntity) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatLine("Humidity", "${weather.humidity?.toInt() ?: "--"}%", Icons.Default.WaterDrop)
            StatLine("Wind", "${weather.windSpeed?.toInt() ?: "--"} km/h", Icons.Default.Air)
        }
    }
}

@Composable
fun StatLine(label: String, value: String, icon: ImageVector) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ForecastItem(forecast: HAWeatherForecast) {
    val appColors = LocalHKIAppColors.current
    val date = try {
        val dt = LocalDateTime.parse(forecast.datetime, DateTimeFormatter.ISO_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    } catch (_: Exception) {
        forecast.datetime.take(3)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(date, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = weatherIcon(forecast.condition ?: ""),
            contentDescription = null,
            tint = appColors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("${forecast.temperature?.toInt() ?: "--"}\u00B0", color = appColors.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeatherConfigView(
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    title: String = "Header Pill",
    displayType: String? = null,
    alarmEntityIds: List<String> = emptyList(),
    onDisplayTypeSelected: ((String) -> Unit)? = null,
    onAlarmEntitiesSelected: ((List<String>) -> Unit)? = null,
    onEntitySelected: (String) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val savedDisplayType by viewModel.weatherDisplayType.collectAsState()
    val currentDisplayType = displayType ?: savedDisplayType
    val use24h by viewModel.use24hFormat.collectAsState()
    val useFullDayName by viewModel.useFullDayName.collectAsState()
    val extraEntities by viewModel.weatherExtraEntities.collectAsState()
    var selectingForRole by remember { mutableStateOf<String?>(null) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }
    val displayTypes = listOf("Weather", "Alarm", "Date", "Time", "DateTime", "None")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Display Mode", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayTypes.forEach { type ->
                        FilterChip(
                            selected = currentDisplayType == type,
                            onClick = { (onDisplayTypeSelected ?: viewModel::setWeatherDisplayType)(type) },
                            label = { Text(type, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = appColors.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            if (currentDisplayType in listOf("Time", "DateTime")) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("24-Hour Format", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted, modifier = Modifier.weight(1f))
                    Switch(checked = use24h, onCheckedChange = { viewModel.setUse24hFormat(it) })
                }
            }

            if (currentDisplayType in listOf("Date", "DateTime")) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Full Day Name", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted, modifier = Modifier.weight(1f))
                    Switch(checked = useFullDayName, onCheckedChange = { viewModel.setUseFullDayName(it) })
                }
            }

            if (currentDisplayType in listOf("Weather", "DateTime")) item {
                Text("Custom Entities", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Device-first setup: picking a weather station auto-fills the role entities
                    // below; each row stays individually adjustable (entity fallback).
                    val deviceName = extraEntities["device"]?.let { id ->
                        deviceRegistry.find { it.id == id }?.let { it.name_by_user ?: it.name } ?: id
                    }
                    WeatherEntityRow("Source device", deviceName ?: "Pick to auto-fill") { showDevicePicker = true }
                    WeatherEntityRow("Weather", allEntities.find { it.entity_id.startsWith("weather.") }?.entity_id) { selectingForRole = "weather" }
                    WeatherEntityRow("Sun", extraEntities["sun"]) { selectingForRole = "sun" }
                    WeatherEntityRow("Moon", extraEntities["moon"]) { selectingForRole = "moon" }
                    WeatherEntityRow("AQI", extraEntities["aqi"]) { selectingForRole = "aqi" }
                    WeatherEntityRow("Season", extraEntities["season"]) { selectingForRole = "season" }
                    WeatherEntityRow("Rain", extraEntities["rain"]) { selectingForRole = "rain" }
                }
            }

            if (currentDisplayType == "Alarm") item {
                Text("Custom Entities", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
                Spacer(Modifier.height(8.dp))
                WeatherEntityRow(
                    "Alarms",
                    alarmEntityIds.takeIf { it.isNotEmpty() }?.joinToString { it.substringAfter(".") }
                ) { selectingForRole = "alarm" }
            }
        }
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = deviceRegistry,
            currentId = extraEntities["device"],
            onDismiss = { showDevicePicker = false },
            onSelected = { deviceId ->
                viewModel.setWeatherExtraEntity("device", deviceId)
                if (deviceId != null) {
                    // Auto-fill the roles from the device's entities (weather station, rain
                    // gauge, air quality). Sun/moon/season are HA-wide, not device-bound.
                    val ids = entityRegistry.filter { it.device_id == deviceId }.map { it.entity_id }.toSet()
                    val dev = allEntities.filter { it.entity_id in ids }
                    fun unit(e: HAEntity) =
                        e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
                    fun name(e: HAEntity) = (e.friendlyName ?: e.entity_id).lowercase()
                    dev.find { it.entity_id.startsWith("weather.") }?.let { onEntitySelected(it.entity_id) }
                    (dev.find { it.deviceClass == "precipitation" || it.deviceClass == "precipitation_intensity" }
                        ?: dev.find { unit(it).contains("mm") && name(it).contains("rain") })
                        ?.let { viewModel.setWeatherExtraEntity("rain", it.entity_id) }
                    (dev.find { it.deviceClass == "aqi" } ?: dev.find { name(it).contains("air quality") || name(it).contains("aqi") })
                        ?.let { viewModel.setWeatherExtraEntity("aqi", it.entity_id) }
                }
                showDevicePicker = false
            }
        )
    }

    if (selectingForRole != null) {
        val filter: (HAEntity) -> Boolean = when (selectingForRole) {
            "weather" -> { e -> e.entity_id.startsWith("weather.") }
            "sun" -> { e -> e.entity_id.startsWith("sun.") }
            "moon" -> { e -> e.entity_id.contains("moon") || e.entity_id.startsWith("sensor.") }
            "aqi" -> { e -> e.entity_id.contains("aqi") || e.entity_id.startsWith("sensor.") }
            "season" -> { e -> e.entity_id.contains("season") || e.entity_id.startsWith("sensor.") }
            "rain" -> { e -> e.entity_id.contains("rain") || e.entity_id.contains("precipitation") || e.entity_id.startsWith("sensor.") }
            "alarm" -> { e -> e.entity_id.startsWith("alarm_control_panel.") }
            else -> { _ -> true }
        }

        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter(filter),
            singleSelect = selectingForRole != "alarm",
            preselectedIds = if (selectingForRole == "alarm") alarmEntityIds.toSet() else emptySet(),
            onDismiss = { selectingForRole = null },
            onEntitiesSelected = { selected ->
                val first = selected.firstOrNull()
                when (selectingForRole) {
                    "weather" -> if (first != null) onEntitySelected(first)
                    "alarm" -> onAlarmEntitiesSelected?.invoke(selected)
                    else -> viewModel.setWeatherExtraEntity(selectingForRole!!, first)
                }
                selectingForRole = null
            }
        )
    }
}

private fun headerDisplayIcon(displayType: String, weatherState: String): ImageVector = when (displayType) {
    "Alarm" -> Icons.Default.Security
    "Date" -> Icons.Default.CalendarMonth
    "Time" -> Icons.Default.Schedule
    "DateTime" -> weatherIcon(weatherState)
    else -> weatherIcon(weatherState)
}

@Composable
fun WeatherEntityRow(label: String, entityId: String?, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(appColors.subtleSurface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = appColors.onSurface)
        Text(entityId?.substringAfter(".") ?: "Auto-scan", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    }
}
