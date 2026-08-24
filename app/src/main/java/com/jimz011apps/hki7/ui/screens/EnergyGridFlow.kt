package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HKIEnergyConfig

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
