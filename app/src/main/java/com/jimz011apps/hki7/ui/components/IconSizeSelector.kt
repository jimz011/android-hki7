package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.R
import com.jimz011apps.hki7.data.ICON_SIZE_AUTO

/**
 * Stack-level appearance that every button in the stack inherits, so a stack of twenty buttons
 * does not need the icon-only switch flipped twenty times. The size here is a default a single
 * button can still override; the icon-only switch is an override, because a button's own
 * `iconOnly = false` cannot be told apart from "never set".
 */
@Composable
fun StackChildAppearance(
    childIconOnly: Boolean,
    onChildIconOnlyChange: (Boolean) -> Unit,
    childIconSize: Int,
    onChildIconSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.stack_child_icon_only),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(R.string.stack_child_icon_only_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = childIconOnly, onCheckedChange = onChildIconOnlyChange)
        }
        if (childIconOnly) {
            IconSizeSelector(
                value = childIconSize,
                onValueChange = onChildIconSizeChange,
                label = stringResource(R.string.stack_child_icon_size)
            )
        }
    }
}

/** The offered glyph sizes, in dp. Auto is the first chip and stays the default. */
private val ICON_SIZE_OPTIONS = listOf(20, 24, 28, 34, 40, 48)

/**
 * Picks the glyph size for icon-only buttons. Auto derives it from the stack's column count, which
 * is what keeps a six-column popup readable without anyone touching this; the explicit sizes are
 * for when a particular button wants to be bigger or smaller than its neighbours.
 */
@Composable
fun IconSizeSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.button_icon_size)
) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.button_icon_size_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = value == ICON_SIZE_AUTO,
                onClick = { onValueChange(ICON_SIZE_AUTO) },
                label = { Text(stringResource(R.string.button_icon_size_auto)) }
            )
            ICON_SIZE_OPTIONS.forEach { size ->
                FilterChip(
                    selected = value == size,
                    onClick = { onValueChange(size) },
                    label = { Text(stringResource(R.string.button_icon_size_value, size)) }
                )
            }
        }
    }
}
