package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Material slider whose visual pieces follow the dashboard's global corner-roundness setting.
 *
 * Material 3 otherwise uses its own fixed shape tokens for the thumb and track, so changing item
 * roundness can leave sliders in dialogs visibly out of step with the surrounding controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HKISlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = itemCornerShape()

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            // Draw the handle with our shape directly. Clipping Material's stock 4dp-wide,
            // fully-rounded handle cannot make its transparent corners less round.
            Spacer(
                Modifier
                    .size(width = 24.dp, height = 32.dp)
                    .clip(shape)
                    .background(if (enabled) colors.thumbColor else colors.disabledThumbColor)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.clip(shape),
                colors = colors,
                enabled = enabled
            )
        }
    )
}
