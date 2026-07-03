package com.example.hki7.ui.utils

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hki7.R

/** The bundled Material Design Icons webfont (`res/font/mdi_icons.ttf`). */
val MdiFontFamily = FontFamily(Font(R.font.mdi_icons))

/**
 * Loads and caches the MDI name→glyph and name→keyword tables from `assets/`.
 *
 * The codepoint table (~160 KB) is parsed once on first use and kept in memory;
 * the keyword table (~290 KB) is loaded lazily the first time the picker
 * searches. Glyphs themselves live in the memory-mapped font, so rendering an
 * icon never allocates a vector — it just draws a character.
 */
object MdiIconStore {
    @Volatile private var glyphs: Map<String, String>? = null   // slug -> glyph (1 codepoint)
    @Volatile private var order: List<String>? = null           // slugs, alphabetical
    @Volatile private var keywords: Map<String, String>? = null // slug -> "alias tag ..."

    private fun ensureGlyphs(context: Context): Map<String, String> {
        glyphs?.let { return it }
        return synchronized(this) {
            glyphs ?: run {
                val g = HashMap<String, String>(9000)
                val ord = ArrayList<String>(7500)
                context.applicationContext.assets.open("mdi_codepoints.txt")
                    .bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val sp = line.indexOf(' ')
                            if (sp <= 0) continue
                            val cp = line.substring(sp + 1).trim().toIntOrNull(16) ?: continue
                            val name = line.substring(0, sp)
                            g[name] = String(Character.toChars(cp))
                            ord.add(name)
                        }
                    }
                order = ord
                glyphs = g
                g
            }
        }
    }

    private fun ensureKeywords(context: Context): Map<String, String> {
        keywords?.let { return it }
        return synchronized(this) {
            keywords ?: run {
                val k = HashMap<String, String>(7000)
                context.applicationContext.assets.open("mdi_keywords.txt")
                    .bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val tab = line.indexOf('\t')
                            if (tab <= 0) continue
                            k[line.substring(0, tab)] = line.substring(tab + 1)
                        }
                    }
                keywords = k
                k
            }
        }
    }

    /**
     * Resolves any slug to its glyph, accepting Home Assistant `mdi:` prefixes and
     * legacy identifiers (see [LEGACY_ICON_MAP]). Returns null for unknown names.
     */
    fun glyphOf(context: Context, name: String?): String? {
        if (name.isNullOrBlank()) return null
        val g = ensureGlyphs(context)
        val raw = name.trim()
        val slug = raw.removePrefix("mdi:").lowercase()
        g[slug]?.let { return it }
        (LEGACY_ICON_MAP[raw] ?: LEGACY_ICON_MAP[slug])?.let { mapped -> g[mapped]?.let { return it } }
        return null
    }

    /** All icon slugs, alphabetical. */
    fun allNames(context: Context): List<String> {
        ensureGlyphs(context)
        return order ?: emptyList()
    }

    /** Slugs whose name or keywords contain [query] (case-insensitive). Empty query returns all. */
    fun search(context: Context, query: String): List<String> {
        val ord = allNames(context)
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ord
        val kw = ensureKeywords(context)
        return ord.filter { it.contains(q) || (kw[it]?.contains(q) == true) }
    }

    /** Slugs tagged with the MDI category [tag] (e.g. "home automation", "weather"). */
    fun byCategory(context: Context, tag: String): List<String> {
        val ord = allNames(context)
        val kw = ensureKeywords(context)
        val needle = "#" + tag.lowercase()
        return ord.filter { kw[it]?.contains(needle) == true }
    }
}

/**
 * Renders an MDI icon by slug from the bundled webfont — a drop-in for
 * `Icon(mdiIcon(name), …)`. Sizing is via [size] (not `Modifier.size`, since the
 * glyph is drawn as text). Unknown/blank names fall back to `lightbulb`, matching
 * the previous default.
 */
@Composable
fun MdiIcon(
    name: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val glyph = remember(name) {
        MdiIconStore.glyphOf(context, name) ?: MdiIconStore.glyphOf(context, "lightbulb").orEmpty()
    }
    val fontSize = with(LocalDensity.current) { size.toSp() }
    val descModifier = if (contentDescription != null)
        Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(modifier.then(descModifier).size(size), contentAlignment = Alignment.Center) {
        BasicText(
            text = glyph,
            style = TextStyle(
                color = tint,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontFamily = MdiFontFamily,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
