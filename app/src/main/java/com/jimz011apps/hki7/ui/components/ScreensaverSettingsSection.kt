package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.MAX_SCREENSAVER_TIMEOUT_SECONDS
import com.jimz011apps.hki7.data.MIN_SCREENSAVER_TIMEOUT_SECONDS
import com.jimz011apps.hki7.data.SCREENSAVER_LAYOUT_CLOCK_ONLY
import com.jimz011apps.hki7.data.SCREENSAVER_LAYOUT_CLOCK_PANEL
import com.jimz011apps.hki7.data.ScreensaverAction
import com.jimz011apps.hki7.data.ScreensaverSettings
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import com.jimz011apps.hki7.ui.utils.MdiIcon
import kotlin.math.roundToInt

@Composable
fun ScreensaverSettingsSection(
    settings: ScreensaverSettings,
    entities: List<HAEntity>,
    onChange: (ScreensaverSettings) -> Unit,
) {
    val appColors = LocalHKIAppColors.current
    var timeout by remember(settings.timeoutSeconds) { mutableFloatStateOf(settings.timeoutSeconds.toFloat()) }
    var picker by remember { mutableStateOf<String?>(null) }
    var actionPickerIndex by remember { mutableStateOf<Int?>(null) }
    var iconPickerIndex by remember { mutableStateOf<Int?>(null) }

    SettingsGroup {
        Text(stringResource(R.string.settings_screensaver_title), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        Text(stringResource(R.string.settings_screensaver_hint), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_screensaver_enable),
            subtitle = stringResource(R.string.settings_screensaver_enable_subtitle),
            checked = settings.enabled,
            onCheckedChange = { onChange(settings.copy(enabled = it)) },
        )
        Text(
            stringResource(R.string.settings_screensaver_timeout, timeout.roundToInt()),
            color = appColors.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        HKISlider(
            value = timeout,
            onValueChange = { timeout = it },
            onValueChangeFinished = { onChange(settings.copy(timeoutSeconds = timeout.roundToInt())) },
            valueRange = MIN_SCREENSAVER_TIMEOUT_SECONDS.toFloat()..MAX_SCREENSAVER_TIMEOUT_SECONDS.toFloat(),
            steps = ((MAX_SCREENSAVER_TIMEOUT_SECONDS - MIN_SCREENSAVER_TIMEOUT_SECONDS) / 15) - 1,
        )
        Text(stringResource(R.string.settings_screensaver_layout), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.layout != SCREENSAVER_LAYOUT_CLOCK_ONLY,
                onClick = { onChange(settings.copy(layout = SCREENSAVER_LAYOUT_CLOCK_PANEL)) },
                label = { Text(stringResource(R.string.settings_screensaver_layout_panel)) },
            )
            FilterChip(
                selected = settings.layout == SCREENSAVER_LAYOUT_CLOCK_ONLY,
                onClick = { onChange(settings.copy(layout = SCREENSAVER_LAYOUT_CLOCK_ONLY)) },
                label = { Text(stringResource(R.string.settings_screensaver_layout_clock)) },
            )
        }
        Text(stringResource(R.string.settings_screensaver_background), color = appColors.onSurface, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.background == "picsum",
                onClick = { onChange(settings.copy(background = "picsum")) },
                label = { Text(stringResource(R.string.settings_screensaver_background_photo)) },
            )
            FilterChip(
                selected = settings.background != "picsum",
                onClick = { onChange(settings.copy(background = "none")) },
                label = { Text(stringResource(R.string.settings_screensaver_background_dark)) },
            )
        }
    }

    SettingsGroup {
        Text(stringResource(R.string.settings_screensaver_weather), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_screensaver_show_weather),
            subtitle = stringResource(R.string.settings_screensaver_show_weather_subtitle),
            checked = settings.showWeather,
            onCheckedChange = { onChange(settings.copy(showWeather = it)) },
        )
        EntityPickRow(
            label = stringResource(R.string.settings_screensaver_weather_entity),
            value = entityLabel(entities, settings.weatherEntityId, stringResource(R.string.settings_screensaver_use_header_weather)),
            onClick = { picker = "weather" },
        )
        if (settings.weatherEntityId.isNotBlank()) {
            androidx.compose.material3.TextButton(onClick = { onChange(settings.copy(weatherEntityId = "")) }) {
                Text(stringResource(R.string.settings_screensaver_clear))
            }
        }
        SettingsSwitchRow(
            title = stringResource(R.string.settings_screensaver_show_indoor),
            subtitle = stringResource(R.string.settings_screensaver_show_indoor_subtitle),
            checked = settings.showIndoor,
            onCheckedChange = { onChange(settings.copy(showIndoor = it)) },
        )
        EntityPickRow(
            label = stringResource(R.string.settings_screensaver_temperature),
            value = entityLabel(entities, settings.temperatureEntityId, stringResource(R.string.settings_screensaver_choose)),
            onClick = { picker = "temperature" },
        )
        EntityPickRow(
            label = stringResource(R.string.settings_screensaver_humidity),
            value = entityLabel(entities, settings.humidityEntityId, stringResource(R.string.settings_screensaver_choose)),
            onClick = { picker = "humidity" },
        )
    }

    SettingsGroup {
        Text(stringResource(R.string.settings_screensaver_calendar), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_screensaver_show_calendar),
            subtitle = stringResource(R.string.settings_screensaver_show_calendar_subtitle),
            checked = settings.showCalendar,
            onCheckedChange = { onChange(settings.copy(showCalendar = it)) },
        )
        OutlinedButton(
            onClick = { picker = "calendar" },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = itemCornerShape(),
        ) {
            Text(
                if (settings.calendarEntityIds.isEmpty()) stringResource(R.string.settings_screensaver_pick_calendars)
                else stringResource(R.string.settings_screensaver_calendars_count, settings.calendarEntityIds.size)
            )
        }
    }

    SettingsGroup {
        Text(stringResource(R.string.settings_screensaver_actions), style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
        Text(stringResource(R.string.settings_screensaver_actions_hint), style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_screensaver_show_actions),
            subtitle = stringResource(R.string.settings_screensaver_show_actions_subtitle),
            checked = settings.showActions,
            onCheckedChange = { onChange(settings.copy(showActions = it)) },
        )
        settings.actions.forEachIndexed { index, action ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clickable { iconPickerIndex = index }.padding(end = 8.dp)
                    ) {
                        MdiIcon(action.icon, size = 28.dp, tint = appColors.onSurface)
                    }
                    OutlinedTextField(
                        value = action.label,
                        onValueChange = { label ->
                            onChange(settings.copy(actions = settings.actions.mapIndexed { i, item -> if (i == index) item.copy(label = label) else item }))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.settings_screensaver_action_label)) },
                        singleLine = true,
                    )
                    IconButton(onClick = { onChange(settings.copy(actions = settings.actions.filterIndexed { i, _ -> i != index })) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.settings_screensaver_delete_action), tint = appColors.onMuted)
                    }
                }
                EntityPickRow(
                    label = stringResource(R.string.settings_screensaver_action_entity),
                    value = entityLabel(entities, action.entityId, stringResource(R.string.settings_screensaver_choose)),
                    onClick = { actionPickerIndex = index; picker = "action" },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("toggle", "open", "close").forEach { service ->
                        FilterChip(
                            selected = action.service == service,
                            onClick = {
                                onChange(settings.copy(actions = settings.actions.mapIndexed { i, item -> if (i == index) item.copy(service = service) else item }))
                            },
                            label = { Text(serviceLabel(service)) },
                        )
                    }
                }
            }
        }
        if (settings.actions.size < 4) {
            OutlinedButton(
                onClick = { onChange(settings.copy(actions = settings.actions + ScreensaverAction())) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = itemCornerShape(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_screensaver_add_action))
            }
        }
    }

    val pickerKind = picker
    if (pickerKind != null) {
        val candidates = when (pickerKind) {
            "weather" -> entities.filter { it.entity_id.startsWith("weather.") }
            "temperature" -> entities.filter { it.entity_id.startsWith("sensor.") }
            "humidity" -> entities.filter { it.entity_id.startsWith("sensor.") }
            "calendar" -> entities.filter { it.entity_id.startsWith("calendar.") }
            else -> entities
        }
        AdvancedEntitySearchDialog(
            allEntities = candidates,
            title = when (pickerKind) {
                "weather" -> stringResource(R.string.settings_screensaver_pick_weather)
                "temperature" -> stringResource(R.string.settings_screensaver_pick_temperature)
                "humidity" -> stringResource(R.string.settings_screensaver_pick_humidity)
                "calendar" -> stringResource(R.string.settings_screensaver_pick_calendars)
                else -> stringResource(R.string.settings_screensaver_action_entity)
            },
            singleSelect = pickerKind != "calendar",
            preselectedIds = when (pickerKind) {
                "weather" -> setOfNotNull(settings.weatherEntityId.takeIf { it.isNotBlank() })
                "temperature" -> setOfNotNull(settings.temperatureEntityId.takeIf { it.isNotBlank() })
                "humidity" -> setOfNotNull(settings.humidityEntityId.takeIf { it.isNotBlank() })
                "calendar" -> settings.calendarEntityIds.toSet()
                "action" -> setOfNotNull(actionPickerIndex?.let { settings.actions.getOrNull(it)?.entityId }?.takeIf { it.isNotBlank() })
                else -> emptySet()
            },
            onDismiss = { picker = null; actionPickerIndex = null },
            onEntitiesSelected = { ids ->
                when (pickerKind) {
                    "weather" -> onChange(settings.copy(weatherEntityId = ids.firstOrNull().orEmpty()))
                    "temperature" -> onChange(settings.copy(temperatureEntityId = ids.firstOrNull().orEmpty()))
                    "humidity" -> onChange(settings.copy(humidityEntityId = ids.firstOrNull().orEmpty()))
                    "calendar" -> onChange(settings.copy(calendarEntityIds = ids.filter { it.startsWith("calendar.") }))
                    "action" -> {
                        val index = actionPickerIndex
                        val selected = ids.firstOrNull().orEmpty()
                        if (index != null) {
                            onChange(settings.copy(actions = settings.actions.mapIndexed { i, item ->
                                if (i == index) item.copy(entityId = selected) else item
                            }))
                        }
                    }
                }
                picker = null
                actionPickerIndex = null
            },
        )
    }

    val iconIndex = iconPickerIndex
    if (iconIndex != null) {
        MdiIconPickerDialog(
            current = settings.actions.getOrNull(iconIndex)?.icon.orEmpty(),
            onDismiss = { iconPickerIndex = null },
            onSelect = { slug ->
                onChange(settings.copy(actions = settings.actions.mapIndexed { i, item ->
                    if (i == iconIndex) item.copy(icon = slug) else item
                }))
                iconPickerIndex = null
            },
        )
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val appColors = LocalHKIAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EntityPickRow(label: String, value: String, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = appColors.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun serviceLabel(service: String): String = when (service) {
    "open" -> stringResource(R.string.settings_screensaver_service_open)
    "close" -> stringResource(R.string.settings_screensaver_service_close)
    else -> stringResource(R.string.settings_screensaver_service_toggle)
}

private fun entityLabel(entities: List<HAEntity>, entityId: String, fallback: String): String {
    if (entityId.isBlank()) return fallback
    return entities.firstOrNull { it.entity_id == entityId }?.friendlyName ?: entityId
}
