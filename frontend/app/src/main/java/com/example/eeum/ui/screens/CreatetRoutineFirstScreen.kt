package com.example.eeum.ui.screens

import android.widget.NumberPicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.R
import com.example.eeum.base.ApplicationClass
import com.example.eeum.data.model.dto.ActionItem
import com.example.eeum.data.model.dto.ActionUi
import com.example.eeum.data.model.dto.NewActionResult

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

    val actions = remember {
        mutableStateListOf(
            ActionUi(ActionItem(1, "거실 · 조명 켜기", "08:00 - 22:00"), "조명"),
            ActionUi(ActionItem(2, "안방 · 가습기 켜기", "09:00 - 21:00"), "공기청정기")
        )
    }
    var nextId by remember { mutableIntStateOf(3) }

    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<NewActionResult>("new_action")
        ?.let { res ->
            val sub = when {
                res.acTemp != null -> "${res.acTemp}℃"
                res.windLevel != null -> "세기 ${res.windLevel}"
                else -> ""
            }
            val titleText = "${res.room} · ${res.device} ${res.state}"
            actions.add(ActionUi(ActionItem(nextId++, titleText, sub), res.device))
            navController.currentBackStackEntry?.savedStateHandle?.remove<NewActionResult>("new_action")
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
            // 카드 1: 루틴 이름/설명 + 아이콘 선택
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 서버 아이콘 URL 프리뷰
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
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(days.size) { idx ->
                                val selected = selectedDays.contains(idx)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedDays =
                                            if (selected) selectedDays - idx else selectedDays + idx
                                    },
                                    label = { Text(days[idx]) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
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
                            actions.forEach { actionUi ->
                                ActionCard(
                                    item = actionUi,
                                    onDelete = { toRemove ->
                                        actions.removeAll { it.item.id == toRemove.item.id }
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = { navController.navigate("createRoutineSecond") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextBlue, contentColor = Color.White
                            )
                        ) { Text("동작 추가", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            // 완료 버튼
            item {
                Button(
                    onClick = { /* TODO: 생성 요청에 selectedIconId/Url 포함 */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue, contentColor = Color.White
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

    // ✅ 루틴 아이콘 선택 다이얼로그 (서버 아이콘 URL 사용)
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
 * 아이콘 프리뷰(카드 1의 상단) - URL 사용
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
                // 기본 플레이스홀더
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

        // 우하단 + 버튼
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
 * 루틴 아이콘 다이얼로그 (서버 아이콘 URL만 표시)
 * ========================= */
@Composable
private fun RoutineIconDialog(
    title: String,
    onSelect: (iconId: Int, url: String) -> Unit,   // ✅ id와 url 함께 반환
    onDismiss: () -> Unit,
    vm: RoutineViewModel = viewModel()
) {
    // 서버 아이콘 로딩
    LaunchedEffect(Unit) { vm.fetchRoutineIcons() }
    val serverIcons by vm.icons.observeAsState(emptyList())
    val ctx = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
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

                // ✅ 응답받은 URL 목록을 5열 그리드로 모두 표시 (스크롤 가능)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
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
                                    // ✅ 선택 시 id와 원본 url 모두 전달
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

@Composable
private fun ActionCard(
    item: ActionUi,
    onDelete: (ActionUi) -> Unit
) {
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
                Icon(iconForDevice(item.device), contentDescription = null, tint = TextBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.item.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "delete", tint = Color(0xFFDD4B39))
            }
        }
    }
}

@Preview
@Composable
private fun CreateRoutineFirstScreenPreview() {
    val nav = androidx.navigation.compose.rememberNavController()
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        CreateRoutineFirstScreen(nav)
    }
}

private fun iconForDevice(device: String): ImageVector = when (device) {
    "에어컨" -> Icons.Filled.Star
    "선풍기" -> Icons.Filled.Star
    "TV" -> Icons.Filled.Star
    "공기청정기" -> Icons.Filled.Star
    "빔프로젝터" -> Icons.Filled.Star
    "조명" -> Icons.Filled.Star
    else -> Icons.Filled.Star
}
@Composable
private fun TimeWheelDialog(
    initialHour24: Int,
    initialMinute: Int,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var ampm by remember { mutableIntStateOf(if (initialHour24 >= 12) 1 else 0) } // 0=오전, 1=오후
    var hour12 by remember {
        mutableIntStateOf(
            when {
                initialHour24 == 0 -> 12
                initialHour24 > 12 -> initialHour24 - 12
                else -> initialHour24
            }
        )
    }
    val minuteValues = listOf(0, 10, 20, 30, 40, 50)
    var minuteIndex by remember {
        mutableIntStateOf(minuteValues.indexOf(initialMinute).coerceAtLeast(0))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "시간 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        values = arrayOf("오전", "오후"),
                        value = ampm,
                        onValueChange = { ampm = it },
                        width = 72.dp
                    )
                    WheelPicker(
                        min = 1, max = 12,
                        value = hour12,
                        onValueChange = { hour12 = it },
                        width = 64.dp
                    )
                    WheelPicker(
                        values = arrayOf("00", "10", "20", "30", "40", "50"),
                        value = minuteIndex,
                        onValueChange = { minuteIndex = it },
                        width = 72.dp
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val m = minuteValues[minuteIndex]
                            val h24 = when {
                                ampm == 0 && hour12 == 12 -> 0
                                ampm == 1 && hour12 == 12 -> 12
                                ampm == 1 -> hour12 + 12
                                else -> hour12
                            }
                            onConfirm(h24, m)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("설정", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8EBF1),
                            contentColor = Color(0xFF4B5563)
                        )
                    ) {
                        Text("취소", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPicker(
    min: Int = 0,
    max: Int = 0,
    values: Array<String>? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
    width: Dp = 72.dp
) {
    AndroidView(
        modifier = Modifier
            .width(width)
            .height(150.dp),
        factory = { context ->
            NumberPicker(context).apply {
                wrapSelectorWheel = true
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                if (values != null) {
                    minValue = 0
                    maxValue = values.size - 1
                    displayedValues = values
                } else {
                    minValue = min
                    maxValue = max
                }
                this.value = value
                setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        setTextColor(android.graphics.Color.BLACK)
                    }
                } catch (_: Throwable) { /* ignore */ }
            }
        },
        update = { picker ->
            if (values != null) {
                if (picker.displayedValues?.contentEquals(values) != true) {
                    picker.displayedValues = values
                }
                picker.minValue = 0
                picker.maxValue = values.size - 1
            } else {
                picker.minValue = min
                picker.maxValue = max
            }
            picker.value = value
        }
    )
}
