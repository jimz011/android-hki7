@file:Suppress("SpellCheckingInspection")

package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HAEntityRegistryEntry
import com.jimz011apps.hki7.data.HAHistoryEntry
import com.jimz011apps.hki7.data.HAStatPoint
import com.jimz011apps.hki7.data.HKIEnergyCardWidget
import com.jimz011apps.hki7.data.HKIEnergyConfig
import com.jimz011apps.hki7.data.HKIEnergyStack
import com.jimz011apps.hki7.data.HKIPageConfig
import com.jimz011apps.hki7.data.withDisplayName
import com.jimz011apps.hki7.ui.MainViewModel
import com.jimz011apps.hki7.ui.components.AdvancedEntitySearchDialog
import com.jimz011apps.hki7.ui.components.surfaceGradient
import com.jimz011apps.hki7.ui.components.EditSettingsButton
import com.jimz011apps.hki7.ui.components.RenameCardDialog
import com.jimz011apps.hki7.ui.components.HKIPage
import com.jimz011apps.hki7.ui.components.fadingEdges
import com.jimz011apps.hki7.ui.components.parseHistoryMillis
import com.jimz011apps.hki7.ui.components.itemCornerShape
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

private const val ENERGY_PAGE_KEY = "energy"

// Homey-inspired palette
private val ElecBlue    = Color(0xFF42A5F5)
private val SolarAmber  = Color(0xFFFFB300)
private val ExportGreen = Color(0xFF66BB6A)
private val ImportRed   = Color(0xFFEF5350)
private val BattPurple  = Color(0xFF7E57C2)
private val GasPink     = Color(0xFFEC407A)
private val WaterBlue   = Color(0xFF29B6F6)
private val DodgerBlue  = Color(0xFF1E90FF)
private val WindowWarm  = Color(0xFFFFDF9E)

/** Chart time ranges, HA-style: hourly buckets for a day, daily for week/month, monthly for year. */
private enum class EnergyRange(val label: String) {
    DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year")
}

/** A concrete calendar window: the selected range shifted [offset] periods back from now. */
private data class EnergyWindow(
    val range: EnergyRange, val offset: Int,
    val startDate: LocalDate, val startMs: Long, val endMs: Long, val buckets: Int
) {
    /** Cache key for fetched statistics, so ranges and offsets don't mix. */
    fun key() = "${range.name}:$offset"

    fun statPeriod(): String = when (range) {
        EnergyRange.DAY -> "hour"
        EnergyRange.WEEK, EnergyRange.MONTH -> "day"
        EnergyRange.YEAR -> "month"
    }

    fun bucketOf(t: Long): Int {
        val dt = java.time.Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault())
        return when (range) {
            EnergyRange.DAY -> ((t - startMs) / 3_600_000L).toInt()
            EnergyRange.WEEK, EnergyRange.MONTH ->
                java.time.temporal.ChronoUnit.DAYS.between(startDate, dt.toLocalDate()).toInt()
            EnergyRange.YEAR -> if (dt.year == startDate.year) dt.monthValue - 1 else -1
        }
    }

    /** Bucket index holding "now", or null when browsing a past period. */
    fun nowIndex(): Int? {
        if (offset != 0) return null
        val now = java.time.ZonedDateTime.now()
        return when (range) {
            EnergyRange.DAY -> now.hour
            EnergyRange.WEEK -> now.dayOfWeek.value - 1
            EnergyRange.MONTH -> now.dayOfMonth - 1
            EnergyRange.YEAR -> now.monthValue - 1
        }
    }

    /** Human title, e.g. "Today", "Yesterday", "23 Jun – 29 Jun", "May 2026", "2025". */
    fun title(): String {
        val dayMonth = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        return when {
            offset == 0 -> when (range) {
                EnergyRange.DAY -> "Today"; EnergyRange.WEEK -> "This week"
                EnergyRange.MONTH -> "This month"; EnergyRange.YEAR -> "This year"
            }
            range == EnergyRange.DAY && offset == -1 -> "Yesterday"
            range == EnergyRange.DAY -> startDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM"))
            range == EnergyRange.WEEK -> "${startDate.format(dayMonth)} – ${startDate.plusDays(6).format(dayMonth)}"
            range == EnergyRange.MONTH -> startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            else -> startDate.year.toString()
        }
    }

    /** Short label used in "Used <label>" strings. */
    fun periodLabel(): String = when {
        offset == 0 -> when (range) {
            EnergyRange.DAY -> "today"; EnergyRange.WEEK -> "this week"
            EnergyRange.MONTH -> "this month"; EnergyRange.YEAR -> "this year"
        }
        range == EnergyRange.DAY && offset == -1 -> "yesterday"
        else -> title()
    }

    fun tooltipLabels(): List<String> = when (range) {
        EnergyRange.DAY -> (0..23).map { "%d:00".format(it) }
        EnergyRange.WEEK -> (0 until buckets).map {
            startDate.plusDays(it.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM"))
        }
        EnergyRange.MONTH -> (0 until buckets).map {
            startDate.plusDays(it.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
        }
        EnergyRange.YEAR -> (0 until buckets).map {
            startDate.plusMonths(it.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("MMM"))
        }
    }

    fun axisLabels(tooltips: List<String>): List<String> = when (range) {
        EnergyRange.DAY -> listOf("0:00", "6:00", "12:00", "18:00", "")
        EnergyRange.WEEK -> tooltips.map { it.substringBefore(" ") }
        EnergyRange.MONTH -> listOf(
            tooltips.getOrElse(0) { "" }, tooltips.getOrElse(buckets / 3) { "" },
            tooltips.getOrElse(buckets * 2 / 3) { "" }, tooltips.getOrElse(buckets - 1) { "" }, ""
        )
        EnergyRange.YEAR -> tooltips.map { it.first().toString() }
    }
}

private fun energyWindow(range: EnergyRange, offset: Int): EnergyWindow {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
    return when (range) {
        EnergyRange.DAY -> {
            val start = today.plusDays(offset.toLong())
            EnergyWindow(range, offset, start, ms(start), ms(start.plusDays(1)), 24)
        }
        EnergyRange.WEEK -> {
            val start = today.with(java.time.DayOfWeek.MONDAY).plusWeeks(offset.toLong())
            EnergyWindow(range, offset, start, ms(start), ms(start.plusWeeks(1)), 7)
        }
        EnergyRange.MONTH -> {
            val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
            EnergyWindow(range, offset, start, ms(start), ms(start.plusMonths(1)), start.lengthOfMonth())
        }
        EnergyRange.YEAR -> {
            val start = today.withDayOfYear(1).plusYears(offset.toLong())
            EnergyWindow(range, offset, start, ms(start), ms(start.plusYears(1)), 12)
        }
    }
}

private fun energyOffsetForDate(range: EnergyRange, selectedDate: LocalDate): Int {
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (range) {
        EnergyRange.DAY -> java.time.temporal.ChronoUnit.DAYS.between(today, selectedDate).toInt()
        EnergyRange.WEEK -> {
            val thisWeek = today.with(java.time.DayOfWeek.MONDAY)
            val selectedWeek = selectedDate.with(java.time.DayOfWeek.MONDAY)
            java.time.temporal.ChronoUnit.WEEKS.between(thisWeek, selectedWeek).toInt()
        }
        EnergyRange.MONTH -> java.time.temporal.ChronoUnit.MONTHS.between(
            today.withDayOfMonth(1),
            selectedDate.withDayOfMonth(1)
        ).toInt()
        EnergyRange.YEAR -> selectedDate.year - today.year
    }.coerceAtMost(0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(viewModel: MainViewModel) {
    val energyEntityFlow = remember(viewModel) {
        viewModel.entitiesMatching("domain:sensor") { it.entity_id.startsWith("sensor.") }
    }
    val rawEntities by energyEntityFlow.collectAsState()
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val historyMap     by viewModel.historyMapping.collectAsState()
    val homeAssistantSolarForecasts by viewModel.energySolarForecasts.collectAsState()
    val energyConfig: HKIEnergyConfig =
        (pageConfigsMap[ENERGY_PAGE_KEY] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()
    val isEmptyManualEnergyConfig = remember(energyConfig) {
        energyConfig.manualOnly && energyConfig.copy(
            manualOnly = false,
            cardOrder = emptyList(),
            customNames = emptyMap(),
            hiddenPowerDeviceEntityIds = emptyList(),
            hiddenEnergyDeviceEntityIds = emptyList(),
            hiddenWaterDeviceEntityIds = emptyList()
        ) == HKIEnergyConfig()
    }
    LaunchedEffect(
        rawEntities.isNotEmpty(),
        energyConfig.usesHomeAssistantEnergyPreferences,
        energyConfig.gridCarbonFootprintEntityId
    ) {
        if (
            rawEntities.isNotEmpty() &&
            !energyConfig.manualOnly &&
            (!energyConfig.usesHomeAssistantEnergyPreferences || energyConfig.gridCarbonFootprintEntityId == null)
        ) {
            viewModel.importHomeAssistantEnergyPreferences(ENERGY_PAGE_KEY)
        }
    }
    LaunchedEffect(energyConfig.usesHomeAssistantEnergyPreferences, energyConfig.solarForecastConfigEntryIds) {
        if (energyConfig.usesHomeAssistantEnergyPreferences && energyConfig.solarForecastConfigEntryIds.isNotEmpty()) {
            viewModel.fetchHomeAssistantEnergySolarForecasts()
        }
    }
    val isEditMode by viewModel.isEditMode.collectAsState()
    var renameEntity by remember { mutableStateOf<HAEntity?>(null) }
    renameEntity?.let { entity ->
        RenameCardDialog(energyConfig.customNames[entity.entity_id].orEmpty(), entity.friendlyName ?: entity.entity_id,
            onDismiss = { renameEntity = null }) { name ->
            val names = if (name == null) energyConfig.customNames - entity.entity_id else energyConfig.customNames + (entity.entity_id to name)
            viewModel.updateEnergyConfig(ENERGY_PAGE_KEY, energyConfig.copy(customNames = names))
            renameEntity = null
        }
    }
    val entities = remember(rawEntities, energyConfig.customNames) {
        rawEntities.map { it.withDisplayName(energyConfig.customNames[it.entity_id]) }
    }

    // One id->entity map per state update; the previous per-lookup linear scans made every
    // websocket batch O(sensors x lookups) and visibly janked the page.
    val entityById = remember(entities) { entities.associateBy { it.entity_id } }

    // Power/energy entities update live via the websocket subscription (see MainViewModel realtime
    // sync), so no per-screen full-state polling is needed here.

    fun entityWatts(id: String?): Float? {
        if (id.isNullOrBlank()) return null
        val e = entityById[id] ?: return null
        val v = e.state.toFloatOrNull() ?: return null
        val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
        return if (unit.contains("kW", ignoreCase = true)) v * 1000f else v
    }
    fun entityFloat(id: String?): Float? =
        id?.takeIf { it.isNotBlank() }?.let { entityById[it]?.state?.toFloatOrNull() }
    fun entityUnit(id: String?, fallback: String): String =
        id?.takeIf { it.isNotBlank() }
            ?.let { entityById[it]?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull }
            ?: fallback
    fun entityDisplay(id: String?): String? {
        if (id.isNullOrBlank()) return null
        val e = entityById[id] ?: return null
        val v = e.state.toFloatOrNull() ?: return null
        val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
        val num = if (v >= 100f) "%.0f".format(v) else "%.1f".format(v)
        return listOf(num, unit).filter { it.isNotBlank() }.joinToString(" ")
    }
    fun entityCarbonDisplay(id: String?): String? {
        if (id.isNullOrBlank()) return null
        val e = entityById[id] ?: return null
        val v = e.state.toFloatOrNull() ?: return null
        val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
        return formatCarbonIntensity(v, unit)
    }

    val solarW   = entityWatts(energyConfig.solarPowerEntityId) ?: 0f
    val gridW    = entityWatts(energyConfig.gridPowerEntityId) ?: 0f
    val homeW    = entityWatts(energyConfig.homePowerEntityId) ?: (solarW + gridW).coerceAtLeast(0f)
    val batteryW = entityWatts(energyConfig.batteryPowerEntityId) ?: 0f
    val solarKwh   = entityFloat(energyConfig.solarEnergyEntityId) ?: 0f
    val costVal    = entityFloat(energyConfig.energyCostEntityId) ?: 0f
    val carbonFootprintDisplay = entityCarbonDisplay(energyConfig.gridCarbonFootprintEntityId)
    val batteryPct = energyConfig.batteryEntityId?.let { entityById[it] }?.state?.toIntOrNull()
    val hasBattery = !energyConfig.batteryEntityId.isNullOrBlank()

    val gasVal   = entityFloat(energyConfig.gasEntityId)
    val gasCost  = entityFloat(energyConfig.gasCostEntityId)
    val waterVal = entityFloat(energyConfig.waterEntityId)
    val waterCost = entityFloat(energyConfig.waterCostEntityId)
    val gasUnit   = entityUnit(energyConfig.gasEntityId, "m³")
    val waterUnit = entityUnit(energyConfig.waterEntityId, "L")
    // Water is always shown in liters, even when the sensor reports m³.
    val waterIsM3 = waterUnit.contains("m³") || waterUnit.contains("m3", ignoreCase = true)
    val waterFactor = if (waterIsM3) 1000f else 1f
    val waterDisplayUnit = if (waterIsM3) "L" else waterUnit
    // Live flow rate for the tiles; when absent the tiles fall back to the meter total.
    val gasCurrentDisplay   = entityDisplay(energyConfig.gasCurrentEntityId)
    val waterCurrentDisplay = entityDisplay(energyConfig.waterCurrentEntityId)

    val phaseIds = listOf(
        energyConfig.powerPhase1EntityId, energyConfig.powerPhase2EntityId, energyConfig.powerPhase3EntityId
    ).map { it?.takeIf { id -> id.isNotBlank() } }
    val currentIds = listOf(
        energyConfig.currentPhase1EntityId, energyConfig.currentPhase2EntityId, energyConfig.currentPhase3EntityId
    ).map { it?.takeIf { id -> id.isNotBlank() } }
    val voltageIds = listOf(
        energyConfig.voltagePhase1EntityId, energyConfig.voltagePhase2EntityId, energyConfig.voltagePhase3EntityId
    ).map { it?.takeIf { id -> id.isNotBlank() } }

    val forecastIds = (energyConfig.solarForecastEntityIds + listOfNotNull(energyConfig.solarForecastEntityId))
        .filter { it.isNotBlank() }.distinct()

    val hasSolar = !energyConfig.solarPowerEntityId.isNullOrBlank() ||
        !energyConfig.solarEnergyEntityId.isNullOrBlank() ||
        !energyConfig.solarDeviceId.isNullOrBlank()
    var page by rememberSaveable { mutableStateOf("energy") }
    androidx.activity.compose.BackHandler(enabled = page != "energy") { page = "energy" }
    var rangeName by rememberSaveable { mutableStateOf(EnergyRange.DAY.name) }
    // 0 = current period, -1 = previous, ... (HA-style look-back navigation).
    var rangeOffset by rememberSaveable { mutableIntStateOf(0) }
    val range = EnergyRange.valueOf(rangeName)
    val window = remember(range, rangeOffset) { energyWindow(range, rangeOffset) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = window.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        rangeOffset = energyOffsetForDate(range, selectedDate)
                    }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        rangeOffset = 0
                        showDatePicker = false
                    }) { Text("Today") }
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Chart data comes from HA's recorder statistics: pre-aggregated hourly/daily mean+change
    // values, the same source the official energy dashboard uses. Raw history for a per-second
    // P1 meter over a month is millions of rows - parsing that crashed the app.
    val energyStats by viewModel.energyStats.collectAsState()
    val chartPowerId = energyConfig.homePowerEntityId?.takeIf { it.isNotBlank() }
        ?: energyConfig.gridPowerEntityId?.takeIf { it.isNotBlank() }
    val solarPowerId = energyConfig.solarPowerEntityId?.takeIf { it.isNotBlank() }
    val gasId    = energyConfig.gasEntityId?.takeIf { it.isNotBlank() }
    val waterId  = energyConfig.waterEntityId?.takeIf { it.isNotBlank() }
    val importId = energyConfig.gridImportEntityId?.takeIf { it.isNotBlank() }
    val exportId = energyConfig.gridExportEntityId?.takeIf { it.isNotBlank() }
    val solarEnergyId = energyConfig.solarEnergyEntityId?.takeIf { it.isNotBlank() }
    val forecastPowerIds = forecastIds.filter { id ->
        entityUnit(id, "").let { it.contains("W", ignoreCase = true) && !it.contains("Wh", ignoreCase = true) }
    }
    val batteryPowerId = energyConfig.batteryPowerEntityId?.takeIf { it.isNotBlank() }
    val statIds = (listOfNotNull(
        chartPowerId, solarPowerId, gasId, waterId, importId, exportId, solarEnergyId, batteryPowerId
    ) + phaseIds.filterNotNull()).distinct()
    LaunchedEffect(statIds, window) {
        viewModel.fetchEnergyStatistics(statIds, window.startMs, window.statPeriod(), window.key(), window.endMs)
    }
    // Today's gas/water usage for the live tiles — always the current day, whatever the filter.
    val todayWindow = remember { energyWindow(EnergyRange.DAY, 0) }
    LaunchedEffect(gasId, waterId) {
        val ids = listOfNotNull(gasId, waterId)
        if (ids.isNotEmpty())
            viewModel.fetchEnergyStatistics(ids, todayWindow.startMs, "hour", "TODAY", todayWindow.endMs)
    }
    // Forecast sensors usually have no long-term statistics; chart them from raw history (today only).
    LaunchedEffect(forecastPowerIds, window) {
        if (range == EnergyRange.DAY && rangeOffset == 0) forecastPowerIds.forEach { viewModel.fetchEntityHistory(it, 24) }
    }
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    // Bucketed chart data + labels for the active window. Charts always render, even without data.
    val tooltipLabels = remember(window) { window.tooltipLabels() }
    val axisLabels = remember(window, tooltipLabels) { window.axisLabels(tooltipLabels) }
    val nowIndex = window.nowIndex()

    fun statPoints(id: String?): List<HAStatPoint>? = id?.let { energyStats["$it|${window.key()}"] }
    fun statMeans(id: String?): FloatArray {
        val scale = if (id != null && entityIsKw(entities, id)) 1000f else 1f
        val out = FloatArray(window.buckets)
        statPoints(id)?.forEach { p ->
            val i = window.bucketOf(p.startMs)
            if (i in 0 until window.buckets) out[i] = (p.mean ?: 0f) * scale
        }
        return out
    }
    fun statChanges(id: String?): FloatArray {
        val out = FloatArray(window.buckets)
        statPoints(id)?.forEach { p ->
            val i = window.bucketOf(p.startMs)
            if (i in 0 until window.buckets) out[i] += (p.change ?: 0f).coerceAtLeast(0f)
        }
        return out
    }
    fun statTotal(id: String?): Float =
        statPoints(id)?.sumOf { (it.change ?: 0f).coerceAtLeast(0f).toDouble() }?.toFloat() ?: 0f
    fun todayTotal(id: String?): Float =
        id?.let { energyStats["$it|TODAY"] }
            ?.sumOf { (it.change ?: 0f).coerceAtLeast(0f).toDouble() }?.toFloat() ?: 0f
    fun todayRecentUsage(id: String?, hours: Int = 1): Boolean {
        val now = todayWindow.nowIndex() ?: return false
        val from = (now - hours + 1).coerceAtLeast(0)
        return id?.let { energyStats["$it|TODAY"] }
            ?.any { p ->
                val i = todayWindow.bucketOf(p.startMs)
                i in from..now && (p.change ?: 0f) > 0.0001f
            } == true
    }
    val gasToday = todayTotal(gasId)
    val waterTodayL = todayTotal(waterId) * waterFactor
    val gasUsedRecently = todayRecentUsage(gasId) || (entityFloat(energyConfig.gasCurrentEntityId) ?: 0f) > 0.001f

    val homeSeries  = remember(energyStats, chartPowerId, window) { statMeans(chartPowerId) }
    val solarSeries = remember(energyStats, solarPowerId, window) { statMeans(solarPowerId) }
    val gasSeries   = remember(energyStats, gasId, window) { statChanges(gasId) }
    val waterSeries = remember(energyStats, waterId, window) {
        val raw = statChanges(waterId)
        if (waterIsM3) FloatArray(raw.size) { raw[it] * 1000f } else raw
    }
    val battSeries  = remember(energyStats, batteryPowerId, window) { statMeans(batteryPowerId) }
    // HA-style electricity usage layers (kWh per bucket): import + consumed solar stack up,
    // export stacks down. Consumed solar = production - export, clamped per bucket.
    val importEnergySeries = remember(energyStats, importId, window) { statChanges(importId) }
    val exportEnergySeries = remember(energyStats, exportId, window) { statChanges(exportId) }
    val solarEnergySeries  = remember(energyStats, solarEnergyId, window) { statChanges(solarEnergyId) }
    val consumedSolarSeries = remember(solarEnergySeries, exportEnergySeries) {
        FloatArray(window.buckets) { (solarEnergySeries[it] - exportEnergySeries[it]).coerceAtLeast(0f) }
    }
    val usagePosLayers = remember(importEnergySeries, consumedSolarSeries, importId, solarEnergyId) {
        buildList {
            // Self-consumed solar is the base of total consumption, starting at the zero line.
            if (solarEnergyId != null) add(Triple("Consumed solar", consumedSolarSeries, SolarAmber))
            if (importId != null) add(Triple("Imported", importEnergySeries, ElecBlue))
        }
    }
    val usageNegLayers = remember(exportEnergySeries, exportId) {
        if (exportId != null) listOf(Triple("Exported", exportEnergySeries, BattPurple)) else emptyList()
    }
    val hasUsageChart = usagePosLayers.isNotEmpty() || usageNegLayers.isNotEmpty()
    val phaseColors = listOf(Color(0xFF42A5F5), Color(0xFFFFB300), Color(0xFFEF5350))
    val phaseSeries = remember(energyStats, phaseIds, window) {
        phaseIds.mapIndexedNotNull { i, id ->
            id ?: return@mapIndexedNotNull null
            Triple("Phase ${i + 1}", statMeans(id), phaseColors[i])
        }
    }
    // Period totals: deltas of the (lifetime) energy counters over the selected window - how HA
    // derives energy use, instead of showing meaningless lifetime totals.
    val importPeriod   = statTotal(importId)
    val exportPeriod   = statTotal(exportId)
    val producedPeriod = statTotal(solarEnergyId)
    val usedPeriod = (importPeriod + producedPeriod - exportPeriod).coerceAtLeast(0f)
    val gasPeriod   = statTotal(gasId)
    val waterPeriod = statTotal(waterId)
    // Self-used solar: whatever was produced but not exported stayed in the house.
    val selfUsedPeriod = (producedPeriod - exportPeriod).coerceIn(0f, producedPeriod)
    val selfUsedPct = if (producedPeriod > 0.01f) (selfUsedPeriod / producedPeriod * 100).toInt() else null
    val selfSufficiencyPct = if (solarEnergyId != null && usedPeriod > 0.01f) {
        (selfUsedPeriod / usedPeriod * 100f).toInt().coerceIn(0, 100)
    } else null
    val periodLabel = window.periodLabel()

    val forecastPalette = listOf(Color(0xFF29B6F6), Color(0xFFAB47BC), Color(0xFF26A69A), Color(0xFF8D6E63))
    val forecastHists = forecastPowerIds.map { historyMap[it] }
    val forecastSeries = remember(forecastHists, window) {
        if (range != EnergyRange.DAY || rangeOffset != 0) emptyList()
        else forecastPowerIds.mapIndexedNotNull { i, id ->
            val values = bucketPowerAverages(forecastHists[i], entityIsKw(entities, id), window) ?: return@mapIndexedNotNull null
            val name = entityById[id]?.friendlyName ?: "Forecast ${i + 1}"
            Triple(name, values, forecastPalette[i % forecastPalette.size])
        }
    }
    val importedForecastSeries = remember(homeAssistantSolarForecasts, energyConfig.solarForecastConfigEntryIds, window, range, rangeOffset) {
        if (range != EnergyRange.DAY || rangeOffset != 0) emptyList()
        else energyConfig.solarForecastConfigEntryIds.mapNotNull { providerId ->
            val hours = homeAssistantSolarForecasts[providerId].orEmpty()
            if (hours.isEmpty()) return@mapNotNull null
            val values = FloatArray(window.buckets)
            hours.forEach { (timestamp, wh) ->
                val index = window.bucketOf(timestamp)
                if (index in values.indices) values[index] += wh
            }
            Triple(
                if (energyConfig.solarForecastConfigEntryIds.size == 1) "Home Assistant forecast" else "Forecast ${energyConfig.solarForecastConfigEntryIds.indexOf(providerId) + 1}",
                values,
                forecastPalette[(forecastSeries.size + energyConfig.solarForecastConfigEntryIds.indexOf(providerId)) % forecastPalette.size]
            )
        }
    }
    val importedForecastKwhToday = remember(homeAssistantSolarForecasts, energyConfig.solarForecastConfigEntryIds, window) {
        energyConfig.solarForecastConfigEntryIds.sumOf { providerId ->
            homeAssistantSolarForecasts[providerId].orEmpty()
                .filterKeys { it in window.startMs until window.endMs }
                .values.sum().toDouble()
        }.toFloat() / 1000f
    }.takeIf { it > 0f }
    val forecastKwhToday = importedForecastKwhToday ?: forecastIds.firstNotNullOfOrNull { id ->
        if (entityUnit(id, "").contains("Wh", ignoreCase = true)) entityFloat(id) else null
    } ?: energyConfig.solarForecastEntityId?.let { entityFloat(it) }

    val mainPowerIds = remember(energyConfig) {
        setOfNotNull(
            energyConfig.solarPowerEntityId, energyConfig.gridPowerEntityId,
            energyConfig.homePowerEntityId, energyConfig.batteryPowerEntityId,
            energyConfig.powerPhase1EntityId, energyConfig.powerPhase2EntityId, energyConfig.powerPhase3EntityId
        )
    }
    val topConsumers = remember(entities, mainPowerIds, energyConfig.deviceEntityIds, energyConfig.hiddenPowerDeviceEntityIds, energyConfig.usesHomeAssistantEnergyPreferences) {
        fun wattsOf(e: HAEntity): Float? {
            val v = e.state.toFloatOrNull() ?: return null
            val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
            return if (unit.contains("kW", ignoreCase = true)) v * 1000f else v
        }
        val manual = energyConfig.deviceEntityIds.mapNotNull { id ->
            entityById[id]?.let { e -> e to (wattsOf(e) ?: 0f) }
        }
        val manualIds = manual.map { it.first.entity_id }.toSet()
        val auto = (if (energyConfig.usesHomeAssistantEnergyPreferences || energyConfig.manualOnly) emptySequence() else entities.asSequence())
            .filter {
                it.entity_id.startsWith("sensor.") && it.entity_id !in mainPowerIds &&
                it.entity_id !in manualIds && it.entity_id !in energyConfig.hiddenPowerDeviceEntityIds &&
                    it.deviceClass == "power"
            }
            .map { e -> e to (wattsOf(e) ?: 0f) }
            .toList()
        (manual + auto).distinctBy { it.first.entity_id }.sortedByDescending { it.second }
    }
    val primaryEnergyIds = remember(energyConfig) {
        setOfNotNull(
            energyConfig.gridImportEntityId, energyConfig.gridExportEntityId, energyConfig.solarEnergyEntityId,
            energyConfig.gridImportTariff1EntityId, energyConfig.gridImportTariff2EntityId,
            energyConfig.gridExportTariff1EntityId, energyConfig.gridExportTariff2EntityId
        )
    }
    val deviceEnergyEntities = remember(entities, primaryEnergyIds, energyConfig.energyDeviceEntityIds, energyConfig.hiddenEnergyDeviceEntityIds, energyConfig.usesHomeAssistantEnergyPreferences) {
        val manual = energyConfig.energyDeviceEntityIds.mapNotNull(entityById::get)
        val manualIds = manual.map { it.entity_id }.toSet()
        val auto = if (energyConfig.usesHomeAssistantEnergyPreferences || energyConfig.manualOnly) emptyList() else entities.filter {
            it.entity_id.startsWith("sensor.") && it.deviceClass == "energy" &&
                it.entity_id !in primaryEnergyIds && it.entity_id !in manualIds &&
                it.entity_id !in energyConfig.hiddenEnergyDeviceEntityIds
        }
        (manual + auto).distinctBy { it.entity_id }
    }
    fun deviceEnergyKwh(entity: HAEntity): Float {
        val value = statChanges(entity.entity_id).sum()
        val unit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
        return if (unit.equals("Wh", ignoreCase = true)) value / 1000f else value
    }
    val deviceEnergyIds = remember(deviceEnergyEntities) { deviceEnergyEntities.map { it.entity_id } }
    LaunchedEffect(deviceEnergyIds.toSet(), window) {
        if (deviceEnergyIds.isNotEmpty())
            viewModel.fetchEnergyStatistics(deviceEnergyIds, window.startMs, window.statPeriod(), window.key(), window.endMs)
    }
    val waterDeviceEntities = remember(entities, energyConfig.waterDeviceEntityIds) {
        energyConfig.waterDeviceEntityIds.mapNotNull(entityById::get).distinctBy { it.entity_id }
    }
    val waterDeviceIds = remember(waterDeviceEntities) { waterDeviceEntities.map { it.entity_id } }
    LaunchedEffect(waterDeviceIds.toSet(), window) {
        if (waterDeviceIds.isNotEmpty()) {
            viewModel.fetchEnergyStatistics(waterDeviceIds, window.startMs, window.statPeriod(), window.key(), window.endMs)
        }
    }

    val energySettingsSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> = "Energy Sensors" to { setBack ->
        EnergySensorSection(
            viewModel = viewModel,
            energyConfig = energyConfig,
            allEntities = entities,
            onSave = { newCfg -> viewModel.updateEnergyConfig(ENERGY_PAGE_KEY, newCfg) },
            setBack = setBack
        )
    }
    var showEnergyReimport by remember { mutableStateOf(false) }
    var showClearEnergy by remember { mutableStateOf(false) }
    val energyImportSection: Pair<String, @Composable ColumnScope.(setBack: ((() -> Unit)?) -> Unit) -> Unit> = "Re-import" to { _ ->
        Text("Fetch the Home Assistant energy dashboard configuration again.", color = LocalHKIAppColors.current.onMuted)
        Button(onClick = { showEnergyReimport = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Re-import Energy")
        }
        OutlinedButton(onClick = { showClearEnergy = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear Energy View", color = MaterialTheme.colorScheme.error)
        }
    }
    if (showEnergyReimport) {
        AlertDialog(
            onDismissRequest = { showEnergyReimport = false },
            title = { Text("Re-import energy") },
            text = { Text("Import settings that have not been edited, or remove all energy edits and import from scratch.") },
            confirmButton = { Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { viewModel.reimportEnergy(false); showEnergyReimport = false }) { Text("Import unedited") }
                TextButton(onClick = { viewModel.reimportEnergy(true); showEnergyReimport = false }) { Text("Remove edits and import all", color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(onClick = { showEnergyReimport = false }) { Text("Cancel") } }
        )
    }
    if (showClearEnergy) {
        AlertDialog(
            onDismissRequest = { showClearEnergy = false },
            title = { Text("Clear energy view?") },
            text = { Text("This removes all imported energy entities from this view.") },
            confirmButton = { TextButton(onClick = { viewModel.clearEnergyImports(); showClearEnergy = false }) { Text("Clear", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showClearEnergy = false }) { Text("Cancel") } }
        )
    }

    val pageTitle = when (page) {
        "solar" -> "Solar"; "electricity" -> "Electricity"; "gas" -> "Gas"
        "water" -> "Water"; "battery" -> "Battery"; else -> "Energy"
    }
    val pageSubtitle = when (page) {
        "solar" -> "Production overview"; "electricity" -> "Grid & phases"
        "gas" -> "Usage overview"; "water" -> "Usage overview"
        "battery" -> "Charge & flow"; else -> "Power overview"
    }
    HKIPage(
        viewModel = viewModel,
        title = pageTitle,
        subtitle = pageSubtitle,
        pageKey = ENERGY_PAGE_KEY,
        pageSettingsTitle = "Energy Settings",
        extraPageSettingsSection = energySettingsSection,
        additionalPageSettingsSections = listOf(energyImportSection),
        showBadgeBar = false,
        // Time filter lives in the pinned header slot so it never scrolls away.
        headerBar = if (isEmptyManualEnergyConfig) null else ({
            val appColors = LocalHKIAppColors.current
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    EnergyRange.entries.forEach { r ->
                        FilterChip(
                            selected = range == r,
                            onClick = { rangeName = r.name; rangeOffset = 0 },
                            label = { Text(r.label) },
                            shape = itemCornerShape()
                        )
                    }
                }
                // HA-style period navigation: step back/forward through past days/weeks/months/years.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { rangeOffset-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronLeft, "Previous period", tint = appColors.onSurface)
                    }
                    Text(
                        window.title(),
                        style = MaterialTheme.typography.titleSmall,
                        color = appColors.onSurface, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(min = 148.dp)
                            .clip(itemCornerShape())
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    IconButton(onClick = { rangeOffset++ }, enabled = rangeOffset < 0, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.ChevronRight, "Next period",
                            tint = if (rangeOffset < 0) appColors.onSurface else appColors.onMuted.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }),
        onBack = if (page != "energy") ({ page = "energy" }) else null
    ) { padding ->
        val appColors = LocalHKIAppColors.current
        if (isEmptyManualEnergyConfig) {
            EmptyEditHint(
                Modifier.fillMaxSize().padding(padding),
                "This is an empty energy view. Swipe down on the header and open Energy Settings to add entities manually."
            )
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp + com.jimz011apps.hki7.ui.components.LocalMediaPlayerBarInset.current)
        ) {
            if (page == "energy") {
                // ── the animated house ────────────────────────────────────────
                item {
                    EnergyHero(
                        solarW = solarW, gridW = gridW, homeW = homeW,
                        batteryW = batteryW, batteryPct = batteryPct, hasBattery = hasBattery,
                        hasSolar = hasSolar, hasGas = gasId != null, hasWater = waterId != null,
                        gasFlowing = gasUsedRecently,
                        waterFlowing = (entityFloat(energyConfig.waterCurrentEntityId) ?: 0f) > 0.001f
                    )
                }

                // ── live source tiles (each opens its own tab) ────────────────
                item {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    class TileSpec(
                        val icon: androidx.compose.ui.graphics.vector.ImageVector,
                        val color: Color, val title: String, val status: String,
                        val onClick: (() -> Unit)?
                    )
                    val gridStatus = when {
                        gridW > 10f  -> "Importing"
                        gridW < -10f -> "Exporting"
                        else         -> "Idle"
                    }
                    val battStatus = when {
                        batteryW > 10f  -> "Charging"
                        batteryW < -10f -> "Discharging"
                        else            -> "Idle"
                    }
                    val battText = listOfNotNull(
                        batteryPct?.let { "$it%" },
                        if (battStatus != "Idle") formatW(abs(batteryW)) else null
                    ).joinToString(" · ").ifEmpty { "—" } + " · $battStatus"
                    val tiles = buildList {
                        add(TileSpec(Icons.Default.ElectricBolt, ElecBlue, "Electricity",
                            "${formatW(abs(gridW))} · $gridStatus") { page = "electricity" })
                        add(TileSpec(Icons.Default.WbSunny, SolarAmber, "Solar",
                            "${formatW(solarW.coerceAtLeast(0f))} · ${if (solarW > 10f) "Producing" else "Idle"}",
                            if (hasSolar) ({ page = "solar" }) else null))
                        add(TileSpec(Icons.Default.Home, primaryColor, "Home",
                            "${formatW(homeW)} · ${if (homeW > 10f) "Consuming" else "Idle"}", null))
                        if (hasBattery) add(TileSpec(
                            if (batteryW > 10f) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            when { (batteryPct ?: 0) > 50 -> ExportGreen; (batteryPct ?: 0) > 20 -> SolarAmber; else -> ImportRed },
                            "Battery", battText) { page = "battery" })
                        if (gasId != null) add(TileSpec(Icons.Default.LocalFireDepartment, GasPink, "Gas",
                            gasCurrentDisplay ?: "%.1f %s today".format(gasToday, gasUnit)) { page = "gas" })
                        if (waterId != null) add(TileSpec(Icons.Default.WaterDrop, WaterBlue, "Water",
                            waterCurrentDisplay ?: (if (waterTodayL >= 100f) "%.0f %s today" else "%.1f %s today")
                                .format(waterTodayL, waterDisplayUnit)) { page = "water" })
                    }
                    BoxWithConstraints(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Auto-fit: 2 tiles across when there's room for a readable ~160dp tile,
                        // dropping to 1 on narrow windows so labels never letter-wrap.
                        val tileColumns = ((maxWidth + 10.dp) / 170.dp).toInt().coerceIn(1, 2)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tiles.chunked(tileColumns).forEach { rowTiles ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowTiles.forEach { t ->
                                        EnergyLiveTile(t.icon, t.color, t.title, t.status, Modifier.weight(1f), onClick = t.onClick)
                                    }
                                    repeat(tileColumns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }

                // ── Electricity Total (phases/tariffs live on the Electricity tab) ─
                item {
                    SectionHeader("Electricity Total", if (!energyConfig.energyCostEntityId.isNullOrBlank()) "€ ${"%.2f".format(costVal)}" else null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // FlowRow lets the stats keep their natural width and wrap onto a
                                // second line on narrow screens, instead of a weighted SpaceEvenly
                                // Row squeezing each label until it stacks one glyph per line.
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TotalStat(Icons.Default.ArrowDownward, ElecBlue, "%.1f kWh".format(usedPeriod), "Used $periodLabel")
                                    TotalStat(Icons.Default.ArrowDownward, ImportRed, "%.1f kWh".format(importPeriod), "Imported")
                                    TotalStat(Icons.Default.ArrowUpward, ExportGreen, "%.1f kWh".format(exportPeriod), "Exported")
                                    selfSufficiencyPct?.let {
                                        TotalStat(Icons.Default.Home, SolarAmber, "$it%", "Self-sufficient")
                                    }
                                }
                                TextButton(onClick = { page = "electricity" }) { Text("Details") }
                            }
                            carbonFootprintDisplay?.let {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    TotalStat(Icons.Default.Cloud, ExportGreen, it, "Carbon footprint")
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            if (hasUsageChart) {
                                EnergyStackedBarChart(usagePosLayers, usageNegLayers, "kWh", axisLabels, tooltipLabels, nowIndex = nowIndex)
                            } else {
                                EnergyBarChart(homeSeries, ElecBlue, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                            }
                        }
                    }
                }

                // ── Solar summary ─────────────────────────────────────────────
                if (hasSolar) {
                    item {
                        SectionHeader("Solar", null)
                        Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    IconBadge(Icons.Default.WbSunny, SolarAmber)
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("%.1f".format(producedPeriod), style = MaterialTheme.typography.headlineSmall,
                                                color = appColors.onSurface, fontWeight = FontWeight.Bold)
                                            Text("kWh", style = MaterialTheme.typography.labelMedium, color = SolarAmber,
                                                modifier = Modifier.padding(bottom = 3.dp))
                                        }
                                        Text(
                                            "Produced $periodLabel" + (selfUsedPct?.let {
                                                " · %.1f kWh self-used ($it%%)".format(selfUsedPeriod)
                                            } ?: ""),
                                            style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                                        )
                                    }
                                    TextButton(onClick = { page = "solar" }) { Text("Details") }
                                }
                                Spacer(Modifier.height(14.dp))
                                EnergyBarChart(solarSeries, SolarAmber, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                            }
                        }
                    }
                }

                // ── Gas ───────────────────────────────────────────────────────
                if (gasId != null) {
                    item {
                        SectionHeader("Gas", gasCost?.let { "€ ${"%.2f".format(it)}" })
                        UtilityCard(
                            icon = Icons.Default.LocalFireDepartment, color = GasPink,
                            value = "%.1f".format(gasPeriod), unit = gasUnit,
                            label = "Used $periodLabel"
                        ) {
                            EnergyBarChart(gasSeries, GasPink, gasUnit, axisLabels, tooltipLabels, nowIndex = nowIndex)
                        }
                    }
                }

                // ── Water ─────────────────────────────────────────────────────
                if (waterId != null) {
                    item {
                        val waterUsed = waterPeriod * waterFactor
                        SectionHeader("Water", waterCost?.let { "€ ${"%.2f".format(it)}" })
                        UtilityCard(
                            icon = Icons.Default.WaterDrop, color = WaterBlue,
                            value = if (waterUsed >= 100f) "%.0f".format(waterUsed) else "%.1f".format(waterUsed), unit = waterDisplayUnit,
                            label = "Used $periodLabel"
                        ) {
                            EnergyBarChart(waterSeries, WaterBlue, waterDisplayUnit, axisLabels, tooltipLabels, nowIndex = nowIndex)
                        }
                    }
                }

                // ── Top consumers (last) ──────────────────────────────────────
                if (topConsumers.isNotEmpty()) {
                    item {
                        SectionHeader("Top consumers", null)
                        Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                        ) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                topConsumers.forEachIndexed { idx, (entity, watts) ->
                                    ConsumerRow(
                                        rank = idx + 1,
                                        name = entity.friendlyName ?: entity.entity_id,
                                        watts = watts,
                                        shareOfHome = if (homeW > 1f) (watts / homeW * 100).toInt().coerceAtMost(100) else null,
                                        onSettings = if (isEditMode) ({ renameEntity = entity }) else null
                                    )
                                    if (idx < topConsumers.lastIndex)
                                        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }

                    // ── Energy per device: horizontal bars over the window ────
                }
                if (deviceEnergyEntities.isNotEmpty()) {
                    item {
                        val deviceEnergies = deviceEnergyEntities
                            .map { e -> e to deviceEnergyKwh(e) }
                            .sortedByDescending { it.second }
                        val maxKwh = (deviceEnergies.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(0.001f)
                        SectionHeader("Device energy", "Used $periodLabel")
                        Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                deviceEnergies.forEach { (entity, kwh) ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                entity.friendlyName ?: entity.entity_id,
                                                style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isEditMode) EditSettingsButton(onClick = { renameEntity = entity })
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (kwh >= 10f) "%.1f kWh".format(kwh) else "%.2f kWh".format(kwh),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            Modifier.fillMaxWidth().height(8.dp)
                                                .background(ElecBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth((kwh / maxKwh).coerceIn(0.02f, 1f))
                                                    .fillMaxHeight()
                                                    .background(ElecBlue, RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (waterDeviceEntities.isNotEmpty()) {
                    item {
                        SectionHeader("Individual water usage", "Used $periodLabel")
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = itemCornerShape(),
                            color = appColors.elevated
                        ) {
                            IndividualWaterUsageContent(
                                entities = waterDeviceEntities,
                                rawUsage = { id -> statChanges(id).sum() },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else if (page == "solar") {
                // ═══ SOLAR PAGE ═══════════════════════════════════════════════
                item {
                    SectionHeader("Production", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TotalStat(Icons.Default.Bolt, SolarAmber, formatW(solarW.coerceAtLeast(0f)), "Now")
                                TotalStat(Icons.Default.WbSunny, SolarAmber, "%.1f kWh".format(producedPeriod), "Produced $periodLabel")
                                TotalStat(Icons.Default.Home, ExportGreen, "%.1f kWh".format(selfUsedPeriod),
                                    "Self-used" + (selfUsedPct?.let { " · $it%" } ?: ""))
                            }
                            val last7 = entityDisplay(energyConfig.solarLast7DaysEntityId)
                            val lifetime = entityDisplay(energyConfig.solarLifetimeEntityId)
                            if (last7 != null || lifetime != null) {
                                Spacer(Modifier.height(12.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (last7 != null) TotalStat(Icons.Default.DateRange, SolarAmber, last7, "Last 7 days")
                                    if (lifetime != null) TotalStat(Icons.Default.AllInclusive, SolarAmber, lifetime, "Lifetime")
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            EnergyBarChart(solarSeries, SolarAmber, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                        }
                    }
                }

                // ── Forecast (today only: forecast sensors have no history) ───
                if ((forecastIds.isNotEmpty() || importedForecastSeries.isNotEmpty()) && range == EnergyRange.DAY && rangeOffset == 0) {
                    item {
                        SectionHeader("Forecast", null)
                        Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                if (forecastKwhToday != null && forecastKwhToday > 0f) {
                                    val frac = (solarKwh / forecastKwhToday).coerceIn(0f, 1f)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Expected today", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                                        Text("%.1f of %.1f kWh · %d%%".format(solarKwh, forecastKwhToday, (frac * 100).toInt()),
                                            style = MaterialTheme.typography.labelSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier.fillMaxWidth().height(8.dp)
                                            .background(SolarAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    ) {
                                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(SolarAmber, RoundedCornerShape(4.dp)))
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }
                                val chartSeries = buildList {
                                    add(Triple("Production", solarSeries, SolarAmber))
                                    addAll(forecastSeries)
                                    addAll(importedForecastSeries)
                                }
                                EnergyMultiLineChart(chartSeries, "W", axisLabels, tooltipLabels)
                            }
                        }
                    }
                }

                // ── Inverters (includes child devices, e.g. inverters behind an Envoy) ─
                item {
                    val entityRegistry by viewModel.entityRegistry.collectAsState()
                    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
                    val deviceId = energyConfig.solarDeviceId
                    SectionHeader("Inverters", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        if (deviceId.isNullOrBlank()) {
                            Text(
                                "Select an inverter device in Energy Settings → Solar to list its inverters here.",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            val deviceName = deviceRegistry.find { it.id == deviceId }
                                ?.let { it.name_by_user ?: it.name } ?: "Device"
                            // Hubs like the Enphase Envoy expose each inverter as a child device
                            // (via_device); include those so the actual inverters are listed.
                            // Only the child devices (the actual inverters) - the hub itself
                            // (e.g. the Envoy) has aggregate sensors we don't want in this list.
                            val allDeviceIds = remember(deviceRegistry, deviceId) {
                                val children = deviceRegistry.filter { it.via_device_id == deviceId }.map { it.id }.toSet()
                                children.ifEmpty { setOf(deviceId) }
                            }
                            val deviceEntityIds = remember(entityRegistry, allDeviceIds) {
                                entityRegistry.filter { it.device_id in allDeviceIds }.map { it.entity_id }.toSet()
                            }
                            val inverterEntities = remember(entities, deviceEntityIds) {
                                entities.filter { e ->
                                    e.entity_id in deviceEntityIds && e.state.toFloatOrNull() != null &&
                                        (e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: "")
                                            .contains("W", ignoreCase = true)
                                }.sortedBy { it.friendlyName ?: it.entity_id }
                            }
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(deviceName, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                if (inverterEntities.isEmpty()) {
                                    Text(
                                        "No power sensors found on this device (or its sub-devices) yet.",
                                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                } else {
                                    inverterEntities.forEachIndexed { idx, e ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.SolarPower, null, tint = SolarAmber, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                e.friendlyName ?: e.entity_id,
                                                style = MaterialTheme.typography.bodySmall, color = appColors.onSurface,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                entityDisplay(e.entity_id) ?: e.state,
                                                style = MaterialTheme.typography.labelLarge, color = appColors.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (idx < inverterEntities.lastIndex)
                                            HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (page == "electricity") {
                // ═══ ELECTRICITY PAGE ═════════════════════════════════════════
                item {
                    SectionHeader("Now", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            val gridStatus = when {
                                gridW > 10f  -> "Importing"
                                gridW < -10f -> "Exporting"
                                else         -> "Grid idle"
                            }
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TotalStat(
                                    if (gridW < -10f) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    if (gridW < -10f) ExportGreen else ElecBlue,
                                    formatW(abs(gridW)), gridStatus
                                )
                                TotalStat(Icons.Default.Home, MaterialTheme.colorScheme.primary, formatW(homeW), "Home")
                                carbonFootprintDisplay?.let {
                                    TotalStat(Icons.Default.Cloud, ExportGreen, it, "Carbon footprint")
                                }
                            }
                            val phaseRows = (0..2).mapNotNull { i ->
                                val p = entityDisplay(phaseIds[i])
                                val a = entityDisplay(currentIds[i])
                                val v = entityDisplay(voltageIds[i])
                                if (p == null && a == null && v == null) null
                                else Pair(i, listOfNotNull(p, a, v))
                            }
                            if (phaseRows.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
                                Spacer(Modifier.height(4.dp))
                                phaseRows.forEach { (i, values) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(Modifier.size(8.dp).background(phaseColors[i], CircleShape))
                                        Text("Phase ${i + 1}", style = MaterialTheme.typography.labelMedium,
                                            color = appColors.onSurface, modifier = Modifier.weight(1f))
                                        Text(values.joinToString("  ·  "),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── consumption + per-phase charts ────────────────────────────
                item {
                    SectionHeader("Power", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            EnergyBarChart(homeSeries, ElecBlue, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                            if (phaseSeries.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Text("Power per phase", style = MaterialTheme.typography.labelLarge,
                                    color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                EnergyMultiLineChart(phaseSeries, "W", axisLabels, tooltipLabels)
                            }
                        }
                    }
                }

                // ── period totals + tariff meter readings ─────────────────────
                item {
                    SectionHeader("Energy", if (!energyConfig.energyCostEntityId.isNullOrBlank()) "€ ${"%.2f".format(costVal)}" else null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TotalStat(Icons.Default.ArrowDownward, ElecBlue, "%.1f kWh".format(usedPeriod), "Used $periodLabel")
                                TotalStat(Icons.Default.ArrowDownward, ImportRed, "%.1f kWh".format(importPeriod), "Imported")
                                TotalStat(Icons.Default.ArrowUpward, ExportGreen, "%.1f kWh".format(exportPeriod), "Exported")
                            }
                            if (hasUsageChart) {
                                Spacer(Modifier.height(14.dp))
                                EnergyStackedBarChart(usagePosLayers, usageNegLayers, "kWh", axisLabels, tooltipLabels, nowIndex = nowIndex)
                            }
                            val impT1 = entityDisplay(energyConfig.gridImportTariff1EntityId)
                            val impT2 = entityDisplay(energyConfig.gridImportTariff2EntityId)
                            val expT1 = entityDisplay(energyConfig.gridExportTariff1EntityId)
                            val expT2 = entityDisplay(energyConfig.gridExportTariff2EntityId)
                            if (listOfNotNull(impT1, impT2, expT1, expT2).isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
                                Spacer(Modifier.height(8.dp))
                                Text("Meter readings", style = MaterialTheme.typography.labelLarge,
                                    color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                if (impT1 != null || expT1 != null) TariffLine("Tariff 1", impT1, expT1)
                                if (impT2 != null || expT2 != null) TariffLine("Tariff 2", impT2, expT2)
                            }
                        }
                    }
                }
            } else if (page == "gas") {
                // ═══ GAS PAGE ═════════════════════════════════════════════════
                item {
                    SectionHeader("Usage", gasCost?.let { "€ ${"%.2f".format(it)}" })
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (gasCurrentDisplay != null)
                                    TotalStat(Icons.Default.Speed, GasPink, gasCurrentDisplay, "Now")
                                TotalStat(Icons.Default.LocalFireDepartment, GasPink,
                                    "%.1f %s".format(gasPeriod, gasUnit), "Used $periodLabel")
                                // The bound gas entity is the meter's lifetime counter, not a daily value.
                                gasVal?.let {
                                    TotalStat(Icons.Default.LocalFireDepartment, GasPink, "%.1f %s".format(it, gasUnit), "Total")
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            EnergyBarChart(gasSeries, GasPink, gasUnit, axisLabels, tooltipLabels, nowIndex = nowIndex)
                        }
                    }
                }
            } else if (page == "water") {
                // ═══ WATER PAGE ═══════════════════════════════════════════════
                item {
                    SectionHeader("Usage", waterCost?.let { "€ ${"%.2f".format(it)}" })
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            fun fmtWater(v: Float) = (if (v >= 100f) "%.0f %s" else "%.1f %s").format(v, waterDisplayUnit)
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (waterCurrentDisplay != null)
                                    TotalStat(Icons.Default.Speed, WaterBlue, waterCurrentDisplay, "Now")
                                TotalStat(Icons.Default.WaterDrop, WaterBlue, fmtWater(waterPeriod * waterFactor), "Used $periodLabel")
                                // The bound water entity is the meter's lifetime counter, not a daily value.
                                waterVal?.let { TotalStat(Icons.Default.WaterDrop, WaterBlue, fmtWater(it * waterFactor), "Total") }
                            }
                            Spacer(Modifier.height(14.dp))
                            EnergyBarChart(waterSeries, WaterBlue, waterDisplayUnit, axisLabels, tooltipLabels, nowIndex = nowIndex)
                        }
                    }
                }
                if (waterDeviceEntities.isNotEmpty()) {
                    item {
                        SectionHeader("Individual water usage", null)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = itemCornerShape(),
                            color = appColors.elevated
                        ) {
                            IndividualWaterUsageContent(
                                entities = waterDeviceEntities,
                                rawUsage = { id -> statChanges(id).sum() },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else if (page == "battery") {
                // ═══ BATTERY PAGE ═════════════════════════════════════════════
                item {
                    SectionHeader("Battery", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
                        shape = itemCornerShape(), color = Color.Transparent
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            val battStatus = when {
                                batteryW > 10f  -> "Charging"
                                batteryW < -10f -> "Discharging"
                                else            -> "Idle"
                            }
                            val levelColor = when {
                                (batteryPct ?: 0) > 50 -> ExportGreen
                                (batteryPct ?: 0) > 20 -> SolarAmber
                                else -> ImportRed
                            }
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TotalStat(
                                    if (batteryW > 10f) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                    levelColor, batteryPct?.let { "$it%" } ?: "—", "Charge"
                                )
                                TotalStat(Icons.Default.Bolt, BattPurple, formatW(abs(batteryW)), battStatus)
                            }
                            Spacer(Modifier.height(14.dp))
                            // Split the signed battery power into charge/discharge lines.
                            val charging = FloatArray(battSeries.size) { battSeries[it].coerceAtLeast(0f) }
                            val discharging = FloatArray(battSeries.size) { (-battSeries[it]).coerceAtLeast(0f) }
                            EnergyMultiLineChart(
                                listOf(
                                    Triple("Charging", charging, ExportGreen),
                                    Triple("Discharging", discharging, ImportRed)
                                ),
                                "W", axisLabels, tooltipLabels
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TariffLine(label: String, importText: String?, exportText: String?) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
        Text(
            listOfNotNull(importText?.let { "↓ $it" }, exportText?.let { "↑ $it" }).joinToString("   "),
            style = MaterialTheme.typography.labelSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Isometric energy house (Homey style): corner-view house with a panel-covered
// roof plane, garage with slatted door, glowing gable window, wall meter and
// cable runs. Light pulses travel the cables while power flows.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyHero(
    solarW: Float,
    gridW: Float,
    homeW: Float,
    batteryW: Float,
    batteryPct: Int?,
    hasBattery: Boolean,
    hasSolar: Boolean,
    hasGas: Boolean,
    hasWater: Boolean,
    gasFlowing: Boolean,
    waterFlowing: Boolean
) {
    val appColors = LocalHKIAppColors.current
    val primary = MaterialTheme.colorScheme.primary
    val solarActive = hasSolar && solarW > 10f
    val importing = gridW > 10f
    val exporting = gridW < -10f
    val batteryCharging = hasBattery && batteryW > 10f
    val batteryDischarging = hasBattery && batteryW < -10f
    val accent = when {
        exporting -> ExportGreen
        importing -> ElecBlue
        batteryDischarging || batteryCharging -> BattPurple
        solarActive -> SolarAmber
        homeW > 10f -> primary
        else -> appColors.onMuted
    }
    val status = when {
        exporting -> "Exporting power"
        importing -> "Importing power"
        batteryDischarging -> "Battery supporting"
        batteryCharging -> "Battery charging"
        solarActive -> "Solar producing"
        homeW > 10f -> "Home consuming"
        else -> "Energy idle"
    }
    val gridIcon = when {
        exporting -> Icons.Default.ArrowUpward
        importing -> Icons.Default.ArrowDownward
        else -> Icons.Default.ElectricBolt
    }
    val batteryColor = when {
        !hasBattery || batteryPct == null -> appColors.onMuted
        batteryPct > 50 -> ExportGreen
        batteryPct > 20 -> SolarAmber
        else -> ImportRed
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(252.dp)) {
            EnergyHouseScene(
                solarW = solarW,
                gridW = gridW,
                homeW = homeW,
                batteryW = batteryW,
                batteryPct = batteryPct,
                hasBattery = hasBattery,
                hasSolar = hasSolar,
                hasGas = hasGas,
                hasWater = hasWater,
                gasFlowing = gasFlowing,
                waterFlowing = waterFlowing,
                modifier = Modifier.align(Alignment.BottomCenter).aspectRatio(850f / 620f)
            )
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 8.dp)
            ) {
                Text(
                    "HOME POWER",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.onMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    formatW(homeW.coerceAtLeast(0f)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = appColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 8.dp),
                color = accent.copy(alpha = 0.16f),
                shape = itemCornerShape()
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).background(accent, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(status, color = appColors.onSurface, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EnergyHeroStat(
                Icons.Default.WbSunny,
                if (hasSolar) SolarAmber else appColors.onMuted,
                if (hasSolar) formatW(solarW.coerceAtLeast(0f)) else "—",
                "Solar"
            )
            EnergyHeroStat(
                gridIcon,
                when { exporting -> ExportGreen; importing -> ElecBlue; else -> appColors.onMuted },
                formatW(abs(gridW)),
                "Grid flow"
            )
            EnergyHeroStat(
                if (batteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                batteryColor,
                if (hasBattery) batteryPct?.let { "$it%" } ?: "—" else "—",
                "Battery"
            )
        }
    }
}

@Composable
private fun EnergyHeroStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    value: String,
    label: String
) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Text(value, color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Text(label, color = appColors.onMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun EnergyHouseScene(
    solarW: Float, gridW: Float, homeW: Float,
    batteryW: Float, batteryPct: Int?, hasBattery: Boolean,
    modifier: Modifier = Modifier,
    hasSolar: Boolean = true, hasGas: Boolean = false, hasWater: Boolean = false,
    gasFlowing: Boolean = false, waterFlowing: Boolean = false
) {
    val appColors = LocalHKIAppColors.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = appColors.background.luminance() < 0.5f

    val solarActive     = hasSolar && solarW > 10f
    val importActive    = gridW > 10f
    val exportActive    = gridW < -10f
    val battCharging    = batteryW > 10f
    val battDischarging = batteryW < -10f
    val homeActive      = homeW > 10f
    val anyFlow = solarActive || importActive || exportActive || battCharging || battDischarging

    val infinite = rememberInfiniteTransition(label = "energyScene")
    val flowPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "flowPhase"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )

    Canvas(modifier) {
        val w = size.width; val h = size.height

        // ── palette ─────────────────────────────────────────────────────────
        val leftWall   = if (isDark) Color(0xFF232B35) else Color(0xFFD7DFE7)
        val rightWall  = if (isDark) Color(0xFF2E3844) else Color(0xFFEDF2F6)
        val gableFace  = if (isDark) Color(0xFF2A333F) else Color(0xFFE4EAF0)
        val roofFace   = if (isDark) Color(0xFF1B2129) else Color(0xFFB9C4CE)
        val panelCell  = if (isDark) Color(0xFF2A3547) else Color(0xFF2C4A6E)
        val panelEdge  = if (isDark) Color(0xFF46586E) else Color(0xFF5B7FA6)
        val garFront   = if (isDark) Color(0xFF29323D) else Color(0xFFE0E7ED)
        val garLeft    = if (isDark) Color(0xFF1F272F) else Color(0xFFCBD5DE)
        val garRoof    = if (isDark) Color(0xFF242C36) else Color(0xFFAFBCC7)
        val doorFill   = if (isDark) Color(0xFF333D4A) else Color(0xFFC2CDD7)
        val doorSlat   = if (isDark) Color(0xFF1F262E) else Color(0xFF9FAEBB)
        val cableColor = if (isDark) Color(0xFF55606B) else Color(0xFF90A0AE)
        val meterFill  = if (isDark) Color(0xFF3A4450) else Color(0xFFCAD4DD)
        val edgeLight  = if (isDark) Color(0xFF4A5866) else Color.White.copy(alpha = 0.7f)

        // ── isometric skeleton ──────────────────────────────────────────────
        val fc  = Offset(w * 0.565f, h * 0.795f)              // front corner, ground
        val rAx = Offset(w * 0.315f, -h * 0.105f)             // along the gable face →
        val lAx = Offset(-w * 0.375f, -h * 0.070f)            // along the long face ←
        val wallH = h * 0.235f
        val fcT = fc + Offset(0f, -wallH)
        val rbB = fc + rAx;  val rbT = fcT + rAx
        val lbB = fc + lAx;  val lbT = fcT + lAx
        val apex  = Offset((fcT.x + rbT.x) / 2f, (fcT.y + rbT.y) / 2f - h * 0.125f)
        // Point on the solar roof plane: u along lAx (front→back), v eave→ridge
        fun roof(u: Float, v: Float) = fcT + lAx * u + (apex - fcT) * v
        // Point on a wall face: base + f along axis, then g up the wall
        fun onFace(base: Offset, axis: Offset, f: Float, up: Float) = base + axis * f + Offset(0f, -up)
        val waterWallPt = onFace(fc, lAx, 0.32f, h * 0.032f)
        val gasMeterBase = fc + rAx * 0.24f + Offset(0f, -h * 0.004f)

        // ── ambience ────────────────────────────────────────────────────────
        drawCircle(
            brush = Brush.radialGradient(
                listOf((if (isDark) primary else ElecBlue).copy(alpha = if (isDark) 0.07f else 0.10f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.55f), radius = w * 0.55f
            ),
            radius = w * 0.55f, center = Offset(w * 0.5f, h * 0.55f)
        )
        drawOval(
            color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.10f),
            topLeft = Offset(w * 0.06f, h * 0.775f),
            size = Size(w * 0.88f, h * 0.135f)
        )

        fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
        }

        // ── main house walls ────────────────────────────────────────────────
        drawPath(quad(fc, lbB, lbT, fcT), leftWall)                       // long face
        drawPath(quad(fc, rbB, rbT, fcT), rightWall)                      // gable face
        drawPath(Path().apply {                                            // gable triangle
            moveTo(fcT.x, fcT.y); lineTo(apex.x, apex.y); lineTo(rbT.x, rbT.y); close()
        }, gableFace)

        // ── garage (front-left, box with door face parallel to the gable) ──
        val gc  = Offset(w * 0.205f, h * 0.850f)
        val gR  = rAx * 0.66f
        val gL  = lAx * 0.30f
        val gV  = h * 0.135f
        val gcT = gc + Offset(0f, -gV)
        drawPath(quad(gc, gc + gL, gc + gL + Offset(0f, -gV), gcT), garLeft)
        drawPath(quad(gc, gc + gR, gc + gR + Offset(0f, -gV), gcT), garFront)
        drawPath(quad(gcT, gcT + gR, gcT + gR + gL, gcT + gL), garRoof)
        // Slatted garage door
        run {
            val db0 = onFace(gc, gR, 0.10f, h * 0.008f)
            val db1 = onFace(gc, gR, 0.90f, h * 0.008f)
            val doorH = h * 0.100f
            drawPath(quad(db0, db1, db1 + Offset(0f, -doorH), db0 + Offset(0f, -doorH)), doorFill)
            for (i in 1..4) {
                val yOff = doorH * i / 5f
                drawLine(doorSlat, db0 + Offset(0f, -yOff), db1 + Offset(0f, -yOff), 1.2.dp.toPx())
            }
        }
        // Two small windows on the garage's left face
        for (f in listOf(0.25f, 0.62f)) {
            val b0 = onFace(gc, gL, f, gV * 0.42f)
            val b1 = onFace(gc, gL, f + 0.22f, gV * 0.42f)
            val winH = gV * 0.22f
            drawPath(
                quad(b0, b1, b1 + Offset(0f, -winH), b0 + Offset(0f, -winH)),
                if (homeActive) WindowWarm.copy(alpha = 0.30f + 0.20f * glowPulse)
                else (if (isDark) Color(0xFF39434E) else Color(0xFFB6C4D0))
            )
        }

        // ── roof plane (+ panel array only when the home has solar) ─────────
        drawPath(quad(roof(-0.04f, -0.08f), roof(1.04f, -0.08f), roof(1.04f, 1.02f), roof(-0.04f, 1.02f)), roofFace)
        drawLine(edgeLight, roof(-0.04f, -0.08f), roof(-0.04f, 1.02f), 1.5.dp.toPx())   // front roof edge
        drawLine(edgeLight, roof(-0.04f, 1.02f), roof(1.04f, 1.02f), 1.2.dp.toPx())      // ridge
        if (hasSolar) {
            val cols = 6; val rows = 3
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val u0 = 0.06f + c * (0.90f / cols) + 0.010f
                    val u1 = 0.06f + (c + 1) * (0.90f / cols) - 0.010f
                    val v0 = 0.12f + r * (0.76f / rows) + 0.018f
                    val v1 = 0.12f + (r + 1) * (0.76f / rows) - 0.018f
                    val cell = quad(roof(u0, v0), roof(u1, v0), roof(u1, v1), roof(u0, v1))
                    drawPath(cell, panelCell)
                    drawPath(cell, panelEdge, style = Stroke(0.8.dp.toPx()))
                }
            }
            if (solarActive) {
                drawPath(
                    quad(roof(0.06f, 0.12f), roof(0.96f, 0.12f), roof(0.96f, 0.88f), roof(0.06f, 0.88f)),
                    SolarAmber.copy(alpha = 0.04f + 0.06f * glowPulse)
                )
            }
        }

        // ── front door on the long face, glowing with the house ────────────
        run {
            val b0 = onFace(fc, lAx, 0.10f, h * 0.004f)
            val b1 = onFace(fc, lAx, 0.26f, h * 0.004f)
            val doorH = h * 0.150f
            val doorFillC = if (isDark) Color(0xFF3A4450) else Color(0xFFADBBC8)
            drawPath(quad(b0, b1, b1 + Offset(0f, -doorH), b0 + Offset(0f, -doorH)), doorFillC)
            drawPath(
                quad(b0, b1, b1 + Offset(0f, -doorH), b0 + Offset(0f, -doorH)),
                Color.Black.copy(alpha = 0.22f), style = Stroke(1.2.dp.toPx())
            )
            val p0 = onFace(fc, lAx, 0.13f, h * 0.075f)
            val p1 = onFace(fc, lAx, 0.23f, h * 0.075f)
            val paneH = h * 0.045f
            drawPath(
                quad(p0, p1, p1 + Offset(0f, -paneH), p0 + Offset(0f, -paneH)),
                if (homeActive) WindowWarm.copy(alpha = 0.5f + 0.3f * glowPulse)
                else (if (isDark) Color(0xFF2A333D) else Color(0xFFC9D5DF))
            )
            drawCircle(
                if (isDark) Color(0xFF8A99A7) else Color(0xFF5F6E7C),
                1.6.dp.toPx(), onFace(fc, lAx, 0.245f, h * 0.070f)
            )
        }

        // ── water faucet on the long face (drips while water flows) ─────────
        if (hasWater) {
            val pipe = if (isDark) Color(0xFF7B8894) else Color(0xFF7E8C99)
            val stroke = 2.4.dp.toPx()
            val bodyEnd = waterWallPt + Offset(-7.dp.toPx(), -1.dp.toPx())
            val spoutEnd = bodyEnd + Offset(-4.dp.toPx(), 7.dp.toPx())
            drawCircle(pipe.copy(alpha = 0.35f), 4.2.dp.toPx(), waterWallPt)
            drawCircle(pipe, 2.5.dp.toPx(), waterWallPt)
            drawLine(pipe, waterWallPt, bodyEnd, stroke, cap = StrokeCap.Round)
            drawLine(pipe, bodyEnd, spoutEnd, stroke, cap = StrokeCap.Round)
            drawLine(pipe, waterWallPt + Offset(-3.dp.toPx(), -5.dp.toPx()), waterWallPt + Offset(3.dp.toPx(), -5.dp.toPx()), 1.7.dp.toPx(), cap = StrokeCap.Round)
            drawLine(pipe, waterWallPt + Offset(0f, -7.dp.toPx()), waterWallPt + Offset(0f, -3.dp.toPx()), 1.7.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(DodgerBlue.copy(alpha = 0.9f), 1.8.dp.toPx(), spoutEnd + Offset(0f, 1.dp.toPx()))
            if (waterFlowing) {
                for (i in 0..2) {
                    val t = (flowPhase * 1.6f + i / 3f) % 1f
                    drawCircle(
                        DodgerBlue.copy(alpha = (1f - t) * 0.9f), 1.8.dp.toPx(),
                        Offset(spoutEnd.x, spoutEnd.y + t * h * 0.055f)
                    )
                }
                drawOval(
                    DodgerBlue.copy(alpha = 0.22f + 0.14f * glowPulse),
                    topLeft = Offset(spoutEnd.x - 4.dp.toPx(), spoutEnd.y + h * 0.052f),
                    size = Size(8.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        // ── gas meter with pilot flame on the gable face ────────────────────
        if (hasGas) {
            val gbw = w * 0.030f; val gbh = h * 0.042f
            drawRoundRect(
                if (isDark) Color(0xFF39424E) else Color(0xFFB9C6D2),
                topLeft = Offset(gasMeterBase.x - gbw / 2f, gasMeterBase.y - gbh),
                size = Size(gbw, gbh), cornerRadius = CornerRadius(2.dp.toPx())
            )
            val flameC = Offset(gasMeterBase.x, gasMeterBase.y - gbh - 5.dp.toPx())
            if (gasFlowing) {
                val fh = 6.dp.toPx() * (0.8f + 0.4f * glowPulse)
                drawCircle(ImportRed.copy(alpha = 0.25f * glowPulse), fh * 1.4f, flameC)
                drawCircle(ImportRed.copy(alpha = 0.9f), fh * 0.60f, flameC)
                drawCircle(Color(0xFFFFCDD2), fh * 0.28f, flameC + Offset(0f, fh * 0.15f))
            } else {
                drawCircle(if (isDark) Color(0xFF55606B) else Color(0xFF90A0AE), 2.dp.toPx(), flameC)
            }
        }

        // ── glowing gable window (3×3) ──────────────────────────────────────
        run {
            val liftB = h * 0.055f; val winH = h * 0.150f
            val bl = onFace(fc, rAx, 0.34f, liftB)
            val br = onFace(fc, rAx, 0.72f, liftB)
            val tl = bl + Offset(0f, -winH); val tr = br + Offset(0f, -winH)
            val center = Offset((bl.x + tr.x) / 2f, (bl.y + tr.y) / 2f)
            if (homeActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(WindowWarm.copy(alpha = (if (isDark) 0.38f else 0.22f) * glowPulse), Color.Transparent),
                        center = center, radius = w * 0.16f
                    ),
                    radius = w * 0.16f, center = center
                )
            }
            val fill = if (homeActive) WindowWarm.copy(alpha = 0.75f + 0.25f * glowPulse)
                       else (if (isDark) Color(0xFF3A4552) else Color(0xFFC3D2DF))
            drawPath(quad(bl, br, tr, tl), fill)
            drawPath(quad(bl, br, tr, tl), Color.Black.copy(alpha = 0.25f), style = Stroke(1.5.dp.toPx()))
            // Mullions: 3×3 grid aligned to the face
            val mullion = if (isDark) Color(0xFF20272F) else Color(0xFF8A99A7)
            for (f in listOf(1f / 3f, 2f / 3f)) {
                val vb = bl + (br - bl) * f
                drawLine(mullion, vb, vb + Offset(0f, -winH), 1.5.dp.toPx())
                drawLine(mullion, bl + Offset(0f, -winH * f), br + Offset(0f, -winH * f), 1.5.dp.toPx())
            }
        }

        // ── wall meter + cables ─────────────────────────────────────────────
        val meterC = onFace(fc, rAx, 0.11f, h * 0.130f)
        val meterW = w * 0.038f; val meterH = h * 0.048f
        // Cable runs (polylines the pulses travel)
        val groundAtMeter = onFace(fc, rAx, 0.11f, -h * 0.004f)
        val gridCable = listOf(
            meterC + Offset(0f, meterH / 2f),
            groundAtMeter,
            fc + rAx * 1.45f + Offset(0f, -h * 0.004f)
        )
        val roofEdgeY = run {   // point on the gable roof edge (fcT→apex) above the meter
            val t = ((meterC.x - fcT.x) / (apex.x - fcT.x)).coerceIn(0f, 1f)
            Offset(meterC.x, fcT.y + (apex.y - fcT.y) * t)
        }
        val solarCable = listOf(roofEdgeY, meterC + Offset(0f, -meterH / 2f))
        val battStand = fc + rAx * 0.88f + Offset(0f, -h * 0.004f)
        val battCable = listOf(groundAtMeter, battStand)

        fun drawCablePath(pts: List<Offset>) {
            val p = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(p, cableColor, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCablePath(gridCable)
        if (hasSolar) {
            drawCablePath(solarCable)
            // Second thin line beside the solar drop, like the video's paired cables
            drawLine(cableColor.copy(alpha = 0.6f),
                solarCable[0] + Offset(3.dp.toPx(), 1.dp.toPx()),
                solarCable[1] + Offset(3.dp.toPx(), 0f), 1.dp.toPx())
        }
        if (hasBattery) drawCablePath(battCable)

        // Meter box
        drawRoundRect(
            meterFill,
            topLeft = Offset(meterC.x - meterW / 2f, meterC.y - meterH / 2f),
            size = Size(meterW, meterH), cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(meterC.x - meterW / 2f, meterC.y - meterH / 2f),
            size = Size(meterW, meterH), cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )
        drawLine(Color.Black.copy(alpha = 0.20f),
            Offset(meterC.x - meterW * 0.28f, meterC.y + meterH * 0.16f),
            Offset(meterC.x + meterW * 0.28f, meterC.y + meterH * 0.16f), 1.2.dp.toPx())
        drawCircle(
            if (anyFlow) ExportGreen.copy(alpha = 0.4f + 0.6f * glowPulse) else Color.White.copy(alpha = 0.20f),
            2.dp.toPx(), Offset(meterC.x, meterC.y - meterH * 0.22f)
        )

        // Battery cabinet standing against the gable wall
        if (hasBattery) {
            val bw = w * 0.052f; val bh = h * 0.100f
            val tl = Offset(battStand.x - bw / 2f, battStand.y - bh)
            val levelColor = when {
                batteryPct == null -> appColors.onMuted
                batteryPct > 50 -> ExportGreen
                batteryPct > 20 -> SolarAmber
                else -> ImportRed
            }
            drawRoundRect(if (isDark) Color(0xFF39424E) else Color(0xFF546575), tl, Size(bw, bh), CornerRadius(3.dp.toPx()))
            val pad = 2.5.dp.toPx()
            val frac = (batteryPct ?: 0).coerceIn(0, 100) / 100f
            if (frac > 0f) {
                val fillH = (bh - pad * 2f) * frac
                drawRoundRect(
                    levelColor.copy(alpha = 0.9f),
                    topLeft = Offset(tl.x + pad, tl.y + pad + (bh - pad * 2f) - fillH),
                    size = Size(bw - pad * 2f, fillH), cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
            drawCircle(
                if (battCharging || battDischarging) BattPurple.copy(alpha = 0.4f + 0.6f * glowPulse) else Color.White.copy(alpha = 0.25f),
                1.8.dp.toPx(), Offset(tl.x + bw - 4.dp.toPx(), tl.y + 4.dp.toPx())
            )
        }

        // ── light pulses along the cables ───────────────────────────────────
        fun pointAlong(pts: List<Offset>, t: Float): Offset {
            if (pts.size < 2) return pts.firstOrNull() ?: Offset.Zero
            var total = 0f
            for (i in 0 until pts.size - 1) total += (pts[i + 1] - pts[i]).getDistance()
            if (total <= 0f) return pts.first()
            var target = t.coerceIn(0f, 1f) * total
            for (i in 0 until pts.size - 1) {
                val d = (pts[i + 1] - pts[i]).getDistance()
                if (target <= d || i == pts.size - 2) {
                    val f = if (d > 0f) (target / d).coerceIn(0f, 1f) else 0f
                    return pts[i] + (pts[i + 1] - pts[i]) * f
                }
                target -= d
            }
            return pts.last()
        }
        fun drawPulses(pts: List<Offset>, color: Color, watts: Float, reverse: Boolean = false) {
            val n = 1 + min(2, (watts / 1200f).toInt())
            for (i in 0 until n) {
                val head = (flowPhase + i.toFloat() / n) % 1f
                for (k in 0..3) {
                    val tk = head - k * 0.035f
                    if (tk < 0f) continue
                    val t = if (reverse) 1f - tk else tk
                    val p = pointAlong(pts, t)
                    if (k == 0) drawCircle(color.copy(alpha = 0.25f), 5.dp.toPx(), p)
                    drawCircle(color.copy(alpha = 1f - k * 0.24f), 2.4.dp.toPx() * (1f - k * 0.15f), p)
                }
            }
        }
        if (solarActive)     drawPulses(solarCable, SolarAmber, solarW)                        // roof → meter
        if (importActive)    drawPulses(gridCable, ElecBlue, gridW, reverse = true)            // grid → meter
        if (exportActive)    drawPulses(gridCable, ExportGreen, abs(gridW))                    // meter → grid
        if (hasBattery && battCharging)    drawPulses(battCable, BattPurple, batteryW)         // meter → battery
        if (hasBattery && battDischarging) drawPulses(battCable, BattPurple, abs(batteryW), reverse = true)
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// History → bucketed chart data for the active time range
// ─────────────────────────────────────────────────────────────────────────────

private fun entityIsKw(entities: List<HAEntity>, id: String): Boolean =
    entities.find { it.entity_id == id }
        ?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
        ?.contains("kW", ignoreCase = true) == true

/** Average power per bucket from raw history, within the window (used for forecast sensors). */
private fun bucketPowerAverages(entries: List<HAHistoryEntry>?, isKw: Boolean, window: EnergyWindow): FloatArray? {
    if (entries.isNullOrEmpty()) return null
    val pts = entries.mapNotNull { e ->
        val t = parseHistoryMillis(e.last_changed) ?: return@mapNotNull null
        val v = e.state.toFloatOrNull() ?: return@mapNotNull null
        if (t in window.startMs until window.endMs) t to v else null
    }
    if (pts.isEmpty()) return null
    val sums = FloatArray(window.buckets); val counts = IntArray(window.buckets)
    pts.forEach { (t, v) ->
        val i = window.bucketOf(t)
        if (i in 0 until window.buckets) {
            sums[i] += if (isKw) v * 1000f else v
            counts[i]++
        }
    }
    return FloatArray(window.buckets) { i -> if (counts[i] > 0) sums[i] / counts[i] else 0f }
}

private fun chartValueLabel(v: Float, unit: String): String = when {
    unit == "W" -> formatW(v)
    v >= 100f -> "%.0f %s".format(v, unit)
    else -> "%.2f %s".format(v, unit)
}

// ─────────────────────────────────────────────────────────────────────────────
// Interactive charts: tap or scrub to inspect a bucket (like the dialog graphs)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyBarChart(
    values: FloatArray, color: Color, unit: String,
    axisLabels: List<String>, tooltipLabels: List<String>,
    modifier: Modifier = Modifier,
    nowIndex: Int? = null
) {
    val appColors = LocalHKIAppColors.current
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val n = values.size
    val maxV = values.max().coerceAtLeast(0.001f)
    val maxLabel = when {
        unit == "W" && maxV >= 1000f -> "%.1fk".format(maxV / 1000f)
        maxV >= 100f -> "%.0f".format(maxV)
        else -> "%.1f".format(maxV)
    }
    Column(modifier.fillMaxWidth()) {
        Text(
            selected?.let { "${tooltipLabels.getOrElse(it) { "" }} · ${chartValueLabel(values[it], unit)}" } ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected != null) appColors.onSurface else Color.Transparent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 36.dp, bottom = 2.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier.width(36.dp).height(96.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text("0", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(96.dp)
                    .pointerInput(values) {
                        detectTapGestures { off ->
                            val i = (off.x / size.width * n).toInt().coerceIn(0, n - 1)
                            selected = if (selected == i) null else i
                        }
                    }
                    .pointerInput(values) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                selected = (change.position.x / size.width * n).toInt().coerceIn(0, n - 1)
                            }
                        )
                    }
            ) {
                val cw = size.width; val ch = size.height
                val guide = appColors.onMuted.copy(alpha = 0.18f)
                for (i in 0..4) {
                    val x = cw * i / 4f
                    drawLine(
                        guide, Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )
                }
                drawLine(guide, Offset(0f, ch), Offset(cw, ch), 1.dp.toPx())
                val slot = cw / n
                val barW = (slot * 0.55f).coerceAtLeast(2.dp.toPx())
                for (i in 0 until n) {
                    val v = values[i]
                    if (v <= 0f) continue
                    val bh = (v / maxV) * (ch - 2.dp.toPx())
                    val x = slot * i + (slot - barW) / 2f
                    val alpha = when (selected) {
                        null -> if (i == nowIndex) 1f else 0.75f
                        i -> 1f
                        else -> 0.30f
                    }
                    drawRoundRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(x, ch - bh),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2.5f)
                    )
                }
                selected?.let { i ->
                    val x = slot * i + slot / 2f
                    drawLine(appColors.onSurface.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            axisLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
        }
    }
}

/** HA-style stacked energy chart: positive layers stack up from the zero line (import, consumed
 *  solar), negative layers stack down (export). Values are per-bucket energy, not power. */
@Composable
private fun EnergyStackedBarChart(
    positives: List<Triple<String, FloatArray, Color>>,
    negatives: List<Triple<String, FloatArray, Color>>,
    unit: String,
    axisLabels: List<String>, tooltipLabels: List<String>,
    modifier: Modifier = Modifier,
    nowIndex: Int? = null
) {
    val appColors = LocalHKIAppColors.current
    var selected by remember(positives, negatives) { mutableStateOf<Int?>(null) }
    val n = positives.firstOrNull()?.second?.size ?: negatives.firstOrNull()?.second?.size ?: 24
    val posTotals = FloatArray(n) { i -> positives.fold(0f) { acc, l -> acc + l.second[i] } }
    val negTotals = FloatArray(n) { i -> negatives.fold(0f) { acc, l -> acc + l.second[i] } }
    val maxP = (posTotals.maxOrNull() ?: 0f).coerceAtLeast(0.001f)
    val maxN = negTotals.maxOrNull() ?: 0f
    val span = (maxP + maxN).coerceAtLeast(0.001f)
    fun fmt(v: Float) = if (v >= 100f) "%.0f".format(v) else "%.1f".format(v)
    val chartHeight = 120.dp
    Column(modifier.fillMaxWidth()) {
        Text(
            selected?.let { i ->
                val parts = (positives + negatives).mapNotNull { (label, values, _) ->
                    values[i].takeIf { abs(it) > 0.005f }?.let { "$label ${chartValueLabel(it, unit)}" }
                }
                "${tooltipLabels.getOrElse(i) { "" }} · " +
                    (parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "No data")
            } ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected != null) appColors.onSurface else Color.Transparent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 36.dp, bottom = 2.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier.width(36.dp).height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fmt(maxP), style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(if (maxN > 0f) "-${fmt(maxN)}" else "0", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .pointerInput(positives, negatives) {
                        detectTapGestures { off ->
                            val i = (off.x / size.width * n).toInt().coerceIn(0, n - 1)
                            selected = if (selected == i) null else i
                        }
                    }
                    .pointerInput(positives, negatives) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                selected = (change.position.x / size.width * n).toInt().coerceIn(0, n - 1)
                            }
                        )
                    }
            ) {
                val cw = size.width; val ch = size.height
                val guide = appColors.onMuted.copy(alpha = 0.18f)
                for (i in 0..4) {
                    val x = cw * i / 4f
                    drawLine(
                        guide, Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )
                }
                val zeroY = ch * (maxP / span)
                drawLine(appColors.onMuted.copy(alpha = 0.35f), Offset(0f, zeroY), Offset(cw, zeroY), 1.dp.toPx())
                val slot = cw / n
                val barW = (slot * 0.55f).coerceAtLeast(2.dp.toPx())
                val corner = CornerRadius(barW / 4f)
                for (i in 0 until n) {
                    val x = slot * i + (slot - barW) / 2f
                    val alpha = when (selected) {
                        null -> if (i == nowIndex) 1f else 0.8f
                        i -> 1f
                        else -> 0.30f
                    }
                    val positiveSegments = positives.mapNotNull { (_, values, color) ->
                        ((values[i] / span) * ch).takeIf { it > 0f }?.let { it to color.copy(alpha = alpha) }
                    }
                    var yUp = zeroY
                    positiveSegments.forEachIndexed { index, (h, color) ->
                        val top = yUp - h
                        if (index == positiveSegments.lastIndex) {
                            // Round only the exposed top. Refill the lower corner area so the join
                            // to the segment below (or the zero line) is perfectly flush.
                            drawRoundRect(color, Offset(x, top), Size(barW, h), corner)
                            val seamHeight = minOf(h, corner.y)
                            drawRect(color, Offset(x, yUp - seamHeight), Size(barW, seamHeight))
                        } else {
                            drawRect(color, Offset(x, top), Size(barW, h))
                        }
                        yUp = top
                    }
                    val negativeSegments = negatives.mapNotNull { (_, values, color) ->
                        ((values[i] / span) * ch).takeIf { it > 0f }?.let { it to color.copy(alpha = alpha) }
                    }
                    var yDown = zeroY
                    negativeSegments.forEachIndexed { index, (h, color) ->
                        if (index == negativeSegments.lastIndex) {
                            // Mirror the positive bar: only the exposed bottom remains rounded.
                            drawRoundRect(color, Offset(x, yDown), Size(barW, h), corner)
                            val seamHeight = minOf(h, corner.y)
                            drawRect(color, Offset(x, yDown), Size(barW, seamHeight))
                        } else {
                            drawRect(color, Offset(x, yDown), Size(barW, h))
                        }
                        yDown += h
                    }
                }
                selected?.let { i ->
                    val x = slot * i + slot / 2f
                    drawLine(appColors.onSurface.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            axisLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 36.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            (positives + negatives).forEach { (label, _, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(7.dp).background(color, CircleShape))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EnergyMultiLineChart(
    series: List<Triple<String, FloatArray, Color>>,
    unit: String,
    axisLabels: List<String>, tooltipLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    var selected by remember(series) { mutableStateOf<Int?>(null) }
    val n = series.firstOrNull()?.second?.size ?: 24
    val rawMin = series.minOfOrNull { it.second.minOrNull() ?: 0f } ?: 0f
    val rawMax = series.maxOfOrNull { it.second.maxOrNull() ?: 0f } ?: 0f
    val minV = minOf(rawMin, 0f)
    val maxV = maxOf(rawMax, 0f)
    val valueRange = (maxV - minV).coerceAtLeast(0.001f)
    fun axisLabel(value: Float): String = when {
        unit == "W" && abs(value) >= 1000f -> "%.1fk".format(value / 1000f)
        abs(value) >= 100f -> "%.0f".format(value)
        else -> "%.1f".format(value)
    }
    val maxLabel = axisLabel(maxV)
    val minLabel = if (minV < 0f) axisLabel(minV) else "0"
    Column(modifier.fillMaxWidth()) {
        Text(
            selected?.let { i ->
                "${tooltipLabels.getOrElse(i) { "" }} · " +
                    series.joinToString(" · ") { (label, values, _) -> "$label ${chartValueLabel(values[i], unit)}" }
            } ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected != null) appColors.onSurface else Color.Transparent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 36.dp, bottom = 2.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier.width(36.dp).height(96.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(minLabel, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(96.dp)
                    .pointerInput(series) {
                        detectTapGestures { off ->
                            val i = (off.x / size.width * n).toInt().coerceIn(0, n - 1)
                            selected = if (selected == i) null else i
                        }
                    }
                    .pointerInput(series) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                selected = (change.position.x / size.width * n).toInt().coerceIn(0, n - 1)
                            }
                        )
                    }
            ) {
                val cw = size.width; val ch = size.height
                val guide = appColors.onMuted.copy(alpha = 0.18f)
                for (i in 0..4) {
                    val x = cw * i / 4f
                    drawLine(
                        guide, Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )
                }
                val zeroY = ((maxV - 0f) / valueRange) * ch
                drawLine(guide, Offset(0f, zeroY), Offset(cw, zeroY), 1.dp.toPx())
                series.forEach { (_, values, color) ->
                    val path = Path()
                    for (i in 0 until n) {
                        val x = cw * (i + 0.5f) / n
                        val y = ((maxV - values[i]) / valueRange) * (ch - 2.dp.toPx())
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                selected?.let { i ->
                    val x = cw * (i + 0.5f) / n
                    drawLine(appColors.onSurface.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
                    series.forEach { (_, values, color) ->
                        val y = ((maxV - values[i]) / valueRange) * (ch - 2.dp.toPx())
                        drawCircle(color, 3.5.dp.toPx(), Offset(x, y))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            axisLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 36.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            series.forEach { (label, _, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(7.dp).background(color, CircleShape))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section building blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    val appColors = LocalHKIAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(
        Modifier.size(34.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EnergyLiveTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, title: String, status: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        shape = itemCornerShape(),
        color = Color.Transparent,
        modifier = modifier
            .background(surfaceGradient(appColors.elevated), itemCornerShape())
            .then(
                if (onClick != null) Modifier.clip(itemCornerShape()).clickable { onClick() } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(icon, color)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(status, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TotalStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, value: String, label: String
) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            // softWrap = false keeps a value like "20,9 kWh" on one line instead of stacking it
            // one glyph per row when the column is squeezed on a narrow screen.
            Text(value, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ConsumerRow(rank: Int, name: String, watts: Float, shareOfHome: Int?, onSettings: (() -> Unit)? = null) {
    val appColors = LocalHKIAppColors.current
    val rankColors = listOf(ExportGreen, Color(0xFF26A69A), SolarAmber, Color(0xFF8D6E63), Color(0xFF78909C))
    val badge = rankColors.getOrElse(rank - 1) { Color(0xFF78909C) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(22.dp).background(badge, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(formatW(watts), shareOfHome?.let { "$it% of home" }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
        }
        if (onSettings != null) EditSettingsButton(onClick = onSettings)
        else Icon(Icons.Default.ElectricBolt, null, tint = appColors.onMuted.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun UtilityCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, value: String, unit: String, label: String,
    chart: @Composable () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(surfaceGradient(appColors.elevated), itemCornerShape()),
        shape = itemCornerShape(), color = Color.Transparent
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(icon, color)
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(value, style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                        Text(unit, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            chart()
        }
    }
}

private fun formatW(w: Float) = when { w < 1f -> "0 W"; w < 1000f -> "${w.toInt()} W"; else -> "${"%.1f".format(w / 1000f)} kW" }

private fun formatCarbonIntensity(value: Float, rawUnit: String): String {
    if (rawUnit.trim() == "%") {
        return "${if (abs(value) >= 10f) "%.0f".format(value) else "%.1f".format(value)}% fossil"
    }
    val unit = when {
        rawUnit.contains("kWh", ignoreCase = true) -> rawUnit
        rawUnit.contains("kg", ignoreCase = true) -> "kg CO2/kWh"
        rawUnit.contains("t", ignoreCase = true) -> "t CO2/kWh"
        else -> "g CO2/kWh"
    }
    val magnitude = abs(value)
    val num = when {
        magnitude >= 100f -> "%.0f".format(value)
        magnitude >= 10f -> "%.1f".format(value)
        else -> "%.2f".format(value)
    }
    return "$num $unit"
}

// ─────────────────────────────────────────────────────────────────────────────
// Device auto-mapping: pick a device, read its entities, fill the sensor roles
// ─────────────────────────────────────────────────────────────────────────────

private fun autoMapDeviceEntities(
    category: String,
    deviceId: String,
    entityRegistry: List<HAEntityRegistryEntry>,
    entities: List<HAEntity>,
    cfg: HKIEnergyConfig
): HKIEnergyConfig {
    // A selected source device is an exact boundary. Child devices can expose similarly named
    // sensors, but choosing one of those would make the selector appear arbitrary.
    val ids = entityRegistry
        .filter { it.device_id == deviceId && it.disabled_by == null }
        .map { it.entity_id }
        .toSet()
    val dev = entities.filter { it.entity_id in ids }

    fun unit(e: HAEntity) = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
    fun name(e: HAEntity) = (e.friendlyName ?: e.entity_id).lowercase()
    fun pick(pred: (HAEntity) -> Boolean): String? = dev.firstOrNull(pred)?.entity_id
    fun pickUnique(predicate: (HAEntity) -> Boolean): String? = dev.singleOrNull(predicate)?.entity_id
    fun keep(role: String, current: String?, guessed: String?): String? =
        if (role in cfg.customizedEntityRoles) current else guessed ?: current
    val isPower  = { e: HAEntity -> e.deviceClass == "power" || unit(e) == "W" || unit(e) == "kW" }
    val isEnergy = { e: HAEntity -> e.deviceClass == "energy" || unit(e).contains("Wh") }
    val isCurrent = { e: HAEntity -> e.deviceClass == "current" || unit(e) == "A" }
    val isVoltage = { e: HAEntity -> e.deviceClass == "voltage" || unit(e) == "V" }
    fun phaseMatch(e: HAEntity, n: Int) = name(e).contains("phase $n") || name(e).contains(" l$n")
    fun isCarbon(e: HAEntity): Boolean {
        val u = unit(e).lowercase()
        val n = name(e)
        return n.contains("carbon") || n.contains("co2") || n.contains("co₂") ||
            u.contains("co2") || u.contains("co₂")
    }
    fun pickCarbon(): String? =
        pick { isCarbon(it) && unit(it).contains("kWh", ignoreCase = true) }
            ?: pick { isCarbon(it) }
    fun gridPowerGuess(): String? = pick {
        isPower(it) && !name(it).contains("phase") &&
            listOf("grid", "current power", "active power", "power consumption", "power import", "net power")
                .any(name(it)::contains)
    } ?: pickUnique { isPower(it) && !name(it).contains("phase") }

    return when (category) {
        "electricity" -> cfg.copy(
            electricityDeviceId = deviceId,
            gridPowerEntityId = keep("grid_power", cfg.gridPowerEntityId, gridPowerGuess()),
            homePowerEntityId = keep("home_power", cfg.homePowerEntityId, pick {
                isPower(it) && listOf("home", "house", "load").any(name(it)::contains)
            }),
            powerPhase1EntityId = keep("phase1", cfg.powerPhase1EntityId, pick { isPower(it) && phaseMatch(it, 1) }),
            powerPhase2EntityId = keep("phase2", cfg.powerPhase2EntityId, pick { isPower(it) && phaseMatch(it, 2) }),
            powerPhase3EntityId = keep("phase3", cfg.powerPhase3EntityId, pick { isPower(it) && phaseMatch(it, 3) }),
            currentPhase1EntityId = keep("current1", cfg.currentPhase1EntityId, pick { isCurrent(it) && phaseMatch(it, 1) } ?: pick { isCurrent(it) }),
            currentPhase2EntityId = keep("current2", cfg.currentPhase2EntityId, pick { isCurrent(it) && phaseMatch(it, 2) }),
            currentPhase3EntityId = keep("current3", cfg.currentPhase3EntityId, pick { isCurrent(it) && phaseMatch(it, 3) }),
            voltagePhase1EntityId = keep("voltage1", cfg.voltagePhase1EntityId, pick { isVoltage(it) && phaseMatch(it, 1) } ?: pick { isVoltage(it) }),
            voltagePhase2EntityId = keep("voltage2", cfg.voltagePhase2EntityId, pick { isVoltage(it) && phaseMatch(it, 2) }),
            voltagePhase3EntityId = keep("voltage3", cfg.voltagePhase3EntityId, pick { isVoltage(it) && phaseMatch(it, 3) }),
            gridImportEntityId = keep("import_kwh", cfg.gridImportEntityId, pick { isEnergy(it) && name(it).contains("import") && !name(it).contains("tariff") }),
            gridImportTariff1EntityId = keep("import_t1", cfg.gridImportTariff1EntityId, pick { isEnergy(it) && name(it).contains("import") && name(it).contains("tariff 1") }),
            gridImportTariff2EntityId = keep("import_t2", cfg.gridImportTariff2EntityId, pick { isEnergy(it) && name(it).contains("import") && name(it).contains("tariff 2") }),
            gridExportEntityId = keep("export_kwh", cfg.gridExportEntityId, pick { isEnergy(it) && name(it).contains("export") && !name(it).contains("tariff") }),
            gridExportTariff1EntityId = keep("export_t1", cfg.gridExportTariff1EntityId, pick { isEnergy(it) && name(it).contains("export") && name(it).contains("tariff 1") }),
            gridExportTariff2EntityId = keep("export_t2", cfg.gridExportTariff2EntityId, pick { isEnergy(it) && name(it).contains("export") && name(it).contains("tariff 2") })
        )
        "carbon" -> cfg.copy(
            carbonDeviceId = deviceId,
            gridCarbonFootprintEntityId = keep("carbon", cfg.gridCarbonFootprintEntityId, pickCarbon())
        )
        "solar" -> cfg.copy(
            solarDeviceId = deviceId,
            solarPowerEntityId = keep("solar_power", cfg.solarPowerEntityId, pick { isPower(it) && name(it).contains("current") }
                ?: pick { isPower(it) && (name(it).contains("production") || name(it).contains("power")) }
            ),
            solarEnergyEntityId = keep("solar_kwh", cfg.solarEnergyEntityId, pick { isEnergy(it) && name(it).contains("today") }),
            solarLast7DaysEntityId = keep("solar_7d", cfg.solarLast7DaysEntityId, pick { isEnergy(it) && (name(it).contains("seven") || name(it).contains("7 days")) }),
            solarLifetimeEntityId = keep("solar_lifetime", cfg.solarLifetimeEntityId, pick { isEnergy(it) && name(it).contains("lifetime") })
        )
        "battery" -> cfg.copy(
            batteryDeviceId = deviceId,
            batteryEntityId = keep("battery_pct", cfg.batteryEntityId, pick { it.deviceClass == "battery" || (unit(it) == "%" && name(it).contains("batter")) }),
            batteryPowerEntityId = keep("battery_power", cfg.batteryPowerEntityId, pick { isPower(it) })
        )
        "gas" -> cfg.copy(
            gasDeviceId = deviceId,
            gasEntityId = keep("gas", cfg.gasEntityId, pick { it.deviceClass == "gas" }
                ?: pick { unit(it).contains("m³") && name(it).contains("gas") }
                ?: pick { unit(it).contains("m³") }
            ),
            gasCurrentEntityId = keep("gas_current", cfg.gasCurrentEntityId, pick { unit(it).contains("m³/h") })
        )
        "water" -> cfg.copy(
            waterDeviceId = deviceId,
            waterEntityId = keep("water", cfg.waterEntityId, pick { it.deviceClass == "water" }
                ?: pick { unit(it) == "L" || unit(it).contains("m³") }
            ),
            waterCurrentEntityId = keep("water_current", cfg.waterCurrentEntityId, pick { unit(it).contains("/min") || unit(it).contains("m³/h") })
        )
        else -> cfg
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Energy sensor settings: category menu; each category is device-first (pick a
// device to autofill its sensors) with the individual rows still adjustable.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergySensorSection(
    viewModel: MainViewModel,
    energyConfig: HKIEnergyConfig,
    allEntities: List<HAEntity>,
    onSave: (HKIEnergyConfig) -> Unit,
    setBack: ((() -> Unit)?) -> Unit = {}
) {
    val appColors = LocalHKIAppColors.current
    val sensors = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("sensor.") } }
    val entityRegistry by viewModel.entityRegistry.collectAsState()
    val deviceRegistry by viewModel.deviceRegistry.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchRegistries() }

    var cfg by remember(energyConfig) { mutableStateOf(energyConfig) }
    var category by remember { mutableStateOf<String?>(null) }
    var pickingField by remember { mutableStateOf<String?>(null) }
    var pickingDeviceFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(category) { setBack(if (category != null) { { category = null } } else null) }

    fun fieldValue(field: String?): String? = when (field) {
        "grid_power"     -> cfg.gridPowerEntityId
        "home_power"     -> cfg.homePowerEntityId
        "phase1"         -> cfg.powerPhase1EntityId
        "phase2"         -> cfg.powerPhase2EntityId
        "phase3"         -> cfg.powerPhase3EntityId
        "current1"       -> cfg.currentPhase1EntityId
        "current2"       -> cfg.currentPhase2EntityId
        "current3"       -> cfg.currentPhase3EntityId
        "voltage1"       -> cfg.voltagePhase1EntityId
        "voltage2"       -> cfg.voltagePhase2EntityId
        "voltage3"       -> cfg.voltagePhase3EntityId
        "import_kwh"     -> cfg.gridImportEntityId
        "import_t1"      -> cfg.gridImportTariff1EntityId
        "import_t2"      -> cfg.gridImportTariff2EntityId
        "export_kwh"     -> cfg.gridExportEntityId
        "export_t1"      -> cfg.gridExportTariff1EntityId
        "export_t2"      -> cfg.gridExportTariff2EntityId
        "cost"           -> cfg.energyCostEntityId
        "carbon"         -> cfg.gridCarbonFootprintEntityId
        "solar_power"    -> cfg.solarPowerEntityId
        "solar_kwh"      -> cfg.solarEnergyEntityId
        "solar_7d"       -> cfg.solarLast7DaysEntityId
        "solar_lifetime" -> cfg.solarLifetimeEntityId
        "battery_pct"    -> cfg.batteryEntityId
        "battery_power"  -> cfg.batteryPowerEntityId
        "gas"            -> cfg.gasEntityId
        "gas_current"    -> cfg.gasCurrentEntityId
        "gas_cost"       -> cfg.gasCostEntityId
        "water"          -> cfg.waterEntityId
        "water_current"  -> cfg.waterCurrentEntityId
        "water_cost"     -> cfg.waterCostEntityId
        else -> null
    }

    fun applyField(field: String?, id: String?) {
        cfg = when (field) {
            "grid_power"     -> cfg.copy(gridPowerEntityId = id)
            "home_power"     -> cfg.copy(homePowerEntityId = id)
            "phase1"         -> cfg.copy(powerPhase1EntityId = id)
            "phase2"         -> cfg.copy(powerPhase2EntityId = id)
            "phase3"         -> cfg.copy(powerPhase3EntityId = id)
            "current1"       -> cfg.copy(currentPhase1EntityId = id)
            "current2"       -> cfg.copy(currentPhase2EntityId = id)
            "current3"       -> cfg.copy(currentPhase3EntityId = id)
            "voltage1"       -> cfg.copy(voltagePhase1EntityId = id)
            "voltage2"       -> cfg.copy(voltagePhase2EntityId = id)
            "voltage3"       -> cfg.copy(voltagePhase3EntityId = id)
            "import_kwh"     -> cfg.copy(gridImportEntityId = id)
            "import_t1"      -> cfg.copy(gridImportTariff1EntityId = id)
            "import_t2"      -> cfg.copy(gridImportTariff2EntityId = id)
            "export_kwh"     -> cfg.copy(gridExportEntityId = id)
            "export_t1"      -> cfg.copy(gridExportTariff1EntityId = id)
            "export_t2"      -> cfg.copy(gridExportTariff2EntityId = id)
            "cost"           -> cfg.copy(energyCostEntityId = id)
            "carbon"         -> cfg.copy(gridCarbonFootprintEntityId = id)
            "solar_power"    -> cfg.copy(solarPowerEntityId = id)
            "solar_kwh"      -> cfg.copy(solarEnergyEntityId = id)
            "solar_7d"       -> cfg.copy(solarLast7DaysEntityId = id)
            "solar_lifetime" -> cfg.copy(solarLifetimeEntityId = id)
            "battery_pct"    -> cfg.copy(batteryEntityId = id)
            "battery_power"  -> cfg.copy(batteryPowerEntityId = id)
            "gas"            -> cfg.copy(gasEntityId = id)
            "gas_current"    -> cfg.copy(gasCurrentEntityId = id)
            "gas_cost"       -> cfg.copy(gasCostEntityId = id)
            "water"          -> cfg.copy(waterEntityId = id)
            "water_current"  -> cfg.copy(waterCurrentEntityId = id)
            "water_cost"     -> cfg.copy(waterCostEntityId = id)
            else -> cfg
        }
        field?.let { role ->
            cfg = cfg.copy(customizedEntityRoles = cfg.customizedEntityRoles + role)
        }
        onSave(cfg)
    }

    if (pickingField != null) {
        AdvancedEntitySearchDialog(
            allEntities = sensors, title = "Select Sensor", singleSelect = true,
            preselectedIds = setOfNotNull(fieldValue(pickingField)?.takeIf { it.isNotBlank() }),
            onDismiss = { pickingField = null },
            onEntitiesSelected = { ids ->
                applyField(pickingField, ids.firstOrNull())
                pickingField = null
            }
        )
    }

    if (pickingDeviceFor != null) {
        com.jimz011apps.hki7.ui.components.DevicePickerDialog(
            devices = deviceRegistry,
            currentId = when (pickingDeviceFor) {
                "electricity" -> cfg.electricityDeviceId
                "solar"       -> cfg.solarDeviceId
                "battery"     -> cfg.batteryDeviceId
                "carbon"      -> cfg.carbonDeviceId
                "gas"         -> cfg.gasDeviceId
                "water"       -> cfg.waterDeviceId
                else -> null
            },
            onDismiss = { pickingDeviceFor = null },
            onSelected = { id ->
                val cat = pickingDeviceFor
                if (cat != null) {
                    cfg = if (id == null) {
                        when (cat) {
                            "electricity" -> cfg.copy(electricityDeviceId = null)
                            "solar"       -> cfg.copy(solarDeviceId = null)
                            "battery"     -> cfg.copy(batteryDeviceId = null)
                            "carbon"      -> cfg.copy(carbonDeviceId = null, gridCarbonFootprintEntityId = null)
                            "gas"         -> cfg.copy(gasDeviceId = null)
                            "water"       -> cfg.copy(waterDeviceId = null)
                            else -> cfg
                        }
                    } else {
                        autoMapDeviceEntities(cat, id, entityRegistry, allEntities, cfg)
                    }
                    onSave(cfg)
                }
                pickingDeviceFor = null
            }
        )
    }

    @Composable
    fun sensorRow(field: String, label: String) {
        val entityId = fieldValue(field)
        val name = entityId?.takeIf { it.isNotBlank() }?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { pickingField = field }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                Text(name ?: "Not set", style = MaterialTheme.typography.bodySmall, color = if (name != null) MaterialTheme.colorScheme.primary else appColors.onMuted)
            }
            if (entityId?.isNotBlank() == true) {
                IconButton(
                    onClick = { applyField(field, null) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }

    @Composable
    fun deviceRow(cat: String, currentDeviceId: String?) {
        val deviceName = currentDeviceId?.let { id ->
            deviceRegistry.find { it.id == id }?.let { it.name_by_user ?: it.name } ?: id
        }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { pickingDeviceFor = cat },
            shape = itemCornerShape(),
            color = appColors.subtleSurface
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Source device", style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                    Text(
                        deviceName ?: "Pick a device to auto-fill the sensors below",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (deviceName != null) MaterialTheme.colorScheme.primary else appColors.onMuted
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
    }

    @Composable
    fun categoryButton(key: String, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { category = key },
            shape = itemCornerShape(),
            color = appColors.subtleSurface
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = appColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted)
            }
        }
    }

    if (category == null) {
        categoryButton("electricity", "Electricity", "Grid power, phases, import/export, tariffs, cost", Icons.Default.ElectricBolt, ElecBlue)
        categoryButton("solar", "Solar", "Production, forecast, and inverter device", Icons.Default.WbSunny, SolarAmber)
        categoryButton("battery", "Battery", "Charge level and battery power", Icons.Default.BatteryChargingFull, BattPurple)
        categoryButton("carbon", "Carbon", "Grid carbon footprint device", Icons.Default.Cloud, ExportGreen)
        categoryButton("gas", "Gas", "Usage and cost", Icons.Default.LocalFireDepartment, GasPink)
        categoryButton("water", "Water", "Usage and cost", Icons.Default.WaterDrop, WaterBlue)
        categoryButton("devices", "Devices", "Power, energy, and individual water devices", Icons.Default.Power, ExportGreen)
        return
    }

    when (category) {
        "electricity" -> {
            deviceRow("electricity", cfg.electricityDeviceId)
            sensorRow("grid_power", "Current power (W, + = import)")
            sensorRow("phase1", "Power phase 1 (W)")
            sensorRow("phase2", "Power phase 2 (W)")
            sensorRow("phase3", "Power phase 3 (W)")
            sensorRow("current1", "Current phase 1 (A)")
            sensorRow("current2", "Current phase 2 (A)")
            sensorRow("current3", "Current phase 3 (A)")
            sensorRow("voltage1", "Voltage phase 1 (V)")
            sensorRow("voltage2", "Voltage phase 2 (V)")
            sensorRow("voltage3", "Voltage phase 3 (V)")
            sensorRow("home_power", "Home consumption power (W)")
            sensorRow("import_kwh", "Energy import (kWh)")
            sensorRow("import_t1", "Energy import tariff 1 (kWh)")
            sensorRow("import_t2", "Energy import tariff 2 (kWh)")
            sensorRow("export_kwh", "Energy export (kWh)")
            sensorRow("export_t1", "Energy export tariff 1 (kWh)")
            sensorRow("export_t2", "Energy export tariff 2 (kWh)")
            sensorRow("cost", "Energy cost today")
        }
        "solar" -> {
            deviceRow("solar", cfg.solarDeviceId)
            sensorRow("solar_power", "Current power production (W)")
            sensorRow("solar_kwh", "Energy production today (kWh)")
            sensorRow("solar_7d", "Energy production last 7 days")
            sensorRow("solar_lifetime", "Lifetime energy production")

            var showForecastPicker by remember { mutableStateOf(false) }
            if (showForecastPicker) {
                AdvancedEntitySearchDialog(
                    allEntities = sensors, title = "Select Forecast Entities", singleSelect = false,
                    preselectedIds = cfg.solarForecastEntityIds.toSet(),
                    onDismiss = { showForecastPicker = false },
                    onEntitiesSelected = { ids ->
                        cfg = cfg.copy(solarForecastEntityIds = ids)
                        onSave(cfg)
                        showForecastPicker = false
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showForecastPicker = true }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Forecast entities", style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                    Text(
                        if (cfg.solarForecastEntityIds.isEmpty()) "Not set"
                        else cfg.solarForecastEntityIds.joinToString { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cfg.solarForecastEntityIds.isNotEmpty()) MaterialTheme.colorScheme.primary else appColors.onMuted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
        }
        "battery" -> {
            deviceRow("battery", cfg.batteryDeviceId)
            sensorRow("battery_pct", "Battery level (%)")
            sensorRow("battery_power", "Battery power (W, + = charging)")
        }
        "carbon" -> {
            deviceRow("carbon", cfg.carbonDeviceId)
            val carbonEntity = cfg.gridCarbonFootprintEntityId
                ?.let { id -> allEntities.find { it.entity_id == id } }
            val carbonValue = carbonEntity?.state?.toFloatOrNull()?.let { value ->
                val unit = carbonEntity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
                formatCarbonIntensity(value, unit)
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("Detected carbon intensity", style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                Text(
                    when {
                        carbonEntity != null && carbonValue != null ->
                            "${carbonEntity.friendlyName ?: carbonEntity.entity_id} - $carbonValue"
                        cfg.carbonDeviceId != null -> "No carbon intensity sensor found on this device"
                        else -> "Pick a carbon footprint device"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (carbonEntity != null) MaterialTheme.colorScheme.primary else appColors.onMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
        }
        "gas" -> {
            deviceRow("gas", cfg.gasDeviceId)
            sensorRow("gas", "Gas used today (m³)")
            sensorRow("gas_current", "Current gas flow (m³/h, optional)")
            sensorRow("gas_cost", "Gas cost today")
        }
        "water" -> {
            deviceRow("water", cfg.waterDeviceId)
            sensorRow("water", "Water used today (L/m³)")
            sensorRow("water_current", "Current water flow (L/min, optional)")
            sensorRow("water_cost", "Water cost today")
        }
        "devices" -> {
            var pickerType by remember { mutableStateOf<String?>(null) }
            val excludedPowerIds = setOfNotNull(
                cfg.solarPowerEntityId, cfg.gridPowerEntityId, cfg.homePowerEntityId, cfg.batteryPowerEntityId,
                cfg.powerPhase1EntityId, cfg.powerPhase2EntityId, cfg.powerPhase3EntityId
            )
            val excludedEnergyIds = setOfNotNull(
                cfg.gridImportEntityId, cfg.gridExportEntityId, cfg.solarEnergyEntityId,
                cfg.gridImportTariff1EntityId, cfg.gridImportTariff2EntityId,
                cfg.gridExportTariff1EntityId, cfg.gridExportTariff2EntityId
            )
            val autoPower = remember(sensors, cfg.hiddenPowerDeviceEntityIds, excludedPowerIds, cfg.usesHomeAssistantEnergyPreferences) {
                if (cfg.usesHomeAssistantEnergyPreferences || cfg.manualOnly) emptyList()
                else sensors.filter { it.deviceClass == "power" && it.entity_id !in excludedPowerIds && it.entity_id !in cfg.hiddenPowerDeviceEntityIds }
            }
            val autoEnergy = remember(sensors, cfg.hiddenEnergyDeviceEntityIds, excludedEnergyIds, cfg.usesHomeAssistantEnergyPreferences) {
                if (cfg.usesHomeAssistantEnergyPreferences || cfg.manualOnly) emptyList()
                else sensors.filter { it.deviceClass == "energy" && it.entity_id !in excludedEnergyIds && it.entity_id !in cfg.hiddenEnergyDeviceEntityIds }
            }
            val visiblePowerIds = (cfg.deviceEntityIds + autoPower.map { it.entity_id }).distinct()
            val visibleEnergyIds = (cfg.energyDeviceEntityIds + autoEnergy.map { it.entity_id }).distinct()
            val visibleWaterIds = cfg.waterDeviceEntityIds.distinct()
            if (pickerType != null) {
                val type = pickerType!!
                val excluded = when (type) {
                    "power" -> excludedPowerIds
                    "energy" -> excludedEnergyIds
                    else -> setOfNotNull(cfg.waterEntityId)
                }
                val candidates = sensors.filter { entity ->
                    val matches = when (type) {
                        "power" -> entity.deviceClass == "power"
                        "energy" -> entity.deviceClass == "energy"
                        else -> entity.entity_id in cfg.waterDeviceEntityIds || entity.deviceClass in setOf("water", "volume")
                    }
                    matches && entity.entity_id !in excluded
                }
                val selected = when (type) {
                    "power" -> visiblePowerIds
                    "energy" -> visibleEnergyIds
                    else -> visibleWaterIds
                }
                AdvancedEntitySearchDialog(
                    allEntities = candidates,
                    title = when (type) {
                        "power" -> "Top consumer devices"
                        "energy" -> "Device energy counters"
                        else -> "Individual water devices"
                    },
                    singleSelect = false,
                    preselectedIds = selected.toSet(),
                    onDismiss = { pickerType = null },
                    onEntitiesSelected = { ids ->
                        cfg = when (type) {
                            "power" -> cfg.copy(
                                deviceEntityIds = ids,
                                hiddenPowerDeviceEntityIds = candidates.map { it.entity_id }.filterNot { it in ids }
                            )
                            "energy" -> cfg.copy(
                                energyDeviceEntityIds = ids,
                                hiddenEnergyDeviceEntityIds = candidates.map { it.entity_id }.filterNot { it in ids }
                            )
                            else -> cfg.copy(
                                waterDeviceEntityIds = ids,
                                hiddenWaterDeviceEntityIds = candidates.map { it.entity_id }.filterNot { it in ids }
                            )
                        }
                        onSave(cfg)
                        pickerType = null
                    }
                )
            }
            Text("Top consumers", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                if (cfg.usesHomeAssistantEnergyPreferences)
                    "Live power sensors imported from Home Assistant's configured Energy devices."
                else "All sensors with device_class power are included automatically. Remove any device to create your own list.",
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
            visiblePowerIds.forEach { id ->
                val name = allEntities.find { it.entity_id == id }?.friendlyName ?: id
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        cfg = cfg.copy(
                            deviceEntityIds = cfg.deviceEntityIds - id,
                            hiddenPowerDeviceEntityIds = (cfg.hiddenPowerDeviceEntityIds + id).distinct()
                        )
                        onSave(cfg)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
            TextButton(onClick = { pickerType = "power" }) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Customize top consumers")
            }

            Spacer(Modifier.height(8.dp))
            Text("Device energy", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                if (cfg.usesHomeAssistantEnergyPreferences)
                    "Energy counters imported from Home Assistant's Energy dashboard."
                else "All sensors with device_class energy are included automatically. Their counter changes are used for the selected period.",
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
            visibleEnergyIds.forEach { id ->
                    val name = allEntities.find { it.entity_id == id }?.friendlyName ?: id
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        cfg = cfg.copy(
                            energyDeviceEntityIds = cfg.energyDeviceEntityIds - id,
                            hiddenEnergyDeviceEntityIds = (cfg.hiddenEnergyDeviceEntityIds + id).distinct()
                        )
                        onSave(cfg)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
            TextButton(onClick = { pickerType = "energy" }) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Customize device energy")
            }

            Spacer(Modifier.height(8.dp))
            Text("Individual water usage", style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                if (cfg.usesHomeAssistantEnergyPreferences)
                    "Water meters imported from Home Assistant's Energy dashboard."
                else "Add individual water meters to compare their usage.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.onMuted
            )
            visibleWaterIds.forEach { id ->
                val name = allEntities.find { it.entity_id == id }?.friendlyName ?: cfg.customNames[id] ?: id
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        cfg = cfg.copy(
                            waterDeviceEntityIds = cfg.waterDeviceEntityIds - id,
                            hiddenWaterDeviceEntityIds = (cfg.hiddenWaterDeviceEntityIds + id).distinct()
                        )
                        onSave(cfg)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
            }
            TextButton(onClick = { pickerType = "water" }) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Customize water devices")
            }
        }
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// Energy cards as widgets: any card of the Energy view can be embedded on other
// pages, standalone or grouped in an energy stack. Data always reflects "today".
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun IndividualWaterUsageContent(
    entities: List<HAEntity>,
    rawUsage: (String) -> Float,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val usage = entities.map { entity ->
        val rawUnit = entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
        val isM3 = rawUnit.contains("m³") || rawUnit.contains("m3", ignoreCase = true)
        Triple(entity, if (isM3) rawUsage(entity.entity_id) * 1000f else rawUsage(entity.entity_id), if (isM3) "L" else rawUnit.ifBlank { "L" })
    }.sortedByDescending { it.second }
    val maxUsage = (usage.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(0.001f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (usage.isEmpty()) {
            Text("No individual water devices configured.", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
        }
        usage.forEach { (entity, amount, unit) ->
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        entity.friendlyName ?: entity.entity_id,
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (amount >= 100f) "%.0f %s" else "%.1f %s").format(amount, unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(WaterBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))) {
                    Box(
                        Modifier.fillMaxWidth((amount / maxUsage).coerceIn(0.02f, 1f)).fillMaxHeight()
                            .background(WaterBlue, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

data class EnergyCardSpec(val key: String, val label: String, val category: String, val mdiIcon: String)

val energyCardCatalog = listOf(
    // "house" stays renderable for previously saved widgets but is no longer offered.
    EnergyCardSpec("tiles", "Live source tiles", "Overview", "view-grid"),
    EnergyCardSpec("usage", "Electricity totals & usage", "Electricity", "transmission-tower"),
    EnergyCardSpec("phases", "Power per phase", "Electricity", "sine-wave"),
    EnergyCardSpec("tariffs", "Tariff meter readings", "Electricity", "counter"),
    EnergyCardSpec("solar", "Solar production", "Solar", "solar-power"),
    EnergyCardSpec("battery", "Home battery", "Battery", "battery-charging"),
    EnergyCardSpec("gas", "Gas usage", "Gas", "fire"),
    EnergyCardSpec("water", "Water usage", "Water", "water"),
    EnergyCardSpec("water_devices", "Individual water usage", "Water", "water-pump"),
    EnergyCardSpec("top_consumers", "Top consumers", "Devices", "power-plug"),
    EnergyCardSpec("device_energy", "Device energy bars", "Devices", "chart-bar")
)

private fun Map<String, HAEntity>.unitOf(id: String?): String =
    id?.let { this[it]?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull } ?: ""

private fun Map<String, HAEntity>.numOf(id: String?): Float? =
    id?.takeIf { it.isNotBlank() }?.let { this[it]?.state?.toFloatOrNull() }

private fun Map<String, HAEntity>.wattsOf(id: String?): Float? {
    val v = numOf(id) ?: return null
    return if (unitOf(id).contains("kW", ignoreCase = true)) v * 1000f else v
}

private fun Map<String, HAEntity>.displayOf(id: String?): String? {
    val v = numOf(id) ?: return null
    val unit = unitOf(id)
    val num = if (v >= 100f) "%.0f".format(v) else "%.1f".format(v)
    return listOf(num, unit).filter { it.isNotBlank() }.joinToString(" ")
}

/** Renders one energy card for a "today" window, self-contained (fetches its own stats).
 *  [configOverride] replaces the Energy view's entity bindings for this card only. */
@Composable
fun EnergyCardWidgetView(
    cardKey: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    rangeName: String = EnergyRange.DAY.name,
    rangeOffset: Int = 0,
    onNavigate: ((String) -> Unit)? = null,
    configOverride: HKIEnergyConfig? = null
) {
    val appColors = LocalHKIAppColors.current
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val cfg = configOverride
        ?: (pageConfigsMap[ENERGY_PAGE_KEY] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()
    val energyEntityFlow = remember(viewModel) {
        viewModel.entitiesMatching("domain:sensor") { it.entity_id.startsWith("sensor.") }
    }
    val entities by energyEntityFlow.collectAsState()
    val energyStats by viewModel.energyStats.collectAsState()
    val homeAssistantSolarForecasts by viewModel.energySolarForecasts.collectAsState()
    val byId = remember(entities) { entities.associateBy { it.entity_id } }
    val range = remember(rangeName) { runCatching { EnergyRange.valueOf(rangeName) }.getOrDefault(EnergyRange.DAY) }
    val window = remember(range, rangeOffset) { energyWindow(range, rangeOffset) }
    val periodLabel = window.periodLabel()

    val solarW = byId.wattsOf(cfg.solarPowerEntityId) ?: 0f
    val gridW = byId.wattsOf(cfg.gridPowerEntityId) ?: 0f
    val homeW = byId.wattsOf(cfg.homePowerEntityId) ?: (solarW + gridW).coerceAtLeast(0f)
    val batteryW = byId.wattsOf(cfg.batteryPowerEntityId) ?: 0f
    val batteryPct = cfg.batteryEntityId?.let { byId[it] }?.state?.toIntOrNull()
    val hasBattery = !cfg.batteryEntityId.isNullOrBlank()
    val hasSolar = !cfg.solarPowerEntityId.isNullOrBlank() ||
        !cfg.solarEnergyEntityId.isNullOrBlank() || !cfg.solarDeviceId.isNullOrBlank()

    val gasId = cfg.gasEntityId?.takeIf { it.isNotBlank() }
    val waterId = cfg.waterEntityId?.takeIf { it.isNotBlank() }
    val importId = cfg.gridImportEntityId?.takeIf { it.isNotBlank() }
    val exportId = cfg.gridExportEntityId?.takeIf { it.isNotBlank() }
    val solarEnergyId = cfg.solarEnergyEntityId?.takeIf { it.isNotBlank() }
    val solarPowerId = cfg.solarPowerEntityId?.takeIf { it.isNotBlank() }
    val chartPowerId = cfg.homePowerEntityId?.takeIf { it.isNotBlank() }
        ?: cfg.gridPowerEntityId?.takeIf { it.isNotBlank() }
    val phaseIds = listOf(cfg.powerPhase1EntityId, cfg.powerPhase2EntityId, cfg.powerPhase3EntityId)
        .map { it?.takeIf { s -> s.isNotBlank() } }
    val batteryPowerId = cfg.batteryPowerEntityId?.takeIf { it.isNotBlank() }
    val phaseColors = listOf(Color(0xFF42A5F5), Color(0xFFFFB300), Color(0xFFEF5350))

    val mainPowerIds = remember(cfg) {
        setOfNotNull(
            cfg.solarPowerEntityId, cfg.gridPowerEntityId, cfg.homePowerEntityId, cfg.batteryPowerEntityId,
            cfg.powerPhase1EntityId, cfg.powerPhase2EntityId, cfg.powerPhase3EntityId
        )
    }
    val topConsumers = remember(entities, mainPowerIds, cfg.deviceEntityIds, cfg.hiddenPowerDeviceEntityIds, cfg.usesHomeAssistantEnergyPreferences) {
        fun wattsOfE(e: HAEntity): Float? {
            val v = e.state.toFloatOrNull() ?: return null
            val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
            return if (unit.contains("kW", ignoreCase = true)) v * 1000f else v
        }
        val manual = cfg.deviceEntityIds.mapNotNull { id -> byId[id]?.let { e -> e to (wattsOfE(e) ?: 0f) } }
        val manualIds = manual.map { it.first.entity_id }.toSet()
        val auto = (if (cfg.usesHomeAssistantEnergyPreferences || cfg.manualOnly) emptySequence() else entities.asSequence())
            .filter {
                it.entity_id.startsWith("sensor.") && it.entity_id !in mainPowerIds &&
                    it.entity_id !in manualIds && it.entity_id !in cfg.hiddenPowerDeviceEntityIds &&
                    it.deviceClass == "power"
            }
            .map { e -> e to (wattsOfE(e) ?: 0f) }
            .toList()
        (manual + auto).distinctBy { it.first.entity_id }.sortedByDescending { it.second }
    }
    val primaryEnergyIds = remember(cfg) {
        setOfNotNull(
            cfg.gridImportEntityId, cfg.gridExportEntityId, cfg.solarEnergyEntityId,
            cfg.gridImportTariff1EntityId, cfg.gridImportTariff2EntityId,
            cfg.gridExportTariff1EntityId, cfg.gridExportTariff2EntityId
        )
    }
    val deviceEnergyEntities = remember(entities, primaryEnergyIds, cfg.energyDeviceEntityIds, cfg.hiddenEnergyDeviceEntityIds, cfg.usesHomeAssistantEnergyPreferences) {
        val manual = cfg.energyDeviceEntityIds.mapNotNull(byId::get)
        val manualIds = manual.map { it.entity_id }.toSet()
        val auto = if (cfg.usesHomeAssistantEnergyPreferences || cfg.manualOnly) emptyList() else entities.filter {
            it.entity_id.startsWith("sensor.") && it.deviceClass == "energy" &&
                it.entity_id !in primaryEnergyIds && it.entity_id !in manualIds &&
                it.entity_id !in cfg.hiddenEnergyDeviceEntityIds
        }
        (manual + auto).distinctBy { it.entity_id }
    }
    val waterDeviceEntities = remember(entities, cfg.waterDeviceEntityIds, cfg.customNames) {
        cfg.waterDeviceEntityIds.mapNotNull(byId::get)
            .map { it.withDisplayName(cfg.customNames[it.entity_id]) }
            .distinctBy { it.entity_id }
    }
    LaunchedEffect(cfg.solarForecastConfigEntryIds) {
        if (cfg.solarForecastConfigEntryIds.isNotEmpty()) viewModel.fetchHomeAssistantEnergySolarForecasts()
    }

    val statIds = when (cardKey) {
        "usage" -> listOfNotNull(chartPowerId, importId, exportId, solarEnergyId)
        "phases" -> phaseIds.filterNotNull()
        "solar" -> listOfNotNull(solarPowerId, solarEnergyId, exportId)
        "battery" -> listOfNotNull(batteryPowerId)
        "gas" -> listOfNotNull(gasId)
        "water" -> listOfNotNull(waterId)
        "water_devices" -> waterDeviceEntities.map { it.entity_id }
        "house" -> listOfNotNull(gasId)
        "device_energy" -> deviceEnergyEntities.map { it.entity_id }
        else -> emptyList()
    }
    LaunchedEffect(statIds.toSet()) {
        if (statIds.isNotEmpty())
            viewModel.fetchEnergyStatistics(statIds, window.startMs, window.statPeriod(), window.key(), window.endMs)
    }

    fun points(id: String?) = id?.let { energyStats["$it|${window.key()}"] }
    fun means(id: String?): FloatArray {
        val scale = if (id != null && entityIsKw(entities, id)) 1000f else 1f
        val out = FloatArray(window.buckets)
        points(id)?.forEach { p ->
            val i = window.bucketOf(p.startMs)
            if (i in 0 until window.buckets) out[i] = (p.mean ?: 0f) * scale
        }
        return out
    }
    fun changes(id: String?): FloatArray {
        val out = FloatArray(window.buckets)
        points(id)?.forEach { p ->
            val i = window.bucketOf(p.startMs)
            if (i in 0 until window.buckets) out[i] += (p.change ?: 0f).coerceAtLeast(0f)
        }
        return out
    }
    fun total(id: String?): Float =
        points(id)?.sumOf { (it.change ?: 0f).coerceAtLeast(0f).toDouble() }?.toFloat() ?: 0f

    val importedForecastSeries = remember(homeAssistantSolarForecasts, cfg.solarForecastConfigEntryIds, window, range, rangeOffset) {
        if (range != EnergyRange.DAY || rangeOffset != 0) emptyList()
        else cfg.solarForecastConfigEntryIds.mapNotNull { providerId ->
            val hours = homeAssistantSolarForecasts[providerId].orEmpty()
            if (hours.isEmpty()) return@mapNotNull null
            val values = FloatArray(window.buckets)
            hours.forEach { (timestamp, wh) ->
                val index = window.bucketOf(timestamp)
                if (index in values.indices) values[index] += wh
            }
            Triple(
                if (cfg.solarForecastConfigEntryIds.size == 1) "Forecast" else "Forecast ${cfg.solarForecastConfigEntryIds.indexOf(providerId) + 1}",
                values,
                Color(0xFF29B6F6)
            )
        }
    }

    val tooltipLabels = remember(window) { window.tooltipLabels() }
    val axisLabels = remember(window, tooltipLabels) { window.axisLabels(tooltipLabels) }
    val nowIndex = window.nowIndex()
    fun recentUsage(id: String?, hours: Int = 1): Boolean {
        val now = nowIndex ?: return false
        val from = (now - hours + 1).coerceAtLeast(0)
        return points(id)?.any { p ->
            val i = window.bucketOf(p.startMs)
            i in from..now && (p.change ?: 0f) > 0.0001f
        } == true
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        color = appColors.elevated
    ) {
        when (cardKey) {
            "house" -> EnergyHouseScene(
                solarW = solarW, gridW = gridW, homeW = homeW,
                batteryW = batteryW, batteryPct = batteryPct, hasBattery = hasBattery,
                hasSolar = hasSolar, hasGas = gasId != null, hasWater = waterId != null,
                gasFlowing = recentUsage(gasId) || (byId.numOf(cfg.gasCurrentEntityId) ?: 0f) > 0.001f,
                waterFlowing = (byId.numOf(cfg.waterCurrentEntityId) ?: 0f) > 0.001f,
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(vertical = 8.dp)
            )
            "tiles" -> Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val gridStatus = when {
                    gridW > 10f -> "Importing"; gridW < -10f -> "Exporting"; else -> "Idle"
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    EnergyLiveTile(Icons.Default.ElectricBolt, ElecBlue, "Electricity",
                        "${formatW(abs(gridW))} · $gridStatus", Modifier.weight(1f),
                        onClick = onNavigate?.let { navigate -> { navigate("electricity") } })
                    EnergyLiveTile(Icons.Default.Home, MaterialTheme.colorScheme.primary, "Home",
                        "${formatW(homeW)} · ${if (homeW > 10f) "Consuming" else "Idle"}", Modifier.weight(1f))
                }
                if (hasSolar || hasBattery) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (hasSolar) EnergyLiveTile(Icons.Default.WbSunny, SolarAmber, "Solar",
                            "${formatW(solarW.coerceAtLeast(0f))} · ${if (solarW > 10f) "Producing" else "Idle"}", Modifier.weight(1f),
                            onClick = onNavigate?.let { navigate -> { navigate("solar") } })
                        if (hasBattery) EnergyLiveTile(
                            if (batteryW > 10f) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            BattPurple, "Battery",
                            listOfNotNull(batteryPct?.let { "$it%" }, formatW(abs(batteryW))).joinToString(" · "),
                            Modifier.weight(1f),
                            onClick = onNavigate?.let { navigate -> { navigate("battery") } })
                        if (!hasSolar || !hasBattery) Spacer(Modifier.weight(1f))
                    }
                }
            }
            "usage" -> Column(Modifier.padding(16.dp)) {
                val importPeriod = total(importId)
                val exportPeriod = total(exportId)
                val producedPeriod = total(solarEnergyId)
                val usedPeriod = (importPeriod + producedPeriod - exportPeriod).coerceAtLeast(0f)
                val selfUsed = (producedPeriod - exportPeriod).coerceIn(0f, producedPeriod)
                val selfSufficiency = if (solarEnergyId != null && usedPeriod > 0.01f) (selfUsed / usedPeriod * 100f).toInt().coerceIn(0, 100) else null
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TotalStat(Icons.Default.ArrowDownward, ElecBlue, "%.1f kWh".format(usedPeriod), "Used $periodLabel")
                    TotalStat(Icons.Default.ArrowDownward, ImportRed, "%.1f kWh".format(importPeriod), "Imported")
                    TotalStat(Icons.Default.ArrowUpward, ExportGreen, "%.1f kWh".format(exportPeriod), "Exported")
                    selfSufficiency?.let { TotalStat(Icons.Default.Home, SolarAmber, "$it%", "Self-sufficient") }
                }
                Spacer(Modifier.height(14.dp))
                val exportSeries = changes(exportId)
                val solarSeries = changes(solarEnergyId)
                val pos = buildList {
                    if (solarEnergyId != null) add(Triple(
                        "Consumed solar",
                        FloatArray(window.buckets) { (solarSeries[it] - exportSeries[it]).coerceAtLeast(0f) },
                        SolarAmber
                    ))
                    if (importId != null) add(Triple("Imported", changes(importId), ElecBlue))
                }
                val neg = if (exportId != null) listOf(Triple("Exported", exportSeries, BattPurple)) else emptyList()
                if (pos.isNotEmpty() || neg.isNotEmpty()) {
                    EnergyStackedBarChart(pos, neg, "kWh", axisLabels, tooltipLabels, nowIndex = nowIndex)
                } else {
                    EnergyBarChart(means(chartPowerId), ElecBlue, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                }
            }
            "phases" -> Column(Modifier.padding(16.dp)) {
                Text("Power per phase", style = MaterialTheme.typography.labelLarge,
                    color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val series = phaseIds.mapIndexedNotNull { i, id ->
                    id ?: return@mapIndexedNotNull null
                    Triple("Phase ${i + 1}", means(id), phaseColors[i])
                }
                if (series.isEmpty()) {
                    Text("No phase sensors configured in Energy Settings.",
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                } else {
                    EnergyMultiLineChart(series, "W", axisLabels, tooltipLabels)
                }
            }
            "tariffs" -> Column(Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Meter readings", style = MaterialTheme.typography.labelLarge,
                        color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                    byId.numOf(cfg.energyCostEntityId)?.let {
                        Text("€ ${"%.2f".format(it)}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val impT1 = byId.displayOf(cfg.gridImportTariff1EntityId)
                val impT2 = byId.displayOf(cfg.gridImportTariff2EntityId)
                val expT1 = byId.displayOf(cfg.gridExportTariff1EntityId)
                val expT2 = byId.displayOf(cfg.gridExportTariff2EntityId)
                if (listOfNotNull(impT1, impT2, expT1, expT2).isEmpty()) {
                    Text("No tariff sensors configured in Energy Settings.",
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                } else {
                    if (impT1 != null || expT1 != null) TariffLine("Tariff 1", impT1, expT1)
                    if (impT2 != null || expT2 != null) TariffLine("Tariff 2", impT2, expT2)
                }
            }
            "solar" -> Column(Modifier.padding(16.dp)) {
                val produced = total(solarEnergyId)
                val selfUsed = (produced - total(exportId)).coerceIn(0f, produced)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TotalStat(Icons.Default.Bolt, SolarAmber, formatW(solarW.coerceAtLeast(0f)), "Now")
                    TotalStat(Icons.Default.WbSunny, SolarAmber, "%.1f kWh".format(produced), "Produced $periodLabel")
                    TotalStat(Icons.Default.Home, ExportGreen, "%.1f kWh".format(selfUsed), "Self-used")
                }
                Spacer(Modifier.height(14.dp))
                val production = means(solarPowerId)
                if (importedForecastSeries.isNotEmpty()) {
                    EnergyMultiLineChart(
                        listOf(Triple("Production", production, SolarAmber)) + importedForecastSeries,
                        "W", axisLabels, tooltipLabels
                    )
                } else {
                    EnergyBarChart(production, SolarAmber, "W", axisLabels, tooltipLabels, nowIndex = nowIndex)
                }
            }
            "battery" -> Column(Modifier.padding(16.dp)) {
                val battStatus = when {
                    batteryW > 10f -> "Charging"; batteryW < -10f -> "Discharging"; else -> "Idle"
                }
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TotalStat(
                        if (batteryW > 10f) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        BattPurple, batteryPct?.let { "$it%" } ?: "—", "Charge"
                    )
                    TotalStat(Icons.Default.Bolt, BattPurple, formatW(abs(batteryW)), battStatus)
                }
                Spacer(Modifier.height(14.dp))
                val batt = means(batteryPowerId)
                EnergyMultiLineChart(
                    listOf(
                        Triple("Charging", FloatArray(batt.size) { batt[it].coerceAtLeast(0f) }, ExportGreen),
                        Triple("Discharging", FloatArray(batt.size) { (-batt[it]).coerceAtLeast(0f) }, ImportRed)
                    ),
                    "W", axisLabels, tooltipLabels
                )
            }
            "gas" -> Column(Modifier.padding(16.dp)) {
                val gasUnit = byId.unitOf(gasId).ifBlank { "m³" }
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    byId.displayOf(cfg.gasCurrentEntityId)?.let { TotalStat(Icons.Default.Speed, GasPink, it, "Now") }
                    TotalStat(Icons.Default.LocalFireDepartment, GasPink,
                        "%.1f %s".format(total(gasId), gasUnit), "Used $periodLabel")
                    byId.numOf(cfg.gasCostEntityId)?.let {
                        TotalStat(Icons.Default.LocalFireDepartment, GasPink, "€ ${"%.2f".format(it)}", "Cost")
                    }
                }
                Spacer(Modifier.height(14.dp))
                EnergyBarChart(changes(gasId), GasPink, gasUnit, axisLabels, tooltipLabels, nowIndex = nowIndex)
            }
            "water" -> Column(Modifier.padding(16.dp)) {
                val rawUnit = byId.unitOf(waterId).ifBlank { "L" }
                val isM3 = rawUnit.contains("m³") || rawUnit.contains("m3", ignoreCase = true)
                val factor = if (isM3) 1000f else 1f
                val unit = if (isM3) "L" else rawUnit
                val used = total(waterId) * factor
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    byId.displayOf(cfg.waterCurrentEntityId)?.let { TotalStat(Icons.Default.Speed, WaterBlue, it, "Now") }
                    TotalStat(Icons.Default.WaterDrop, WaterBlue,
                        (if (used >= 100f) "%.0f %s" else "%.1f %s").format(used, unit), "Used $periodLabel")
                    byId.numOf(cfg.waterCostEntityId)?.let {
                        TotalStat(Icons.Default.WaterDrop, WaterBlue, "€ ${"%.2f".format(it)}", "Cost")
                    }
                }
                Spacer(Modifier.height(14.dp))
                val raw = changes(waterId)
                EnergyBarChart(
                    if (isM3) FloatArray(raw.size) { raw[it] * 1000f } else raw,
                    WaterBlue, unit, axisLabels, tooltipLabels, nowIndex = nowIndex
                )
            }
            "water_devices" -> IndividualWaterUsageContent(
                entities = waterDeviceEntities,
                rawUsage = { id -> changes(id).sum() },
                modifier = Modifier.padding(16.dp)
            )
            "top_consumers" -> Column(Modifier.padding(vertical = 6.dp)) {
                if (topConsumers.isEmpty()) {
                    Text("No power sensors found.", style = MaterialTheme.typography.bodySmall,
                        color = appColors.onMuted, modifier = Modifier.padding(16.dp))
                }
                topConsumers.forEachIndexed { idx, (entity, watts) ->
                    ConsumerRow(
                        rank = idx + 1,
                        name = entity.friendlyName ?: entity.entity_id,
                        watts = watts,
                        shareOfHome = if (homeW > 1f) (watts / homeW * 100).toInt().coerceAtMost(100) else null
                    )
                    if (idx < topConsumers.lastIndex)
                        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            "device_energy" -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val deviceEnergies = deviceEnergyEntities
                    .map { e ->
                        val raw = changes(e.entity_id).sum()
                        val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull.orEmpty()
                        e to if (unit.equals("Wh", ignoreCase = true)) raw / 1000f else raw
                    }
                    .sortedByDescending { it.second }
                val maxKwh = (deviceEnergies.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(0.001f)
                if (deviceEnergies.isEmpty()) {
                    Text("No energy sensors found.", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                }
                deviceEnergies.forEach { (entity, kwh) ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entity.friendlyName ?: entity.entity_id,
                                style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(8.dp))
                            Text(if (kwh >= 10f) "%.1f kWh".format(kwh) else "%.2f kWh".format(kwh),
                                style = MaterialTheme.typography.labelMedium,
                                color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).background(ElecBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))) {
                            Box(Modifier.fillMaxWidth((kwh / maxKwh).coerceIn(0.02f, 1f)).fillMaxHeight()
                                .background(ElecBlue, RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
            else -> Text("Unknown energy card: $cardKey",
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                modifier = Modifier.padding(16.dp))
        }
    }
}

/** Standalone energy card widget with the standard edit-mode overlay. */
@Composable
fun EnergyCardWidgetItem(
    widget: HKIEnergyCardWidget,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (widget.isHidden && !isEditMode) return
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!widget.title.isNullOrBlank() || !widget.icon.isNullOrBlank()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!widget.icon.isNullOrBlank()) {
                        com.jimz011apps.hki7.ui.utils.MdiIcon(widget.icon, tint = Color.Gray, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!widget.title.isNullOrBlank()) {
                        Text(widget.title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            EnergyCardWidgetView(widget.cardKey, viewModel, cornerRadius = widget.cornerRadius, configOverride = widget.energyConfig)
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            com.jimz011apps.hki7.ui.components.EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/** A collapsible stack of energy cards. */
@Composable
fun EnergyStackWidgetItem(
    stack: HKIEnergyStack,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    onToggleCollapsed: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit
) {
    if (stack.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    val collapsed = stack.collapsible && (stack.isCollapsed ?: stack.defaultCollapsed)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                com.jimz011apps.hki7.ui.utils.MdiIcon(stack.icon ?: "lightning-bolt", tint = Color.Gray, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(stack.title ?: "Energy", color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (stack.collapsible) {
                    IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(24.dp)) {
                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!collapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stack.cardKeys.isEmpty()) {
                        Surface(shape = RoundedCornerShape(stack.cornerRadius.dp), color = appColors.elevated) {
                            Text("No cards yet — open the stack settings to pick energy cards.",
                                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                    stack.cardKeys.forEach { key ->
                        EnergyCardWidgetView(key, viewModel, cornerRadius = stack.cornerRadius, configOverride = stack.energyConfig)
                    }
                }
            }
        }
        if (isEditMode) {
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
            com.jimz011apps.hki7.ui.components.EditRemoveBadge(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/** Scrollable list over the energy card catalog, shared by the standalone picker dialog and by
 *  any embedded picker step (e.g. inside AddRoomWidgetDialog) that wants the same list without a
 *  second nested Dialog / mismatched open-close animation. */
@Composable
fun EnergyCardPickerList(
    selected: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .heightIn(max = 420.dp)
            .fadingEdges(listState),
        state = listState
    ) {
        energyCardCatalog.groupBy { it.category }.forEach { (category, specs) ->
            item {
                Text(category, style = MaterialTheme.typography.labelLarge,
                    color = appColors.onMuted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(specs.size) { i ->
                val spec = specs[i]
                val isSel = spec.key in selected
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(itemCornerShape())
                        .clickable { onToggle(spec.key) },
                    shape = itemCornerShape(),
                    color = appColors.subtleSurface,
                    border = if (isSel) BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.28f)) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.jimz011apps.hki7.ui.utils.MdiIcon(
                            spec.mdiIcon, contentDescription = null,
                            tint = appColors.onSurface, size = 28.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(spec.label, color = appColors.onSurface,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null,
                            tint = appColors.onMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** Categorized picker over the energy card catalog. */
@Composable
fun EnergyCardPickerDialog(
    multiSelect: Boolean,
    preselected: List<String> = emptyList(),
    title: String = "Select Energy Cards",
    onDismiss: () -> Unit,
    onSelected: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(preselected.toList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back")
                }
                EnergyCardPickerList(
                    selected = selected,
                    onToggle = { key ->
                        selected = when {
                            multiSelect && key in selected -> selected - key
                            multiSelect -> selected + key
                            else -> listOf(key)
                        }
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSelected(selected) }) { Text("Done") } }
    )
}

// ── Per-card entity overrides ────────────────────────────────────────────────
// Each card key exposes only the config roles it actually reads. An override
// starts as a copy of the Energy view's bindings and is edited role by role.

private data class EnergyRole(
    val key: String,
    val label: String,
    val multi: Boolean = false
)

private fun energyRolesFor(cardKey: String): List<EnergyRole> = when (cardKey) {
    "tiles" -> listOf(
        EnergyRole("grid_power", "Grid power (W)"),
        EnergyRole("home_power", "Home power (W)"),
        EnergyRole("solar_power", "Solar power (W)"),
        EnergyRole("battery_power", "Battery power (W)"),
        EnergyRole("battery_level", "Battery level (%)")
    )
    "usage" -> listOf(
        EnergyRole("grid_import", "Grid import (kWh)"),
        EnergyRole("grid_export", "Grid export (kWh)"),
        EnergyRole("solar_energy", "Solar production (kWh)"),
        EnergyRole("home_power", "Home power (W)"),
        EnergyRole("grid_power", "Grid power (W)")
    )
    "phases" -> listOf(
        EnergyRole("phase1", "Phase 1 power"),
        EnergyRole("phase2", "Phase 2 power"),
        EnergyRole("phase3", "Phase 3 power")
    )
    "tariffs" -> listOf(
        EnergyRole("import_t1", "Import meter tariff 1"),
        EnergyRole("import_t2", "Import meter tariff 2"),
        EnergyRole("export_t1", "Export meter tariff 1"),
        EnergyRole("export_t2", "Export meter tariff 2"),
        EnergyRole("energy_cost", "Energy cost")
    )
    "solar" -> listOf(
        EnergyRole("solar_power", "Solar power (W)"),
        EnergyRole("solar_energy", "Solar production (kWh)"),
        EnergyRole("grid_export", "Grid export (kWh)")
    )
    "battery" -> listOf(
        EnergyRole("battery_power", "Battery power (W)"),
        EnergyRole("battery_level", "Battery level (%)")
    )
    "gas" -> listOf(
        EnergyRole("gas", "Gas total (m³)"),
        EnergyRole("gas_current", "Gas flow (now)"),
        EnergyRole("gas_cost", "Gas cost")
    )
    "water" -> listOf(
        EnergyRole("water", "Water total"),
        EnergyRole("water_current", "Water flow (now)"),
        EnergyRole("water_cost", "Water cost")
    )
    "water_devices" -> listOf(EnergyRole("water_devices", "Individual water devices", multi = true))
    "top_consumers" -> listOf(
        EnergyRole("power_devices", "Power devices", multi = true),
        EnergyRole("home_power", "Home power (W)")
    )
    "device_energy" -> listOf(EnergyRole("energy_devices", "Energy devices", multi = true))
    else -> emptyList()
}

private fun HKIEnergyConfig.roleValue(key: String): List<String> = when (key) {
    "grid_power" -> listOfNotNull(gridPowerEntityId)
    "home_power" -> listOfNotNull(homePowerEntityId)
    "solar_power" -> listOfNotNull(solarPowerEntityId)
    "battery_power" -> listOfNotNull(batteryPowerEntityId)
    "battery_level" -> listOfNotNull(batteryEntityId)
    "grid_import" -> listOfNotNull(gridImportEntityId)
    "grid_export" -> listOfNotNull(gridExportEntityId)
    "solar_energy" -> listOfNotNull(solarEnergyEntityId)
    "phase1" -> listOfNotNull(powerPhase1EntityId)
    "phase2" -> listOfNotNull(powerPhase2EntityId)
    "phase3" -> listOfNotNull(powerPhase3EntityId)
    "import_t1" -> listOfNotNull(gridImportTariff1EntityId)
    "import_t2" -> listOfNotNull(gridImportTariff2EntityId)
    "export_t1" -> listOfNotNull(gridExportTariff1EntityId)
    "export_t2" -> listOfNotNull(gridExportTariff2EntityId)
    "energy_cost" -> listOfNotNull(energyCostEntityId)
    "gas" -> listOfNotNull(gasEntityId)
    "gas_current" -> listOfNotNull(gasCurrentEntityId)
    "gas_cost" -> listOfNotNull(gasCostEntityId)
    "water" -> listOfNotNull(waterEntityId)
    "water_current" -> listOfNotNull(waterCurrentEntityId)
    "water_cost" -> listOfNotNull(waterCostEntityId)
    "power_devices" -> deviceEntityIds
    "energy_devices" -> energyDeviceEntityIds
    "water_devices" -> waterDeviceEntityIds
    else -> emptyList()
}

private fun HKIEnergyConfig.withRole(key: String, ids: List<String>): HKIEnergyConfig {
    val id = ids.firstOrNull()
    return when (key) {
        "grid_power" -> copy(gridPowerEntityId = id)
        "home_power" -> copy(homePowerEntityId = id)
        "solar_power" -> copy(solarPowerEntityId = id)
        "battery_power" -> copy(batteryPowerEntityId = id)
        "battery_level" -> copy(batteryEntityId = id)
        "grid_import" -> copy(gridImportEntityId = id)
        "grid_export" -> copy(gridExportEntityId = id)
        "solar_energy" -> copy(solarEnergyEntityId = id)
        "phase1" -> copy(powerPhase1EntityId = id)
        "phase2" -> copy(powerPhase2EntityId = id)
        "phase3" -> copy(powerPhase3EntityId = id)
        "import_t1" -> copy(gridImportTariff1EntityId = id)
        "import_t2" -> copy(gridImportTariff2EntityId = id)
        "export_t1" -> copy(gridExportTariff1EntityId = id)
        "export_t2" -> copy(gridExportTariff2EntityId = id)
        "energy_cost" -> copy(energyCostEntityId = id)
        "gas" -> copy(gasEntityId = id)
        "gas_current" -> copy(gasCurrentEntityId = id)
        "gas_cost" -> copy(gasCostEntityId = id)
        "water" -> copy(waterEntityId = id)
        "water_current" -> copy(waterCurrentEntityId = id)
        "water_cost" -> copy(waterCostEntityId = id)
        "power_devices" -> copy(deviceEntityIds = ids)
        "energy_devices" -> copy(energyDeviceEntityIds = ids)
        "water_devices" -> copy(waterDeviceEntityIds = ids)
        else -> this
    }
}

/** "Custom entities" section of the energy card/stack settings: a toggle that switches the card
 *  from the Energy view's bindings to its own copy, plus one entity picker row per role.
 *  Returns the role being picked so the caller can suppress its own dialog while picking. */
@Composable
private fun EnergyEntityOverridesSection(
    roles: List<EnergyRole>,
    config: HKIEnergyConfig?,
    pageDefault: HKIEnergyConfig,
    allEntities: List<HAEntity>,
    onChange: (HKIEnergyConfig?) -> Unit,
    onPickRole: (EnergyRole) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Custom entities", style = MaterialTheme.typography.labelLarge)
            Text(
                if (config == null) "Using the Energy view's entities" else "This card uses its own entities",
                style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
            )
        }
        Switch(
            checked = config != null,
            onCheckedChange = { custom -> onChange(if (custom) pageDefault else null) }
        )
    }
    if (config != null) {
        roles.forEach { role ->
            val ids = config.roleValue(role.key)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(role.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        when {
                            ids.isEmpty() -> "None"
                            role.multi -> "${ids.size} selected"
                            else -> allEntities.find { it.entity_id == ids.first() }?.friendlyName ?: ids.first()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ids.isEmpty()) appColors.onMuted else MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (ids.isNotEmpty()) {
                    IconButton(onClick = {
                        val cleared = config.withRole(role.key, emptyList())
                        onChange(when (role.key) {
                            "power_devices" -> cleared.copy(hiddenPowerDeviceEntityIds = allEntities.filter { it.deviceClass == "power" }.map { it.entity_id })
                            "energy_devices" -> cleared.copy(hiddenEnergyDeviceEntityIds = allEntities.filter { it.deviceClass == "energy" }.map { it.entity_id })
                            else -> cleared
                        })
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Clear", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
                    }
                }
                TextButton(onClick = { onPickRole(role) }) { Text("Change") }
            }
        }
    }
}

@Composable
fun EnergyCardWidgetSettingsDialog(
    widget: HKIEnergyCardWidget,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIEnergyCardWidget) -> Unit
) {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val pageDefault = (pageConfigsMap[ENERGY_PAGE_KEY] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()
    val allEntities by viewModel.entities.collectAsState()
    var title by remember { mutableStateOf(widget.title ?: "") }
    var width by remember { mutableStateOf(if (widget.width == "third") "half" else widget.width) }
    var radius by remember { mutableIntStateOf(widget.cornerRadius) }
    var cardKey by remember { mutableStateOf(widget.cardKey) }
    var override by remember { mutableStateOf(widget.energyConfig) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingRole by remember { mutableStateOf<EnergyRole?>(null) }
    var settingsPage by remember(widget) { mutableStateOf("data") }
    if (showPicker) {
        EnergyCardPickerDialog(
            multiSelect = false, preselected = listOf(cardKey), title = "Select Energy Card",
            onDismiss = { showPicker = false },
            onSelected = { sel -> sel.firstOrNull()?.let { cardKey = it }; showPicker = false }
        )
    }
    pickingRole?.let { role ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter {
                it.entity_id.startsWith("sensor.") && when (role.key) {
                    "power_devices" -> it.deviceClass == "power"
                    "energy_devices" -> it.deviceClass == "energy"
                    else -> true
                }
            },
            title = role.label,
            singleSelect = !role.multi,
            preselectedIds = override?.roleValue(role.key)?.toSet().orEmpty(),
            onDismiss = { pickingRole = null },
            onEntitiesSelected = { ids ->
                override = override?.withRole(role.key, ids)?.let { updated ->
                    when (role.key) {
                        "power_devices" -> updated.copy(hiddenPowerDeviceEntityIds = allEntities.filter { it.deviceClass == "power" }.map { it.entity_id }.filterNot { it in ids })
                        "energy_devices" -> updated.copy(hiddenEnergyDeviceEntityIds = allEntities.filter { it.deviceClass == "energy" }.map { it.entity_id }.filterNot { it in ids })
                        else -> updated
                    }
                }
                pickingRole = null
            }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = { com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle("Energy card", "Data sources and card appearance") },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf("data" to "Card & data", "appearance" to "Appearance"),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "data") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory("Card & data", "Choose the energy view and override its entities if needed")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Card", style = MaterialTheme.typography.labelLarge)
                        Text(energyCardCatalog.find { it.key == cardKey }?.label ?: cardKey,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { showPicker = true }) { Text("Change") }
                }
                EnergyEntityOverridesSection(
                    roles = energyRolesFor(cardKey),
                    config = override,
                    pageDefault = pageDefault,
                    allEntities = allEntities,
                    onChange = { override = it },
                    onPickRole = { pickingRole = it }
                )
                }
                if (settingsPage == "appearance") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory("Appearance", "Optional title and dashboard width")
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                com.jimz011apps.hki7.ui.components.WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(
                    cardKey = cardKey, title = title.ifBlank { null }, width = width,
                    cornerRadius = radius, energyConfig = override
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EnergyStackSettingsDialog(
    stack: HKIEnergyStack,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (HKIEnergyStack) -> Unit
) {
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val pageDefault = (pageConfigsMap[ENERGY_PAGE_KEY] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()
    val allEntities by viewModel.entities.collectAsState()
    var title by remember { mutableStateOf(stack.title ?: "") }
    var width by remember { mutableStateOf(if (stack.width == "third") "half" else stack.width) }
    var radius by remember { mutableIntStateOf(stack.cornerRadius) }
    var cardKeys by remember { mutableStateOf(stack.cardKeys) }
    var collapsible by remember { mutableStateOf(stack.collapsible) }
    var override by remember { mutableStateOf(stack.energyConfig) }
    var showPicker by remember { mutableStateOf(false) }
    var pickingRole by remember { mutableStateOf<EnergyRole?>(null) }
    var settingsPage by remember(stack) { mutableStateOf("cards") }
    if (showPicker) {
        EnergyCardPickerDialog(
            multiSelect = true, preselected = cardKeys, title = "Stack Cards",
            onDismiss = { showPicker = false },
            onSelected = { cardKeys = it; showPicker = false }
        )
    }
    pickingRole?.let { role ->
        AdvancedEntitySearchDialog(
            allEntities = allEntities.filter {
                it.entity_id.startsWith("sensor.") && when (role.key) {
                    "power_devices" -> it.deviceClass == "power"
                    "energy_devices" -> it.deviceClass == "energy"
                    else -> true
                }
            },
            title = role.label,
            singleSelect = !role.multi,
            preselectedIds = override?.roleValue(role.key)?.toSet().orEmpty(),
            onDismiss = { pickingRole = null },
            onEntitiesSelected = { ids ->
                override = override?.withRole(role.key, ids)?.let { updated ->
                    when (role.key) {
                        "power_devices" -> updated.copy(hiddenPowerDeviceEntityIds = allEntities.filter { it.deviceClass == "power" }.map { it.entity_id }.filterNot { it in ids })
                        "energy_devices" -> updated.copy(hiddenEnergyDeviceEntityIds = allEntities.filter { it.deviceClass == "energy" }.map { it.entity_id }.filterNot { it in ids })
                        else -> updated
                    }
                }
                pickingRole = null
            }
        )
        // Do not compose the settings AlertDialog over the entity picker.
        return
    }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = { com.jimz011apps.hki7.ui.components.ModernSettingsDialogTitle("Energy stack", "Cards, data sources, and layout") },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.jimz011apps.hki7.ui.components.SettingsTabRow(
                    tabs = listOf("cards" to "Cards & data", "layout" to "Layout"),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "cards") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory("Cards & data", "Choose included cards and shared entity overrides")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Cards", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (cardKeys.isEmpty()) "None selected"
                            else cardKeys.joinToString { key -> energyCardCatalog.find { it.key == key }?.label ?: key },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { showPicker = true }) { Text("Change") }
                }
                EnergyEntityOverridesSection(
                    roles = cardKeys.flatMap { energyRolesFor(it) }.distinctBy { it.key },
                    config = override,
                    pageDefault = pageDefault,
                    allEntities = allEntities,
                    onChange = { override = it },
                    onPickRole = { pickingRole = it }
                )
                }
                if (settingsPage == "layout") {
                com.jimz011apps.hki7.ui.components.SettingsSubcategory("Stack layout", "Title, collapse behavior, and dashboard width")
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Collapsible", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = collapsible, onCheckedChange = { collapsible = it })
                }
                com.jimz011apps.hki7.ui.components.WidgetWidthSelector(width = width, onWidthChange = { width = it }, includeThird = false)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(stack.copy(
                    title = title.ifBlank { null }, width = width, cornerRadius = radius,
                    cardKeys = cardKeys, collapsible = collapsible, energyConfig = override
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
