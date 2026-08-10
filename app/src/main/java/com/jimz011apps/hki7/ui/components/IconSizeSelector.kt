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
 * Stack-level appearance every button in the stack inherits, so a stack of twenty buttons does not
 * need the same thing set twenty times. The size is a default a single button can still override.
 *
 * The icon-only switch that used to live here is now the stack's Compact button type, so this only
 * appears once that is chosen — [isCompact] is the caller saying so.
 */
@Composable
fun StackChildAppearance(
    isCompact: Boolean,
    childIconSize: Int,
    onChildIconSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isCompact) return
    Column(modifier.fillMaxWidth()) {
        IconSizeSelector(
            value = childIconSize,
            onValueChange = onChildIconSizeChange,
            label = stringResource(R.string.stack_child_icon_size)
        )
    }
}

/**
 * The three show/hide switches for a button's face, shared by a single button and by a stack
 * setting them for all its children.
 *
 * [title] and [description] differ between those two callers: on a stack these mask what every
 * child may show, which needs saying in the heading and not only in the small print underneath —
 * the switches look identical either way, so the label is the only thing that says whose buttons
 * are about to change.
 */
@Composable
fun ButtonElementSwitches(
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    showName: Boolean,
    onShowNameChange: (Boolean) -> Unit,
    showState: Boolean,
    onShowStateChange: (Boolean) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.button_elements),
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(
            Triple(R.string.button_element_icon, showIcon, onShowIconChange),
            Triple(R.string.button_element_name, showName, onShowNameChange),
            Triple(R.string.button_element_state, showState, onShowStateChange),
        ).forEach { (labelRes, checked, onChange) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = checked, onCheckedChange = onChange)
            }
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
