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
import coil3.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.GridCells
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAWeatherForecast
import com.example.hki7.data.HKIButtonConfig
import com.example.hki7.data.HKIButtonStack
import com.example.hki7.data.HKIWeatherWidget
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.ReorderableGrid
import com.example.hki7.ui.components.ForecastCard
import com.example.hki7.ui.components.WeatherMainCard
import com.example.hki7.ui.components.formatWeatherState
import com.example.hki7.ui.components.weatherIcon
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.MdiIconPickerDialog
import com.example.hki7.ui.components.WidgetWidthSelector
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
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    val defaultWeatherEntity by viewModel.weather.collectAsState()
    val specificFlow = remember(viewModel, widget.entityId) {
        viewModel.entitiesFor(listOfNotNull(widget.entityId))
    }
    val specificEntities by specificFlow.collectAsState()
    val weatherEntity = widget.entityId?.let { specificEntities.firstOrNull() } ?: defaultWeatherEntity

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

            if (weatherEntity == null) {
                Surface(
                    shape = RoundedCornerShape(widget.cornerRadius.dp),
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
                        ForecastCard(forecasts, widget.cornerRadius)
                    }
                    "hourly" -> {
                        val forecasts = rememberEntityForecast(weatherEntity, viewModel, "hourly")
                        HourlyForecastCard(forecasts, widget.cornerRadius)
                    }
                    "wind" -> WindCompassCard(weatherEntity, widget.cornerRadius)
                    "rainmap" -> RainMapCard(widget.imageUrl, widget.cornerRadius)
                    else -> WeatherMainCard(weatherEntity, widget.cornerRadius)
                }
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
            }
            EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
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
fun WindCompassCard(weather: HAEntity, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    val bearing = weather.attributes?.get("wind_bearing")?.jsonPrimitive?.doubleOrNull ?: 0.0
    val speed = weather.windSpeed
    val gust = weather.attributes?.get("wind_gust_speed")?.jsonPrimitive?.doubleOrNull
    val needleColor = Color(0xFF4A90E2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
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
fun RainMapCard(imageUrl: String?, cornerRadius: Int = 24) {
    val appColors = LocalHKIAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
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
    var iconName by remember(widget) { mutableStateOf(widget.icon ?: "") }
    var cornerRadius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var showEntityPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

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

    if (showIconPicker) {
        MdiIconPickerDialog(
            current = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName = it; showIconPicker = false }
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
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iconName.isNotEmpty()) MdiIcon(iconName, size = 20.dp)
                    TextButton(onClick = { showIconPicker = true }) { Text(if (iconName.isEmpty()) "Choose" else "Change") }
                    if (iconName.isNotEmpty()) TextButton(onClick = { iconName = "" }) { Text("None") }
                }
                Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cornerRadius == 8, onClick = { cornerRadius = 8 }, label = { Text("Sharp") })
                    FilterChip(selected = cornerRadius == 20, onClick = { cornerRadius = 20 }, label = { Text("Modern") })
                    FilterChip(selected = cornerRadius == 28, onClick = { cornerRadius = 28 }, label = { Text("Round") })
                }
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
                        title = title.ifBlank { null },
                        icon = iconName.ifBlank { null },
                        cornerRadius = cornerRadius
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
                    if (isEditMode) "Tap + to add a weather card" else "No weather cards",
                    color = appColors.onMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }
    val columns = stack.columns.coerceIn(1, 3)
    if (isEditMode) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stack.entityIds.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { itemId ->
                        Box(modifier = Modifier.weight(1f)) {
                            WeatherStackCard(stack.buttonConfigs[itemId], allEntities, viewModel, stack.cornerRadius)
                            IconButton(
                                onClick = { onItemSettings(itemId) },
                                modifier = Modifier.align(Alignment.Center).size(24.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Weather card settings", tint = appColors.onSurface, modifier = Modifier.size(16.dp))
                            }
                            EditRemoveBadge(
                                onClick = { onRemoveItem(itemId) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)
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
    val weatherEntity = allEntities.find { it.entity_id == config?.weatherEntityId }
        ?: allEntities.find { it.entity_id.startsWith("weather.") }
    if (weatherEntity == null && style != "rainmap") {
        Surface(
            shape = RoundedCornerShape(cornerRadius.dp),
            color = appColors.elevated.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No weather entity available", color = appColors.onMuted)
            }
        }
        return
    }
    when (style) {
        "forecast" -> ForecastCard(rememberEntityForecast(weatherEntity!!, viewModel, "daily"), cornerRadius)
        "hourly" -> HourlyForecastCard(rememberEntityForecast(weatherEntity!!, viewModel, "hourly"), cornerRadius)
        "wind" -> WindCompassCard(weatherEntity!!, cornerRadius)
        "rainmap" -> RainMapCard(config?.weatherImageUrl, cornerRadius)
        else -> WeatherMainCard(weatherEntity!!, cornerRadius)
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
            title = "Select Weather Entity",
            singleSelect = true,
            preselectedIds = setOfNotNull(entityId),
            onDismiss = { showEntityPicker = false },
            onEntitiesSelected = { ids -> entityId = ids.firstOrNull(); showEntityPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    (initial ?: HKIButtonConfig()).copy(
                        weatherStyle = style,
                        weatherEntityId = entityId,
                        weatherImageUrl = imageUrl.ifBlank { null }
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
