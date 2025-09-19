package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.R
import com.example.eeum.base.ApplicationClass
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.ui.theme.EeumTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMap: () -> Unit = {},
    onAddHome: () -> Unit = {},
    vm: HomeViewModel = viewModel()
) {
    // 서버 데이터
    val homes by vm.homes.observeAsState(emptyList())
    val floorplans by vm.floorplans.observeAsState(emptyList())
    val devices by vm.devices.observeAsState(emptyList())

    // SharedPreferences 유틸
    val ctx = LocalContext.current
    val prefs = remember { com.example.eeum.util.SharedPreferencesUtil(ctx) }

    // 최초 진입 시 집 목록 조회
    LaunchedEffect(Unit) { vm.fetchUserHomes() }

    // 선택된 집 이름
    var selectedHomeName by remember { mutableStateOf<String?>(null) }

    // homes 갱신 시 초기 선택 & 평면도 자동 조회
    LaunchedEffect(homes) {
        if (homes.isNotEmpty()) {
            val initial = selectedHomeName?.let { n -> homes.find { it.homeName == n } } ?: homes.first()
            selectedHomeName = initial.homeName
            vm.selectHome(initial.homeId)
            vm.fetchDevicesIcon()
        } else {
            selectedHomeName = null
            vm.clearFloorplans()
        }
    }

    // 카드에 보여줄 이미지 URL
    val firstImageUrl = floorplans.firstOrNull()?.imageUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Greeting("제니님!")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-5).dp)
            ) {
                IconButton(onClick = { /* TODO: notifications */ }) {
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
            showMoveIcon = homes.isNotEmpty()
        )

        Spacer(Modifier.height(8.dp))

        HomeDropdown(
            selectedName = selectedHomeName,
            homes = homes,
            onSelect = { home ->
                // 선택 즉시 UI 반영 + 서버에 대표집 설정 + 로컬에 저장
                selectedHomeName = home.homeName
                vm.selectHome(home.homeId)
                vm.setPrimaryHome(home.homeId)
                vm.fetchDevicesIcon()
                // ✅ SharedPreferences에 즉시 저장
                prefs.setSelectedHomeId(home.homeId)
            },
            onAddNew = onOpenMap
        )

        Spacer(Modifier.height(12.dp))

        FloorplanCard(
            imageUrl = firstImageUrl,
            devices = devices,
            onCardClick = onOpenMap
        )

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDropdown(
    selectedName: String?,
    homes: List<Home>,
    onSelect: (Home) -> Unit,
    onAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && homes.isNotEmpty() }
    ) {
        TextField(
            value = selectedName ?: "집을 선택하세요",
            onValueChange = {},
            readOnly = true,
            label = { Text("집 선택") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            homes.forEach { home ->
                DropdownMenuItem(
                    text = { Text(home.homeName, fontSize = 14.sp) },
                    onClick = {
                        onSelect(home)
                        expanded = false
                    }
                )
            }

            Divider()

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "새 집 추가",
                        fontSize = 14.sp,
                        color = Color(0xFF007AFF)
                    )
                },
                onClick = {
                    expanded = false
                    onAddNew()
                }
            )
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
private fun FloorplanHeader(
    title: String,
    showMoveIcon: Boolean
) {
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
        if (showMoveIcon) {
            Icon(
                painter = painterResource(id = R.drawable.ic_move),
                contentDescription = "이동",
                tint = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun FloorplanCard(
    imageUrl: String?,
    devices: List<DeviceItem>,
    onCardClick: () -> Unit
) {
    val ctx = LocalContext.current
    val absoluteUrl = remember(imageUrl) { toAbsoluteUrl(ApplicationClass.SERVER_URL, imageUrl) }

    val clickableModifier = if (absoluteUrl.isNullOrBlank()) {
        Modifier.clickable { onCardClick() }
    } else Modifier

    val density = LocalDensity.current
    val iconSizeDp = 24.dp
    val iconSizePx = with(density) { iconSizeDp.toPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 이미지의 intrinsic 크기 (픽셀) 저장
    var imageIntrinsic by remember { mutableStateOf(IntSize.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .then(clickableModifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .onGloballyPositioned { coords -> containerSize = coords.size },
            contentAlignment = Alignment.Center
        ) {
            if (absoluteUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "추가",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                // 바닥 이미지
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(absoluteUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "평면도",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val dw = state.result.drawable.intrinsicWidth
                        val dh = state.result.drawable.intrinsicHeight
                        if (dw > 0 && dh > 0) imageIntrinsic = IntSize(dw, dh)
                    }
                )

                // 표시된 이미지(rect) 기준 픽셀로 변환
                Box(modifier = Modifier.matchParentSize()) {
                    if (imageIntrinsic.width > 0 && imageIntrinsic.height > 0 && containerSize.width > 0 && containerSize.height > 0) {
                        // Fit 스케일 계산
                        val scaleW = containerSize.width.toFloat() / imageIntrinsic.width.toFloat()
                        val scaleH = containerSize.height.toFloat() / imageIntrinsic.height.toFloat()
                        val scale = minOf(scaleW, scaleH)

                        val drawnW = imageIntrinsic.width * scale
                        val drawnH = imageIntrinsic.height * scale

                        // 레터박스로 생긴 여백
                        val leftMargin = (containerSize.width - drawnW) / 2f
                        val topMargin  = (containerSize.height - drawnH) / 2f

                        devices.forEach { item ->
                            // 서버는 [0..1] 정규화 좌표 → 표시 영역으로 스케일
                            val xPx = (item.x.toFloat() * drawnW)
                            val yPx = (item.y.toFloat() * drawnH)

                            // 1사분면(y 위로 증가) → Compose(top-down)로 변환
                            val leftFromLeftPx = (leftMargin + xPx - iconSizePx / 2f)
                                .coerceIn(0f, containerSize.width - iconSizePx)
                            val topFromTopPx = (topMargin + (drawnH - yPx) - iconSizePx / 2f)
                                .coerceIn(0f, containerSize.height - iconSizePx)

                            iconResForDevice(item.deviceType)?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = item.deviceName,
                                    modifier = Modifier
                                        .size(iconSizeDp)
                                        .offset {
                                            IntOffset(
                                                leftFromLeftPx.toInt(),
                                                topFromTopPx.toInt()
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconResForDevice(deviceType: Any?): Int? {
    val key = deviceType?.toString()?.trim()?.lowercase() ?: return null
    return when (key) {
        "에어컨" -> R.drawable.ic_icon_air_conditioning
        "선풍기" -> R.drawable.ic_icon_electric_fan
        "텔레비전" -> R.drawable.ic_icon_television
        "빔프로젝터" -> R.drawable.ic_icon_beam_projector
        "공기청정기" -> R.drawable.ic_icon_air_purifier
        "조명" -> R.drawable.ic_icon_light
        else -> null
    }
}

private fun toAbsoluteUrl(base: String, path: String?): String? {
    if (path.isNullOrBlank()) return null
    val b = base.trimEnd('/')
    val p = path.trim()
    if (p.startsWith("http://") || p.startsWith("https://")) return p
    return if (p.startsWith("/")) "$b$p" else "$b/$p"
}

@Composable
@Preview(showBackground = true)
private fun HomeScreenPreview() {
    EeumTheme(dynamicColor = false) {
        HomeScreen()
    }
}
