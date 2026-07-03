@file:Suppress("UnusedBoxWithConstraintsScope")

package com.example.hki7.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hki7.data.HKIEnergyConfig
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

private const val ENERGY_PAGE_KEY = "energy"

@Composable
fun EnergyScreen(viewModel: MainViewModel) {
    val entities       by viewModel.entities.collectAsState()
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val energyConfig: HKIEnergyConfig =
        (pageConfigsMap[ENERGY_PAGE_KEY] ?: HKIPageConfig()).energyConfig ?: HKIEnergyConfig()

    // Power/energy entities update live via the websocket subscription (see MainViewModel realtime
    // sync), so no per-screen full-state polling is needed here.

    fun entityWatts(id: String?): Float? {
        if (id.isNullOrBlank()) return null
        val e = entities.find { it.entity_id == id } ?: return null
        val v = e.state.toFloatOrNull() ?: return null
        val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
        return if (unit.contains("kW", ignoreCase = true)) v * 1000f else v
    }
    fun entityKwh(id: String?): Float? =
        id?.takeIf { it.isNotBlank() }?.let { entities.find { e -> e.entity_id == it }?.state?.toFloatOrNull() }

    val solarW   = entityWatts(energyConfig.solarPowerEntityId) ?: 0f
    val gridW    = entityWatts(energyConfig.gridPowerEntityId) ?: 0f
    val homeW    = entityWatts(energyConfig.homePowerEntityId) ?: (solarW + gridW.coerceAtLeast(0f))
    val batteryW = entityWatts(energyConfig.batteryPowerEntityId) ?: 0f
    val solarKwh   = entityKwh(energyConfig.solarEnergyEntityId) ?: 0f
    val importKwh  = entityKwh(energyConfig.gridImportEntityId) ?: 0f
    val exportKwh  = entityKwh(energyConfig.gridExportEntityId) ?: 0f
    val costVal    = entityKwh(energyConfig.energyCostEntityId) ?: 0f
    val batteryPct = entities.find { it.entity_id == energyConfig.batteryEntityId }?.state?.toIntOrNull()
    val hasBattery = !energyConfig.batteryEntityId.isNullOrBlank()
    val selfSufficiency = if (homeW > 0) (solarW.coerceAtMost(homeW) / homeW * 100).toInt().coerceIn(0, 100) else 0

    val infiniteTransition = rememberInfiniteTransition(label = "energy_flow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "phase"
    )

    // Build the energy settings section for the page-settings sheet
    val energySettingsSection: Pair<String, @Composable ColumnScope.() -> Unit> = "Energy Sensors" to {
        EnergySensorSection(
            energyConfig = energyConfig,
            allEntities = entities,
            onSave = { newCfg -> viewModel.updateEnergyConfig(ENERGY_PAGE_KEY, newCfg) }
        )
    }

    HKIPage(
        viewModel = viewModel,
        title = "Energy",
        subtitle = "Power overview",
        pageKey = ENERGY_PAGE_KEY,
        pageSettingsTitle = "Energy Settings",
        extraPageSettingsSection = energySettingsSection
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                EnergyFlowCanvas(
                    solarW = solarW, gridW = gridW, homeW = homeW,
                    batteryW = batteryW, hasBattery = hasBattery, phase = phase,
                    modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        EnergyCard("Solar", solarKwh, "kWh", Color(0xFFFFB300), Icons.Default.WbSunny, Modifier.weight(1f))
                        val gridLabel  = if (gridW >= 0) "Grid Import" else "Grid Export"
                        val gridColor  = if (gridW >= 0) Color(0xFFEF5350) else Color(0xFF66BB6A)
                        EnergyCard(gridLabel, if (gridW >= 0) importKwh else exportKwh, "kWh", gridColor, Icons.Default.ElectricBolt, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        EnergyCard("Self-use", selfSufficiency.toFloat(), "%", Color(0xFF42A5F5), Icons.Default.Home, Modifier.weight(1f))
                        EnergyCard("Cost", costVal, "€", MaterialTheme.colorScheme.primary, Icons.Default.Euro, Modifier.weight(1f))
                    }
                    if (hasBattery && batteryPct != null) {
                        val battColor = when { batteryPct > 50 -> Color(0xFF66BB6A); batteryPct > 20 -> Color(0xFFFFB300); else -> Color(0xFFEF5350) }
                        EnergyCard("Battery", batteryPct.toFloat(), "%", battColor, Icons.Default.BatteryChargingFull, Modifier.fillMaxWidth())
                    }
                    if (exportKwh > 0f) {
                        EnergyCard("Surplus", exportKwh, "kWh", Color(0xFF66BB6A), Icons.Default.UploadFile, Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = LocalHKIAppColors.current.subtleSurface
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        LivePowerItem("Now Solar", solarW, Color(0xFFFFB300))
                        LivePowerItem("Home", homeW, Color(0xFF42A5F5))
                        LivePowerItem(if (gridW >= 0) "Importing" else "Exporting", abs(gridW),
                            if (gridW >= 0) Color(0xFFEF5350) else Color(0xFF66BB6A))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated power flow canvas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyFlowCanvas(
    solarW: Float, gridW: Float, homeW: Float,
    batteryW: Float, hasBattery: Boolean, phase: Float,
    modifier: Modifier = Modifier
) {
    val primary      = MaterialTheme.colorScheme.primary
    val appColors    = LocalHKIAppColors.current
    val solarColor   = Color(0xFFFFB300)
    val gridImpColor = Color(0xFFEF5350)
    val gridExpColor = Color(0xFF66BB6A)
    val battColor    = Color(0xFF42A5F5)

    val solarActive     = solarW > 10f
    val importActive    = gridW > 10f
    val exportActive    = gridW < -10f
    val battCharging    = batteryW > 10f
    val battDischarging = batteryW < -10f

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val cx = w / 2f

        val solarPos  = Offset(cx, h * 0.10f)
        val homePos   = Offset(cx, h * 0.50f)
        val gridPos   = Offset(w * 0.14f, h * 0.80f)
        val battPos   = Offset(w * 0.86f, h * 0.80f)
        val nodeR     = 34.dp.toPx()
        val dotR      = 5.dp.toPx()

        fun drawConnectionLine(from: Offset, to: Offset, color: Color, active: Boolean) {
            drawLine(
                color = if (active) color.copy(alpha = 0.45f) else appColors.elevated,
                start = from, end = to, strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
            )
        }
        drawConnectionLine(solarPos, homePos, solarColor, solarActive)
        drawConnectionLine(gridPos,  homePos, if (exportActive) gridExpColor else gridImpColor, importActive || exportActive)
        if (hasBattery) drawConnectionLine(battPos, homePos, battColor, battCharging || battDischarging)

        fun drawFlowDots(from: Offset, to: Offset, color: Color, reverse: Boolean = false) {
            listOf(0f, 0.33f, 0.66f).forEach { offset ->
                val t = ((phase + offset) % 1f).let { if (reverse) 1f - it else it }
                val pos = Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
                drawCircle(color.copy(alpha = 0.25f), dotR * 2.2f, pos)
                drawCircle(color, dotR, pos)
            }
        }
        if (solarActive)     drawFlowDots(solarPos, homePos, solarColor)
        if (importActive)    drawFlowDots(gridPos,  homePos, gridImpColor)
        if (exportActive)    drawFlowDots(homePos,  gridPos, gridExpColor)
        if (battCharging)    drawFlowDots(homePos,  battPos, battColor)
        if (battDischarging) drawFlowDots(battPos,  homePos, battColor, reverse = true)

        fun drawNode(pos: Offset, color: Color, active: Boolean) {
            drawCircle(if (active) color.copy(alpha = 0.18f) else appColors.elevated, nodeR, pos)
            drawCircle(if (active) color else appColors.onMuted.copy(alpha = 0.25f), nodeR, pos, style = Stroke(2.dp.toPx()))
        }
        drawNode(solarPos, solarColor, solarActive)
        drawNode(homePos,  primary,    homeW > 10f)
        drawNode(gridPos,  if (exportActive) gridExpColor else gridImpColor, importActive || exportActive)
        if (hasBattery) drawNode(battPos, battColor, battCharging || battDischarging)

        // Simple house icon inside home node
        val hp = homePos; val s = nodeR * 0.6f
        drawPath(Path().apply {
            moveTo(hp.x, hp.y - s)
            lineTo(hp.x + s, hp.y - 0.05f * s); lineTo(hp.x + s * 0.65f, hp.y - 0.05f * s)
            lineTo(hp.x + s * 0.65f, hp.y + s * 0.85f); lineTo(hp.x - s * 0.65f, hp.y + s * 0.85f)
            lineTo(hp.x - s * 0.65f, hp.y - 0.05f * s); lineTo(hp.x - s, hp.y - 0.05f * s); close()
        }, color = if (homeW > 10f) primary.copy(alpha = 0.8f) else appColors.onMuted.copy(alpha = 0.4f))
    }

    // Node text labels via overlay
    Box(modifier = modifier) {
        NodeLabel("☀️", if (solarW > 10f) formatW(solarW) else "Solar", Modifier.align(Alignment.TopCenter).offset(y = 4.dp))
        NodeLabel("🏠", "Home", Modifier.align(Alignment.Center).offset(y = 44.dp))
        NodeLabel("⚡", if (gridW != 0f) formatW(abs(gridW)) else "Grid", Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 4.dp))
        if (hasBattery) NodeLabel("🔋", "Battery", Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))
    }
}

@Composable
private fun NodeLabel(emoji: String, value: String, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

private fun formatW(w: Float) = when { w < 1f -> "0 W"; w < 1000f -> "${w.toInt()} W"; else -> "${"%.1f".format(w / 1000f)} kW" }

// ─────────────────────────────────────────────────────────────────────────────
// Cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyCard(label: String, value: Float, unit: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = RoundedCornerShape(18.dp), color = appColors.elevated, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (unit == "%") value.toInt().toString() else "%.1f".format(value), style = MaterialTheme.typography.headlineMedium, color = appColors.onSurface, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun LivePowerItem(label: String, watts: Float, color: Color) {
    val appColors = LocalHKIAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatW(watts), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Energy sensor settings (shown inside PageSettingsDialog extra section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.EnergySensorSection(
    energyConfig: HKIEnergyConfig,
    allEntities: List<com.example.hki7.data.HAEntity>,
    onSave: (HKIEnergyConfig) -> Unit
) {
    val sensors = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("sensor.") } }
    var cfg by remember(energyConfig) { mutableStateOf(energyConfig) }
    var pickingField by remember { mutableStateOf<String?>(null) }

    if (pickingField != null) {
        val currentId = when (pickingField) {
            "solar_power"    -> cfg.solarPowerEntityId
            "grid_power"     -> cfg.gridPowerEntityId
            "home_power"     -> cfg.homePowerEntityId
            "solar_kwh"      -> cfg.solarEnergyEntityId
            "import_kwh"     -> cfg.gridImportEntityId
            "export_kwh"     -> cfg.gridExportEntityId
            "cost"           -> cfg.energyCostEntityId
            "battery_pct"    -> cfg.batteryEntityId
            "battery_power"  -> cfg.batteryPowerEntityId
            else -> null
        }
        AdvancedEntitySearchDialog(
            allEntities = sensors, title = "Select Sensor", singleSelect = true,
            preselectedIds = setOfNotNull(currentId?.takeIf { it.isNotBlank() }),
            onDismiss = { pickingField = null },
            onEntitiesSelected = { ids ->
                val id = ids.firstOrNull()
                cfg = when (pickingField) {
                    "solar_power"   -> cfg.copy(solarPowerEntityId = id)
                    "grid_power"    -> cfg.copy(gridPowerEntityId = id)
                    "home_power"    -> cfg.copy(homePowerEntityId = id)
                    "solar_kwh"     -> cfg.copy(solarEnergyEntityId = id)
                    "import_kwh"    -> cfg.copy(gridImportEntityId = id)
                    "export_kwh"    -> cfg.copy(gridExportEntityId = id)
                    "cost"          -> cfg.copy(energyCostEntityId = id)
                    "battery_pct"   -> cfg.copy(batteryEntityId = id)
                    "battery_power" -> cfg.copy(batteryPowerEntityId = id)
                    else -> cfg
                }
                onSave(cfg)
                pickingField = null
            }
        )
    }

    val rows = listOf(
        "solar_power"   to "Solar power (W)",
        "grid_power"    to "Grid power (W, + = import)",
        "home_power"    to "Home power (W)",
        "solar_kwh"     to "Solar today (kWh)",
        "import_kwh"    to "Grid import today (kWh)",
        "export_kwh"    to "Grid export today (kWh)",
        "cost"          to "Energy cost today",
        "battery_pct"   to "Battery level (%)",
        "battery_power" to "Battery power (W, + = charging)"
    )
    val appColors = LocalHKIAppColors.current
    rows.forEach { (field, label) ->
        val entityId = when (field) {
            "solar_power"   -> cfg.solarPowerEntityId
            "grid_power"    -> cfg.gridPowerEntityId
            "home_power"    -> cfg.homePowerEntityId
            "solar_kwh"     -> cfg.solarEnergyEntityId
            "import_kwh"    -> cfg.gridImportEntityId
            "export_kwh"    -> cfg.gridExportEntityId
            "cost"          -> cfg.energyCostEntityId
            "battery_pct"   -> cfg.batteryEntityId
            "battery_power" -> cfg.batteryPowerEntityId
            else -> null
        }
        val name = entityId?.let { id -> allEntities.find { it.entity_id == id }?.friendlyName ?: id }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { pickingField = field }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface)
                Text(name ?: "Not set", style = MaterialTheme.typography.bodySmall, color = if (name != null) MaterialTheme.colorScheme.primary else appColors.onMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }
}
