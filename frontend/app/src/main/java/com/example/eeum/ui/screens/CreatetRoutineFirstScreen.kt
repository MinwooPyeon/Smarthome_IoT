package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.R
import com.example.eeum.base.ApplicationClass
import com.example.eeum.ui.theme.EeumTheme

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineFirstScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf(0)) } // 0=일 ~ 6=토

    var hour24 by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timeText = remember(hour24, minute) { "%02d:%02d".format(hour24, minute) }

    var selectedIconId by remember { mutableStateOf<Int?>(null) }
    var selectedIconUrl by remember { mutableStateOf<String?>(null) }
    var showIconDialog by remember { mutableStateOf(false) }

    // ✅ actions 를 rememberSaveable + listSaver 로 영구화(Parcelable 목록 저장/복원)
    val actions = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },                // SnapshotStateList -> List
            restore = { it.toMutableStateList() }  // List -> SnapshotStateList
        )
    ) { mutableStateListOf<ActionAddedPayload>() }

    val newActionLive = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<ActionAddedPayload>("new_action_full")

    val newAction: ActionAddedPayload? = newActionLive
        ?.observeAsState()
        ?.value

    LaunchedEffect(newAction) {
        newAction?.let { payload ->
            actions.add(payload) // 누적
            // 다음 재구독 시 중복 추가 방지
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<ActionAddedPayload>("new_action_full")
        }
    }

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
                Text(" ", color = TextBlue, fontSize = 16.sp)
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 24.dp),
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
                        val days = listOf("일", "월", "화", "수", "목", "금", "토")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            days.forEachIndexed { idx, label ->
                                FilterChip(
                                    selected = selectedDays.contains(idx),
                                    onClick = {
                                        selectedDays =
                                            if (selectedDays.contains(idx)) selectedDays - idx else selectedDays + idx
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

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            actions.forEach { payload ->
                                ActionCard(
                                    payload = payload,
                                    onDelete = { toRemove -> actions.remove(toRemove) }
                                )
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

            // 완료 버튼 (TODO: 서버 전송 시 actions 포함)
            item {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue,
                        contentColor = Color.White
                    )
                ) { Text("완료", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }

    // 시간 다이얼로그
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

    // 루틴 아이콘 선택 다이얼로그
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

@Composable
private fun RoutineIconDialog(
    title: String,
    onSelect: (iconId: Int, url: String) -> Unit,
    onDismiss: () -> Unit,
    vm: RoutineViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
                                .clickable {
                                    onSelect(icon.iconId, icon.url)
                                }
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

private fun toAbsoluteUrl(base: String, path: String?): String? {
    if (path.isNullOrBlank()) return null
    val b = base.trimEnd('/')
    val p = path.trim()
    if (p.startsWith("http://") || p.startsWith("https://")) return p
    return if (p.startsWith("/")) "$b$p" else "$b/$p"
}

/* =========================
 * 동작 카드 (payload 기반)
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

/* =========================
 * 시간 선택 다이얼로그 (복원)
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
                    // 시
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("시", fontSize = 14.sp, color = Color.Gray)
                        NumberPicker(
                            value = hour,
                            range = 0..23,
                            onValueChange = { hour = it }
                        )
                    }
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    // 분
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
        initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0)
    )
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .height(120.dp)
            .width(60.dp),
        state = listState
    ) {
        items(values.size) { idx ->
            val v = values[idx]
            val isSelected = v == value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { onValueChange(v) },
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

@Preview
@Composable
private fun CreateRoutineFirstScreenPreview() {
    val nav = rememberNavController()
    EeumTheme(dynamicColor = false) {
        CreateRoutineFirstScreen(nav)
    }
}
