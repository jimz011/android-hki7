package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAWeatherForecast
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val defaultWeatherCardWidths = mapOf(
    "current" to "full",
    "forecast" to "full",
    "horizon" to "full",
    "moon" to "half",
    "aqi" to "half",
    "rain" to "half",
    "stats" to "half"
)

private fun weatherCardSpan(width: String): Int = when (width) {
    "third" -> 2
    "half" -> 3
    else -> 6
}

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
    val isEditMode by viewModel.isEditMode.collectAsState()
    val use24h by viewModel.use24hFormat.collectAsState()
    val extraEntities by viewModel.weatherExtraEntities.collectAsState()
    val fetchedForecast by viewModel.weatherForecast.collectAsState()
    val savedCardWidths by viewModel.weatherCardWidths.collectAsState()
    val cardWidths = remember(savedCardWidths) { defaultWeatherCardWidths + savedCardWidths }
    val roleEntityIds = remember(weather.entity_id, extraEntities, alarmEntityIds) {
        buildSet {
            add(weather.entity_id)
            addAll(extraEntities.values.filterNotNull())
            addAll(alarmEntityIds)
        }
    }
    val weatherDialogEntityFlow = remember(viewModel, roleEntityIds, isEditMode) {
        if (isEditMode) {
            viewModel.entitiesMatching { true }
        } else {
            viewModel.entitiesMatching { entity ->
                entity.entity_id in roleEntityIds || entity.entity_id == "sun.sun" ||
                    entity.entity_id == "sensor.moon" || entity.entity_id == "sensor.season" ||
                    entity.entity_id.contains("aqi", ignoreCase = true) ||
                    entity.entity_id.contains("rain", ignoreCase = true) ||
                    entity.entity_id.contains("precipitation", ignoreCase = true)
            }
        }
    }
    val allEntities by weatherDialogEntityFlow.collectAsState()
    val forecastCacheKey = "${weather.entity_id}:daily"
    val forecastFlow = remember(viewModel, forecastCacheKey) { viewModel.weatherForecastFor(forecastCacheKey) }
    val cachedForecast by forecastFlow.collectAsState()
    val currentDisplayType = if (isEditMode) displayType ?: "Weather" else "Weather"

    // Modern HA no longer puts forecasts in the entity attributes; fetch (TTL-cached) on open.
    LaunchedEffect(weather.entity_id) { viewModel.fetchWeatherForecastFor(weather.entity_id, "daily") }
    val dialogForecast = weather.forecast.takeUnless { it.isNullOrEmpty() }
        ?: cachedForecast.takeUnless { it.isEmpty() }
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
        iconTint = weatherStateColor(weather.state),
        headerIconContent = if (currentDisplayType == "Weather" || currentDisplayType == "DateTime") {
            { WeatherStateIcon(state = weather.state, size = 28.dp, contentDescription = formatWeatherState(weather.state)) }
        } else null,
        titleOverride = if (isEditMode) settingsTitle else "Weather",
        statusText = if (isEditMode) "SETTINGS" else "${formatWeatherState(weather.state)} - ${season?.state ?: "Season"}",
        allowInEditMode = true
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
            val weatherGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = weatherGridState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                modifier = Modifier.weight(1f).fadingEdges(weatherGridState)
            ) {
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("current"))) }) { WeatherMainCard(weather) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("forecast"))) }) { ForecastCard(dialogForecast) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("horizon"))) }) { HorizonCard(sun, use24h) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("moon"))) }) { MoonCard(moon) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("aqi"))) }) { AqiCard(aqi) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("rain"))) }) { RainCard(rain) }
                item(span = { GridItemSpan(weatherCardSpan(cardWidths.getValue("stats"))) }) { StatsCard(weather) }
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
fun WeatherMainCard(
    weather: HAEntity,
    modifier: Modifier = Modifier,
    cornerRadius: Int = LocalItemCornerRadius.current
) {
    val appColors = LocalHKIAppColors.current
    val accent = weatherStateColor(weather.state)
    val temperatureUnit = weather.attributes?.get("temperature_unit")?.jsonPrimitive?.contentOrNull ?: "°"
    val apparent = weather.attributes?.get("apparent_temperature")?.jsonPrimitive?.doubleOrNull
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.30f), appColors.elevated.copy(alpha = 0.96f))
                    )
                )
        ) {
            val compact = maxWidth < 250.dp
            val veryCompact = maxWidth < 180.dp
            Column(
                modifier = Modifier.fillMaxWidth().padding(if (veryCompact) 12.dp else if (compact) 16.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
            ) {
                if (veryCompact) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = weather.friendlyName ?: "Current weather",
                            style = MaterialTheme.typography.labelSmall,
                            color = appColors.onMuted,
                            maxLines = 1,
                        )
                        WeatherStateIcon(
                            state = weather.state,
                            size = 54.dp,
                            contentDescription = formatWeatherState(weather.state)
                        )
                        Text(
                            text = "${weather.temperature?.toInt() ?: "--"}${temperatureUnit}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = appColors.onSurface,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            formatWeatherState(weather.state),
                            color = appColors.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = weather.friendlyName ?: "Current weather",
                                style = MaterialTheme.typography.labelMedium,
                                color = appColors.onMuted,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${weather.temperature?.toInt() ?: "--"}°",
                                    style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                                    color = appColors.onSurface,
                                    fontWeight = FontWeight.Light
                                )
                                if (temperatureUnit != "°" && temperatureUnit.isNotBlank()) {
                                    Text(
                                        text = temperatureUnit.removePrefix("°"),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = appColors.onMuted,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                            Text(
                                formatWeatherState(weather.state),
                                color = appColors.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        WeatherStateIcon(
                            state = weather.state,
                            size = if (compact) 62.dp else 92.dp,
                            contentDescription = formatWeatherState(weather.state)
                        )
                    }
                }

                if (!veryCompact) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        apparent?.let { WeatherMetricChip(Icons.Default.Thermostat, "Feels ${it.toInt()}°") }
                        weather.humidity?.let { WeatherMetricChip(Icons.Default.WaterDrop, "${it.toInt()}%") }
                        weather.windSpeed?.let { WeatherMetricChip(Icons.Default.Air, "${it.toInt()} km/h") }
                        weather.pressure?.let { WeatherMetricChip(Icons.Default.Speed, "${it.toInt()} hPa") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricChip(icon: ImageVector, text: String) {
    val appColors = LocalHKIAppColors.current
    Surface(
        color = appColors.surface.copy(alpha = 0.46f),
        shape = itemCornerShape()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = appColors.onMuted, modifier = Modifier.size(15.dp))
            Text(text, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ForecastCard(
    forecasts: List<HAWeatherForecast>?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = LocalItemCornerRadius.current
) {
    val appColors = LocalHKIAppColors.current
    val accent = weatherStateColor(forecasts?.firstOrNull()?.condition)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.16f), appColors.elevated.copy(alpha = 0.96f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Forecast", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            if (!forecasts.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    forecasts.take(5).forEach { day -> ForecastItem(day) }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
                    Text("No forecast available", color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun MoonCard(moon: HAEntity?) {
    WeatherInfoCard(
        title = "Moon",
        value = moon?.state?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown",
        subtitle = "Lunar phase",
        icon = Icons.Default.Brightness2,
        accent = Color(0xFF9FA8DA)
    )
}

@Composable
fun AqiCard(aqi: HAEntity?) {
    val value = aqi?.state?.toDoubleOrNull()
    val color = when {
        value == null -> Color(0xFF90A4AE)
        value <= 50 -> Color(0xFF66BB6A)
        value <= 100 -> Color(0xFFFFCA28)
        value <= 150 -> Color(0xFFFF8A65)
        else -> Color(0xFFEF5350)
    }
    WeatherInfoCard("Air quality", aqi?.state ?: "--", "AQI", Icons.Default.Air, color)
}

@Composable
fun RainCard(rain: HAEntity?) {
    val unit = rain?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: "mm"
    WeatherInfoCard(
        title = "Rain",
        value = "${rain?.state ?: "0"} $unit",
        subtitle = "Precipitation",
        icon = Icons.Default.WaterDrop,
        accent = weatherStateColor("rainy")
    )
}

@Composable
fun StatsCard(weather: HAEntity) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = itemCornerShape(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(weatherStateColor(weather.state).copy(alpha = 0.15f), appColors.elevated.copy(alpha = 0.96f))
                    )
                )
        ) {
            val compact = maxWidth < 180.dp
            Column(
                Modifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                Text(
                    if (compact) "Details" else "Weather details",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                StatLine("Humidity", "${weather.humidity?.toInt() ?: "--"}%", Icons.Default.WaterDrop, compact)
                StatLine("Wind", "${weather.windSpeed?.toInt() ?: "--"} km/h", Icons.Default.Air, compact)
                weather.pressure?.let { StatLine("Pressure", "${it.toInt()} hPa", Icons.Default.Speed, compact) }
            }
        }
    }
}

@Composable
private fun WeatherInfoCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color
) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = itemCornerShape(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), appColors.elevated.copy(alpha = 0.96f))))
        ) {
            val compact = maxWidth < 160.dp
            Column(
                modifier = Modifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
            ) {
                if (compact) {
                    Box(
                        modifier = Modifier.size(38.dp).background(accent.copy(alpha = 0.16f), itemCornerShape()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
                        Box(
                            modifier = Modifier.size(34.dp).background(accent.copy(alpha = 0.16f), itemCornerShape()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Column(horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
                    Text(
                        value,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (compact) 2 else 1
                    )
                    Text(
                        if (compact) title else subtitle,
                        color = appColors.onMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun StatLine(label: String, value: String, icon: ImageVector, compact: Boolean = false) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        if (!compact) {
            Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        }
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun ForecastItem(forecast: HAWeatherForecast) {
    val appColors = LocalHKIAppColors.current
    val locale = LocalConfiguration.current.locales[0]
    val date = runCatching {
        OffsetDateTime.parse(forecast.datetime, DateTimeFormatter.ISO_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("EEE", locale))
    }.recoverCatching {
        LocalDateTime.parse(forecast.datetime, DateTimeFormatter.ISO_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("EEE", locale))
    }.getOrDefault(forecast.datetime.take(3))

    Surface(
        modifier = Modifier.width(82.dp),
        color = appColors.subtleSurface,
        shape = itemCornerShape()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(date, color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
            WeatherStateIcon(
                state = forecast.condition,
                size = 38.dp,
                contentDescription = forecast.condition?.let(::formatWeatherState),
                loop = false
            )
            Text(
                buildString {
                    append(forecast.temperature?.toInt() ?: "--")
                    append("°")
                    forecast.templow?.let { append("  ${it.toInt()}°") }
                },
                color = appColors.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            forecast.precipitation?.takeIf { it > 0.0 }?.let {
                Text("${it.toInt()} mm", color = weatherStateColor("rainy"), style = MaterialTheme.typography.labelSmall)
            }
        }
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
    val savedCardWidths by viewModel.weatherCardWidths.collectAsState()
    val cardWidths = remember(savedCardWidths) { defaultWeatherCardWidths + savedCardWidths }
    var selectingForRole by remember { mutableStateOf<String?>(null) }
    var showDevicePicker by remember { mutableStateOf(false) }
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }
    val displayTypes = listOf("Weather", "Alarm", "Date", "Time", "DateTime", "None")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        val settingsListState = androidx.compose.foundation.lazy.rememberLazyListState()
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().fadingEdges(settingsListState),
            state = settingsListState,
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
                    // Device-first setup: picking a weather station automatically fills the role entities
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

            if (currentDisplayType in listOf("Weather", "DateTime")) item {
                Text("Weather dialog card sizes", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        "current" to "Current weather",
                        "forecast" to "Forecast",
                        "horizon" to "Sun horizon",
                        "moon" to "Moon",
                        "aqi" to "Air quality",
                        "rain" to "Rain",
                        "stats" to "Weather details"
                    ).forEach { (key, label) ->
                        WeatherCardWidthRow(
                            label = label,
                            width = cardWidths.getValue(key),
                            onWidthChange = { viewModel.setWeatherCardWidth(key, it) }
                        )
                    }
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
                    // Autofill the roles from the device's entities (weather station, rain
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

@Composable
private fun WeatherCardWidthRow(label: String, width: String, onWidthChange: (String) -> Unit) {
    val appColors = LocalHKIAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.subtleSurface, itemCornerShape())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("full" to "Full", "half" to "Half", "third" to "Third").forEach { (value, text) ->
                FilterChip(
                    selected = width == value,
                    onClick = { onWidthChange(value) },
                    label = { Text(text, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
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
            .background(appColors.subtleSurface, itemCornerShape())
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = appColors.onSurface)
        Text(entityId?.substringAfter(".") ?: "Auto-scan", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
    }
}
