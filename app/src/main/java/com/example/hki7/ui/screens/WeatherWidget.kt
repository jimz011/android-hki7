package com.example.hki7.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAWeatherForecast
import com.example.hki7.data.HKIWeatherWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.ForecastCard
import com.example.hki7.ui.components.WeatherMainCard
import com.example.hki7.ui.components.formatWeatherState
import com.example.hki7.ui.components.weatherIcon
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.theme.LocalHKIAppColors
import com.example.hki7.ui.utils.MdiIcon
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

val weatherWidgetStyles = listOf(
    "current" to "Current weather",
    "forecast" to "Daily forecast",
    "hourly" to "Hourly forecast",
    "wind" to "Wind compass",
    "rainmap" to "Rain map (image URL)"
)

@Composable
fun WeatherRoomWidget(
    widget: HKIWeatherWidget,
    allEntities: List<HAEntity>,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val weatherEntity = allEntities.find { it.entity_id == widget.entityId }
        ?: allEntities.find { it.entity_id.startsWith("weather.") }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isEditMode) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    MdiIcon("weather-partly-cloudy", tint = Color.Gray, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        widget.title ?: weatherWidgetStyles.find { it.first == widget.style }?.second ?: "Weather",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettings, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (weatherEntity == null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = appColors.elevated.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No weather entity available", color = appColors.onMuted)
                    }
                }
            } else {
                when (widget.style) {
                    "forecast" -> {
                        val forecasts = rememberEntityForecast(weatherEntity, viewModel, "daily")
                        ForecastCard(forecasts)
                    }
                    "hourly" -> {
                        val forecasts = rememberEntityForecast(weatherEntity, viewModel, "hourly")
                        HourlyForecastCard(forecasts)
                    }
                    "wind" -> WindCompassCard(weatherEntity)
                    "rainmap" -> RainMapCard(widget.imageUrl)
                    else -> WeatherMainCard(weatherEntity)
                }
            }
        }

        if (isEditMode) {
            // Sits on the content card's top-right, just below the edit header row.
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 42.dp, end = 6.dp)
            )
        }
    }
}

@Composable
private fun rememberEntityForecast(weatherEntity: HAEntity, viewModel: MainViewModel, type: String): List<HAWeatherForecast> {
    val cache by viewModel.weatherForecastCache.collectAsState()
    LaunchedEffect(weatherEntity.entity_id, type) {
        viewModel.fetchWeatherForecastFor(weatherEntity.entity_id, type)
    }
    return cache["${weatherEntity.entity_id}:$type"]
        ?: weatherEntity.forecast.takeUnless { type != "daily" || it.isNullOrEmpty() }
        ?: emptyList()
}

@Composable
fun HourlyForecastCard(forecasts: List<HAWeatherForecast>) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Hourly", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(12.dp))
            if (forecasts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    forecasts.take(12).forEach { hour -> HourlyForecastItem(hour) }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    Text("No hourly forecast available", color = appColors.onMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastItem(forecast: HAWeatherForecast) {
    val appColors = LocalHKIAppColors.current
    val timeLabel = try {
        LocalDateTime.parse(forecast.datetime, DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        forecast.datetime.take(5)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(timeLabel, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Icon(weatherIcon(forecast.condition ?: ""), contentDescription = null, tint = appColors.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text("${forecast.temperature?.toInt() ?: "--"}°", color = appColors.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WindCompassCard(weather: HAEntity) {
    val appColors = LocalHKIAppColors.current
    val bearing = weather.attributes?.get("wind_bearing")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val speed = weather.windSpeed
    val gust = weather.attributes?.get("wind_gust_speed")?.jsonPrimitive?.doubleOrNull
    val needleColor = Color(0xFF4A90E2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Wind", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f * 0.86f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = appColors.onMuted.copy(alpha = 0.18f), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
                    for (deg in 0 until 360 step 30) {
                        val rad = Math.toRadians(deg.toDouble() - 90)
                        val outer = Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
                        val inner = Offset(center.x + (radius - 8.dp.toPx()) * cos(rad).toFloat(), center.y + (radius - 8.dp.toPx()) * sin(rad).toFloat())
                        drawLine(appColors.onMuted.copy(alpha = 0.35f), inner, outer, strokeWidth = 2.dp.toPx())
                    }
                    val needleRad = Math.toRadians(bearing - 90)
                    val tip = Offset(center.x + radius * 0.82f * cos(needleRad).toFloat(), center.y + radius * 0.82f * sin(needleRad).toFloat())
                    val tailRad = Math.toRadians(bearing - 90 + 180)
                    val tail = Offset(center.x + radius * 0.35f * cos(tailRad).toFloat(), center.y + radius * 0.35f * sin(tailRad).toFloat())
                    drawLine(needleColor, tail, tip, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(needleColor, radius = 5.dp.toPx(), center = tip)
                    drawCircle(appColors.onSurface, radius = 4.dp.toPx(), center = center)
                }
                Text("N", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp))
                Text("E", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
                Text("S", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                Text("W", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterStart).padding(start = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${speed?.toInt() ?: "--"} km/h" + (gust?.let { " (gust ${it.toInt()})" } ?: ""),
                color = appColors.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(bearingToCompassLabel(bearing), color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun bearingToCompassLabel(bearing: Double): String {
    val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val normalized = ((bearing % 360) + 360) % 360
    val idx = (normalized / 22.5).toInt().coerceIn(0, 15)
    return dirs[idx]
}

@Composable
fun RainMapCard(imageUrl: String?) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.elevated.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Rain Map", style = MaterialTheme.typography.labelLarge, color = appColors.onMuted)
            Spacer(Modifier.height(12.dp))
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Rain map",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Set a rain map image URL in this widget's settings",
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
    var showEntityPicker by remember { mutableStateOf(false) }

    val weatherEntities = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("weather.") } }

    if (showEntityPicker) {
        AdvancedEntitySearchDialog(
            allEntities = weatherEntities,
            title = "Select Weather Entity",
            singleSelect = true,
            preselectedIds = setOfNotNull(entityId),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids -> entityId = ids.firstOrNull(); showEntityPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather Widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Weather entity", style = MaterialTheme.typography.labelLarge)
                val entityName = entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entityName ?: "Default weather entity",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entityName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showEntityPicker = true }) { Text("Change") }
                    if (entityId != null) { TextButton(onClick = { entityId = null }) { Text("Clear") } }
                }
                Text("Style", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weatherWidgetStyles.forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(selected = style == value, onClick = { style = value })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (style == "rainmap") {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Rain map image URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        title = title.ifBlank { null }
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
