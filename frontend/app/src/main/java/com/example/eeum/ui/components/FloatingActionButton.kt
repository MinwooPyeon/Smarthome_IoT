package com.example.eeum.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton as M2FAB
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon as M2Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.eeum.R

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
        M2Icon(
            painter = painterResource(id = R.drawable.ic_mike),
            contentDescription = "음성인식",
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}
