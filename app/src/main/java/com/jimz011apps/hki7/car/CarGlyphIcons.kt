package com.jimz011apps.hki7.car

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import com.jimz011apps.hki7.ui.utils.IconPack
import com.jimz011apps.hki7.ui.utils.MdiIconStore

/**
 * Turns the app's icon-font slugs into car icons.
 *
 * HKI 7 draws icons as glyphs from bundled webfonts, but the Car App Library only accepts
 * drawables or bitmaps — it renders in the host's process, so there is no text-drawing path to
 * hand it. Rasterising the glyph once per item keeps the car grid looking like the dashboard
 * rather than falling back to a generic placeholder.
 *
 * Draws white on transparent and lets the host tint: a car head unit switches between day and
 * night themes, and a fixed colour would be invisible in one of them.
 */
object CarGlyphIcons {

    private const val SIZE_PX = 128

    /** Rasterises [slug], or null if no pack knows it. Loads font tables — call off the main thread. */
    fun forSlug(context: Context, slug: String?): CarIcon? {
        val glyph = MdiIconStore.glyphOf(context, slug) ?: return null
        val pack = IconPack.parse(slug).first
        val typeface = runCatching { ResourcesCompat.getFont(context, pack.fontRes) }.getOrNull()
            ?: return null

        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = SIZE_PX * 0.78f
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        // Centre on the glyph's own box rather than the baseline, so icons of differing heights
        // line up with each other in the grid.
        val baseline = SIZE_PX / 2f - (metrics.ascent + metrics.descent) / 2f
        Canvas(bitmap).drawText(glyph, SIZE_PX / 2f, baseline, paint)

        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap))
            .setTint(CarColor.DEFAULT)
            .build()
    }
}
