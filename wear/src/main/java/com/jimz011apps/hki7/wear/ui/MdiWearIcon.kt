package com.jimz011apps.hki7.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.jimz011apps.hki7.wear.R
import java.io.IOException

/**
 * Draws HKI 7's icons on the watch, from the same Material Design Icons webfont the phone uses.
 *
 * Only the MDI pack ships here. The phone additionally bundles Simple Icons, Tabler and Phosphor,
 * which together are about five more megabytes — too much to put on a watch for icons that appear
 * on a handful of shortcuts. An icon from one of the other packs falls back to a generic one
 * rather than shipping the fonts; MDI covers the overwhelming majority of what people pick.
 */
private val MdiFontFamily = FontFamily(Font(R.font.mdi_icons))

/** Fallback when a slug is blank, unknown, or belongs to a pack the watch does not carry. */
private const val FALLBACK_SLUG = "lightbulb"

private object WearGlyphs {
    @Volatile
    private var table: Map<String, String>? = null

    fun of(context: android.content.Context, slug: String?): String {
        val glyphs = ensure(context)
        val bare = slug.orEmpty().trim().substringAfterLast(':').lowercase()
        return glyphs[bare] ?: glyphs[FALLBACK_SLUG] ?: "?"
    }

    private fun ensure(context: android.content.Context): Map<String, String> {
        table?.let { return it }
        return synchronized(this) {
            table ?: run {
                val parsed = HashMap<String, String>(9000)
                try {
                    context.applicationContext.assets.open("mdi_codepoints.txt")
                        .bufferedReader().useLines { lines ->
                            for (line in lines) {
                                val space = line.indexOf(' ')
                                if (space <= 0) continue
                                val codepoint = line.substring(space + 1).trim().toIntOrNull(16)
                                    ?: continue
                                parsed[line.substring(0, space)] =
                                    String(Character.toChars(codepoint))
                            }
                        }
                } catch (_: IOException) {
                    // An unreadable asset should cost an icon, not the whole screen.
                }
                parsed.also { table = it }
            }
        }
    }
}

/**
 * Renders an icon by pack-qualified slug. Sizing is via [size], not `Modifier.size`, because the
 * glyph is drawn as text — the same rule as the phone's `MdiIcon`.
 */
@Composable
fun MdiWearIcon(
    slug: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 22.dp,
) {
    val context = LocalContext.current
    val glyph = remember(slug) { WearGlyphs.of(context, slug) }
    Text(
        text = glyph,
        modifier = modifier,
        color = tint,
        style = TextStyle(fontFamily = MdiFontFamily, fontSize = size.value.sp),
    )
}
