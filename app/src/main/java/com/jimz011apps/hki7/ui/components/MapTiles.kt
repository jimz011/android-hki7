package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/**
 * Raster basemap used by every map in the app (person location, Find my devices).
 *
 * The tiles are CARTO's Voyager basemap — the same one Home Assistant's own map card uses. The
 * previous source was `tile.openstreetmap.org`, the "OSM Carto" style: heavy saturated greens and
 * thick orange/yellow roads, designed to be a *map editor's* reference render rather than a
 * background for an app UI, which is why it reads as cartoonish next to the rest of HKI.
 *
 * Voyager is a muted, low-contrast cartography with a proper dark variant, so the map finally
 * follows the app's theme instead of glowing white inside a dark dialog. It is plain 256px raster
 * tiles over HTTPS like before, so this is a URL swap: no new dependency, no API key, no SDK.
 *
 * Attribution is required by both OpenStreetMap (the data) and CARTO (the rendering); see
 * [MAP_ATTRIBUTION], which callers must display over the map.
 */
object MapTiles {

    /** OSM requires attribution for the data, CARTO for the tiles. Keep both visible on the map. */
    const val MAP_ATTRIBUTION = "© OpenStreetMap © CARTO"

    /**
     * Tile URL for a light or dark basemap. `@2x` would fetch retina tiles, but at the 256dp tile
     * size used here the standard tiles already land near 1:1 on typical densities and cost a
     * quarter of the bytes.
     */
    fun url(zoom: Int, x: Int, y: Int, dark: Boolean): String {
        val style = if (dark) "dark_all" else "voyager"
        return "https://basemaps.cartocdn.com/rastertiles/$style/$zoom/$x/$y.png"
    }

    /**
     * True when the map should use the dark basemap. Read from the resolved theme's own surface
     * luminance rather than `isSystemInDarkTheme()`, because HKI's theme mode can be forced to
     * light or dark independently of the system — asking the system directly would light-map a
     * user who pinned the app to dark.
     */
    @Composable
    fun useDarkTiles(): Boolean = LocalHKIAppColors.current.background.luminance() < 0.5f
}

// ─────────────────────────────────────────────────────────────────────────────
// Web-Mercator projection, shared by every tiled map in the app
// ─────────────────────────────────────────────────────────────────────────────

/** A point in pixel space at a given zoom, with the world's top-left as the origin. */
internal data class WorldPoint(val x: Double, val y: Double)

internal fun latLonToWorld(lat: Double, lon: Double, zoom: Int, tileSizePx: Float): WorldPoint {
    // Web Mercator is undefined at the poles; ±85.0511° is where the projection goes square.
    val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
    val latRad = Math.toRadians(clampedLat)
    val tileCount = 1 shl zoom
    val x = (lon + 180.0) / 360.0 * tileCount
    val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * tileCount
    return WorldPoint(x * tileSizePx, y.coerceIn(0.0, tileCount - 1.0) * tileSizePx)
}

internal fun clampWorldPoint(point: WorldPoint, zoom: Int, tileSizePx: Float): WorldPoint {
    val worldSize = (1 shl zoom) * tileSizePx
    return WorldPoint(
        // x wraps (the world repeats east-west), y clamps (there is nothing above the north pole).
        x = ((point.x % worldSize) + worldSize) % worldSize,
        y = point.y.coerceIn(0.0, worldSize.toDouble())
    )
}

internal fun wrapTileX(x: Int, zoom: Int): Int {
    val tileCount = 1 shl zoom
    return ((x % tileCount) + tileCount) % tileCount
}
