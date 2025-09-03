package com.example.eeum.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton as M2FAB
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon as M2Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EeumFloatingActionButton(
    onClick: () -> Unit
) {
    M2FAB(
        onClick = onClick,
        shape = CircleShape,
        backgroundColor = Color(0xFF6BB6FF),
        contentColor = Color.Black,
        elevation = FloatingActionButtonDefaults.elevation(12.dp)
    ) {
        M2Icon(Icons.Filled.FavoriteBorder, contentDescription = "음성인식")
    }
}