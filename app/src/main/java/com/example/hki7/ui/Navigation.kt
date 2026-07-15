package com.example.hki7.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.hki7.data.HKICustomPage

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    // When set, the bottom bar renders this MDI glyph instead of [icon] (see MdiIcon).
    val mdiIcon: String? = null
) {
    companion object {
        const val CUSTOM_PAGE_ROUTE = "custom_page/{pageId}"
    }
    object Home     : Screen("home",     "Home",     Icons.Default.Home)
    object Rooms    : Screen("rooms",    "Rooms",    Icons.AutoMirrored.Filled.ViewQuilt)
    object Security : Screen("security", "Security", Icons.Default.Security)
    object Energy   : Screen("energy",   "Energy",   Icons.Default.ElectricBolt)
    object Climate  : Screen("climate",  "Climate",  Icons.Default.Thermostat, mdiIcon = "thermostat")
    object Battery  : Screen("battery",  "Battery",  Icons.Default.BatteryAlert)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data class Custom(val page: HKICustomPage) : Screen(
        route = "custom_page/${page.id}",
        title = page.name,
        icon = Icons.Default.Dashboard,
        mdiIcon = page.icon
    )
    object RoomDetail : Screen("room_detail/{areaId}", "Room Detail", Icons.AutoMirrored.Filled.ViewQuilt) {
        fun createRoute(areaId: String) = "room_detail/$areaId"
    }
}

/**
 * Bottom-navigation-bar layout. [fixed] tabs (Home, Rooms) are always shown first and can be
 * neither reordered nor hidden; [configurable] tabs can be reordered and hidden from Settings →
 * Appearance → Navigation Bar. Order/visibility are persisted as route lists in PreferencesManager.
 */
/** Maps an [HKIAction] navigation target to a NavController route, or null if unknown.
 *  Targets: "home"|"rooms"|"security"|"energy"|"climate"|"battery"|"settings"|"room:<areaId>". */
fun navRouteForTarget(target: String): String? = when {
    target == "home" -> Screen.Home.route
    target == "rooms" -> Screen.Rooms.route
    target == "security" -> Screen.Security.route
    target == "energy" -> Screen.Energy.route
    target == "climate" -> Screen.Climate.route
    target == "battery" -> Screen.Battery.route
    target == "settings" -> Screen.Settings.route
    target.startsWith("room:") -> Screen.RoomDetail.createRoute(target.removePrefix("room:"))
    else -> null
}

object NavBarConfig {
    val fixed: List<Screen> = listOf(Screen.Home, Screen.Rooms)
    val configurable: List<Screen> = listOf(Screen.Security, Screen.Energy, Screen.Climate, Screen.Battery)

    /** Configurable tabs in the saved order; unknown/legacy routes are dropped and new ones appended. */
    fun orderedConfigurable(savedOrder: List<String>, customPages: List<HKICustomPage> = emptyList()): List<Screen> {
        val available = configurable + customPages.map(Screen::Custom)
        val byRoute = available.associateBy { it.route }
        val ordered = savedOrder.mapNotNull { byRoute[it] }
        val rest = available.filter { it !in ordered }
        return ordered + rest
    }

    /** Tabs shown in the bottom bar: the fixed pair followed by the visible configurable tabs. */
    fun visibleTabs(savedOrder: List<String>, hidden: List<String>, customPages: List<HKICustomPage> = emptyList()): List<Screen> {
        val hiddenSet = hidden.toSet()
        return fixed + orderedConfigurable(savedOrder, customPages).filter { it.route !in hiddenSet }
    }
}
