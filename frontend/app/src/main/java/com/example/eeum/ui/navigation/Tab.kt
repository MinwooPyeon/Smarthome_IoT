package com.example.eeum.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.eeum.R

enum class Tab(
    val route: String,
    val label: String,
    val getIcon: @androidx.compose.runtime.Composable () -> ImageVector
) {
    Home("home", "홈", { Icons.Outlined.Home }),
    Device("device", "디바이스", { ImageVector.vectorResource(R.drawable.menu_icon_device) }),
    Use("use", "사용량", { ImageVector.vectorResource(R.drawable.menu_icon_usage) }),
    Menu("menu", "메뉴", { Icons.Outlined.Menu })
}
