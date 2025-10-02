package com.example.eeum.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AirConditionerTemperatureControl(
    currentTemperature: Int,
    onTemperatureChange: (Int) -> Unit,
    onApply: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var temperature by remember(currentTemperature) { mutableIntStateOf(currentTemperature) }

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
                text = "에어컨 온도 설정",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // 온도 표시
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
                    Text(
                        text = "${temperature}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }

            // 온도 조절 버튼들
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 온도 감소 버튼 (왼쪽)
                RectangleIconButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    enabled = temperature > 18,
                    modifier = Modifier.weight(1f)
                ) {
                    if (temperature > 18) {
                        temperature--
                        onTemperatureChange(temperature)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 온도 증가 버튼 (오른쪽)
                RectangleIconButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    enabled = temperature < 30,
                    modifier = Modifier.weight(1f)
                ) {
                    if (temperature < 30) {
                        temperature++
                        onTemperatureChange(temperature)
                    }
                }
            }
        }
    }
}

@Composable
private fun RectangleIconButton(
    icon: ImageVector,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) Color(0xFF3B82F6) else Color.Gray,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled) { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Preview
@Composable
private fun AirConditionerTemperatureControlPreview() {
    AirConditionerTemperatureControl(
        currentTemperature = 24,
        onTemperatureChange = {},
        onApply = {},
        onCancel = {}
    )
}
