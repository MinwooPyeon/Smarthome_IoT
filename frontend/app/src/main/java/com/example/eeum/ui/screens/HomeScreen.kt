package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지가 없을 때도 안전하게
        val bgPainter = runCatching { painterResource(id = R.drawable.background) }.getOrNull()
        if (bgPainter != null) {
            Image(
                painter = bgPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FA))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Greeting("제니님!")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { /* TODO: settings */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF475569)
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: notifications */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "알림",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF475569)
                            )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            StatsRow()
            Spacer(Modifier.height(24.dp))
            FloorplanHeader(
                title = "우리 집 평면도",
                onAddClick = { /* TODO */ }
            )
            Spacer(Modifier.height(12.dp))
            FloorplanCard(onClick = { /* TODO: 평면도 추가/선택 */ })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Greeting(name: String) {
    Text(
        text = name,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0F172A)
    )
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "조명",
            subtitle = "3개 켜짐",
            iconResource = R.drawable.ic_light,
            tint = Color(0xFFFACC15),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "전력량",
            subtitle = "250kWh",
            iconResource = R.drawable.ic_energy,
            tint = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "활성 기기 수",
            subtitle = "4개 가동",
            iconResource = R.drawable.ic_device,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconResource: Int? = null,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                iconResource != null -> Icon(
                    painter = painterResource(id = iconResource),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun FloorplanHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )
        IconButton(onClick = onAddClick) {
            val movePainter = runCatching { painterResource(id = R.drawable.ic_move) }.getOrNull()
            if (movePainter != null) {
                Icon(painter = movePainter, contentDescription = "위치 변경")
            } else {
                Icon(Icons.Outlined.Add, contentDescription = "위치 변경") // 프리뷰 대체
            }
        }
    }
}

@Composable
private fun FloorplanCard(
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1A93C5FD))
        )

        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F7FA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, Color(0xFFE6EDF5))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "평면도 추가",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
