package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eeum.R
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.util.SharedPreferencesUtil

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)
private val IconBg = Color(0xFFEAF2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSecondScreen(
    navController: NavController,
    vm: RoutineViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val prefs = remember { SharedPreferencesUtil(ctx) }

    // SharedPreferences 선택된 homeId
    val homeId = prefs.getSelectedHomeId() ?: -1

    // ViewModel rooms, devices 구독
    val rooms by vm.rooms.observeAsState(emptyList())
    val devices by vm.devices.observeAsState(emptyList())

    var selectedRoomIdx by remember { mutableIntStateOf(-1) }
    var selectedDeviceIdx by remember { mutableIntStateOf(-1) }

    // 값 입력 여부(사용자가 직접 선택/조작했는지) 플래그들
    var stateChosen by remember { mutableStateOf(false) }
    var windChosen by remember { mutableStateOf(false) }
    var acTempChosen by remember { mutableStateOf(false) }

    // 실제 값들
    var selectedStateIdx by remember { mutableIntStateOf(-1) } // -1: 아직 미선택
    var windLevel by remember { mutableIntStateOf(2) }         // 1..5
    var acTemp by remember { mutableIntStateOf(24) }           // 16..30

    // 진입 시 rooms 요청
    LaunchedEffect(homeId) {
        if (homeId != -1) {
            vm.fetchRooms(homeId)
        }
    }

    // 방 선택 시 해당 roomName으로 디바이스 요청 + 디바이스 선택 초기화
    LaunchedEffect(selectedRoomIdx) {
        if (selectedRoomIdx in rooms.indices) {
            val roomName = rooms[selectedRoomIdx].roomName
            vm.fetchDevicesSimple(roomName)
            selectedDeviceIdx = -1
            // 디바이스가 바뀌므로 종속 값/플래그 초기화
            stateChosen = false
            windChosen = false
            acTempChosen = false
            selectedStateIdx = -1
        }
    }

    val selectedDeviceType: String? = devices.getOrNull(selectedDeviceIdx)?.deviceType?.toString()
    val features = remember(selectedDeviceIdx, devices) { featuresFor(selectedDeviceType) }

    // 저장 버튼 활성화 조건
    val canSave = (selectedRoomIdx >= 0) &&
            (selectedDeviceIdx >= 0) &&
            (!features.showState || stateChosen) &&
            (!features.showWind || windChosen) &&
            (!features.showAcTemp || acTempChosen)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
                Text("루틴 만들기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
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
                    if (rooms.isEmpty()) {
                        HintText("집의 방 정보를 불러오지 못했어요.")
                    }
                }
            }

            // 디바이스 선택
            SectionCard(title = "디바이스 선택") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    devices.forEachIndexed { idx, item: DeviceItem ->
                        val iconRes = iconResForDevice(item.deviceType)
                        RadioListRow(
                            title = item.deviceType.toString(),
                            iconVector = null,
                            iconResId = iconRes,
                            selected = selectedDeviceIdx == idx,
                            onClick = {
                                selectedDeviceIdx = idx
                                // 디바이스가 바뀌면 종속 값/플래그 초기화
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

            // ✅ 디바이스 선택 전에는 아래 섹션 안 보임
            if (selectedDeviceIdx >= 0) {
                // 상태 설정
                if (features.showState) {
                    SectionCard(title = "상태 설정") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                StateItem("ON", "켜기"),
                                StateItem("OFF", "끄기")
                            ).forEachIndexed { idx, item ->
                                RadioListRowWithChip(
                                    chipText = item.chip, title = item.title,
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

                // 바람 세기
                if (features.showWind) {
                    SectionCard(title = "바람 세기") {
                        SegmentedNumberSelector(
                            count = 5,
                            selected = if (windChosen) windLevel else 0, // 0이면 아무것도 선택 안 된 상태처럼 보이게
                            onSelect = {
                                windLevel = it
                                windChosen = true
                            }
                        )
                        if (!windChosen) HintText("세기를 선택하세요.")
                    }
                }

                // 에어컨 온도
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

                // ✅ 동작 저장 버튼 (모든 값 입력 시에만 활성)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        // TODO: 실제 저장/전송 로직 연결 (예: ViewModel API 호출 또는 이전 화면으로 결과 전달)
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
private fun SegmentedNumberSelector(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit
) {
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
                    Text(
                        "$i",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

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

// deviceType → icon 매핑
private fun iconResForDevice(deviceType: Any?): Int? {
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

// 디바이스 타입별 표시할 섹션 결정
private data class Features(val showState: Boolean, val showWind: Boolean, val showAcTemp: Boolean)
private fun featuresFor(deviceType: String?): Features {
    val key = deviceType?.trim()?.lowercase() ?: return Features(false, false, false)
    return when (key) {
        "조명" -> Features(showState = true, showWind = false, showAcTemp = false)
        "에어컨" -> Features(showState = true, showWind = true, showAcTemp = true)
        "공기청정기" -> Features(showState = true, showWind = true, showAcTemp = false)
        "선풍기" -> Features(showState = true, showWind = true, showAcTemp = false)
        "tv", "텔레비전" -> Features(showState = true, showWind = false, showAcTemp = false)
        "빔프로젝터" -> Features(showState = true, showWind = false, showAcTemp = false)
        else -> Features(showState = true, showWind = false, showAcTemp = false)
    }
}

// 힌트 텍스트
@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
}

@Preview
@Composable
private fun CreateRoutineSecondScreenPreview() {
    val nav = rememberNavController()
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        CreateRoutineSecondScreen(nav)
    }
}
