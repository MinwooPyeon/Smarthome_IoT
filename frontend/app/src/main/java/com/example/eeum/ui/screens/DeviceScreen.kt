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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch
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

class DeviceListViewModel : ViewModel() {
    private val _items = androidx.lifecycle.MutableLiveData<List<DeviceResponse>>(emptyList())
    val items: androidx.lifecycle.LiveData<List<DeviceResponse>> get() = _items
    private val _loading = androidx.lifecycle.MutableLiveData(false)
    val loading: androidx.lifecycle.LiveData<Boolean> get() = _loading
    private val _error = androidx.lifecycle.MutableLiveData<String?>(null)
    val error: androidx.lifecycle.LiveData<String?> get() = _error

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { RetrofitUtil.deviceService.readDevices() }
                .onSuccess { res ->
                    if (res.isSuccessful) {
                        val body = res.body()
                        _items.value = body?.data?.items ?: emptyList()
                        _error.value = null
                    } else {
                        _error.value = "HTTP ${res.code()}"
                    }
                }
                .onFailure { e -> _error.value = e.message }
            _loading.value = false
        }
    }
}

@Composable
fun DeviceScreen(navController: NavController? = null) {
    // Activity 범위의 등록 ViewModel을 미리 획득하여 클릭 콜백에서 사용
    val activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    val regVm: DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)

    // Device 탭 자동 새로고침 신호 구독
    val mainEntry = remember(navController) {
        runCatching { navController?.getBackStackEntry("main_tabs") }.getOrNull()
    }
    val refreshLiveData = remember(mainEntry) {
        mainEntry?.savedStateHandle?.getLiveData<Long>("device_refresh")
    }
    val refreshKey by refreshLiveData?.observeAsState(0L) ?: remember { mutableStateOf(0L) }

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

        // 1) 더미 리스트 (기존 그대로 유지)
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

        // 1-1) 서버 목록(ViewModel) - 더미 리스트 아래에 추가
        val listVm: DeviceListViewModel = viewModel()
        val serverItems by listVm.items.observeAsState(emptyList())
        val loading by listVm.loading.observeAsState(false)
        val loadError by listVm.error.observeAsState()

        // 최초 진입 시 1회 로드 (원하시면 제거 가능)
        LaunchedEffect(Unit) { listVm.load() }
        // 자동 새로고침 신호 수신 시 서버 목록 재조회
        LaunchedEffect(refreshKey) {
            if (refreshKey != 0L) {
                android.widget.Toast.makeText(activity, "디바이스 목록을 새로고침했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                listVm.load()
            }
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

        // 2) 그리드 렌더링 (플러스 추가 카드 포함) - 더미 카드
        DeviceGrid(items = devices, showAddTile = true, onToggle = ::toggleDevice, onAddClick = {
            // 등록 초안 초기화 후 등록 플로우 진입
            regVm.resetDraft()
            navController?.navigate("device_registration")
        })

        Spacer(Modifier.height(24.dp))

        // 3) 서버 목록 렌더링 (카드 UI, 페이징/정렬/필터 없음)
        if (loading) {
            Text("서버 목록 불러오는 중...", color = Gray600)
        }
        loadError?.let { err ->
            if (err.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("오류: $err", color = Red500)
                    Button(
                        onClick = { listVm.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("다시 시도")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (serverItems.isNotEmpty()) {
            Text("서버 디바이스 목록", style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)), color = Gray800))
            Spacer(Modifier.height(8.dp))

            val serverUi = serverItems.map { deviceResponse ->
                // 방 이름 추출: deviceName의 첫 공백 이전 부분을 방 이름으로 간주
                // 예: "방1 에어컨" -> "방1", "거실 TV" -> "거실"
                val roomName = deviceResponse.deviceName.substringBefore(' ').ifBlank { "방" }
                
                // power 상태 체크 (deviceDetail의 power 필드가 Boolean인 경우)
                val isOn = runCatching {
                    val powerEl = deviceResponse.deviceDetail.get("power")
                    powerEl?.asJsonPrimitive?.asBoolean ?: false
                }.getOrDefault(false)
                
                // 온도 정보가 있는 경우 (에어컨 등)
                val temperature = runCatching {
                    val tempEl = deviceResponse.deviceDetail.get("temperature")
                    tempEl?.asJsonPrimitive?.asInt
                }.getOrNull()
                
                // 상태 텍스트 결정
                val statusText = when {
                    !isOn -> "꺼짐"
                    temperature != null -> "${temperature}°C"
                    else -> "켜짐"
                }
                
                // 아이콘 색상 결정 (디바이스 타입별)
                val iconTint = when (deviceResponse.deviceType?.uppercase()) {
                    "AIR_CONDITIONER" -> if (isOn) Blue500 else Gray500
                    "TV", "BEAM_PROJECTOR" -> if (isOn) Red500 else Gray500
                    "FAN", "AIR_PURIFIER" -> if (isOn) Green500 else Gray500
                    "LIGHT" -> if (isOn) Yellow600 else Gray500
                    else -> Gray500
                }
                
                DeviceUi(
                    id = deviceResponse.deviceId.toString(),
                    title = deviceResponse.deviceName,
                    room = roomName,
                    statusText = statusText,
                    iconRes = iconResForType(deviceResponse.deviceType),
                    statusIconRes = if (isOn) R.drawable.ic_device_on else R.drawable.ic_device_off,
                    iconTint = iconTint,
                    isLarge = false
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(serverUi) { deviceUi ->
                    DeviceCardSmall(
                        title = deviceUi.title,
                        room = deviceUi.room,
                        status = deviceUi.statusText,
                        iconRes = deviceUi.iconRes,
                        statusIconRes = deviceUi.statusIconRes,
                        iconTint = deviceUi.iconTint,
                        onToggle = { /* 서버 기기 토글은 범위 외 */ }
                    )
                }
            }
        }
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

private fun iconResForType(type: String?): Int = when (type?.uppercase()) {
    "AIR_CONDITIONER" -> R.drawable.ic_air_conditioning
    "FAN" -> R.drawable.ic_electric_fan
    "TV" -> R.drawable.ic_television
    "BEAM_PROJECTOR" -> R.drawable.ic_beam_projector
    "AIR_PURIFIER" -> R.drawable.ic_air_purifier
    "LIGHT" -> R.drawable.ic_light
    else -> R.drawable.ic_device
}

@Preview
@Composable
private fun DeviceScreenPreview() {
    EeumTheme(dynamicColor = false) {
        DeviceScreen()
    }
}
