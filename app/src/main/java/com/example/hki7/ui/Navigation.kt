package com.example.hki7.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Default.Home)
    object Rooms    : Screen("rooms",    "Rooms",    Icons.AutoMirrored.Filled.ViewQuilt)
    object Security : Screen("security", "Security", Icons.Default.Security)
    object Energy   : Screen("energy",   "Energy",   Icons.Default.ElectricBolt)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object RoomDetail : Screen("room_detail/{areaId}", "Room Detail", Icons.AutoMirrored.Filled.ViewQuilt) {
        fun createRoute(areaId: String) = "room_detail/$areaId"
    }
}
