package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eeum.R
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.routine.RoomData
import com.example.eeum.ui.theme.*
import com.example.eeum.util.SharedPreferencesUtil

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)
private val IconBg = Color(0xFFEAF2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSecondScreen(
    navController: NavController,
    vm: RoutineViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val prefs = remember { SharedPreferencesUtil(ctx) }
    val homeId = prefs.getSelectedHomeId() ?: -1

    // ViewModel state
    val rooms by vm.rooms.observeAsState(emptyList())
    val devices by vm.devices.observeAsState(emptyList())

    var selectedRoomIdx by remember { mutableIntStateOf(-1) }
    var selectedDeviceIdx by remember { mutableIntStateOf(-1) }

    // 입력 완료 플래그
    var stateChosen by remember { mutableStateOf(false) }
    var windChosen by remember { mutableStateOf(false) }
    var acTempChosen by remember { mutableStateOf(false) }

    // 값
    var selectedStateIdx by remember { mutableIntStateOf(-1) } // 0=켜기, 1=끄기
    var windLevel by remember { mutableIntStateOf(2) }         // 1..5
    var acTemp by remember { mutableIntStateOf(24) }           // 16..30

    // 방 목록 로드
    LaunchedEffect(homeId) {
        if (homeId != -1) vm.fetchRooms(homeId)
    }

    // 방 선택 시 디바이스 로드 및 초기화
    LaunchedEffect(selectedRoomIdx) {
        if (selectedRoomIdx in rooms.indices) {
            vm.fetchDevicesSimple(rooms[selectedRoomIdx].roomName)
        } else {
            vm.clearDevices() // 없다면 뷰모델에 빈 리스트 세팅용 메서드 구현
        }
        selectedDeviceIdx = -1
        stateChosen = false
        windChosen = false
        acTempChosen = false
        selectedStateIdx = -1
    }

    val selectedDeviceType: String? = devices.getOrNull(selectedDeviceIdx)?.deviceType?.toString()
    val features = remember(selectedDeviceIdx, devices) { featuresFor(selectedDeviceType) }

    val canSave = (selectedRoomIdx >= 0) &&
            (selectedDeviceIdx >= 0) &&
            (!features.showState || stateChosen) &&
            (!features.showWind || windChosen) &&
            (!features.showAcTemp || acTempChosen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
    ) {
        // 헤더: 뒤로가기 + 중앙 타이틀
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_page_move_left),
                contentDescription = "뒤로가기",
                colorFilter = ColorFilter.tint(Gray800),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
            )
            Text(
                text = "루틴 만들기",
                color = Gray900,
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 방 선택
            SectionCard(title = "방 선택") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rooms.forEachIndexed { idx, room ->
                        RadioListRow(
                            title = room.roomName,
                            iconVector = Icons.Filled.LocationOn,
                            iconResId = null,
                            selected = selectedRoomIdx == idx,
                            onClick = { selectedRoomIdx = idx }
                        )
                    }
                    if (rooms.isEmpty()) HintText("집의 방 정보를 불러오지 못했어요.")
                }
            }

            // 디바이스 선택
            SectionCard(title = "디바이스 선택") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    devices.forEachIndexed { idx, item: DeviceItem ->
                        val iconRes = iconResForDevice(item.deviceType)
                        RadioListRow(
                            title = item.deviceType?.toString() ?: "알 수 없는 디바이스",
                            iconVector = null,
                            iconResId = iconRes,
                            selected = selectedDeviceIdx == idx,
                            onClick = {
                                selectedDeviceIdx = idx
                                // 종속값 초기화
                                stateChosen = false
                                windChosen = false
                                acTempChosen = false
                                selectedStateIdx = -1
                            }
                        )
                    }
                    if (selectedRoomIdx == -1) {
                        HintText("먼저 방을 선택하세요.")
                    } else if (devices.isEmpty()) {
                        HintText("이 방에 등록된 디바이스가 없습니다.")
                    }
                }
            }

            // 디바이스 선택 후 섹션들
            if (selectedDeviceIdx >= 0) {
                if (features.showState) {
                    SectionCard(title = "상태 설정") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                StateItem("ON", "켜기"),
                                StateItem("OFF", "끄기")
                            ).forEachIndexed { idx, item ->
                                RadioListRowWithChip(
                                    chipText = item.chip,
                                    title = item.title,
                                    selected = selectedStateIdx == idx,
                                    onClick = {
                                        selectedStateIdx = idx
                                        stateChosen = true
                                    }
                                )
                            }
                        }
                    }
                }

                if (features.showWind) {
                    SectionCard(title = "바람 세기") {
                        SegmentedNumberSelector(
                            count = 5,
                            selected = if (windChosen) windLevel else 0,
                            onSelect = {
                                windLevel = it
                                windChosen = true
                            }
                        )
                        if (!windChosen) HintText("세기를 선택하세요.")
                    }
                }

                if (features.showAcTemp) {
                    SectionCard(title = "에어컨 온도") {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("${acTemp}°C", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SquareIconButton(icon = Icons.Filled.KeyboardArrowUp) {
                                    if (acTemp < 30) acTemp++
                                    acTempChosen = true
                                }
                                SquareIconButton(icon = Icons.Filled.KeyboardArrowDown) {
                                    if (acTemp > 16) acTemp--
                                    acTempChosen = true
                                }
                            }
                            if (!acTempChosen) HintText("온도를 조절하여 설정해주세요.")
                        }
                    }
                }

                // 저장 버튼
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        val room: RoomData = rooms[selectedRoomIdx]
                        val device: DeviceItem = devices[selectedDeviceIdx]
                        val power = (selectedStateIdx == 0) // 0=켜기
                        val stateTitle = if (power) "켜기" else "끄기"

                        val roomP = RoomDataP(room.roomColor, room.roomId, room.roomName)
                        val deviceP = DeviceItemP(
                            brand = device.brand,
                            deviceId = device.deviceId,
                            deviceName = device.deviceName,
                            deviceType = device.deviceType.toString(),
                            irDeviceId = device.irDeviceId,
                            model = device.model,
                            registeredAt = device.registeredAt,
                            roomId = device.roomId,
                            x = device.x,
                            y = device.y
                        )
                        val payload = ActionAddedPayload(
                            room = roomP,
                            device = deviceP,
                            stateTitle = stateTitle,
                            power = power,
                            windLevel = if (features.showWind) windLevel else null,
                            acTemp = if (features.showAcTemp) acTemp else null
                        )

                        navController.previousBackStackEntry?.savedStateHandle?.set("new_action_full", payload)
                        navController.popBackStack()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBFD8FF),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text("동작 저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private data class StateItem(val chip: String, val title: String)

@Composable
private fun HintText(text: String) {
    Text(text = text, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Column(content = content)
        }
    }
}

@Composable
private fun RadioListRow(
    title: String,
    iconVector: ImageVector?,
    iconResId: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IconBg),
                contentAlignment = Alignment.Center
            ) {
                when {
                    iconVector != null -> Icon(iconVector, contentDescription = null, tint = TextBlue)
                    iconResId != null -> Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = TextBlue
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TextBlue, unselectedColor = BorderGray)
            )
        }
    }
}

@Composable
private fun RadioListRowWithChip(chipText: String, title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, TextBlue),
                color = Color.Transparent
            ) {
                Box(
                    Modifier
                        .height(28.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chipText, color = TextBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TextBlue, unselectedColor = BorderGray)
            )
        }
    }
}

@Composable
private fun SegmentedNumberSelector(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..count).forEach { i ->
            val isSelected = i == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) TextBlue else Color.White,
                border = BorderStroke(1.dp, if (isSelected) TextBlue else BorderGray),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onSelect(i) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$i", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else Color.Black)
                }
            }
        }
    }
}

/** ▷ SecondScreen 전용: 네모 버튼 (위/아래) */
@Composable
private fun SquareIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = TextBlue,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White) }
    }
}

// deviceType → icon 매핑 (public: FirstScreen에서도 사용 가능)
fun iconResForDevice(deviceType: Any?): Int? {
    val key = deviceType?.toString()?.trim()?.lowercase() ?: return null
    return when (key) {
        "에어컨" -> R.drawable.ic_air_conditioning
        "선풍기" -> R.drawable.ic_electric_fan
        "텔레비전", "tv" -> R.drawable.ic_television
        "빔프로젝터" -> R.drawable.ic_beam_projector
        "공기청정기" -> R.drawable.ic_air_purifier
        "조명" -> R.drawable.ic_light
        else -> null
    }
}

// 디바이스 타입별 노출 섹션
private data class Features(val showState: Boolean, val showWind: Boolean, val showAcTemp: Boolean)
private fun featuresFor(deviceType: String?): Features {
    val key = deviceType?.trim()?.lowercase() ?: return Features(false, false, false)
    return when (key) {
        "조명" -> Features(true, false, false)
        "에어컨" -> Features(true, true, true)
        "공기청정기" -> Features(true, true, false)
        "선풍기" -> Features(true, true, false)
        "tv", "텔레비전" -> Features(true, false, false)
        "빔프로젝터" -> Features(true, false, false)
        else -> Features(true, false, false)
    }
}

@Preview
@Composable
private fun CreateRoutineSecondScreenPreview() {
    val nav = rememberNavController()
    EeumTheme(dynamicColor = false) {
        CreateRoutineSecondScreen(nav)
    }
}
