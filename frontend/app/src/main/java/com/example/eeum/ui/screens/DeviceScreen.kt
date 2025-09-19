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
    val hubVm: HubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)

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

        // 서버 목록(ViewModel)
        val listVm: DeviceListViewModel = viewModel()
        val serverItems by listVm.items.observeAsState(emptyList())
        val loading by listVm.loading.observeAsState(false)
        val loadError by listVm.error.observeAsState()
        
        // 허브 목록 상태 관찰
        val hubList by hubVm.hubList.observeAsState(emptyList())
        val hubLoading by hubVm.isLoading.observeAsState(false)
        val hubError by hubVm.error.observeAsState()

        // 최초 진입 시 1회 로드
        LaunchedEffect(Unit) { 
            listVm.load() 
            hubVm.getHubs() // 허브 목록도 로드
        }
        
        // 허브 등록 성공 시 허브 목록 재조회
        val userHomeId by hubVm.userHomeId.observeAsState()
        val registrationStatus by hubVm.registrationStatus.observeAsState()
        LaunchedEffect(userHomeId, registrationStatus) {
            if (userHomeId != null && registrationStatus == "success") {
                hubVm.getHubs() // 허브 등록 성공 후 목록 새로고침
            }
        }
        // 자동 새로고침 신호 수신 시 서버 목록 재조회
        LaunchedEffect(refreshKey) {
            if (refreshKey != 0L) {
                android.widget.Toast.makeText(activity, "디바이스 목록을 새로고침했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                listVm.load()
                hubVm.getHubs() // 허브 목록도 새로고침
            }
        }

        // 허브 데이터 + 서버 데이터 합치기
        val allDevices = remember(serverItems, hubList) {
            // 등록된 허브들을 DeviceUi로 변환
            val hubDevices = hubList.mapIndexed { index, hubId ->
                DeviceUi(
                    id = "hub_$hubId",
                    title = "허브 ${index + 1}",
                    room = "거실", // 또는 실제 방 정보가 있다면 사용
                    statusText = "켜짐",
                    iconRes = R.drawable.ic_hub,
                    statusIconRes = R.drawable.ic_device_on,
                    iconTint = Gray500,
                    isLarge = true
                )
            }
            
            val serverDevices = serverItems.map { deviceResponse ->
                // 방 이름 추출: deviceName의 첫 공백 이전 부분을 방 이름으로 간주
                val roomName = deviceResponse.deviceName.substringBefore(' ').ifBlank { "방" }
                
                // 디바이스 타입을 한국어로 변환
                android.util.Log.d("DeviceScreen", "deviceType from server: ${deviceResponse.deviceType}")
                val deviceTypeKorean = convertDeviceTypeToKorean(deviceResponse.deviceType)
                
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
                
                DeviceUi(
                    id = deviceResponse.deviceId.toString(),
                    title = deviceTypeKorean,  // 디바이스 타입(한국어)으로 변경
                    room = roomName,
                    statusText = statusText,
                    iconRes = iconResForType(deviceResponse.deviceType),
                    statusIconRes = if (isOn) R.drawable.ic_device_on else R.drawable.ic_device_off,
                    iconTint = Gray500, // 기본값 (사용하지 않지만 유지)
                    isLarge = false,
                    supportsTemperature = temperature != null
                )
            }
            
            // 허브들을 최상단에 배치, 그 다음에 서버 디바이스들
            hubDevices + serverDevices
        }

        if (loading || hubLoading) {
            val loadingText = when {
                loading && hubLoading -> "디바이스 및 허브 목록 불러오는 중..."
                loading -> "디바이스 목록 불러오는 중..."
                hubLoading -> "허브 목록 불러오는 중..."
                else -> "목록 불러오는 중..."
            }
            Text(loadingText, color = Gray600)
        } else {
            // 디바이스 로드 에러 처리
            loadError?.let { err ->
                if (err.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("디바이스 오류: $err", color = Red500)
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
            
            // 허브 로드 에러 처리
            hubError?.let { err ->
                if (err.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("허브 오류: $err", color = Red500)
                        Button(
                            onClick = { hubVm.getHubs() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("다시 시도")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            // 통합된 그리드 렌더링 (허브 + 서버 디바이스 + 추가 버튼)
            DeviceGrid(
                items = allDevices, 
                showAddTile = true, 
                onToggle = { /* TODO: 서버 API 토글 기능 */ }, 
                onAddClick = {
                    // 등록 초안 초기화 후 등록 플로우 진입
                    regVm.resetDraft()
                    navController?.navigate("device_registration")
                }
            )
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

private fun iconResForType(type: String?): Int {
    if (type == null) return R.drawable.ic_device
    
    return when (type) {
        // 영어 타입 (대소문자 구분 없이)
        "HUB", "hub" -> R.drawable.ic_hub
        "AIR_CONDITIONER", "air_conditioner", "Air_Conditioner" -> R.drawable.ic_air_conditioning
        "FAN", "fan", "Fan" -> R.drawable.ic_electric_fan
        "TV", "tv", "Tv" -> R.drawable.ic_television
        "BEAM_PROJECTOR", "beam_projector", "Beam_Projector" -> R.drawable.ic_beam_projector
        "AIR_PURIFIER", "air_purifier", "Air_Purifier" -> R.drawable.ic_air_purifier
        "LIGHT", "light", "Light" -> R.drawable.ic_light
        // 한국어 타입
        "허브" -> R.drawable.ic_hub
        "에어컨" -> R.drawable.ic_air_conditioning
        "선풍기" -> R.drawable.ic_electric_fan
        "텔레비전" -> R.drawable.ic_television
        "빔프로젝터" -> R.drawable.ic_beam_projector
        "공기청정기" -> R.drawable.ic_air_purifier
        "조명" -> R.drawable.ic_light
        else -> {
            android.util.Log.d("DeviceScreen", "Unknown deviceType for icon: $type")
            R.drawable.ic_device
        }
    }
}

private fun convertDeviceTypeToKorean(deviceType: String?): String {
    if (deviceType == null) return "디바이스"
    
    return when (deviceType) {
        // 영어 타입 (대소문자 구분 없이)
        "HUB", "hub" -> "허브"
        "AIR_CONDITIONER", "air_conditioner", "Air_Conditioner" -> "에어컨"
        "FAN", "fan", "Fan" -> "선풍기"
        "TV", "tv", "Tv" -> "텔레비전"
        "BEAM_PROJECTOR", "beam_projector", "Beam_Projector" -> "빔프로젝터"
        "AIR_PURIFIER", "air_purifier", "Air_Purifier" -> "공기청정기"
        "LIGHT", "light", "Light" -> "조명"
        // 한국어 타입 (이미 한국어인 경우 그대로 반환)
        "허브" -> "허브"
        "에어컨" -> "에어컨"
        "선풍기" -> "선풍기"
        "텔레비전" -> "텔레비전"
        "빔프로젝터" -> "빔프로젝터"
        "공기청정기" -> "공기청정기"
        "조명" -> "조명"
        else -> {
            android.util.Log.d("DeviceScreen", "Unknown deviceType: $deviceType")
            deviceType
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
