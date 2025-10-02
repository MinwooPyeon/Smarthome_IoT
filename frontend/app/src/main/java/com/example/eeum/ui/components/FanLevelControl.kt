package com.example.eeum.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FanLevelControl(
    currentLevel: Int,
    onLevelChange: (Int) -> Unit,
    onApply: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var level by remember(currentLevel) { mutableIntStateOf(currentLevel) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "바람 세기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // 현재 레벨 표시
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5F5),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = getLevelDescription(level),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }

            // 레벨 선택 버튼
            SegmentedLevelSelector(
                selectedLevel = level,
                onLevelSelect = { newLevel ->
                    level = newLevel
                    onLevelChange(newLevel)
                }
            )
        }
    }
}

@Composable
private fun SegmentedLevelSelector(
    selectedLevel: Int,
    onLevelSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (1..5).forEach { level ->
            val isSelected = level == selectedLevel
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF3B82F6) else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF3B82F6) else Color(0xFFE0E0E0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onLevelSelect(level) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$level",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

private fun getLevelDescription(level: Int): String {
    return when (level) {
        1 -> "초미풍"
        2 -> "미풍"
        3 -> "약풍"
        4 -> "강풍"
        5 -> "초강풍"
        else -> "설정 안됨"
    }
}

@Preview
@Composable
private fun FanLevelControlPreview() {
    FanLevelControl(
        currentLevel = 2,
        onLevelChange = {},
        onApply = {},
        onCancel = {}
    )
}
