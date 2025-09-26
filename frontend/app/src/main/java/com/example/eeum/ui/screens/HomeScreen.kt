package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.R
import com.example.eeum.base.ApplicationClass
import com.example.eeum.core.AppEffect
import com.example.eeum.core.AppEventBus
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.util.SharedPreferencesUtil
import com.example.eeum.util.PositionNormalizer
import com.example.eeum.util.RenderMetrics
import com.example.eeum.ui.screens.DeviceListViewModel
import com.example.eeum.ui.screens.EnergyViewModel
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenMap: () -> Unit = {},   // 카드 클릭과 동일 동작
    onAddHome: () -> Unit = {},
    vm: HomeViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
) {
    // 서버 데이터
    val homes by vm.homes.observeAsState(emptyList())
    val floorplans by vm.floorplans.observeAsState(emptyList())
    val devices by vm.devices.observeAsState(emptyList())
    val primaryHomeId by vm.primaryHomeId.observeAsState()
    val primaryHomeName by vm.primaryHomeName.observeAsState()

    // 사용자 정보
    val menuVm: MenuViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)
    val userInfo by menuVm.userInfo.observeAsState()

    // 통계 카드용 뷰모델들
    val lightsVm: DeviceListViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity, key = "stats_lights")
    val activeVm: DeviceListViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity, key = "stats_active")
    val energyVm: EnergyViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity, key = "stats_energy")

    val lightsOnCount by lightsVm.totalCount.observeAsState(0)
    val activeOnCount by activeVm.totalCount.observeAsState(0)
    val totalKwh by energyVm.totalKwh.observeAsState(0.0)

    // SharedPreferences 유틸
    val ctx = LocalContext.current
    val prefs = remember { com.example.eeum.util.SharedPreferencesUtil(ctx) }

    // 최초 진입 시 대표 집 및 집 목록 조회 + 사용자 정보 조회
    LaunchedEffect(Unit) {
        vm.fetchUserHomes()
        vm.fetchPrimaryHome() // fetchPrimaryHome에서 자동으로 평면도와 디바이스를 조회함
        menuVm.getUserInfo()

        // 통계용 데이터 로드
        lightsVm.load(power = true, type = "조명")
        activeVm.load(power = true)
    }

    // 선택된 집 이름
    var selectedHomeName by remember { mutableStateOf<String?>(null) }

    // 이동/드래그 제어 상태
    var isEditMode by remember { mutableStateOf(false) }         // 헤더 버튼(완료/이동) 표시용
    var isDragEnabled by remember { mutableStateOf(false) }      // 실제 드래그 가능 여부
    var showLocationAlert by remember { mutableStateOf(false) }

    // 완료 버튼 클릭 트리거 (FloorplanCard에 커밋 요청)
    var commitTrigger by remember { mutableStateOf(0) }

    // 드래그된 디바이스 위치 저장 (deviceId -> Offset)
    var draggedDevicePositions by remember { mutableStateOf<Map<String, IntOffset>>(emptyMap()) }

    // 대표집 정보가 업데이트될 때 selectedHomeName도 업데이트
    LaunchedEffect(primaryHomeName) {
        primaryHomeName?.let { name ->
            selectedHomeName = name
        }
    }

    // 집 목록이 비어있으면 평면도 초기화
    LaunchedEffect(homes) {
        if (homes.isEmpty()) {
            selectedHomeName = null
            vm.clearFloorplans()
        }
    }

    // 대표 집 변경 시 방 목록 조회 (roomColor -> roomId 매핑용)
    LaunchedEffect(primaryHomeId) {
        primaryHomeId?.let { vm.fetchRooms(it) }
        // 에너지 통계 로드 (homeId는 SharedPreferences -> 없으면 대표집 -> 1)
        val homeIdForEnergy = prefs.getSelectedHomeId() ?: primaryHomeId ?: 1
        val today = org.threeten.bp.LocalDate.now()
            .format(org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        energyVm.fetchEnergyTotalUsage(homeId = homeIdForEnergy, range = "day", date = today)
    }

    // 카드에 보여줄 이미지 URL
    val firstImageUrl = floorplans.firstOrNull()?.imageUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 60.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nickname = userInfo?.data?.nickname?.takeIf { it.isNotBlank() } ?: "제니"
            Greeting("${nickname}님!")

            Image(
                painter = painterResource(id = R.drawable.ic_alarm),
                contentDescription = "알림",
                colorFilter = ColorFilter.tint(Color(0xFF64748B)),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { /* TODO: notifications */ }
            )
        }

        Spacer(Modifier.height(12.dp))
        StatsRow(
            lightsOn = lightsOnCount,
            activeOn = activeOnCount,
            totalKwh = totalKwh
        )
        Spacer(Modifier.height(24.dp))

        FloorplanHeader(
            title = "우리 집 평면도",
            showMoveIcon = homes.isNotEmpty(),
            isEditMode = isEditMode,
            onEditModeToggle = {
                if (isEditMode) {
                    // 완료 버튼 클릭: FloorplanCard에 커밋 요청 → 서버 업데이트 후 드래그 비활성화
                    commitTrigger += 1
                    isEditMode = false
                    isDragEnabled = false
                } else {
                    // 이동 아이콘 클릭: 즉시 완료 버튼으로 전환, 드래그는 Alert 확인 후 활성화
                    isEditMode = true
                    showLocationAlert = true
                }
            }
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
                //  SharedPreferences에 즉시 저장
                prefs.setSelectedHomeId(home.homeId)
            },
            onAddNew = onOpenMap
        )

        Spacer(Modifier.height(12.dp))

        FloorplanCard(
            imageUrl = firstImageUrl,
            devices = devices,
            onCardClick = onOpenMap,
            dragEnabled = isDragEnabled,
            draggedPositions = draggedDevicePositions,
            commitSignal = commitTrigger,
onCommit = { metrics, offsets, bitmap ->
                val hId = primaryHomeId
                if (hId != null) {
                    val rooms = vm.rooms.value ?: emptyList()

                    fun colorIntToHex(argb: Int): String {
                        val r = (argb shr 16) and 0xFF
                        val g = (argb shr 8) and 0xFF
                        val b = (argb) and 0xFF
                        return String.format("#%02X%02X%02X", r, g, b)
                    }
                    fun sampleRoomIdFor(offset: IntOffset): Int? {
                        val bmp = bitmap ?: return null
                        // 스케일과 여백 계산
                        val scaleW = metrics.container.width.toFloat() / metrics.imageIntrinsic.width.toFloat()
                        val scaleH = metrics.container.height.toFloat() / metrics.imageIntrinsic.height.toFloat()
                        val scale = kotlin.math.min(scaleW, scaleH)
                        val drawnW = metrics.imageIntrinsic.width * scale
                        val drawnH = metrics.imageIntrinsic.height * scale
                        val leftMargin = (metrics.container.width - drawnW) / 2f
                        val topMargin = (metrics.container.height - drawnH) / 2f

                        val centerX = offset.x + metrics.iconSizePx / 2f
                        val centerY = offset.y + metrics.iconSizePx / 2f

                        if (centerX < leftMargin || centerX > leftMargin + drawnW || centerY < topMargin || centerY > topMargin + drawnH) {
                            return null
                        }
                        val bx = ((centerX - leftMargin) / scale).roundToInt().coerceIn(0, bmp.width - 1)
                        val by = ((centerY - topMargin) / scale).roundToInt().coerceIn(0, bmp.height - 1)
                        val argb = runCatching { bmp.getPixel(bx, by) }.getOrNull() ?: return null
                        val hex = colorIntToHex(argb)
                        val matched = rooms.firstOrNull { it.roomColor.equals(hex, ignoreCase = true) }
                        return matched?.roomId
                    }

                    val locationItems = offsets.mapNotNull { (deviceIdStr, off) ->
                        val deviceId = deviceIdStr.toIntOrNull() ?: return@mapNotNull null
                        val normalized = PositionNormalizer.normalizeOffset(off, metrics)
                        val roomId = sampleRoomIdFor(off)
                            ?: devices.firstOrNull { it.deviceId == deviceId }?.roomId
                            ?: 0
                        com.example.eeum.data.model.dto.device.LocationItem(
                            deviceId = deviceId,
                            homeId = hId,
                            roomId = roomId,
                            x = normalized.x,
                            y = normalized.y
                        )
                    }

                    if (locationItems.isNotEmpty()) {
                        vm.updateDeviceLocations(locationItems)
                    }
                }
            },
            onPositionChange = { deviceId, newOffset ->
                draggedDevicePositions = draggedDevicePositions + (deviceId to newOffset)
            }
        )

        Spacer(Modifier.height(16.dp))
    }

    // 위치 수정 Alert Dialog
    if (showLocationAlert) {
        AlertDialog(
            onDismissRequest = { showLocationAlert = false },
            title = {
                Text(
                    text = "디바이스 위치 수정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "디바이스의 위치를 드래그하여 자유롭게 바꿔주세요!",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationAlert = false
                        // 드래그 활성화는 확인 후 시작
                        isDragEnabled = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF007AFF)
                    )
                ) { Text("확인", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        )
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
        fontSize = 30.sp,
        fontFamily = FontFamily(Font(R.font.goormsansbold)),
        color = Color(0xFF1F2937)
    )
}

@Composable
private fun StatsRow(
    lightsOn: Int,
    activeOn: Int,
    totalKwh: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "조명",
            subtitle = "${lightsOn}개 켜짐",
            iconResource = R.drawable.ic_light,
            tint = Color(0xFFFACC15),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "전력량",
            subtitle = "${totalKwh}kWh",
            iconResource = R.drawable.ic_energy,
            tint = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "활성 기기 수",
            subtitle = "${activeOn}개 가동",
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
    icon: ImageVector? = null,
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
    showMoveIcon: Boolean,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit
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
            if (isEditMode) {
                TextButton(
                    onClick = onEditModeToggle,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF))
                ) { Text("완료", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_move),
                    contentDescription = "이동",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.clickable { onEditModeToggle() }
                )
            }
        }
    }
}

@Composable
private fun FloorplanCard(
    imageUrl: String?,
    devices: List<DeviceItem>,
    onCardClick: () -> Unit,
    dragEnabled: Boolean = false,
    draggedPositions: Map<String, IntOffset> = emptyMap(),
    commitSignal: Int = 0,
    onCommit: (RenderMetrics, Map<String, IntOffset>, Bitmap?) -> Unit = { _, _, _ -> },
    onPositionChange: (String, IntOffset) -> Unit = { _, _ -> }
) {
    val ctx = LocalContext.current
    val absoluteUrl = remember(imageUrl) { toAbsoluteUrl(ApplicationClass.SERVER_URL, imageUrl) }

    val clickableModifier = if (absoluteUrl.isNullOrBlank()) {
        Modifier.clickable { onCardClick() }
    } else Modifier

    val density = LocalDensity.current
    val iconSizeDp = 28.dp
    val iconSizePx = with(density) { iconSizeDp.toPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 이미지의 intrinsic 크기 (픽셀) 저장
    var imageIntrinsic by remember { mutableStateOf(IntSize.Zero) }

    // 샘플링용 비트맵 저장
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
                        .allowHardware(false) // 색상 샘플링 위해 소프트웨어 비트맵 사용
                        .build(),
                    contentDescription = "평면도",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val dw = state.result.drawable.intrinsicWidth
                        val dh = state.result.drawable.intrinsicHeight
                        if (dw > 0 && dh > 0) imageIntrinsic = IntSize(dw, dh)
                        imageBitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
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
                            val deviceId = item.deviceId.toString()

                            val finalOffset = if (draggedPositions.containsKey(deviceId)) {
                                // 이미 드래그된 적이 있으면 그 위치를 사용
                                draggedPositions[deviceId]!!
                            } else {
                                // 서버의 [0..1] 좌표를 표시 영역 좌표로 변환
                                val xPx = (item.x.toFloat() * drawnW)
                                val yPx = (item.y.toFloat() * drawnH)

                                val leftFromLeftPx = (leftMargin + xPx - iconSizePx / 2f)
                                    .coerceIn(0f, containerSize.width - iconSizePx)
                                val topFromTopPx = (topMargin + yPx - iconSizePx / 2f)
                                    .coerceIn(0f, containerSize.height - iconSizePx)

                                IntOffset(leftFromLeftPx.toInt(), topFromTopPx.toInt())
                            }

                            iconResForDevice(item.deviceType, item.deviceDetail.power)?.let { resId ->
                                var currentOffset by remember(deviceId) { mutableStateOf(finalOffset) }
                                LaunchedEffect(finalOffset) { currentOffset = finalOffset }

                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = item.deviceName,
                                    modifier = Modifier
                                        .size(iconSizeDp)
                                        .offset { currentOffset }
                                        .let { modifier ->
                                            if (dragEnabled) {
                                                modifier.pointerInput(deviceId) {
                                                    detectDragGestures(
                                                        onDragStart = { _ ->
                                                            if (!draggedPositions.containsKey(deviceId)) {
                                                                onPositionChange(deviceId, currentOffset)
                                                            }
                                                        },
                                                        onDrag = { _, dragAmount ->
                                                            val newOffset = IntOffset(
                                                                (currentOffset.x + dragAmount.x).toInt()
                                                                    .coerceIn(0, containerSize.width - iconSizePx.toInt()),
                                                                (currentOffset.y + dragAmount.y).toInt()
                                                                    .coerceIn(0, containerSize.height - iconSizePx.toInt())
                                                            )
                                                            currentOffset = newOffset
                                                            onPositionChange(deviceId, newOffset)
                                                        }
                                                    )
                                                }
                                            } else modifier
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 완료 버튼에서 커밋 요청이 들어오면 현재 오프셋을 부모로 전달
    LaunchedEffect(commitSignal) {
        if (commitSignal != 0 && imageIntrinsic.width > 0 && imageIntrinsic.height > 0 && containerSize.width > 0 && containerSize.height > 0) {
            val scaleW = containerSize.width.toFloat() / imageIntrinsic.width.toFloat()
            val scaleH = containerSize.height.toFloat() / imageIntrinsic.height.toFloat()
            val scale = minOf(scaleW, scaleH)
            val drawnW = imageIntrinsic.width * scale
            val drawnH = imageIntrinsic.height * scale
            val leftMargin = (containerSize.width - drawnW) / 2f
            val topMargin = (containerSize.height - drawnH) / 2f

            // 각 디바이스에 대한 최종 오프셋 계산 (드래그 우선, 없으면 서버 좌표)
            val offsets = devices.associate { item ->
                val deviceId = item.deviceId.toString()
                val off = draggedPositions[deviceId] ?: run {
                    val xPx = (item.x.toFloat() * drawnW)
                    val yPx = (item.y.toFloat() * drawnH)
                    val leftFromLeftPx = (leftMargin + xPx - iconSizePx / 2f)
                        .coerceIn(0f, containerSize.width - iconSizePx)
                    val topFromTopPx = (topMargin + yPx - iconSizePx / 2f)
                        .coerceIn(0f, containerSize.height - iconSizePx)
                    IntOffset(leftFromLeftPx.toInt(), topFromTopPx.toInt())
                }
                deviceId to off
            }
            val metrics = RenderMetrics(containerSize, imageIntrinsic, iconSizePx)
            onCommit(metrics, offsets, imageBitmap)
        }
    }
}

private fun iconResForDevice(deviceType: Any?, isPoweredOn: Boolean): Int? {
    val key = deviceType?.toString()?.trim()?.lowercase() ?: return null
    return when (key) {
        "에어컨" -> if (isPoweredOn) R.drawable.ic_icon_air_conditioning_on else R.drawable.ic_icon_air_conditioning
        "선풍기" -> if (isPoweredOn) R.drawable.ic_icon_electric_fan_on else R.drawable.ic_icon_electric_fan
        "텔레비전" -> if (isPoweredOn) R.drawable.ic_icon_television_on else R.drawable.ic_icon_television
        "빔프로젝터" -> if (isPoweredOn) R.drawable.ic_icon_beam_projector_on else R.drawable.ic_icon_beam_projector
        "공기청정기" -> if (isPoweredOn) R.drawable.ic_icon_air_purifier_on else R.drawable.ic_icon_air_purifier
        "조명" -> if (isPoweredOn) R.drawable.ic_icon_light_on else R.drawable.ic_icon_light
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
