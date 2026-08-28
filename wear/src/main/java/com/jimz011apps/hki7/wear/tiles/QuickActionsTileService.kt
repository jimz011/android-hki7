package com.jimz011apps.hki7.wear.tiles

import android.content.Context
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
import com.jimz011apps.hki7.data.HKIQuickAction
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
import java.util.concurrent.TimeUnit

/**
 * The quick actions, one swipe from the watch face.
 *
 * The surface a watch user actually reaches for: no app launch, no waiting. HKI 7's equivalent of
 * the Home Assistant watch app's Shortcuts tile.
 *
 * Deliberately pull-based, unlike the app's WebSocket. A tile updates with nobody looking at it,
 * so holding a connection open for it would cost battery all day to keep a screen fresh that is
 * seen for a second or two. Instead it refreshes when the system asks and after a tap.
 */
class QuickActionsTileService : TileService() {

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
        val actions = if (session.isAuthenticated()) {
            prefs.quickActionsOnce().filter { it.showOnWatch }.take(MAX_ITEMS)
        } else {
            emptyList()
        }

        // Tap handling arrives as a click id rather than a callback, so the entity is encoded in
        // the id and acted on here before the layout is rebuilt.
        (requestParams.currentState.lastClickableId.takeIf { it.isNotEmpty() })?.let { clicked ->
            actions.firstOrNull { it.id == clicked }?.let {
                run(applicationContext, it)
                // Home Assistant applies the call asynchronously, so the layout built below still
                // carries the old state. Ask once more when it has caught up rather than leaving
                // the tile showing the state the tap was meant to change.
                scope.launch {
                    delay(SETTLE_MILLIS)
                    TileRefresh.requestTiles(applicationContext)
                }
            }
        }

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(TimeUnit.MINUTES.toMillis(FRESHNESS_MINUTES))
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    if (actions.isEmpty()) emptyLayout() else layout(actions),
                ),
            )
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    /** Runs the action inline. A tile has no UI to report failure into, so this is best effort. */
    private suspend fun run(context: Context, action: HKIQuickAction) {
        val prefs = WearPreferences(context)
        val session = WearSession(prefs)
        val serverUrl = prefs.serverUrlOnce() ?: return
        runCatching {
            com.jimz011apps.hki7.wear.data.WearActions.run(
                HomeAssistantRest(serverUrl, session),
                action,
            )
        }
    }

    private fun emptyLayout(): LayoutElementBuilders.LayoutElement =
        centred(
            LayoutElementBuilders.Text.Builder()
                .setText(getString(R.string.wear_tile_empty))
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(13f))
                        .setColor(argb(HkiWearColors.OnMuted.toArgb()))
                        .build(),
                )
                .setMaxLines(4)
                .build(),
        )

    /**
     * Centres content and keeps it clear of the round display's edges.
     *
     * The layout is handed a square, but the screen is the circle inside it — full-width content
     * is clipped at both ends and anything packed to the start sits in an unusable corner.
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

    /**
     * A single column of full-width rows rather than a grid.
     *
     * A round screen wastes the corners of a grid, and a name is what identifies a shortcut — an
     * icon alone does not, once three of them are lights.
     */
    private fun layout(actions: List<HKIQuickAction>): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        actions.forEachIndexed { index, action ->
            if (index > 0) {
                column.addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build(),
                )
            }
            column.addContent(row(action))
        }
        return centred(column.build())
    }

    private fun row(action: HKIQuickAction): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(dp(44f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(HkiWearColors.Surface.toArgb()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(dp(HkiWearColors.CornerRadius.value))
                                    .build(),
                            )
                            .build(),
                    )
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            // The click id is how the tap comes back on the next tile request.
                            .setId(action.id)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(action.name?.takeIf { it.isNotBlank() } ?: action.entityId)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(14f))
                            .setColor(argb(HkiWearColors.OnSurface.toArgb()))
                            .build(),
                    )
                    .setMaxLines(1)
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                    .build(),
            )
            .build()

    private companion object {
        const val RESOURCES_VERSION = "1"

        /** Tiles are seen briefly and rarely; asking more often would cost battery for nothing. */
        const val FRESHNESS_MINUTES = 15L

        /** More than this does not fit a watch screen without scrolling past the point of a tile. */
        const val MAX_ITEMS = 4

        /** Keeps content inside the circle the square layout is inscribed with. */
        const val EDGE_INSET_DP = 22f

        /** How long Home Assistant needs before a read reflects the write. */
        const val SETTLE_MILLIS = 900L
    }
}
