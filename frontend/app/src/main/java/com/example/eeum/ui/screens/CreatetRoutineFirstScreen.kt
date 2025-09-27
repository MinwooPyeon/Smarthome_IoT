package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.R
import com.example.eeum.base.ApplicationClass
import com.example.eeum.data.model.dto.routine.DeviceDetail
import com.example.eeum.data.model.dto.routine.Detail
import com.example.eeum.data.model.dto.routine.RoutineRequestDto
import com.example.eeum.ui.theme.*
import com.example.eeum.util.ResourceUtils
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import android.util.Log
import com.example.eeum.util.SharedPreferencesUtil

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineFirstScreen(
    navController: NavController,
    vm: RoutineViewModel = viewModel(),
    routineId: Int = 0,
    isEditMode: Boolean = false
) {
    val ctx = LocalContext.current
    val prefs = remember { SharedPreferencesUtil(ctx) }
    val homeId = prefs.getSelectedHomeId() ?: -1
    
    // --- 입력값들을 모두 rememberSaveable 로 보존 ---
    var title by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }

    // Set<Int> 은 기본적으로 저장 불가 → Saver로 저장/복원
    val setSaver: Saver<Set<Int>, List<Int>> = Saver(
        save = { it.toList() },
        restore = { it.toSet() }
    )
    var selectedDays by rememberSaveable(stateSaver = setSaver) { mutableStateOf(setOf(0)) } // ResourceUtils와 동일: 0=월 ~ 6=일

    var hour24 by rememberSaveable { mutableIntStateOf(8) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) } // 다이얼로그 노출 여부는 돌아와도 크게 상관없어 non-saveable 유지
    val timeText = remember(hour24, minute) { "%02d:%02d".format(hour24, minute) }

    var selectedIconId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedIconUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showIconDialog by remember { mutableStateOf(false) } // 노출 여부는 돌아와도 상관없어 non-saveable

    // 동작 목록(두번째 화면에서 계속 누적) - rememberSaveable + listSaver
    val actions = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<ActionAddedPayload>() }
    
    // 편집 모드에서 초기 로드 완료 상태 추적
    var editModeActionsLoaded by rememberSaveable { mutableStateOf(false) }

    // 편집 모드에서 기존 루틴 데이터 로드 (상세 정보 포함)
    val routineDetailV2 by vm.routineDetailV2.observeAsState()
    val rooms by vm.rooms.observeAsState(emptyList())
    val devices by vm.devices.observeAsState(emptyList())
    
    // 편집 모드일 때 루틴 데이터 로드 및 방 목록 로드
    LaunchedEffect(isEditMode, routineId, homeId) {
        if (isEditMode && routineId > 0) {
            Log.d("CreateRoutineFirst", "📝 Edit mode started for routineId: $routineId")
            editModeActionsLoaded = false // 편집 모드 시작 시 초기화
            vm.fetchRoutineDetail(routineId)
            if (homeId != -1) {
                vm.fetchRooms(homeId)
            }
        } else {
            editModeActionsLoaded = false // 생성 모드에서는 항상 false
        }
    }
    
    // 루틴 데이터가 로드되면 모든 필드들 초기화
    LaunchedEffect(routineDetailV2) {
        routineDetailV2?.let { routine ->
            title = routine.name
            desc = routine.routineDescription
            
            // 시간 파싱 (HH:mm:ss -> hour/minute)
            routine.actTime.let { timeStr ->
                val timeParts = timeStr.split(":")
                if (timeParts.size >= 2) {
                    hour24 = timeParts[0].toIntOrNull() ?: 8
                    minute = timeParts[1].toIntOrNull() ?: 0
                }
            }
            
            // 요일 파싱 - ResourceUtils와 동일한 비트마스크 순서 사용
            // ResourceUtils: 월=0, 화=1, 수=2, 목=3, 금=4, 토=5, 일=6
            val weekdayMask = routine.routineWeekday
            val newSelectedDays = mutableSetOf<Int>()
            for (i in 0..6) {
                if ((weekdayMask and (1 shl i)) != 0) {
                    newSelectedDays.add(i)
                }
            }
            selectedDays = newSelectedDays
            
            // 아이콘 설정
            selectedIconId = routine.iconId
            selectedIconUrl = toAbsoluteUrl(ApplicationClass.SERVER_URL, routine.iconUrl)
            
            // 동작 데이터 초기화
            actions.clear()
            
            // CreateRoutineSecondScreen 방식으로 actions 생성
            // 각 detail의 deviceId로 해당 디바이스가 있는 방을 찾아서 디바이스 목록을 로드해야 함
            Log.d("CreateRoutineFirst", "Loading routine details for ${routine.details.size} actions")
        }
    }
    
    // 편집 모드에서 방 목록이 로드되면 모든 방의 디바이스들을 로드
    LaunchedEffect(rooms, routineDetailV2, isEditMode) {
        if (isEditMode && rooms.isNotEmpty() && routineDetailV2 != null) {
            val routine = routineDetailV2!!
            Log.d("CreateRoutineFirst", "Edit mode: Loading devices from ${rooms.size} rooms for ${routine.details.size} details")
            
            // 모든 방에서 디바이스 로드 (순차적으로)
            rooms.forEachIndexed { index, room ->
                Log.d("CreateRoutineFirst", "Loading devices from room: ${room.roomName} (${index + 1}/${rooms.size})")
                vm.fetchDevicesSimple(room.roomName)
            }
        }
    }
    
    // 디바이스 목록이 로드되면 해당 디바이스들로 ActionAddedPayload 생성
    LaunchedEffect(devices, routineDetailV2, rooms, isEditMode, editModeActionsLoaded) {
        if (isEditMode && devices.isNotEmpty() && routineDetailV2 != null && rooms.isNotEmpty() && !editModeActionsLoaded) {
            val routine = routineDetailV2!!
            Log.d("CreateRoutineFirst", "=== Creating Actions ===")
            Log.d("CreateRoutineFirst", "Available devices: ${devices.size}")
            Log.d("CreateRoutineFirst", "Available rooms: ${rooms.size}")
            Log.d("CreateRoutineFirst", "Routine details: ${routine.details.size}")
            
            devices.forEach { device ->
                Log.d("CreateRoutineFirst", "Available device: ${device.deviceName} (ID: ${device.deviceId})")
            }
            
            routine.details.forEach { detail ->
                Log.d("CreateRoutineFirst", "Looking for device ID: ${detail.deviceId}")
            }
            
            actions.clear()
            Log.d("CreateRoutineFirst", "Cleared existing actions, starting to create new ones...")
            
            routine.details.forEachIndexed { index, detail ->
                Log.d("CreateRoutineFirst", "Processing detail ${index + 1}/${routine.details.size}: deviceId=${detail.deviceId}")
                
                // devices 목록에서 해당 deviceId를 찾기
                val matchingDevice = devices.find { it.deviceId == detail.deviceId }
                if (matchingDevice != null) {
                    Log.d("CreateRoutineFirst", "Found matching device: ${matchingDevice.deviceName} (ID: ${matchingDevice.deviceId})")
                    
                    // 해당 디바이스의 방 정보 찾기
                    val matchingRoom = rooms.find { it.roomId == matchingDevice.roomId }
                    if (matchingRoom != null) {
                        Log.d("CreateRoutineFirst", "Found matching room: ${matchingRoom.roomName} (ID: ${matchingRoom.roomId})")
                        try {
                            // deviceDetail JSON 파싱
                            val deviceDetailJson = JsonParser.parseString(detail.deviceDetail).asJsonObject
                            val power = deviceDetailJson.get("power")?.asBoolean ?: true
                            val temperature = deviceDetailJson.get("temperature")?.asInt
                            val level = deviceDetailJson.get("level")?.asInt
                            
                            // CreateRoutineSecondScreen과 동일한 방식으로 ActionAddedPayload 생성
                            val stateTitle = if (power) "켜기" else "끄기"
                            val roomP = RoomDataP(matchingRoom.roomColor, matchingRoom.roomId, matchingRoom.roomName)
                            val deviceP = DeviceItemP(
                                brand = matchingDevice.brand,
                                deviceId = matchingDevice.deviceId,
                                deviceName = matchingDevice.deviceName,
                                deviceType = matchingDevice.deviceType.toString(),
                                irDeviceId = matchingDevice.irDeviceId,
                                model = matchingDevice.model,
                                registeredAt = matchingDevice.registeredAt,
                                roomId = matchingDevice.roomId,
                                x = matchingDevice.x,
                                y = matchingDevice.y
                            )
                            // 디바이스 타입별 특징 확인
                            val deviceTypeStr = matchingDevice.deviceType.toString()
                            val features = when (deviceTypeStr.trim().lowercase()) {
                                "조명" -> Triple(true, false, false) // showState, showWind, showAcTemp
                                "에어컨" -> Triple(true, true, true)
                                "공기청정기" -> Triple(true, true, false)
                                "선풍기" -> Triple(true, true, false)
                                "tv", "텔레비전" -> Triple(true, false, false)
                                "빔프로젝터" -> Triple(true, false, false)
                                else -> Triple(true, false, false)
                            }
                            val payload = ActionAddedPayload(
                                room = roomP,
                                device = deviceP,
                                stateTitle = stateTitle,
                                power = power,
                                windLevel = if (features.second) level else null,
                                acTemp = if (features.third) temperature else null
                            )
                            
                            actions.add(payload)
                            Log.d("CreateRoutineFirst", "✅ Successfully added action ${actions.size}: ${matchingDevice.deviceName} (${matchingDevice.deviceType}) in ${matchingRoom.roomName}")
                            Log.d("CreateRoutineFirst", "   Power: $power, WindLevel: ${payload.windLevel}, AcTemp: ${payload.acTemp}")
                            
                        } catch (e: JsonSyntaxException) {
                            Log.e("CreateRoutineFirst", "Failed to parse deviceDetail JSON: ${detail.deviceDetail}", e)
                        } catch (e: Exception) {
                            Log.e("CreateRoutineFirst", "Failed to create action for deviceId: ${detail.deviceId}", e)
                        }
                    } else {
                        Log.w("CreateRoutineFirst", "❌ Room not found for deviceId: ${detail.deviceId}, roomId: ${matchingDevice.roomId}")
                        Log.w("CreateRoutineFirst", "   Available rooms: ${rooms.map { "${it.roomName}(${it.roomId})" }}")
                    }
                } else {
                    Log.w("CreateRoutineFirst", "❌ Device not found for deviceId: ${detail.deviceId}")
                    Log.w("CreateRoutineFirst", "   Available devices: ${devices.map { "${it.deviceName}(${it.deviceId})" }}")
                }
            }
            
            Log.d("CreateRoutineFirst", "=== Final Result ===")
            Log.d("CreateRoutineFirst", "🏁 Successfully created ${actions.size} actions from ${routine.details.size} details")
            actions.forEachIndexed { index, action ->
                Log.d("CreateRoutineFirst", "   Action ${index + 1}: ${action.device.deviceName} - ${action.stateTitle}")
            }
            
            // 편집 모드 초기 로드 완료 표시
            editModeActionsLoaded = true
            Log.d("CreateRoutineFirst", "🔒 Edit mode initial load completed, future loads will preserve existing actions")
        }
    }
    

    // 두번째 화면에서 돌아온 payload 수신 및 누적 (BackStack의 LiveData를 관찰)
    val newActionLive = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<ActionAddedPayload>("new_action_full")
    val newAction: ActionAddedPayload? = newActionLive?.observeAsState()?.value
    LaunchedEffect(newAction) {
        newAction?.let {
            actions.add(it) // 누적
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<ActionAddedPayload>("new_action_full")
        }
    }

    // 완료 버튼 활성화 조건(간단 예시)
    val canSubmit = selectedIconId != null &&
            title.isNotBlank() &&
            selectedDays.isNotEmpty() &&
            actions.isNotEmpty()

    // ★ 생성/수정 성공 응답을 관찰하고 그때 적절한 화면으로 이동
    val createResult = vm.createResult.observeAsState().value
    LaunchedEffect(createResult) {
        createResult?.let {
            // 성공 응답을 받은 시점에 라우팅
            if (isEditMode) {
                // 편집 모드에서는 이전 화면으로 돌아가기
                navController.popBackStack()
            } else {
                // 생성 모드에서는 루틴 목록으로 이동
                navController.navigate("routine") {
                    launchSingleTop = true
                    popUpTo("createRoutineFirst") { inclusive = true }
                }
            }
        }
    }

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
                text = if (isEditMode) "루틴 수정" else "루틴 만들기",
                color = Gray900,
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 카드 1: 이름/설명/아이콘
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RoutineIconPreview(
                            iconUrl = selectedIconUrl,
                            onClick = { showIconDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Text("루틴 이름", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("루틴 이름을 입력하세요") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )

                        Text("설명", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            placeholder = { Text("루틴에 대한 설명을 입력하세요") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // 카드 2: 요일 선택
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("요일 선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        val days = listOf("월", "화", "수", "목", "금", "토", "일") // ResourceUtils 순서와 동일
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(days.indices.toList()) { idx ->
                                val label = days[idx]
                                FilterChip(
                                    selected = selectedDays.contains(idx),
                                    onClick = {
                                        selectedDays = if (selectedDays.contains(idx)) {
                                            selectedDays - idx
                                        } else {
                                            selectedDays + idx
                                        }
                                    },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedDays.contains(idx),
                                        borderColor = BorderGray,
                                        selectedBorderColor = BorderGray,
                                        disabledBorderColor = BorderGray,
                                        disabledSelectedBorderColor = BorderGray,
                                        borderWidth = 1.dp
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFFF6F7FB),
                                        selectedContainerColor = TextBlue,
                                        labelColor = Color.Black,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 카드 3: 시간 설정
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("시간 설정", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("설정 시간", style = MaterialTheme.typography.bodyLarge)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.clickable { showTimePicker = true }
                            ) {
                                Text(
                                    text = timeText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 카드 4: 동작 설정
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("동작 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        
                        // 디버깅: actions 리스트 상태 확인
                        Log.d("CreateRoutineFirst", "UI: Rendering ${actions.size} actions")
                        
                        if (actions.isEmpty()) {
                            Text(
                                text = "동작이 없습니다. 동작을 추가해주세요.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                actions.forEachIndexed { index, payload ->
                                    Log.d("CreateRoutineFirst", "UI: Rendering action $index: ${payload.device.deviceName}")
                                    ActionCard(
                                        payload = payload,
                                        onDelete = { toRemove -> actions.remove(toRemove) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { navController.navigate("createRoutineSecond") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextBlue,
                                contentColor = Color.White
                            )
                        ) { Text("동작 추가", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            // 완료 버튼 → 서버 전송(성공 응답 후 RoutineScreen으로 이동)
            item {
                Button(
                    onClick = {
                        val iconId = selectedIconId ?: return@Button

                        // ISO 시간 변환
                        val actTimeIso: String = ResourceUtils.toIsoActTime(timeText)

                        // 선택 요일을 문자열로 만들고 parseWeekdays 사용
                        // ResourceUtils 순서와 동일: 0=월, 1=화, 2=수, 3=목, 4=금, 5=토, 6=일
                        val idxToName = mapOf(0 to "월", 1 to "화", 2 to "수", 3 to "목", 4 to "금", 5 to "토", 6 to "일")
                        val daysRaw = selectedDays.sorted().mapNotNull { idxToName[it] }.joinToString(",")
                        val weekdayMask: Int = ResourceUtils.parseWeekdays(daysRaw) ?: 0

                        // Detail 매핑
                        val details: List<Detail> = actions.map { p ->
                            Detail(
                                deviceId = p.device.deviceId,
                                deviceDetail = DeviceDetail(
                                    level = p.windLevel ?: 0,
                                    power = p.power,
                                    temperature = p.acTemp ?: 0
                                )
                            )
                        }

                        val body = RoutineRequestDto(
                            actTime = actTimeIso,
                            detail = details,
                            iconId = iconId,
                            isAi = false,
                            name = title,
                            routineDescription = desc,
                            routineWeekday = weekdayMask
                        )

                        // 디버깅 로그 추가
                        Log.d("CreateRoutineFirst", "Sending routine with isAi = ${body.isAi}")
                        
                        // 전송 (성공 응답을 observe해서 그때 화면 이동)
                        if (isEditMode && routineId > 0) {
                            vm.updateRoutine(routineId, body)
                        } else {
                            vm.generateRoutine(body)
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBFD8FF),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) { 
                    Text(
                        text = if (isEditMode) "수정 완료" else "완료", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium
                    ) 
                }
            }
        }
    }

    // 시간 다이얼로그 (시간값은 rememberSaveable 된 hour/minute로 복원됨)
    if (showTimePicker) {
        TimeWheelDialog(
            initialHour24 = hour24,
            initialMinute = minute,
            onConfirm = { h, m ->
                hour24 = h; minute = m; showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // 루틴 아이콘 선택 다이얼로그 (아이콘 선택값은 rememberSaveable로 복원됨)
    if (showIconDialog) {
        RoutineIconDialog(
            title = "루틴 아이콘",
            onSelect = { id, url ->
                selectedIconId = id
                selectedIconUrl = toAbsoluteUrl(ApplicationClass.SERVER_URL, url)
                showIconDialog = false
            },
            onDismiss = { showIconDialog = false }
        )
    }
}

/* =========================
 * 아이콘 프리뷰
 * ========================= */
@Composable
private fun RoutineIconPreview(
    iconUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner = 26.dp
    val ctx = LocalContext.current
    Box(modifier = modifier.size(120.dp)) {
        Surface(
            shape = RoundedCornerShape(corner),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            if (iconUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(R.drawable.ic_mainlogo),
                    contentDescription = "placeholder",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "routine icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = TextBlue,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clickable { onClick() }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "아이콘 선택",
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/* =========================
 * 루틴 아이콘 다이얼로그
 * ========================= */
@Composable
private fun RoutineIconDialog(
    title: String,
    onSelect: (iconId: Int, url: String) -> Unit,
    onDismiss: () -> Unit,
    vm: RoutineViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.fetchRoutineIcons() }
    val serverIcons by vm.icons.observeAsState(emptyList())
    val ctx = LocalContext.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = true
                ) {
                    items(serverIcons) { icon ->
                        val corner = 12.dp
                        val absoluteUrl = remember(icon.url) {
                            toAbsoluteUrl(ApplicationClass.SERVER_URL, icon.url)
                        }
                        Surface(
                            shape = RoundedCornerShape(corner),
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(corner))
                                .clickable { onSelect(icon.iconId, icon.url) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx)
                                    .data(absoluteUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("닫기", color = Color(0xFF4B5563))
                }
            }
        }
    }
}

/* =========================
 * 시간 선택 다이얼로그
 * ========================= */
@Composable
private fun TimeWheelDialog(
    initialHour24: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour24) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("시간 설정", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("시", fontSize = 14.sp, color = Color.Gray)
                        NumberPicker(
                            value = hour,
                            range = 0..23,
                            onValueChange = { hour = it }
                        )
                    }
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("분", fontSize = 14.sp, color = Color.Gray)
                        NumberPicker(
                            value = minute,
                            range = 0..59,
                            onValueChange = { minute = it }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("취소", color = Color.Gray) }

                    Button(
                        onClick = { onConfirm(hour, minute) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TextBlue)
                    ) { Text("확인", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val values = range.toList()
    val selectedIndex = values.indexOf(value).coerceAtLeast(0)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        // 첫 번째 보이는 아이템이 바로 위 행이 되도록 설정해 중앙에 value가 오게 함
        initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 40.dp.toPx() }
    val containerHeightPx = with(density) { 120.dp.toPx() }
    val centerPx = containerHeightPx / 2f

    // 중앙 라인에 가장 가까운 아이템을 선택 (콘텐츠 패딩을 자동 반영)
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val closest = layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }
            (closest?.index ?: selectedIndex).coerceIn(0, values.lastIndex)
        }
    }

    // 중앙 값이 바뀌면 선택값으로 반영
    LaunchedEffect(centerIndex) {
        val newValue = values[centerIndex]
        if (newValue != value) onValueChange(newValue)
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .height(120.dp)
            .width(70.dp) // 약간 넓혀서 가독성 확보
            .drawWithContent {
                drawContent()
                // 중앙 선택 영역 가이드 라인 (위/아래 경계)
                val top = (containerHeightPx - itemHeightPx) / 2f
                val bottom = top + itemHeightPx
                val lineColor = Color(0xFFCBD5E1) // slate-300
                val stroke = with(density) { 1.5.dp.toPx() }
                drawLine(lineColor, Offset(0f, top), Offset(size.width, top), strokeWidth = stroke)
                drawLine(lineColor, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = stroke)
            }
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 40.dp) // 양끝 아이템도 중앙에 올 수 있도록 패딩
        ) {
            items(values.size) { idx ->
                val v = values[idx]
                val isSelected = v == value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clickable {
                            // 클릭 시 해당 값이 중앙에 오도록 스크롤
                            val targetFirst = (idx - 1).coerceAtLeast(0)
                            scope.launch { listState.animateScrollToItem(targetFirst, scrollOffset = 0) }
                            onValueChange(v)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(v),
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextBlue else Color.Gray
                    )
                }
            }
        }
    }
}

/* =========================
 * 액션 카드 & 보조 함수
 * ========================= */
@Composable
private fun ActionCard(
    payload: ActionAddedPayload,
    onDelete: (ActionAddedPayload) -> Unit
) {
    val iconRes = iconResForDevice(payload.device.deviceType)
    val powerText = if (payload.power) "켜기" else "끄기"
    val subText = when {
        payload.acTemp != null -> "${payload.acTemp}℃"
        payload.windLevel != null -> "세기 ${payload.windLevel}"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderGray),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2FF)),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = TextBlue
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_light),
                        contentDescription = null,
                        tint = TextBlue
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${payload.room.roomName}, ${payload.device.deviceType}, $powerText",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                if (subText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subText, fontSize = 13.sp, color = Color(0xFF6B7280))
                }
            }
            IconButton(onClick = { onDelete(payload) }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "delete", tint = Color(0xFFDD4B39))
            }
        }
    }
}

private fun toAbsoluteUrl(base: String, path: String?): String? {
    if (path.isNullOrBlank()) return null
    val b = base.trimEnd('/')
    val p = path.trim()
    if (p.startsWith("http://") || p.startsWith("https://")) return p
    return if (p.startsWith("/")) "$b$p" else "$b/$p"
}

// deviceType → icon 매핑
private fun iconResForDevice(deviceType: String?): Int? {
    val key = deviceType?.trim()?.lowercase() ?: return null
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

@Preview
@Composable
private fun CreateRoutineFirstScreenPreview() {
    val nav = rememberNavController()
    EeumTheme(dynamicColor = false) {
        CreateRoutineFirstScreen(nav, isEditMode = false)
    }
}
