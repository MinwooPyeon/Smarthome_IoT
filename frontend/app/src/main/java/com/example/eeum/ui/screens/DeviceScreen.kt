package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import com.example.eeum.ui.theme.*

@Composable
fun DeviceScreen(navController: NavController? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 상단 타이틀 (다른 화면과 일관)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "우리 집",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold)),
                    color = Gray800
                )
            )
        }

        Spacer(Modifier.height(40.dp))

        // 1) 그리드에 넣을 데이터 리스트 (상태 토글을 위해 가변 리스트로 보관)
        val devices = remember {
            mutableStateListOf(
                DeviceUi(
                    id = "hub",
                    title = "허브",
                    room = "거실",
                    statusText = "켜짐",
                    iconRes = R.drawable.ic_hub,
                    statusIconRes = R.drawable.ic_device_on,
                    iconTint = Gray500,
                    isLarge = true
                ),
                DeviceUi(
                    id = "tv",
                    title = "텔레비전",
                    room = "방1",
                    statusText = "꺼짐",
                    iconRes = R.drawable.ic_television,
                    statusIconRes = R.drawable.ic_device_off,
                    iconTint = Red500
                ),
                DeviceUi(
                    id = "ac",
                    title = "에어컨",
                    room = "거실",
                    statusText = "23°C",
                    iconRes = R.drawable.ic_air_conditioning,
                    statusIconRes = R.drawable.ic_device_on,
                    iconTint = Blue500,
                    supportsTemperature = true,
                    defaultTempC = 23
                )
            )
        }

        fun toggleDevice(id: String) {
            val index = devices.indexOfFirst { it.id == id }
            if (index < 0) return
            val d = devices[index]

            val newStatus: String
            val newIcon: Int

            if (d.supportsTemperature) {
                // 온도 지원 디바이스(예: 에어컨)
                if (d.statusText.endsWith("°C")) {
                    // 현재 켜짐(온도 표시) -> 끄기
                    newStatus = "꺼짐"
                    newIcon = R.drawable.ic_device_off
                } else {
                    // 현재 꺼짐 -> 기본 온도로 켜기
                    newStatus = "${d.defaultTempC}°C"
                    newIcon = R.drawable.ic_device_on
                }
            } else {
                // 일반 디바이스: 켜짐/꺼짐 토글
                val isOn = d.statusText == "켜짐"
                newStatus = if (isOn) "꺼짐" else "켜짐"
                newIcon = if (isOn) R.drawable.ic_device_off else R.drawable.ic_device_on
            }

            devices[index] = d.copy(statusText = newStatus, statusIconRes = newIcon)
        }

        // 2) 그리드 렌더링 (플러스 추가 카드 포함)
        DeviceGrid(items = devices, showAddTile = true, onToggle = ::toggleDevice, onAddClick = { navController?.navigate("device_registration") })
    }
}

@Composable
private fun DeviceCardLarge(
    title: String,
    room: String,
    status: String,
    iconRes: Int,
    statusIconRes: Int,
    iconTint: Color,
    onToggle: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1행: 좌측 디바이스 아이콘, 우측 on/off 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(24.dp)
                )
                Image(
                    painter = painterResource(id = statusIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggle() }
                )
            }
            Spacer(Modifier.height(8.dp))
            // 2행: 좌측 제목, 우측 상태 텍스트
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray800
                    )
                )
                Text(
                    text = status,
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500),
                    modifier = Modifier.clickable { onToggle() }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = room,
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500)
            )
        }
    }
}

@Composable
private fun DeviceCardSmall(
    title: String,
    room: String,
    status: String,
    iconRes: Int,
    statusIconRes: Int,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray50),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1행: 좌측 디바이스 아이콘, 우측 on/off 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(22.dp)
                )
                Image(
                    painter = painterResource(id = statusIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggle() }
                )
            }
            Spacer(Modifier.height(8.dp))
            // 2행: 좌측 제목, 우측 상태 텍스트
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                        color = Gray800
                    )
                )
                Text(
                    text = status,
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500),
                    modifier = Modifier.clickable { onToggle() }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = room,
                style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500)
            )
        }
    }
}

@Composable
private fun AddDeviceCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        border = BorderStroke(1.dp, Gray50),
        modifier = modifier
            .height(108.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = "추가",
                colorFilter = ColorFilter.tint(Gray300),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// 그리드 모델 및 컴포저블
private data class DeviceUi(
    val id: String,
    val title: String,
    val room: String,
    val statusText: String,
    val iconRes: Int,
    val statusIconRes: Int,
    val iconTint: Color,
    val isLarge: Boolean = false,
    val supportsTemperature: Boolean = false,
    val defaultTempC: Int = 23
)

@Composable
private fun DeviceGrid(
    items: List<DeviceUi>,
    modifier: Modifier = Modifier,
    showAddTile: Boolean = true,
    onToggle: (String) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { it.id },
            span = { item -> GridItemSpan(if (item.isLarge) 2 else 1) }
        ) { d ->
            if (d.isLarge) {
                DeviceCardLarge(
                    title = d.title,
                    room = d.room,
                    status = d.statusText,
                    iconRes = d.iconRes,
                    statusIconRes = d.statusIconRes,
                    iconTint = d.iconTint,
                    onToggle = { onToggle(d.id) }
                )
            } else {
                DeviceCardSmall(
                    title = d.title,
                    room = d.room,
                    status = d.statusText,
                    iconRes = d.iconRes,
                    statusIconRes = d.statusIconRes,
                    iconTint = d.iconTint,
                    onToggle = { onToggle(d.id) }
                )
            }
        }
        if (showAddTile) {
            item(span = { GridItemSpan(1) }) {
                Box(modifier = Modifier.clickable { onAddClick() }) { AddDeviceCard() }
            }
        }
    }
}

@Preview
@Composable
private fun DeviceScreenPreview() {
    EeumTheme(dynamicColor = false) {
        DeviceScreen()
    }
}
