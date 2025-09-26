package com.example.eeum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon as M2Icon
import androidx.compose.material.Text as M2Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import com.example.eeum.ui.navigation.Tab
import com.example.eeum.ui.navigation.isOnRoute

@Composable
fun EeumBottomAppBar(
    currentDestination: NavDestination?,
    onTabClick: (String) -> Unit
) {
    BottomAppBar(
        cutoutShape = CircleShape,
        backgroundColor = Color.White,
        contentColor = Color.Gray,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌측 2개
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tab.entries.take(2).forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = currentDestination.isOnRoute(tab.route),
                        onClick = { onTabClick(tab.route) }
                    )
                }
            }

            Spacer(Modifier.width(72.dp))

            // 우측 2개
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tab.entries.drop(2).forEach { tab ->
                    BottomNavItem(
                        tab = tab,
                        selected = currentDestination.isOnRoute(tab.route),
                        onClick = { onTabClick(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selColor = MaterialTheme.colorScheme.primary
    val dimColor = Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .widthIn(min = 56.dp)
            .clickable(onClick = onClick)
    ) {
        M2Icon(
            imageVector = tab.getIcon(),
            contentDescription = tab.label,
            tint = if (selected) selColor else dimColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        M2Text(
            text = tab.label,
            fontSize = 12.sp,
            color = if (selected) selColor else dimColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}