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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.data.HAHistoryEntry
import com.example.hki7.data.HKIEnergyConfig
import com.example.hki7.data.HKIPageConfig
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.AdvancedEntitySearchDialog
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.components.parseHistoryMillis
import com.example.hki7.ui.theme.LocalHKIAppColors
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
private val WindowWarm  = Color(0xFFFFDF9E)

@Composable
fun EnergyScreen(viewModel: MainViewModel) {
    val entities       by viewModel.entities.collectAsState()
    val pageConfigsMap by viewModel.pageConfigsMapping.collectAsState()
    val historyMap     by viewModel.historyMapping.collectAsState()
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
    fun entityFloat(id: String?): Float? =
        id?.takeIf { it.isNotBlank() }?.let { entities.find { e -> e.entity_id == it }?.state?.toFloatOrNull() }
    fun entityUnit(id: String?, fallback: String): String =
        id?.takeIf { it.isNotBlank() }
            ?.let { entities.find { e -> e.entity_id == it }?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull }
            ?: fallback

    val solarW   = entityWatts(energyConfig.solarPowerEntityId) ?: 0f
    val gridW    = entityWatts(energyConfig.gridPowerEntityId) ?: 0f
    val homeW    = entityWatts(energyConfig.homePowerEntityId) ?: (solarW + gridW.coerceAtLeast(0f))
    val batteryW = entityWatts(energyConfig.batteryPowerEntityId) ?: 0f
    val solarKwh   = entityFloat(energyConfig.solarEnergyEntityId) ?: 0f
    val importKwh  = entityFloat(energyConfig.gridImportEntityId) ?: 0f
    val exportKwh  = entityFloat(energyConfig.gridExportEntityId) ?: 0f
    val costVal    = entityFloat(energyConfig.energyCostEntityId) ?: 0f
    val batteryPct = entities.find { it.entity_id == energyConfig.batteryEntityId }?.state?.toIntOrNull()
    val hasBattery = !energyConfig.batteryEntityId.isNullOrBlank()
    val selfSufficiency = if (homeW > 0) (solarW.coerceAtMost(homeW) / homeW * 100).toInt().coerceIn(0, 100) else 0

    val solarForecastKwh = entityFloat(energyConfig.solarForecastEntityId)
    val gasVal   = entityFloat(energyConfig.gasEntityId)
    val gasCost  = entityFloat(energyConfig.gasCostEntityId)
    val waterVal = entityFloat(energyConfig.waterEntityId)
    val waterCost = entityFloat(energyConfig.waterCostEntityId)
    val gasUnit   = entityUnit(energyConfig.gasEntityId, "m³")
    val waterUnit = entityUnit(energyConfig.waterEntityId, "L")

    // Fetch today's history for the chart sensors when the screen opens / config changes
    val chartPowerId = energyConfig.homePowerEntityId?.takeIf { it.isNotBlank() }
        ?: energyConfig.gridPowerEntityId?.takeIf { it.isNotBlank() }
    val solarPowerId = energyConfig.solarPowerEntityId?.takeIf { it.isNotBlank() }
    val gasId   = energyConfig.gasEntityId?.takeIf { it.isNotBlank() }
    val waterId = energyConfig.waterEntityId?.takeIf { it.isNotBlank() }
    val historyIds = listOfNotNull(chartPowerId, solarPowerId, gasId, waterId)
    LaunchedEffect(historyIds) {
        historyIds.forEach { viewModel.fetchEntityHistory(it, 24) }
    }

    // Hourly chart data derived from history
    val homeHourly  = remember(historyMap, chartPowerId) {
        chartPowerId?.let { hourlyPowerAverages(historyMap[it], entityIsKw(entities, it)) }
    }
    val solarHourly = remember(historyMap, solarPowerId) {
        solarPowerId?.let { hourlyPowerAverages(historyMap[it], entityIsKw(entities, it)) }
    }
    val gasHourly   = remember(historyMap, gasId) { gasId?.let { hourlyCounterDeltas(historyMap[it]) } }
    val waterHourly = remember(historyMap, waterId) { waterId?.let { hourlyCounterDeltas(historyMap[it]) } }

    // Auto-detected top consumers: any power sensor that isn't one of the configured mains
    val mainPowerIds = remember(energyConfig) {
        setOfNotNull(
            energyConfig.solarPowerEntityId, energyConfig.gridPowerEntityId,
            energyConfig.homePowerEntityId, energyConfig.batteryPowerEntityId
        )
    }
    // Manually tracked devices always show; auto-detected power sensors fill the rest.
    val topConsumers = remember(entities, mainPowerIds, energyConfig.deviceEntityIds) {
        fun wattsOf(e: HAEntity): Float? {
            val v = e.state.toFloatOrNull() ?: return null
            val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""
            return if (unit.contains("kW", ignoreCase = true)) v * 1000f else v
        }
        val manual = energyConfig.deviceEntityIds.mapNotNull { id ->
            entities.find { it.entity_id == id }?.let { e -> e to (wattsOf(e) ?: 0f) }
        }
        val manualIds = manual.map { it.first.entity_id }.toSet()
        val auto = entities.asSequence()
            .filter {
                it.entity_id.startsWith("sensor.") && it.entity_id !in mainPowerIds &&
                it.entity_id !in manualIds && it.deviceClass == "power"
            }
            .mapNotNull { e -> wattsOf(e)?.takeIf { w -> w >= 1f }?.let { w -> e to w } }
            .toList()
        (manual + auto).sortedByDescending { it.second }.take(8)
    }

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
        val appColors = LocalHKIAppColors.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // ── the animated house ────────────────────────────────────────────
            item {
                EnergyHouseScene(
                    solarW = solarW, gridW = gridW, homeW = homeW,
                    batteryW = batteryW, batteryPct = batteryPct, hasBattery = hasBattery,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }

            // ── live source tiles ─────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val gridStatus = when {
                            gridW > 10f  -> "Importing"
                            gridW < -10f -> "Exporting"
                            else         -> "Idle"
                        }
                        EnergyLiveTile(Icons.Default.ElectricBolt, ElecBlue, "Electricity",
                            "${formatW(abs(gridW))} · $gridStatus", Modifier.weight(1f))
                        val solarStatus = buildString {
                            append(formatW(solarW.coerceAtLeast(0f)))
                            append(" · ")
                            append(if (solarW > 10f) "Producing" else "Idle")
                        }
                        EnergyLiveTile(Icons.Default.WbSunny, SolarAmber, "Solar", solarStatus, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        EnergyLiveTile(Icons.Default.Home, MaterialTheme.colorScheme.primary, "Home",
                            "${formatW(homeW)} · ${if (homeW > 10f) "Consuming" else "Idle"}", Modifier.weight(1f))
                        if (hasBattery) {
                            val battStatus = when {
                                batteryW > 10f  -> "Charging"
                                batteryW < -10f -> "Discharging"
                                else            -> "Idle"
                            }
                            val battText = listOfNotNull(
                                batteryPct?.let { "$it%" },
                                if (battStatus != "Idle") formatW(abs(batteryW)) else null
                            ).joinToString(" · ").ifEmpty { "—" } + " · $battStatus"
                            EnergyLiveTile(
                                if (batteryW > 10f) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                when { (batteryPct ?: 0) > 50 -> ExportGreen; (batteryPct ?: 0) > 20 -> SolarAmber; else -> ImportRed },
                                "Battery", battText, Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Electricity Total ─────────────────────────────────────────────
            item {
                SectionHeader("Electricity Total", if (!energyConfig.energyCostEntityId.isNullOrBlank()) "€ ${"%.2f".format(costVal)}" else null)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp), color = appColors.elevated
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val usedKwh = (importKwh + solarKwh - exportKwh).coerceAtLeast(0f)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TotalStat(Icons.Default.ArrowDownward, ElecBlue, "%.1f kWh".format(usedKwh), "Used")
                            TotalStat(Icons.Default.ArrowDownward, ImportRed, "%.1f kWh".format(importKwh), "Imported")
                            TotalStat(Icons.Default.ArrowUpward, ExportGreen, "%.1f kWh".format(exportKwh), "Exported")
                        }
                        if (homeHourly != null) {
                            Spacer(Modifier.height(14.dp))
                            HourlyBarChart(homeHourly, ElecBlue, "W")
                        }
                    }
                }
            }

            // ── Solar ─────────────────────────────────────────────────────────
            if (!energyConfig.solarPowerEntityId.isNullOrBlank() || !energyConfig.solarEnergyEntityId.isNullOrBlank()) {
                item {
                    SectionHeader("Solar", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp), color = appColors.elevated
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IconBadge(Icons.Default.WbSunny, SolarAmber)
                                Column {
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("%.1f".format(solarKwh), style = MaterialTheme.typography.headlineSmall,
                                            color = appColors.onSurface, fontWeight = FontWeight.Bold)
                                        Text("kWh", style = MaterialTheme.typography.labelMedium, color = SolarAmber,
                                            modifier = Modifier.padding(bottom = 3.dp))
                                    }
                                    Text("Produced · $selfSufficiency% self-used", style = MaterialTheme.typography.bodySmall, color = appColors.onMuted)
                                }
                            }
                            if (solarForecastKwh != null && solarForecastKwh > 0f) {
                                Spacer(Modifier.height(12.dp))
                                val frac = (solarKwh / solarForecastKwh).coerceIn(0f, 1f)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Forecast today", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                                    Text("%.1f of %.1f kWh · %d%%".format(solarKwh, solarForecastKwh, (frac * 100).toInt()),
                                        style = MaterialTheme.typography.labelSmall, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(8.dp)
                                        .background(SolarAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth(frac).fillMaxHeight()
                                            .background(SolarAmber, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            if (solarHourly != null) {
                                Spacer(Modifier.height(14.dp))
                                HourlyBarChart(solarHourly, SolarAmber, "W")
                            }
                        }
                    }
                }
            }

            // ── Top consumers (auto-detected power sensors) ───────────────────
            if (topConsumers.isNotEmpty()) {
                item {
                    SectionHeader("Top consumers", null)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp), color = appColors.elevated
                    ) {
                        Column(Modifier.padding(vertical = 6.dp)) {
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
                    }
                }
            }

            // ── Gas ───────────────────────────────────────────────────────────
            if (gasId != null) {
                item {
                    SectionHeader("Gas", gasCost?.let { "€ ${"%.2f".format(it)}" })
                    UtilityCard(
                        icon = Icons.Default.LocalFireDepartment, color = GasPink,
                        value = gasVal?.let { "%.1f".format(it) } ?: "—", unit = gasUnit,
                        label = "Used today", hourly = gasHourly, chartUnit = gasUnit
                    )
                }
            }

            // ── Water ─────────────────────────────────────────────────────────
            if (waterId != null) {
                item {
                    SectionHeader("Water", waterCost?.let { "€ ${"%.2f".format(it)}" })
                    UtilityCard(
                        icon = Icons.Default.WaterDrop, color = WaterBlue,
                        value = waterVal?.let { if (it >= 100f) "%.0f".format(it) else "%.1f".format(it) } ?: "—", unit = waterUnit,
                        label = "Used today", hourly = waterHourly, chartUnit = waterUnit
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Isometric energy house (Homey style): corner-view house with a panel-covered
// roof plane, garage with slatted door, glowing gable window, wall meter and
// cable runs. Light pulses travel the cables while power flows.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyHouseScene(
    solarW: Float, gridW: Float, homeW: Float,
    batteryW: Float, batteryPct: Int?, hasBattery: Boolean,
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = appColors.background.luminance() < 0.5f

    val solarActive     = solarW > 10f
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
        val apexB = apex + lAx
        // Point on the solar roof plane: u along lAx (front→back), v eave→ridge
        fun roof(u: Float, v: Float) = fcT + lAx * u + (apex - fcT) * v
        // Point on a wall face: base + f along axis, then g up the wall
        fun onFace(base: Offset, axis: Offset, f: Float, up: Float) = base + axis * f + Offset(0f, -up)

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

        // ── solar roof plane + panel array ──────────────────────────────────
        drawPath(quad(roof(-0.04f, -0.08f), roof(1.04f, -0.08f), roof(1.04f, 1.02f), roof(-0.04f, 1.02f)), roofFace)
        drawLine(edgeLight, roof(-0.04f, -0.08f), roof(-0.04f, 1.02f), 1.5.dp.toPx())   // front roof edge
        drawLine(edgeLight, roof(-0.04f, 1.02f), roof(1.04f, 1.02f), 1.2.dp.toPx())      // ridge
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
        drawCablePath(solarCable)
        // Second thin line beside the solar drop, like the video's paired cables
        drawLine(cableColor.copy(alpha = 0.6f),
            solarCable[0] + Offset(3.dp.toPx(), 1.dp.toPx()),
            solarCable[1] + Offset(3.dp.toPx(), 0f), 1.dp.toPx())
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
// History → hourly chart data
// ─────────────────────────────────────────────────────────────────────────────

private fun entityIsKw(entities: List<HAEntity>, id: String): Boolean =
    entities.find { it.entity_id == id }
        ?.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
        ?.contains("kW", ignoreCase = true) == true

private fun todayPoints(entries: List<HAHistoryEntry>?): List<Pair<Long, Float>> {
    if (entries.isNullOrEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    return entries.mapNotNull { e ->
        val t = parseHistoryMillis(e.last_changed) ?: return@mapNotNull null
        val v = e.state.toFloatOrNull() ?: return@mapNotNull null
        if (t >= dayStart) t to v else null
    }.sortedBy { it.first }
}

/** Average power per hour-of-day for today. Index = hour, NaN-free, null when no data. */
private fun hourlyPowerAverages(entries: List<HAHistoryEntry>?, isKw: Boolean): FloatArray? {
    val pts = todayPoints(entries)
    if (pts.isEmpty()) return null
    val zone = ZoneId.systemDefault()
    val sums = FloatArray(24); val counts = IntArray(24)
    pts.forEach { (t, v) ->
        val hour = java.time.Instant.ofEpochMilli(t).atZone(zone).hour
        sums[hour] += if (isKw) v * 1000f else v
        counts[hour]++
    }
    return FloatArray(24) { i -> if (counts[i] > 0) sums[i] / counts[i] else 0f }
}

/** Per-hour increase of a (daily-resetting) cumulative counter, e.g. gas m³ or water L today. */
private fun hourlyCounterDeltas(entries: List<HAHistoryEntry>?): FloatArray? {
    val pts = todayPoints(entries)
    if (pts.size < 2) return null
    val zone = ZoneId.systemDefault()
    val out = FloatArray(24)
    for (i in 1 until pts.size) {
        val delta = (pts[i].second - pts[i - 1].second).coerceAtLeast(0f)
        val hour = java.time.Instant.ofEpochMilli(pts[i].first).atZone(zone).hour
        out[hour] += delta
    }
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
// Homey-style hourly bar chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HourlyBarChart(values: FloatArray, color: Color, unit: String, modifier: Modifier = Modifier) {
    val appColors = LocalHKIAppColors.current
    val maxV = values.max().coerceAtLeast(0.001f)
    val maxLabel = when {
        unit == "W" && maxV >= 1000f -> "%.1fk".format(maxV / 1000f)
        maxV >= 100f -> "%.0f".format(maxV)
        else -> "%.1f".format(maxV)
    }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(
                modifier = Modifier.width(36.dp).height(96.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                Text("0", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
            }
            Canvas(Modifier.weight(1f).height(96.dp)) {
                val cw = size.width; val ch = size.height
                // Dotted guides every 6 hours
                val guide = appColors.onMuted.copy(alpha = 0.18f)
                for (i in 0..4) {
                    val x = cw * i / 4f
                    drawLine(
                        guide, Offset(x, 0f), Offset(x, ch), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                    )
                }
                drawLine(guide, Offset(0f, ch), Offset(cw, ch), 1.dp.toPx())
                val slot = cw / 24f
                val barW = slot * 0.55f
                val nowHour = java.time.LocalTime.now().hour
                for (i in 0 until 24) {
                    val v = values[i]
                    if (v <= 0f) continue
                    val bh = (v / maxV) * (ch - 2.dp.toPx())
                    val x = slot * i + (slot - barW) / 2f
                    drawRoundRect(
                        color = if (i == nowHour) color else color.copy(alpha = 0.75f),
                        topLeft = Offset(x, ch - bh),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2.5f)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0:00", "6:00", "12:00", "18:00", "").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
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
    modifier: Modifier = Modifier
) {
    val appColors = LocalHKIAppColors.current
    Surface(shape = RoundedCornerShape(18.dp), color = appColors.elevated, modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(icon, color)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(status, style = MaterialTheme.typography.bodySmall, color = appColors.onMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text(value, style = MaterialTheme.typography.titleSmall, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
    }
}

@Composable
private fun ConsumerRow(rank: Int, name: String, watts: Float, shareOfHome: Int?) {
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
        Icon(Icons.Default.ElectricBolt, null, tint = appColors.onMuted.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun UtilityCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, value: String, unit: String, label: String,
    hourly: FloatArray?, chartUnit: String
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp), color = appColors.elevated
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
            if (hourly != null) {
                Spacer(Modifier.height(14.dp))
                HourlyBarChart(hourly, color, chartUnit)
            }
        }
    }
}

private fun formatW(w: Float) = when { w < 1f -> "0 W"; w < 1000f -> "${w.toInt()} W"; else -> "${"%.1f".format(w / 1000f)} kW" }

// ─────────────────────────────────────────────────────────────────────────────
// Energy sensor settings (shown inside PageSettingsDialog extra section)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.EnergySensorSection(
    energyConfig: HKIEnergyConfig,
    allEntities: List<HAEntity>,
    onSave: (HKIEnergyConfig) -> Unit
) {
    val sensors = remember(allEntities) { allEntities.filter { it.entity_id.startsWith("sensor.") } }
    var cfg by remember(energyConfig) { mutableStateOf(energyConfig) }
    var pickingField by remember { mutableStateOf<String?>(null) }

    fun fieldValue(field: String?): String? = when (field) {
        "solar_power"    -> cfg.solarPowerEntityId
        "grid_power"     -> cfg.gridPowerEntityId
        "home_power"     -> cfg.homePowerEntityId
        "solar_kwh"      -> cfg.solarEnergyEntityId
        "import_kwh"     -> cfg.gridImportEntityId
        "export_kwh"     -> cfg.gridExportEntityId
        "cost"           -> cfg.energyCostEntityId
        "battery_pct"    -> cfg.batteryEntityId
        "battery_power"  -> cfg.batteryPowerEntityId
        "solar_forecast" -> cfg.solarForecastEntityId
        "gas"            -> cfg.gasEntityId
        "gas_cost"       -> cfg.gasCostEntityId
        "water"          -> cfg.waterEntityId
        "water_cost"     -> cfg.waterCostEntityId
        else -> null
    }

    if (pickingField != null) {
        AdvancedEntitySearchDialog(
            allEntities = sensors, title = "Select Sensor", singleSelect = true,
            preselectedIds = setOfNotNull(fieldValue(pickingField)?.takeIf { it.isNotBlank() }),
            onDismiss = { pickingField = null },
            onEntitiesSelected = { ids ->
                val id = ids.firstOrNull()
                cfg = when (pickingField) {
                    "solar_power"    -> cfg.copy(solarPowerEntityId = id)
                    "grid_power"     -> cfg.copy(gridPowerEntityId = id)
                    "home_power"     -> cfg.copy(homePowerEntityId = id)
                    "solar_kwh"      -> cfg.copy(solarEnergyEntityId = id)
                    "import_kwh"     -> cfg.copy(gridImportEntityId = id)
                    "export_kwh"     -> cfg.copy(gridExportEntityId = id)
                    "cost"           -> cfg.copy(energyCostEntityId = id)
                    "battery_pct"    -> cfg.copy(batteryEntityId = id)
                    "battery_power"  -> cfg.copy(batteryPowerEntityId = id)
                    "solar_forecast" -> cfg.copy(solarForecastEntityId = id)
                    "gas"            -> cfg.copy(gasEntityId = id)
                    "gas_cost"       -> cfg.copy(gasCostEntityId = id)
                    "water"          -> cfg.copy(waterEntityId = id)
                    "water_cost"     -> cfg.copy(waterCostEntityId = id)
                    else -> cfg
                }
                onSave(cfg)
                pickingField = null
            }
        )
    }

    val appColors = LocalHKIAppColors.current

    @Composable
    fun groupHeader(label: String, color: Color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelLarge, color = appColors.onSurface, fontWeight = FontWeight.Bold)
        }
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
            Icon(Icons.Default.ChevronRight, null, tint = appColors.onMuted, modifier = Modifier.size(18.dp))
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }

    groupHeader("Electricity", ElecBlue)
    sensorRow("grid_power", "Grid power (W, + = import)")
    sensorRow("home_power", "Home power (W)")
    sensorRow("import_kwh", "Grid import today (kWh)")
    sensorRow("export_kwh", "Grid export today (kWh)")
    sensorRow("cost", "Energy cost today")

    groupHeader("Solar", SolarAmber)
    sensorRow("solar_power", "Solar power (W)")
    sensorRow("solar_kwh", "Solar produced today (kWh)")
    sensorRow("solar_forecast", "Solar forecast today (kWh)")

    groupHeader("Battery", BattPurple)
    sensorRow("battery_pct", "Battery level (%)")
    sensorRow("battery_power", "Battery power (W, + = charging)")

    groupHeader("Gas", GasPink)
    sensorRow("gas", "Gas used today (m³)")
    sensorRow("gas_cost", "Gas cost today")

    groupHeader("Water", WaterBlue)
    sensorRow("water", "Water used today (L/m³)")
    sensorRow("water_cost", "Water cost today")

    // ── individual devices (top consumers) ──────────────────────────────────
    var showDevicePicker by remember { mutableStateOf(false) }
    if (showDevicePicker) {
        // Prefer power sensors, but fall back to all sensors so nothing is unreachable
        val powerSensors = sensors.filter { e ->
            val unit = e.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull
            e.deviceClass == "power" || unit == "W" || unit == "kW"
        }
        AdvancedEntitySearchDialog(
            allEntities = powerSensors.ifEmpty { sensors },
            title = "Track Devices",
            singleSelect = false,
            preselectedIds = cfg.deviceEntityIds.toSet(),
            onDismiss = { showDevicePicker = false },
            onEntitiesSelected = { ids ->
                cfg = cfg.copy(deviceEntityIds = ids)
                onSave(cfg)
                showDevicePicker = false
            }
        )
    }

    groupHeader("Devices", ExportGreen)
    Text(
        "Power sensors tracked as individual devices under Top consumers.",
        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
    )
    cfg.deviceEntityIds.forEach { id ->
        val name = allEntities.find { it.entity_id == id }?.friendlyName ?: id
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = appColors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    cfg = cfg.copy(deviceEntityIds = cfg.deviceEntityIds - id)
                    onSave(cfg)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Close, "Remove", tint = appColors.onMuted, modifier = Modifier.size(16.dp))
            }
        }
        HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.08f))
    }
    TextButton(onClick = { showDevicePicker = true }) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (cfg.deviceEntityIds.isEmpty()) "Add devices" else "Edit devices")
    }
}
