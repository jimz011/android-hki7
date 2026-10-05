package com.jimz011apps.hki7.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

/**
 * Raster basemap used by every map in the app (person location, Find my devices).
 *
 * CARTO's formerly keyless raster service began watermarking tiles with "API KEY REQUIRED" in
 * August 2026. Home Assistant moved its primary map to OSM vector tiles; HKI's lightweight native
 * renderer remains raster-based, so it uses the official OpenStreetMap raster fallback instead.
 * Every request supplies HKI's identifying User-Agent at the call site.
 */
object MapTiles {

    /** OpenStreetMap requires visible attribution on every map. */
    const val MAP_ATTRIBUTION = "© OpenStreetMap contributors"

    /**
     * Tile URL for a light or dark basemap. `@2x` would fetch retina tiles, but at the 256dp tile
     * size used here the standard tiles already land near 1:1 on typical densities and cost a
     * quarter of the bytes.
     */
    fun url(zoom: Int, x: Int, y: Int, dark: Boolean): String =
        "https://tile.openstreetmap.org/$zoom/$x/$y.png"

    /**
     * True when the map should use the dark basemap. Read from the resolved theme's own surface
     * luminance rather than `isSystemInDarkTheme()`, because HKI's theme mode can be forced to
     * light or dark independently of the system — asking the system directly would light-map a
     * user who pinned the app to dark.
     */
    @Composable
    fun useDarkTiles(): Boolean = LocalHKIAppColors.current.background.luminance() < 0.5f

    /**
     * Home Assistant's non-WebGL fallback uses the same OSM raster layer in both themes and
     * transforms it for dark mode. HKI is also a raster renderer, so this matrix reproduces HA's
     * `invert(.9) hue-rotate(170deg) brightness(1.5) contrast(1.2) saturate(.3)` filter instead of
     * silently showing a light map in a dark dashboard.
     */
    fun colorFilter(dark: Boolean): ColorFilter? = if (!dark) null else ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.044040f, -1.280026f, -0.204014f, 0f, 387.6f,
                -0.408102f, -0.918728f, -0.113170f, 0f, 387.6f,
                -0.338338f, -1.387299f, 0.285636f, 0f, 387.6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
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
