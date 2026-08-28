package com.jimz011apps.hki7.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.jimz011apps.hki7.wear.ui.theme.HkiWearColors

/**
 * HKI 7's card, rebuilt for the watch.
 *
 * Not Wear Material3's `Chip`/`Card`: those carry Wear's own shape and colour language, and the
 * point of this app is that it looks like HKI 7. What is kept from Wear is the geometry — a full
 * width row, a large tap target, an icon leading — because that is what works on a round screen
 * and under a fingertip in motion, and no amount of brand styling should fight it.
 */
@Composable
fun HkiWearCard(
    title: String,
    subtitle: String?,
    iconSlug: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Active items lift to the elevated surface and tint their icon with the accent, which is how
    // the phone's entity cards read "on" at a glance.
    val background = if (active) HkiWearColors.Elevated else HkiWearColors.Surface
    val iconTint = if (active) HkiWearColors.Accent else HkiWearColors.OnMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HkiWearColors.CornerRadius))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            MdiWearIcon(iconSlug, tint = iconTint, size = 22.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = HkiWearColors.OnSurface,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = HkiWearColors.OnMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Section heading between groups of cards, matching the phone's subcategory rhythm. */
@Composable
fun HkiWearSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        color = HkiWearColors.OnMuted,
        style = MaterialTheme.typography.labelSmall,
    )
}

/** Centred explanatory text, for the empty and not-set-up states. */
@Composable
fun HkiWearMessage(text: String, modifier: Modifier = Modifier, color: Color = HkiWearColors.OnMuted) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        color = color,
        style = MaterialTheme.typography.bodySmall,
    )
}
