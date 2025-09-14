package com.example.eeum.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eeum.R
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

    // 시간 상태 (24시간제)
    var hour24 by remember { mutableIntStateOf(8) } // 08시
    var minute by remember { mutableIntStateOf(0) } // 00분
    var showTimePicker by remember { mutableStateOf(false) }
    val timeText = remember(hour24, minute) { "%02d:%02d".format(hour24, minute) }

    val actions = remember {
        mutableStateListOf(
            ActionUi(ActionItem(1, "거실 · 조명 켜기", "08:00 - 22:00"), "조명"),
            ActionUi(ActionItem(2, "안방 · 가습기 켜기", "09:00 - 21:00"), "공기청정기")
        )
    }
    var nextId by remember { mutableIntStateOf(3) }

    // 세컨드에서 돌아올 때 저장된 결과를 꺼내 리스트에 1회 추가
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
                // 카드 1: 루틴 이름/설명
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 이미지 선택 영역 (아바타 + 카메라 버튼)
                            RoutineImagePicker(
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

                //카드 3: 시간 설정
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

                // 카드 4: 동작 설정 (리스트 + 추가 버튼)
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            ) {
                                Text("동작 추가", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // 완료 버튼
                item {
                    Button(
                        onClick = { /* TODO: 완료 저장 */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextBlue, contentColor = Color.White
                        )
                    ) { Text("완료", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }
    }

        //시간 휠 다이얼로그
        if (showTimePicker) {
            TimeWheelDialog(
                initialHour24 = hour24,
                initialMinute = minute,
                onConfirm = { h, m ->
                    hour24 = h
                    minute = m
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }

// 프로필/루틴 이미지 업로드 영역
@Composable
private fun RoutineImagePicker(modifier: Modifier = Modifier) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }
    val context = LocalContext.current
    val imageBitmap = remember(selectedImageUri) {
        selectedImageUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val corner = 12.dp

    Box(modifier = modifier.size(120.dp)) {
        // 외곽 Surface도 12dp 라운드로!
        Surface(
            shape = RoundedCornerShape(corner),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxSize()
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "routine image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))  // 내부 이미지도 동일 반경으로 클립
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_mainlogo),
                    contentDescription = "placeholder",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(corner))  // 플레이스홀더도 동일
                )
            }
        }

        // 카메라(추가) 버튼
        Surface(
            shape = CircleShape,
            color = TextBlue,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clickable { launcher.launch("image/*") }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "이미지 추가",
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
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
    // 초기값을 오전/오후 & 12시간제로 변환
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
                // 제목
                Text(
                    text = "시간 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                // 피커 3종
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 오전/오후
                    WheelPicker(
                        values = arrayOf("오전", "오후"),
                        value = ampm,
                        onValueChange = { ampm = it },
                        width = 72.dp
                    )
                    // 시: 1~12
                    WheelPicker(
                        min = 1, max = 12,
                        value = hour12,
                        onValueChange = { hour12 = it },
                        width = 64.dp
                    )
                    // 분: 00,10,20,30,40,50
                    WheelPicker(
                        values = arrayOf("00", "10", "20", "30", "40", "50"),
                        value = minuteIndex,
                        onValueChange = { minuteIndex = it },
                        width = 72.dp
                    )
                }

                Spacer(Modifier.height(18.dp))

                // 하단 버튼 두 개 (좌: 파랑 설정 / 우: 회색 취소)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val m = minuteValues[minuteIndex]
                            val h24 = when {
                                ampm == 0 && hour12 == 12 -> 0      // 12 AM -> 00
                                ampm == 1 && hour12 == 12 -> 12     // 12 PM -> 12
                                ampm == 1 -> hour12 + 12            // PM
                                else -> hour12                      // AM
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
                            containerColor = Color(0xFFE8EBF1),   // 연회색
                            contentColor = Color(0xFF4B5563)       // 진회색 텍스트
                        )
                    ) {
                        Text("취소", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// 그대로 사용 (필요시 높이/폭만 조정)
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

                // (선택) API 29+ 에서 선택 텍스트 색만 살짝 진하게
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
