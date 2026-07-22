package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.jimz011apps.hki7.data.*

/** App-wide corner radius for dashboard buttons, cards, stacks, rooms, and widgets. */
val LocalItemCornerRadius = staticCompositionLocalOf { 20 }

@Composable
fun itemCornerShape() = RoundedCornerShape(LocalItemCornerRadius.current.dp)

/** Applies the global radius without rewriting persisted dashboard data. */
fun HKIRoomWidget.withGlobalCornerRadius(radius: Int): HKIRoomWidget = when (this) {
    is HKIButtonStack -> copy(cornerRadius = radius)
    is HKISwipingStack -> copy(cornerRadius = radius, widgets = widgets.map { it.withGlobalCornerRadius(radius) })
    is HKIEmptyStack -> copy(cornerRadius = radius, widgets = widgets.map { it.withGlobalCornerRadius(radius) })
    is HKISingleEntityWidget -> copy(cornerRadius = radius)
    is HKIEnergyCardWidget -> copy(cornerRadius = radius)
    is HKIEnergyStack -> copy(cornerRadius = radius)
    is HKIClimateCardWidget -> copy(cornerRadius = radius)
    is HKIClimateStack -> copy(cornerRadius = radius)
    is HKIMediaPlayerWidget -> copy(cornerRadius = radius)
    is HKISensorGraphWidget -> copy(cornerRadius = radius)
    is HKISensorGraphStack -> copy(cornerRadius = radius)
    is HKIMarkdownWidget -> copy(cornerRadius = radius)
    is HKIWeatherWidget -> copy(cornerRadius = radius)
    is HKICalendarWidget -> copy(cornerRadius = radius)
    is HKIWasteCollectionWidget -> copy(cornerRadius = radius)
    is HKIBatteryCardWidget -> copy(cornerRadius = radius)
    is HKIParcelsWidget -> copy(cornerRadius = radius)
    is HKISubtitleWidget -> this
}
