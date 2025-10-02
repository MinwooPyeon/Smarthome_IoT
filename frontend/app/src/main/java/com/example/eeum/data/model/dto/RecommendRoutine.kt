package com.example.eeum.data.model.dto

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class RecommendRoutine(
    val icon: ImageVector,
    val iconBg: Color,
    val title: String,
    val description: String,
    val time: String
)
