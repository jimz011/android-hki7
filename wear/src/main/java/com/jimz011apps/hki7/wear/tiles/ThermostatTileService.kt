package com.jimz011apps.hki7.wear.tiles

import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.wear.R
import com.jimz011apps.hki7.wear.data.HomeAssistantRest
import com.jimz011apps.hki7.wear.data.WearPreferences
import com.jimz011apps.hki7.wear.data.WearSession
import com.jimz011apps.hki7.wear.ui.theme.HkiWearColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.future
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import kotlin.math.round

/**
 * Your thermostats, nudgeable from the wrist.
 *
 * The case a watch is genuinely better at than a phone: it is cold, you are in bed, and the phone
 * is somewhere else. Which thermostats appear is chosen on the phone — a house with four of them
 * has no obvious default, and guessing would put the wrong room on someone's wrist.
 *
 * Several thermostats share one tile, cycled by tapping the name, rather than one tile each.
 * ProtoLayout has no gesture beyond `Clickable`, so a swipe inside a tile is not possible at all;
 * and separate tile instances would each need configuring on a screen far too small for it.
 *
 * Deliberately not built on quick actions. A quick action is one tap with one outcome; a
 * thermostat is a value to move, which needs its own reading, its own bounds and its own controls.
 */
class ThermostatTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val prefs = WearPreferences(applicationContext)
        val session = WearSession(prefs)
        val serverUrl = prefs.serverUrlOnce()
        val entityIds = prefs.thermostatEntityIds()
        val clicked = requestParams.currentState.lastClickableId

        val layout = if (serverUrl == null || entityIds.isEmpty() || !session.isAuthenticated()) {
            message(getString(R.string.wear_thermostat_unset))
        } else {
            // Cycling first: the nudge below must act on whichever thermostat the tap selected,
            // not the one that was showing when it was pressed.
            var index = prefs.thermostatIndex().mod(entityIds.size)
            if (clicked == CLICK_CYCLE) {
                index = (index + 1).mod(entityIds.size)
                prefs.saveThermostatIndex(index)
            }
            val entityId = entityIds[index]

            val client = HomeAssistantRest(serverUrl, session)
            // A tap arrives as a click id on the next request, so the nudge happens before the
            // fresh reading is taken — otherwise the tile would redraw with the old target.
            when (clicked) {
                CLICK_WARMER -> nudge(client, entityId, up = true)
                CLICK_COOLER -> nudge(client, entityId, up = false)
                else -> Unit
            }
            if (clicked == CLICK_WARMER || clicked == CLICK_COOLER) {
                // A thermostat takes a moment to report the new target back.
                scope.launch {
                    delay(SETTLE_MILLIS)
                    TileRefresh.requestTiles(applicationContext)
                }
            }
            val entity = runCatching { client.state(entityId) }.getOrNull()
            if (entity == null) {
                message(getString(R.string.wear_unreachable))
            } else {
                thermostat(entity, position = index + 1, total = entityIds.size)
            }
        }

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(TimeUnit.MINUTES.toMillis(FRESHNESS_MINUTES))
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    /**
     * Moves the target by one step, clamped to what the device reports it accepts.
     *
     * Reads before writing rather than tracking a local value: the tile is not the only thing that
     * can change a thermostat, and sending a target derived from a stale reading would undo
     * whatever else moved it.
     */
    private suspend fun nudge(client: HomeAssistantRest, entityId: String, up: Boolean) {
        val entity = runCatching { client.state(entityId) }.getOrNull() ?: return
        val current = entity.temperature ?: entity.currentTemperature ?: return
        val step = entity.targetTempStep
        val target = (current + if (up) step else -step)
            .coerceIn(entity.minTemp, entity.maxTemp)
        // Round to the step so repeated nudges cannot drift onto values the device will not take.
        val snapped = round(target / step) * step
        runCatching {
            client.callService(
                "climate",
                "set_temperature",
                buildJsonObject {
                    put("entity_id", entityId)
                    put("temperature", snapped)
                },
            )
        }
    }

    private fun thermostat(
        entity: HAEntity,
        position: Int,
        total: Int,
    ): LayoutElementBuilders.LayoutElement {
        val target = entity.temperature
        val current = entity.currentTemperature
        val name = entity.friendlyName ?: entity.entity_id

        val column = LayoutElementBuilders.Column.Builder()
            // Without this the column packs to the start edge, which on a round screen puts the
            // content in the corner with the wide middle of the display left empty.
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            // With one thermostat the name is a label; with several it is the control that moves
            // between them, so it says so and becomes tappable.
            .addContent(if (total > 1) cycleRow(name, position, total) else nameLabel(name))
            .addContent(spacer(2f))
            // The target is what the buttons move, so it is the number that gets the size.
            .addContent(
                text(
                    target?.let { format(it) } ?: "—",
                    size = 28f,
                    color = HkiWearColors.OnSurface,
                ),
            )
            .addContent(
                text(
                    current?.let { getString(R.string.wear_thermostat_now, format(it)) }.orEmpty(),
                    size = 12f,
                    color = HkiWearColors.OnMuted,
                ),
            )
            .addContent(spacer(10f))
            .addContent(
                // Left to wrap its children rather than expanded: an expanded row would pin the
                // buttons to the start edge again.
                LayoutElementBuilders.Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(button("−", CLICK_COOLER))
                    .addContent(spacer(10f))
                    .addContent(button("+", CLICK_WARMER))
                    .build(),
            )
            .build()

        return centred(column)
    }

    /**
     * Centres content and keeps it clear of the round display's edges.
     *
     * A watch screen is a circle inscribed in the square the layout is given, so the corners and
     * the top and bottom strips are unusable. Text laid out to the full width is clipped at both
     * ends — which is exactly what the title did before this.
     */
    private fun centred(content: LayoutElementBuilders.LayoutElement): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(EDGE_INSET_DP))
                            .setEnd(dp(EDGE_INSET_DP))
                            .setTop(dp(EDGE_INSET_DP))
                            .setBottom(dp(EDGE_INSET_DP))
                            .build(),
                    )
                    .build(),
            )
            .addContent(content)
            .build()

    private fun nameLabel(name: String): LayoutElementBuilders.LayoutElement =
        text(name, size = 12f, color = HkiWearColors.OnMuted, ellipsize = true)

    /**
     * The name, its position in the list, and a chevron — the whole row advancing to the next
     * thermostat when tapped.
     *
     * The name doubles as the control because a watch tile has no room for a separate button, and
     * the position ("2/4") is what tells the user there is anything to advance to at all.
     */
    private fun cycleRow(
        name: String,
        position: Int,
        total: Int,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId(CLICK_CYCLE)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder().setAll(dp(4f)).build(),
                    )
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(
                        text(name, size = 12f, color = HkiWearColors.OnMuted, ellipsize = true),
                    )
                    .addContent(spacer(4f))
                    .addContent(
                        text(
                            getString(R.string.wear_thermostat_position, position, total),
                            size = 11f,
                            color = HkiWearColors.Accent,
                        ),
                    )
                    .build(),
            )
            .build()

    private fun button(label: String, clickId: String): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(dp(56f))
            .setHeight(dp(44f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(HkiWearColors.Elevated.toArgb()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(dp(HkiWearColors.CornerRadius.value))
                                    .build(),
                            )
                            .build(),
                    )
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId(clickId)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .build(),
            )
            .addContent(text(label, size = 20f, color = HkiWearColors.Accent))
            .build()

    private fun message(body: String): LayoutElementBuilders.LayoutElement =
        centred(text(body, size = 13f, color = HkiWearColors.OnMuted, maxLines = 4))

    private fun text(
        value: String,
        size: Float,
        color: androidx.compose.ui.graphics.Color,
        maxLines: Int = 1,
        ellipsize: Boolean = false,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(maxLines)
            .apply {
                // A long room name is better trimmed than silently cut off mid-glyph.
                if (ellipsize) {
                    setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                }
            }
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color.toArgb()))
                    .build(),
            )
            .build()

    private fun spacer(size: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(size)).setWidth(dp(size)).build()

    /** Whole degrees read better at a glance; halves are kept because many devices step by 0.5. */
    private fun format(value: Double): String =
        if (value == round(value)) "${value.toInt()}°" else String.format("%.1f°", value)

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val FRESHNESS_MINUTES = 10L
        const val CLICK_WARMER = "warmer"
        const val CLICK_COOLER = "cooler"
        const val CLICK_CYCLE = "cycle"

        /** How long Home Assistant needs before a read reflects the write. */
        const val SETTLE_MILLIS = 900L

        /** Keeps content inside the circle the square layout is inscribed with. */
        const val EDGE_INSET_DP = 26f
    }
}
