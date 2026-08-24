package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.data.HKIEnergyConfig
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Below this the grid is treated as idle, matching the thresholds the energy tiles have always used. */
const val GRID_FLOW_THRESHOLD_W = 10f

/**
 * Live grid flow, split by direction.
 *
 * A single-phase connection only ever flows one way at a given instant, so [importW] and [exportW]
 * are mutually exclusive there and [netW] tells the whole story. A three-phase connection metered
 * per phase does not work that way: with solar on one phase only, that phase can feed back while
 * the other two still draw, and the house imports and exports at the same moment. [bothWays] is
 * true in exactly that case, and the tiles show both figures instead of picking a direction.
 *
 * [phases] holds the signed net for L1/L2/L3 — positive drawing, negative feeding back — with null
 * for any phase that has no sensor mapped.
 */
data class GridFlow(
    val importW: Float,
    val exportW: Float,
    val phases: List<Float?> = listOf(null, null, null),
    /** True when the figures were summed from per-phase sensors rather than one net sensor. */
    val perPhase: Boolean = false
) {
    /** Positive while the house draws from the grid, negative while it feeds back. */
    val netW: Float get() = importW - exportW
    val importing: Boolean get() = importW > GRID_FLOW_THRESHOLD_W
    val exporting: Boolean get() = exportW > GRID_FLOW_THRESHOLD_W

    /** Drawing and feeding back at once — only reachable with per-phase metering. */
    val bothWays: Boolean get() = importing && exporting

    /** Phases carrying a meaningful flow, as (index, signed watts) pairs. */
    fun activePhases(): List<Pair<Int, Float>> =
        phases.mapIndexedNotNull { i, w -> w?.let { i to it } }
}

internal fun HKIEnergyConfig.powerPhaseId(index: Int): String? = when (index) {
    0 -> powerPhase1EntityId
    1 -> powerPhase2EntityId
    else -> powerPhase3EntityId
}

internal fun HKIEnergyConfig.gridImportPhaseId(index: Int): String? = when (index) {
    0 -> gridImportPhase1EntityId
    1 -> gridImportPhase2EntityId
    else -> gridImportPhase3EntityId
}

internal fun HKIEnergyConfig.gridExportPhaseId(index: Int): String? = when (index) {
    0 -> gridExportPhase1EntityId
    1 -> gridExportPhase2EntityId
    else -> gridExportPhase3EntityId
}

/** Every entity the grid tiles read, so they can be kept out of the "top consumers" list. */
internal fun HKIEnergyConfig.gridFlowEntityIds(): Set<String> = setOfNotNull(
    gridPowerEntityId,
    powerPhase1EntityId, powerPhase2EntityId, powerPhase3EntityId,
    gridImportPhase1EntityId, gridImportPhase2EntityId, gridImportPhase3EntityId,
    gridExportPhase1EntityId, gridExportPhase2EntityId, gridExportPhase3EntityId
).filter { it.isNotBlank() }.toSet()

/**
 * Reads the configured sensors into a [GridFlow]. [watts] resolves an entity id to watts (null when
 * unset or unavailable), so the screen and the dashboard widget can share this with their own
 * lookups.
 *
 * With [HKIEnergyConfig.perPhaseGridFlow] off — the default — this splits the single net sensor by
 * sign, which is what the tiles did before per-phase support existed.
 *
 * With it on, each phase contributes its own import and export. A phase whose split pair is unmapped
 * falls back to its signed power sensor, so an existing per-phase setup keeps working; a phase with
 * neither contributes nothing. If no phase resolves at all, this falls back to the net sensor rather
 * than reporting an idle grid.
 */
internal fun gridFlowOf(config: HKIEnergyConfig, watts: (String?) -> Float?): GridFlow {
    val net = watts(config.gridPowerEntityId) ?: 0f
    fun fromNet(phases: List<Float?>) = GridFlow(
        importW = net.coerceAtLeast(0f),
        exportW = (-net).coerceAtLeast(0f),
        phases = phases,
        perPhase = false
    )

    if (!config.perPhaseGridFlow) return fromNet((0..2).map { watts(config.powerPhaseId(it)) })

    var importW = 0f
    var exportW = 0f
    var mapped = false
    val phases = (0..2).map { i ->
        // P1/DSMR meters report delivered and returned as separate positive figures.
        val phaseImport = watts(config.gridImportPhaseId(i))?.coerceAtLeast(0f)
        val phaseExport = watts(config.gridExportPhaseId(i))?.coerceAtLeast(0f)
        if (phaseImport != null || phaseExport != null) {
            mapped = true
            importW += phaseImport ?: 0f
            exportW += phaseExport ?: 0f
            (phaseImport ?: 0f) - (phaseExport ?: 0f)
        } else {
            watts(config.powerPhaseId(i))?.also { signed ->
                mapped = true
                importW += signed.coerceAtLeast(0f)
                exportW += (-signed).coerceAtLeast(0f)
            }
        }
    }
    return if (mapped) GridFlow(importW, exportW, phases, perPhase = true) else fromNet(phases)
}


// ─────────────────────────────────────────────────────────────────────────────
// Entity classification. Shared by the source-device autodiscovery and by the
// per-phase detection below, so both agree on what an "export sensor for L2" is.
// ─────────────────────────────────────────────────────────────────────────────

internal fun energyEntityUnit(entity: HAEntity): String =
    entity.attributes?.get("unit_of_measurement")?.jsonPrimitive?.contentOrNull ?: ""

internal fun energyEntityName(entity: HAEntity): String =
    (entity.friendlyName ?: entity.entity_id).lowercase()

internal fun isPowerEntity(entity: HAEntity): Boolean =
    entity.deviceClass == "power" || energyEntityUnit(entity) == "W" || energyEntityUnit(entity) == "kW"

/** Matches the two conventions meters use for phase 1..3: "phase 2" and the terser " l2". */
internal fun phaseMatches(entity: HAEntity, phase: Int): Boolean =
    energyEntityName(entity).let { it.contains("phase $phase") || it.contains(" l$phase") }

private fun isDsmrEntity(entity: HAEntity): Boolean =
    entity.entity_id.startsWith("sensor.dsmr_reading_")

private fun nameHasAny(entity: HAEntity, vararg terms: String): Boolean =
    terms.any(energyEntityName(entity)::contains)

/** DSMR calls what the grid delivers to the house "delivered"; most others call it import. */
internal fun isGridImportEntity(entity: HAEntity): Boolean =
    nameHasAny(entity, "import", "consumption", "consumed", "used", "afname") ||
        (isDsmrEntity(entity) && nameHasAny(entity, "delivered"))

internal fun isGridExportEntity(entity: HAEntity): Boolean =
    nameHasAny(entity, "export", "production", "produced", "returned", "teruglever") ||
        (!isDsmrEntity(entity) && nameHasAny(entity, "delivered"))

/**
 * True when the setup looks like a connection metered per phase, which is the only kind that can
 * import and export at the same moment. An export power sensor tagged to one phase is the tell: a
 * meter that nets the whole connection has no reason to expose one.
 *
 * Returns false once [HKIEnergyConfig.perPhaseGridFlow] is on — there is nothing left to suggest.
 */
internal fun suggestsPerPhaseGridFlow(config: HKIEnergyConfig, entities: List<HAEntity>): Boolean {
    if (config.perPhaseGridFlow) return false
    // Autodiscovery fills these whether or not the toggle is on, so a filled slot is the cheapest
    // and most precise signal: the source device genuinely exposes per-phase export.
    if ((0..2).any { !config.gridExportPhaseId(it).isNullOrBlank() }) return true
    // No source device picked, so fall back to scanning what Home Assistant exposes.
    return entities.any { entity ->
        isPowerEntity(entity) && isGridExportEntity(entity) && (1..3).any { phaseMatches(entity, it) }
    }
}
