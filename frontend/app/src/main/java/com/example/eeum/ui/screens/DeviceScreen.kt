package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eeum.R
import com.example.eeum.core.AppEffect
import com.example.eeum.core.AppEventBus
import com.example.eeum.ui.theme.*
import com.example.eeum.ui.components.AirConditionerTemperatureControl
import com.example.eeum.ui.components.DeviceDeleteDialog
import com.example.eeum.ui.components.FanLevelControl


@Composable
fun DeviceScreen(navController: NavController? = null) {
    // Activity 범위의 등록 ViewModel을 미리 획득하여 클릭 콜백에서 사용
    val activity = androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity
    val regVm: DeviceRegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val hubVm: HubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val statusVm: DeviceStatusViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    val homeVm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)

    // 새로고침 상태
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Device 탭 자동 새로고침 신호 구독
    val mainEntry = remember(navController) {
        runCatching { navController?.getBackStackEntry("main_tabs") }.getOrNull()
    }
    val refreshLiveData = remember(mainEntry) {
        mainEntry?.savedStateHandle?.getLiveData<Long>("device_refresh")
    }
    val refreshKey by refreshLiveData?.observeAsState(0L) ?: remember { mutableStateOf(0L) }

    // SwipeRefresh를 전체 화면에 적용
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize()
    ) {
        RefreshableContent(
            navController = navController,
            regVm = regVm,
            hubVm = hubVm,
            statusVm = statusVm,
            homeVm = homeVm,
            activity = activity,
            refreshKey = refreshKey,
            isRefreshing = isRefreshing,
            onRefreshComplete = { isRefreshing = false },
            modifier = Modifier.fillMaxSize(),
            showHeader = true
        )
    }
}

@Composable
private fun RefreshableContent(
    navController: NavController?,
    regVm: DeviceRegistrationViewModel,
    hubVm: HubViewModel,
    statusVm: DeviceStatusViewModel,
    homeVm: HomeViewModel,
    activity: androidx.activity.ComponentActivity,
    refreshKey: Long,
    isRefreshing: Boolean,
    onRefreshComplete: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = false
) {
    // 다이얼로그 상태 관리
    var showDialog by remember { mutableStateOf(false) }
    var selectedDeviceForControl by remember { mutableStateOf<Pair<Int, String>?>(null) } // deviceId, deviceType

    // 삭제 다이얼로그 상태 관리
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDeviceForDelete by remember { mutableStateOf<Int?>(null) }

    // 개별 디바이스 UI만 갱신하기 위한 로컬 오버라이드 (재구성 트리거 보장)
    var overrideCounter by remember { mutableIntStateOf(0) }
    val powerOverrides = remember { mutableStateMapOf<Int, Boolean>() }
    val tempOverrides = remember { mutableStateMapOf<Int, Int>() }
    val levelOverrides = remember { mutableStateMapOf<Int, Int>() }

    // 서버 목록(ViewModel)
    val listVm: DeviceListViewModel = viewModel()
    val serverItems by listVm.items.observeAsState(emptyList())
    val loading by listVm.loading.observeAsState(false)
    val loadError by listVm.error.observeAsState()

    // 기존 순서를 유지하고 새로운 디바이스를 뒤에 추가하기 위한 순서 리스트
    // SharedPreferences를 사용해서 순서를 영구 저장 (Activity 범위로 이동)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("device_order_prefs", android.content.Context.MODE_PRIVATE) }
    // Activity 범위에서 remember로 상태 유지 및 초기화 시 SharedPreferences로 복원
    val deviceOrder = remember(activity) {
        val savedOrder = prefs.getString("device_order", null)
        val initialOrder = if (!savedOrder.isNullOrEmpty()) {
            try {
                savedOrder.split(",").mapNotNull { it.toIntOrNull() }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        Log.d("DeviceOrder", "Initialize deviceOrder with: $initialOrder")
        mutableStateListOf<Int>().apply { addAll(initialOrder) }
    }

    // 서버 데이터와 순서 동기화
    LaunchedEffect(serverItems) {
        val ids = serverItems.map { it.deviceId }
        Log.d("DeviceOrder", "LaunchedEffect(serverItems) - ids: $ids, current deviceOrder: ${deviceOrder.toList()}")
        if (ids.isEmpty()) return@LaunchedEffect
        
        // 순서리스트가 비어있다면 서버 순서로 초기화
        if (deviceOrder.isEmpty()) {
            deviceOrder.addAll(ids)
            Log.d("DeviceOrder", "Initialized deviceOrder with server order: $ids")
        } else {
            // 기존 순서에서 삭제된 항목 제거
            val beforeRetain = deviceOrder.toList()
            deviceOrder.retainAll(ids.toSet())

            // 새로 추가된 항목을 뒤에 추가
            val newIds = ids.filter { it !in deviceOrder }
            deviceOrder.addAll(newIds)

            Log.d("DeviceOrder", "Updated deviceOrder - before: $beforeRetain, after retain: ${deviceOrder.toList()}, newIds: $newIds")
        }

        // 변경된 순서를 SharedPreferences에 저장
        val orderString = deviceOrder.joinToString(",")
        prefs.edit().putString("device_order", orderString).apply()
        Log.d("DeviceOrder", "Final deviceOrder: ${deviceOrder.toList()}, saved: $orderString")
    }

    // 디바이스 삭제 ViewModel
    val deleteVm: DeviceDeleteViewModel = viewModel()
    val deleteResult by deleteVm.isDeleted.observeAsState(false)
    val deleteError by deleteVm.error.observeAsState()
    val deleteLoading by deleteVm.loading.observeAsState(false)

    // 허브 목록 상태 관찰
    val hubList by hubVm.hubList.observeAsState(emptyList())
    val hubError by hubVm.error.observeAsState()

    // 디바이스 상태 변경 관련 상태 관찰
    val statusChangeResult by statusVm.result.observeAsState()
    val statusChangeError by statusVm.error.observeAsState()
    
    // 대표 집 이름 및 homeId 관찰
    val primaryHomeName by homeVm.primaryHomeName.observeAsState()
    val primaryHomeId by homeVm.primaryHomeId.observeAsState()
    val selectedHomeId by homeVm.selectedHomeId.observeAsState()
    
    // 현재 사용할 homeId (선택된 홈이 있으면 우선, 없으면 대표 홈, 둘 다 없으면 기본값 1)
    val currentHomeId = selectedHomeId ?: primaryHomeId ?: 1

    // 최초 진입 시 1회 로드
    LaunchedEffect(Unit) {
        listVm.load()
        // 대표 집 정보 로드
        homeVm.fetchPrimaryHome()
    }
    
    // homeId가 사용 가능해지면 허브 목록 로드
    LaunchedEffect(currentHomeId) {
        if (currentHomeId > 0) {
            hubVm.getHubs(currentHomeId)
        }
    }

    // 수동 새로고침 처리 (필요 시 사용)
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            try {
                listVm.load()
                homeVm.fetchPrimaryHome()
                if (currentHomeId > 0) {
                    hubVm.getHubs(currentHomeId)
                }
            } finally {
                onRefreshComplete()
            }
        }
    }

    // 허브 등록 성공 시 허브 목록 재조회
    val userHomeId by hubVm.userHomeId.observeAsState()
    val registrationStatus by hubVm.registrationStatus.observeAsState()
    LaunchedEffect(userHomeId, registrationStatus) {
        if (userHomeId != null && registrationStatus == "success") {
            if (currentHomeId > 0) {
                hubVm.getHubs(currentHomeId) // 허브 등록 성공 후 목록 새로고침
            }
        }
    }
    // 자동 새로고침 신호 수신 시 서버 목록 재조회
    LaunchedEffect(refreshKey) {
        if (refreshKey != 0L) {
            android.widget.Toast.makeText(activity, "디바이스 목록을 새로고침했습니다.", android.widget.Toast.LENGTH_SHORT).show()
            listVm.load()
            homeVm.fetchPrimaryHome() // 대표 집 정보도 새로고침
            if (currentHomeId > 0) {
                hubVm.getHubs(currentHomeId) // 허브 목록도 새로고침
            }
        }
    }

    // 디바이스 상태 변경 성공 시 개별 상태만 업데이트 (전체 목록 새로고침 제거)
    LaunchedEffect(statusChangeResult) {
        statusChangeResult?.let {
            // 전체 목록 새로고침 제거 - 상태는 자동으로 반영됨
            statusVm.clearResult() // 결과 초기화
        }
    }

    // 디바이스 상태 변경 에러 처리
    LaunchedEffect(statusChangeError) {
        statusChangeError?.let { error ->
            android.widget.Toast.makeText(activity, "상태 변경 실패: $error", android.widget.Toast.LENGTH_LONG).show()
            statusVm.clearError()
        }
    }

    // 디바이스 삭제 성공 처리
    LaunchedEffect(deleteResult) {
        if (deleteResult) {
            android.widget.Toast.makeText(activity, "디바이스가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
            showDeleteDialog = false
            selectedDeviceForDelete = null
            listVm.load() // 목록 새로고침
            deleteVm.resetDeleteState()
        }
    }

    // 디바이스 삭제 에러 처리
    LaunchedEffect(deleteError) {
        deleteError?.let { error ->
            android.widget.Toast.makeText(activity, "디바이스 삭제 실패: $error", android.widget.Toast.LENGTH_LONG).show()
            deleteVm.clearError()
        }
    }

    LaunchedEffect(Unit) {
            AppEventBus.effects.collect { eff ->
                when (eff) {
                    is AppEffect.DevicesChanged -> {
                        listVm.load()
                        homeVm.fetchPrimaryHome()
                        if (currentHomeId > 0) {
                            hubVm.getHubs(currentHomeId)
                        }
                    }
                    else -> Unit
                }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
            // 헤더 추가
            if (showHeader) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 60.dp, end = 16.dp)
                ) {
                    // 상단 타이틀 (다른 화면과 일관)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = primaryHomeName ?: "우리 집", // 대표 집 이름 사용, 로딩 전에는 기본값
                            style = TextStyle(
                                fontSize = 30.sp,
                                fontFamily = FontFamily(Font(R.font.goormsansbold)),
                                color = Gray800
                            )
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp)
                .padding(top = if (showHeader) 0.dp else 60.dp)
        ) {
            // 허브 데이터 + 서버 데이터 합치기 (오버라이드 카운터를 의존성으로 사용)
            val allDevices = remember(serverItems, hubList, overrideCounter, deviceOrder.toList()) {
                // 등록된 허브들을 DeviceUi로 변환
                val hubDevices = if (hubList.isNotEmpty()) {
                    hubList.mapIndexed { index, hubId ->
                        DeviceUi(
                            id = "hub_$hubId",
                            title = "허브", // 디바이스 ID 표기 제거
                            room = "", // 위치 표시 제거
                            statusText = "연결됨",
                            iconRes = R.drawable.ic_hub,
                            statusIconRes = R.drawable.ic_device_on,
                            iconTint = Gray500,
                            isLarge = true
                        )
                    }
                } else {
                    emptyList()
                }

                // 기존 순서를 보존하고 새 디바이스는 뒤에 붙이기 위해 정렬
                val orderedServerItems = if (deviceOrder.isEmpty()) serverItems else serverItems.sortedBy {
                    val idx = deviceOrder.indexOf(it.deviceId)
                    if (idx == -1) Int.MAX_VALUE else idx
                }

                val serverDevices = orderedServerItems.map { deviceResponse ->
                    val deviceIdInt = deviceResponse.deviceId
                    // 방 이름 추출: deviceName의 첫 공백 이전 부분을 방 이름으로 간주
                    val roomName = deviceResponse.deviceName.substringBefore(' ').ifBlank { "방" }

                    // 디바이스 타입을 한국어로 변환
                    val deviceTypeKorean = convertDeviceTypeToKorean(deviceResponse.deviceType)

                    // power 상태 체크 (server 값)
                    val serverPower = runCatching {
                        val powerEl = deviceResponse.deviceDetail.get("power")
                        powerEl?.asJsonPrimitive?.asBoolean ?: false
                    }.getOrDefault(false)
                    // 로컬 오버라이드 우선
                    val isOn = powerOverrides[deviceIdInt] ?: serverPower

                    // 온도 정보 (server 값)
                    val serverTemp = runCatching {
                        val tempEl = deviceResponse.deviceDetail.get("temperature")
                        tempEl?.asJsonPrimitive?.asInt
                    }.getOrNull()
                    val temperature = tempOverrides[deviceIdInt] ?: serverTemp

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

            if (loading) {
                Text("디바이스 목록 불러오는 중...", color = Gray600)
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
                                onClick = { hubVm.getHubs(1) },
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
                    onToggle = { deviceId ->
                        // 허브는 토글 불가
                        if (deviceId.startsWith("hub_")) {
                            return@DeviceGrid
                        }

                        // 실제 디바이스 토글
                        val deviceIdInt = deviceId.toIntOrNull()
                        if (deviceIdInt != null) {
                            // 현재 디바이스의 전원 상태 찾기 (로컬 오버라이드 우선)
                            val serverDevice = serverItems.find { it.deviceId == deviceIdInt }
                            val serverPower = serverDevice?.let {
                                runCatching {
                                    it.deviceDetail.get("power")?.asJsonPrimitive?.asBoolean ?: false
                                }.getOrDefault(false)
                            } ?: false
                            val currentPower = powerOverrides[deviceIdInt] ?: serverPower

                            // 현재 온도 및 레벨 설정 찾기 (오버라이드 우선)
                            val serverTemp = serverDevice?.let {
                                runCatching {
                                    it.deviceDetail.get("temperature")?.asJsonPrimitive?.asInt
                                }.getOrNull()
                            }
                            val currentTemp = tempOverrides[deviceIdInt] ?: serverTemp ?: 23

                            val serverLevel = serverDevice?.let {
                                runCatching {
                                    it.deviceDetail.get("level")?.asJsonPrimitive?.asInt
                                }.getOrNull()
                            }
                            val currentLevel = levelOverrides[deviceIdInt] ?: serverLevel ?: 1

                            // 즉시 UI 반영
                            powerOverrides[deviceIdInt] = !currentPower
                            overrideCounter++ // 재구성 트리거

                            // API 호출 - 현재 설정값들을 유지하면서 토글
                            statusVm.toggleDevicePower(
                                deviceId = deviceIdInt,
                                currentPower = currentPower,
                                currentTemperature = currentTemp,
                                currentLevel = currentLevel
                            )
                        }
                    },
                    onLongPress = { deviceId ->
                        // 허브는 제외
                        if (deviceId.startsWith("hub_")) {
                            return@DeviceGrid
                        }

                        // 디바이스 타입 찾기
                        val deviceIdInt = deviceId.toIntOrNull()
                        if (deviceIdInt != null) {
                            val device = serverItems.find { it.deviceId == deviceIdInt }
                            val deviceType = device?.deviceType?.uppercase()

                            // 디바이스 전원 상태 확인
                            val serverDevice = serverItems.find { it.deviceId == deviceIdInt }
                            val serverPower = serverDevice?.let {
                                runCatching {
                                    it.deviceDetail.get("power")?.asJsonPrimitive?.asBoolean ?: false
                                }.getOrDefault(false)
                            } ?: false
                            val currentPower = powerOverrides[deviceIdInt] ?: serverPower

                            if (!currentPower) {
                                // 디바이스가 꺼져있으면 삭제 다이얼로그 표시
                                selectedDeviceForDelete = deviceIdInt
                                showDeleteDialog = true
                            } else {
                                // 디바이스가 켜져있는 경우 기존 로직 (에어컨/선풍기만 제어 다이얼로그)
                                val originalType = device?.deviceType?.uppercase()
                                val isAirConditioner = deviceType?.contains("AIR") == true ||
                                        deviceType?.contains("CONDITIONER") == true ||
                                        deviceType == "AC" ||
                                        deviceType == "AIRCONDITIONER" ||
                                        originalType?.contains("에어컨") == true ||
                                        originalType?.contains("AIRCON") == true
                                val isFan = deviceType?.contains("FAN") == true ||
                                        originalType?.contains("선풍기") == true

                                if (isAirConditioner || isFan) {
                                    val finalType = if (isAirConditioner) "AIR_CONDITIONER" else "FAN"
                                    selectedDeviceForControl = Pair(deviceIdInt, finalType)
                                    showDialog = true
                                }
                            }
                        }
                    },
                    onAddClick = {
                        // 등록 초안 초기화 후 등록 플로우 진입
                        regVm.resetDraft()
                        navController?.navigate("device_registration")
                    }
                )
            } // Box 종료
        } // Column 종료

        // 디바이스 컨트롤 다이얼로그
        if (showDialog) {
            selectedDeviceForControl?.let { (deviceId, deviceType) ->
                DeviceControlDialog(
                    deviceId = deviceId,
                    deviceType = deviceType,
                    serverItems = serverItems,
                    statusVm = statusVm,
                    tempOverrides = tempOverrides,
                    levelOverrides = levelOverrides,
                    onDismiss = {
                        showDialog = false
                        selectedDeviceForControl = null
                    },
                    onApplyAirConditioner = { newTemp ->
                        tempOverrides[deviceId] = newTemp
                        powerOverrides[deviceId] = true
                        overrideCounter++ // 재구성 트리거
                        statusVm.setAirConditionerTemperature(
                            deviceId = deviceId,
                            temperature = newTemp,
                            power = true
                        )
                    },
                    onApplyFan = { newLevel ->
                        levelOverrides[deviceId] = newLevel
                        powerOverrides[deviceId] = true
                        overrideCounter++ // 재구성 트리거
                        statusVm.setFanLevel(
                            deviceId = deviceId,
                            level = newLevel,
                            power = true
                        )
                    }
                )
            }
        }

        // 디바이스 삭제 다이얼로그
        if (showDeleteDialog) {
            selectedDeviceForDelete?.let { deviceId ->
                DeviceDeleteDialog(
                    onConfirm = { _ ->
                        deleteVm.deleteDevice(deviceId)
                    },
                    onCancel = {
                        showDeleteDialog = false
                        selectedDeviceForDelete = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCardLarge(
    title: String,
    room: String,
    status: String,
    iconRes: Int,
    statusIconRes: Int,
    iconTint: Color,
    onToggle: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray50),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
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
            if (room.isNotBlank()) {
                Text(
                    text = room,
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCardSmall(
    title: String,
    room: String,
    status: String,
    iconRes: Int,
    statusIconRes: Int,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray50),
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongPress
        )
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
            if (room.isNotBlank()) {
                Text(
                    text = room,
                    style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)), color = Gray500)
                )
            }
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
private fun DeviceControlDialog(
    deviceId: Int,
    deviceType: String,
    serverItems: List<com.example.eeum.data.model.response.device.DeviceResponse>,
    statusVm: DeviceStatusViewModel,
    tempOverrides: Map<Int, Int>,
    levelOverrides: Map<Int, Int>,
    onDismiss: () -> Unit,
    onApplyAirConditioner: (Int) -> Unit = {},
    onApplyFan: (Int) -> Unit = {}
) {
    // 상태 변수들을 기억하여 다이얼로그 닫힘 시 자동 전송
    var finalTemperature by remember { mutableIntStateOf(23) }
    var finalLevel by remember { mutableIntStateOf(1) }

    Dialog(
        onDismissRequest = {
            // 다이얼로그가 닫힐 때 자동으로 상태 반영 - 콜백으로 전달
            when (deviceType.uppercase()) {
                "AIR_CONDITIONER" -> onApplyAirConditioner(finalTemperature)
                "FAN" -> onApplyFan(finalLevel)
            }
            onDismiss()
        }
    ) {
        when (deviceType) {
            "AIR_CONDITIONER" -> {
                // 현재 온도 찾기 - 로컬 오버라이드 우선 사용
                val device = serverItems.find { it.deviceId == deviceId }
                val serverTemp = device?.let {
                    runCatching {
                        it.deviceDetail.get("temperature")?.asJsonPrimitive?.asInt ?: 23
                    }.getOrDefault(23)
                } ?: 23
                val currentTemp = tempOverrides[deviceId] ?: serverTemp

                // 초기 온도 설정
                LaunchedEffect(Unit) {
                    finalTemperature = currentTemp
                }

                AirConditionerTemperatureControl(
                    currentTemperature = currentTemp,
                    onTemperatureChange = { newTemp ->
                        finalTemperature = newTemp
                    },
                    onApply = { /* 사용 안함 */ },
                    onCancel = { /* 사용 안함 */ }
                )
            }
            "FAN" -> {
                // 현재 레벨 찾기 - 로컬 오버라이드 우선 사용
                val device = serverItems.find { it.deviceId == deviceId }
                val serverLevel = device?.let {
                    runCatching {
                        it.deviceDetail.get("level")?.asJsonPrimitive?.asInt ?: 1
                    }.getOrDefault(1)
                } ?: 1
                val currentLevel = levelOverrides[deviceId] ?: serverLevel

                // 초기 레벨 설정
                LaunchedEffect(Unit) {
                    finalLevel = currentLevel
                }

                FanLevelControl(
                    currentLevel = currentLevel,
                    onLevelChange = { newLevel ->
                        finalLevel = newLevel
                    },
                    onApply = { /* 사용 안함 */ },
                    onCancel = { /* 사용 안함 */ }
                )
            }
        }
    }
}

@Composable
private fun DeviceGrid(
    items: List<DeviceUi>,
    modifier: Modifier = Modifier,
    showAddTile: Boolean = true,
    onToggle: (String) -> Unit = {},
    onLongPress: (String) -> Unit = {},
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
                    onToggle = { onToggle(d.id) },
                    onLongPress = { onLongPress(d.id) }
                )
            } else {
                DeviceCardSmall(
                    title = d.title,
                    room = d.room,
                    status = d.statusText,
                    iconRes = d.iconRes,
                    statusIconRes = d.statusIconRes,
                    iconTint = d.iconTint,
                    onToggle = { onToggle(d.id) },
                    onLongPress = { onLongPress(d.id) }
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
        else -> R.drawable.ic_device
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
        else -> deviceType
    }
}
