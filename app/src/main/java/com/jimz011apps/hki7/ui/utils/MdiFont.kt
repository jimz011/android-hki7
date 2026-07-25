package com.jimz011apps.hki7.ui.utils

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
import com.jimz011apps.hki7.R

/**
 * An installable icon pack. Each pack is a bundled webfont plus `name codepointHex`
 * and (optionally) `name<TAB>keywords` lookup tables in `assets/`. Icons are addressed
 * by a **pack-qualified slug** — `mdi:lightbulb`, `si:github` — with a bare slug
 * (no prefix) treated as MDI for backward compatibility with dashboards saved before
 * multiple packs existed.
 */
enum class IconPack(
    val id: String,
    val displayName: String,
    val fontRes: Int,
    val codepointsAsset: String,
    val keywordsAsset: String?,
    /** Whether picker category chips (from `#tag` keywords) apply. Only MDI is tagged. */
    val hasCategories: Boolean,
) {
    MDI("mdi", "Material Design Icons", R.font.mdi_icons, "mdi_codepoints.txt", "mdi_keywords.txt", true),
    SIMPLE("si", "Simple Icons", R.font.simple_icons, "simple_codepoints.txt", "simple_keywords.txt", false),
    TABLER("tb", "Tabler", R.font.tabler_icons, "tabler_codepoints.txt", null, false),
    PHOSPHOR("ph", "Phosphor", R.font.phosphor_icons, "phosphor_codepoints.txt", null, false);

    val fontFamily: FontFamily by lazy { FontFamily(Font(fontRes)) }

    companion object {
        val DEFAULT = MDI
        fun fromId(id: String?): IconPack = entries.firstOrNull { it.id == id } ?: DEFAULT

        /** Splits a qualified slug into its pack and bare slug. Unknown/absent prefix → MDI. */
        fun parse(name: String?): Pair<IconPack, String> {
            val raw = name?.trim().orEmpty()
            val colon = raw.indexOf(':')
            if (colon > 0) {
                val prefix = raw.substring(0, colon).lowercase()
                val pack = entries.firstOrNull { it.id == prefix }
                if (pack != null) return pack to raw.substring(colon + 1)
            }
            return MDI to raw
        }

        /** Re-attaches a pack prefix to a bare slug. MDI stays bare for back-compat. */
        fun qualify(pack: IconPack, slug: String): String =
            if (pack == MDI) slug else "${pack.id}:$slug"
    }
}

/**
 * The user's chosen default icon pack, mirrored from `PreferencesManager.defaultIconPack`
 * so the icon picker can pre-select it when adding a *new* icon without threading the
 * preference through every call site. Updated once at app start (see MainActivity).
 */
object IconPreferences {
    @Volatile var defaultPack: IconPack = IconPack.DEFAULT
}

/** The bundled Material Design Icons webfont (`res/font/mdi_icons.ttf`). */
val MdiFontFamily: FontFamily = IconPack.MDI.fontFamily

/**
 * Loads and caches per-pack name→glyph and name→keyword tables from `assets/`.
 *
 * Each pack's codepoint table is parsed once on first use and kept in memory; its
 * keyword table is loaded lazily the first time the picker searches. Glyphs live in
 * the memory-mapped fonts, so rendering an icon never allocates a vector — it just
 * draws a character.
 */
object MdiIconStore {
    private class Tables {
        @Volatile var glyphs: Map<String, String>? = null   // slug -> glyph (1 codepoint)
        @Volatile var order: List<String>? = null           // slugs, alphabetical
        @Volatile var keywords: Map<String, String>? = null // slug -> "alias tag ..."
    }

    private val tables: Map<IconPack, Tables> = IconPack.entries.associateWith { Tables() }

    private fun ensureGlyphs(context: Context, pack: IconPack): Map<String, String> {
        val t = tables.getValue(pack)
        t.glyphs?.let { return it }
        return synchronized(t) {
            t.glyphs ?: run {
                val g = HashMap<String, String>(9000)
                val ord = ArrayList<String>(7500)
                context.applicationContext.assets.open(pack.codepointsAsset)
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
                t.order = ord
                t.glyphs = g
                g
            }
        }
    }

    private fun ensureKeywords(context: Context, pack: IconPack): Map<String, String> {
        val asset = pack.keywordsAsset ?: return emptyMap()
        val t = tables.getValue(pack)
        t.keywords?.let { return it }
        return synchronized(t) {
            t.keywords ?: run {
                val k = HashMap<String, String>(7000)
                context.applicationContext.assets.open(asset)
                    .bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val tab = line.indexOf('\t')
                            if (tab <= 0) continue
                            k[line.substring(0, tab)] = line.substring(tab + 1).lowercase()
                        }
                    }
                t.keywords = k
                k
            }
        }
    }

    /**
     * Resolves any pack-qualified slug to its glyph, accepting Home Assistant `mdi:`
     * prefixes, `si:` Simple Icons slugs, and legacy identifiers (see [LEGACY_ICON_MAP]).
     * Returns null for unknown names.
     */
    fun glyphOf(context: Context, name: String?): String? {
        if (name.isNullOrBlank()) return null
        val (pack, bare) = IconPack.parse(name)
        val g = ensureGlyphs(context, pack)
        val slug = bare.lowercase()
        g[slug]?.let { return it }
        if (pack == IconPack.MDI) {
            (LEGACY_ICON_MAP[bare] ?: LEGACY_ICON_MAP[slug])?.let { mapped -> g[mapped]?.let { return it } }
        }
        return null
    }

    /** All icon slugs in [pack], alphabetical. */
    fun allNames(context: Context, pack: IconPack = IconPack.MDI): List<String> {
        ensureGlyphs(context, pack)
        return tables.getValue(pack).order ?: emptyList()
    }

    /** Slugs in [pack] whose name or keywords contain [query] (case-insensitive). Empty query returns all. */
    fun search(context: Context, query: String, pack: IconPack = IconPack.MDI): List<String> {
        val ord = allNames(context, pack)
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ord
        val kw = ensureKeywords(context, pack)
        return ord.filter { it.contains(q) || (kw[it]?.contains(q) == true) }
    }

    /** Slugs tagged with the MDI category [tag] (e.g. "home automation", "weather"). MDI only. */
    fun byCategory(context: Context, tag: String): List<String> {
        val pack = IconPack.MDI
        val ord = allNames(context, pack)
        val kw = ensureKeywords(context, pack)
        val needle = "#" + tag.lowercase()
        return ord.filter { kw[it]?.contains(needle) == true }
    }
}

/**
 * Renders an icon by pack-qualified slug from the bundled webfonts — a drop-in for
 * `Icon(mdiIcon(name), …)`. `mdi:foo`/bare slugs use Material Design Icons; `si:foo`
 * uses Simple Icons. Sizing is via [size] (not `Modifier.size`, since the glyph is
 * drawn as text). Unknown/blank names fall back to MDI `lightbulb`.
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
    // Resolve pack + glyph together: on an unknown/blank name fall back to MDI `lightbulb`,
    // so the rendered font always matches the glyph that was actually found.
    val (pack, glyph) = remember(name) {
        val found = MdiIconStore.glyphOf(context, name)
        if (found != null) IconPack.parse(name).first to found
        else IconPack.MDI to MdiIconStore.glyphOf(context, "lightbulb").orEmpty()
    }
    val fontFamily = pack.fontFamily
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
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
