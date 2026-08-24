package com.jimz011apps.hki7.ui

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIEnergyConfig
import com.jimz011apps.hki7.ui.screens.gridFlowEntityIds
import com.jimz011apps.hki7.ui.screens.gridFlowOf
import com.jimz011apps.hki7.ui.screens.suggestsPerPhaseGridFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyGridFlowTest {
    /** Resolves the ids the config points at; anything unmapped reads as null, as on screen. */
    private fun watts(vararg pairs: Pair<String, Float>): (String?) -> Float? {
        val values = pairs.toMap()
        return { id -> id?.takeIf { it.isNotBlank() }?.let(values::get) }
    }

    @Test
    fun `single net sensor splits by sign`() {
        val cfg = HKIEnergyConfig(gridPowerEntityId = "sensor.grid")
        val importing = gridFlowOf(cfg, watts("sensor.grid" to 1400f))
        assertEquals(1400f, importing.importW, 0.01f)
        assertEquals(0f, importing.exportW, 0.01f)
        assertTrue(importing.importing)
        assertFalse(importing.bothWays)

        val exporting = gridFlowOf(cfg, watts("sensor.grid" to -900f))
        assertEquals(0f, exporting.importW, 0.01f)
        assertEquals(900f, exporting.exportW, 0.01f)
        assertEquals(-900f, exporting.netW, 0.01f)
        assertFalse(exporting.bothWays)
    }

    @Test
    fun `three-phase house with solar on one phase imports and exports at once`() {
        val cfg = HKIEnergyConfig(
            perPhaseGridFlow = true,
            gridImportPhase1EntityId = "sensor.imp_l1",
            gridImportPhase2EntityId = "sensor.imp_l2",
            gridImportPhase3EntityId = "sensor.imp_l3",
            gridExportPhase1EntityId = "sensor.exp_l1",
            gridExportPhase2EntityId = "sensor.exp_l2",
            gridExportPhase3EntityId = "sensor.exp_l3"
        )
        // Solar sits on L1 and overproduces; L2 and L3 keep drawing.
        val flow = gridFlowOf(
            cfg,
            watts(
                "sensor.imp_l1" to 0f, "sensor.exp_l1" to 1700f,
                "sensor.imp_l2" to 400f, "sensor.exp_l2" to 0f,
                "sensor.imp_l3" to 500f, "sensor.exp_l3" to 0f
            )
        )
        assertEquals(900f, flow.importW, 0.01f)
        assertEquals(1700f, flow.exportW, 0.01f)
        assertEquals(-800f, flow.netW, 0.01f)
        assertTrue(flow.bothWays)
        assertTrue(flow.perPhase)
        assertEquals(listOf(-1700f, 400f, 500f), flow.phases)
    }

    @Test
    fun `per-phase mode falls back to a signed phase sensor when the split pair is unmapped`() {
        val cfg = HKIEnergyConfig(
            perPhaseGridFlow = true,
            powerPhase1EntityId = "sensor.l1",
            powerPhase2EntityId = "sensor.l2",
            powerPhase3EntityId = "sensor.l3"
        )
        val flow = gridFlowOf(
            cfg,
            watts("sensor.l1" to -1200f, "sensor.l2" to 300f, "sensor.l3" to 250f)
        )
        assertEquals(550f, flow.importW, 0.01f)
        assertEquals(1200f, flow.exportW, 0.01f)
        assertTrue(flow.bothWays)
        assertEquals(listOf(-1200f, 300f, 250f), flow.phases)
    }

    @Test
    fun `per-phase mode with nothing mapped falls back to the net sensor`() {
        val cfg = HKIEnergyConfig(perPhaseGridFlow = true, gridPowerEntityId = "sensor.grid")
        val flow = gridFlowOf(cfg, watts("sensor.grid" to 750f))
        assertEquals(750f, flow.importW, 0.01f)
        assertFalse(flow.perPhase)
        assertFalse(flow.bothWays)
    }

    @Test
    fun `the toggle stays inert until the user turns it on`() {
        val cfg = HKIEnergyConfig(
            gridPowerEntityId = "sensor.grid",
            gridImportPhase1EntityId = "sensor.imp_l1",
            gridExportPhase1EntityId = "sensor.exp_l1"
        )
        // Autodiscovery fills the per-phase slots for everyone; only the toggle may act on them.
        val flow = gridFlowOf(
            cfg,
            watts("sensor.grid" to 200f, "sensor.imp_l1" to 900f, "sensor.exp_l1" to 1700f)
        )
        assertEquals(200f, flow.importW, 0.01f)
        assertFalse(flow.bothWays)
    }

    @Test
    fun `flows under the threshold read as idle`() {
        val cfg = HKIEnergyConfig(
            perPhaseGridFlow = true,
            gridImportPhase1EntityId = "sensor.imp_l1",
            gridExportPhase1EntityId = "sensor.exp_l1"
        )
        val flow = gridFlowOf(cfg, watts("sensor.imp_l1" to 4f, "sensor.exp_l1" to 3f))
        assertFalse(flow.importing)
        assertFalse(flow.exporting)
        assertFalse(flow.bothWays)
    }

    @Test
    fun `negative readings from a positive-only sensor never subtract`() {
        val cfg = HKIEnergyConfig(
            perPhaseGridFlow = true,
            gridImportPhase1EntityId = "sensor.imp_l1",
            gridExportPhase1EntityId = "sensor.exp_l1"
        )
        val flow = gridFlowOf(cfg, watts("sensor.imp_l1" to -50f, "sensor.exp_l1" to 600f))
        assertEquals(0f, flow.importW, 0.01f)
        assertEquals(600f, flow.exportW, 0.01f)
    }

    @Test
    fun `grid flow entity ids cover every sensor the tiles read`() {
        val cfg = HKIEnergyConfig(
            gridPowerEntityId = "sensor.grid",
            powerPhase1EntityId = "sensor.l1",
            gridImportPhase1EntityId = "sensor.imp_l1",
            gridExportPhase3EntityId = "sensor.exp_l3"
        )
        assertEquals(
            setOf("sensor.grid", "sensor.l1", "sensor.imp_l1", "sensor.exp_l3"),
            cfg.gridFlowEntityIds()
        )
    }

    /** A sensor as Home Assistant reports it: friendly name and unit carry the classification. */
    private fun sensor(id: String, name: String, unit: String = "W") = HAEntity(
        entity_id = id,
        state = "0",
        attributes = JsonObject(
            mapOf(
                "friendly_name" to JsonPrimitive(name),
                "unit_of_measurement" to JsonPrimitive(unit)
            )
        )
    )

    @Test
    fun `a filled per-phase export slot suggests turning the mode on`() {
        val cfg = HKIEnergyConfig(gridExportPhase1EntityId = "sensor.exp_l1")
        assertTrue(suggestsPerPhaseGridFlow(cfg, emptyList()))
    }

    @Test
    fun `nothing is suggested once the mode is already on`() {
        val cfg = HKIEnergyConfig(perPhaseGridFlow = true, gridExportPhase1EntityId = "sensor.exp_l1")
        assertFalse(suggestsPerPhaseGridFlow(cfg, emptyList()))
    }

    @Test
    fun `dsmr per-phase returned sensors are detected without a source device`() {
        val entities = listOf(
            sensor("sensor.dsmr_reading_power_delivered_l1", "Power delivered l1"),
            sensor("sensor.dsmr_reading_power_returned_l1", "Power returned l1"),
            sensor("sensor.dsmr_reading_power_returned_l2", "Power returned l2")
        )
        assertTrue(suggestsPerPhaseGridFlow(HKIEnergyConfig(), entities))
    }

    @Test
    fun `homewizard per-phase export sensors are detected without a source device`() {
        val entities = listOf(
            sensor("sensor.p1_meter_active_power_import_l1", "Active power import phase 1"),
            sensor("sensor.p1_meter_active_power_export_l3", "Active power export phase 3")
        )
        assertTrue(suggestsPerPhaseGridFlow(HKIEnergyConfig(), entities))
    }

    @Test
    fun `a single-phase meter is left alone`() {
        val entities = listOf(
            sensor("sensor.p1_active_power", "Active power"),
            sensor("sensor.p1_active_power_import", "Active power import"),
            sensor("sensor.p1_active_power_export", "Active power export"),
            // Per-phase, but import only: no phase can be feeding back.
            sensor("sensor.p1_active_power_import_l1", "Active power import phase 1"),
            sensor("sensor.p1_active_power_import_l2", "Active power import phase 2")
        )
        assertFalse(suggestsPerPhaseGridFlow(HKIEnergyConfig(), entities))
    }

    @Test
    fun `a whole-house export energy counter is not mistaken for per-phase metering`() {
        val entities = listOf(
            sensor("sensor.grid_export_today", "Grid export today", unit = "kWh"),
            sensor("sensor.solar_production", "Solar production", unit = "W")
        )
        assertFalse(suggestsPerPhaseGridFlow(HKIEnergyConfig(), entities))
    }
}
