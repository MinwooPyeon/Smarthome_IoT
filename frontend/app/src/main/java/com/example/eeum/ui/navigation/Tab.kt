package com.example.eeum.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu

enum class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home("home", "홈", Icons.Outlined.Home),
    Device("device", "디바이스", Icons.Outlined.AddCircle),
    Use("use", "사용량", Icons.Outlined.DateRange),
    Menu("menu", "메뉴", Icons.Outlined.Menu)
}