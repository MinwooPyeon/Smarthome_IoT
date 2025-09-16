package com.example.eeum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.base.ApplicationClass
import com.example.eeum.data.model.response.floorplans.FloorPlansList
import com.example.eeum.ui.theme.EeumTheme

private val BrandBlue = Color(0xFF007AFF)

@Composable
fun SelectFloorplanScreen(
    addressId: Int,
    onBack: () -> Unit = {},
    // 등록 성공 시 홈으로 이동하는 콜백
    onNavigateHome: () -> Unit = {},
    vm: FloorplansViewModel = viewModel()
) {
    val floorplans by vm.floorplans.observeAsState(initial = emptyList<FloorPlansList>())
    val registeredHomeId by vm.registeredHomeId.observeAsState()
    val status by vm.status.observeAsState(initial = "")
    val error by vm.error.observeAsState()

    // 진입 시 해당 addressId의 평면도 조회
    LaunchedEffect(addressId) { vm.getFloorplans(addressId) }

    // 선택 상태는 인덱스가 아니라 floorplanId로 유지 (리스트 순서/갱신 안정)
    var selectedFloorplanId by remember { mutableStateOf<Int?>(null) }

    // floorplans 갱신 시 선택 보정
    LaunchedEffect(floorplans) {
        if (floorplans.isNotEmpty()) {
            if (selectedFloorplanId == null ||
                floorplans.none { it.floorplanId == selectedFloorplanId }
            ) {
                selectedFloorplanId = floorplans.first().floorplanId
            }
        } else {
            selectedFloorplanId = null
        }
    }

    val selected: FloorPlansList? =
        floorplans.firstOrNull { it.floorplanId == selectedFloorplanId }

    val ctx = LocalContext.current
    val absoluteUrl = remember(selected?.imageUrl) {
        toAbsoluteUrl(ApplicationClass.SERVER_URL, selected?.imageUrl)
    }

    // 등록 성공 감지 → 한 번만 홈으로 이동
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(registeredHomeId, status) {
        if (!navigated && registeredHomeId != null) {
            navigated = true
            onNavigateHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp, bottom = 50.dp)
    ) {
        // 상단바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "평면도 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Text(
            text = "평면도를 선택해주세요.",
            fontSize = 16.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 아파트명
        Text(
            text = selected?.homeName ?: (floorplans.firstOrNull()?.homeName ?: "아파트명"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 평면도 Chip 리스트
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(
                items = floorplans,
                key = { it.floorplanId }
            ) { item ->
                FloorplanChip(
                    label = "${trimZero(item.square)}㎡",
                    selected = item.floorplanId == selectedFloorplanId,
                    onClick = { selectedFloorplanId = item.floorplanId }
                )
            }
        }

        // 도면 이미지
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(absoluteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "평면도",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BrandBlue
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(text = "이전", fontSize = 14.sp)
            }

            // 다음: 선택된 Chip의 homeId로 등록
            val isNextEnabled = selected != null
            Button(
                onClick = { selected?.let { vm.registerFloorplan(it.homeId) } },
                enabled = isNextEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNextEnabled) BrandBlue else Color(0xFFB0BEC5),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "다음", fontSize = 14.sp)
            }
        }
    }
}

private fun trimZero(d: Double): String {
    val s = d.toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

@Composable
private fun FloorplanChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandBlue else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) Color.White else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun toAbsoluteUrl(base: String, path: String?): String? {
    if (path.isNullOrBlank()) return null
    val b = base.trimEnd('/')
    val p = path.trim()
    if (p.startsWith("http://") || p.startsWith("https://")) return p
    return if (p.startsWith("/")) "$b$p" else "$b/$p"
}

@Preview(showBackground = true)
@Composable
private fun SelectFloorplanScreenPreview() {
    EeumTheme(dynamicColor = false) {
        // 프리뷰 더미
        SelectFloorplanScreen(addressId = 1)
    }
}
