// MyRoutinePage.kt
package com.example.eeum.ui.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eeum.base.ApplicationClass
import com.example.eeum.core.AppEffect
import com.example.eeum.core.AppEventBus
import com.example.eeum.data.model.response.routine.RoutineData
import com.example.eeum.ui.screens.RoutineViewModel
import com.example.eeum.util.ResourceUtils
import java.time.LocalTime

// 기존 DUMMY 제거
private val BodyColor  = Color(0xFF6B7280)
private val PrimaryBlue = Color(0xFF3D6EF7)

@Preview
@Composable
fun MyRoutinePage(
    navController: androidx.navigation.NavController? = null,
    viewModel: RoutineViewModel = viewModel()
) {
    // 최초 로딩
    LaunchedEffect(Unit) { viewModel.fetchAllRoutines() }

    LaunchedEffect(Unit) {
        AppEventBus.effects.collect { eff ->
            if (eff is AppEffect.RoutinesChanged) {
                viewModel.fetchAllRoutines()
            }
        }
    }

    // 삭제 확인 다이얼로그 상태
    var routineToDelete by remember { mutableStateOf<RoutineData?>(null) }

    val routines by viewModel.routines.observeAsState(emptyList())
    val deleteMessage by viewModel.deleteMessage.observeAsState()
    val error by viewModel.error.observeAsState()
    val context = LocalContext.current

    // 삭제 메시지 처리
    LaunchedEffect(deleteMessage) {
        deleteMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearDeleteMessage()
        }
    }

    // 에러 메시지 처리
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val dayTabs = remember { listOf("전체","월","화","수","목","금","토","일") }
    // 요일 필터(응답의 요일을 모아서 구성)
    val allDays = remember(routines) {
        val set = linkedSetOf("전체")
        routines.forEach { r ->
            ResourceUtils.formatWeekdays(r.routineWeekday)
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.forEach { set.add(it) }
        }
        if (set.size == 1) set.addAll(listOf("월","화","수","목","금","토","일"))
        set.toList()
    }

    var selected by remember { mutableStateOf("전체") }
    val filtered = remember(selected, routines) {
        if (selected == "전체") routines
        else routines.filter { r ->
            ResourceUtils.formatWeekdays(r.routineWeekday)
                ?.split(",")
                ?.any { it == selected } == true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DayFilterBar(
                days = dayTabs,
                selected = selected,
                onSelect = { selected = it }
            )
            Spacer(Modifier.height(4.dp))
        }

        items(filtered) { routine ->
            MyRoutineCard(
                routine = routine,
                onToggle = { changedTo ->
                    //토글 API
                },
                onEdit = { routineToEdit ->
                    navController?.navigate("editRoutineFirst/${routineToEdit.routineId}")
                },
                onDelete = { routine ->
                    routineToDelete = routine
                }
            )
        }
    }

    // 삭제 확인 다이얼로그
    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("루틴 삭제") },
            text = { Text("'${routine.name}' 루틴을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeRoutine(routine.routineId)
                        routineToDelete = null
                    }
                ) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { routineToDelete = null }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

private fun formatKoreanTime(actTime: String?): String? {
    if (actTime.isNullOrBlank()) return null
    return try {
        val t = LocalTime.parse(actTime) // expects HH:mm:ss
        val h = t.hour
        val m = t.minute
        val period = if (h < 12) "오전" else "오후"
        val h12 = when {
            h == 0 -> 12
            h <= 12 -> h
            else -> h - 12
        }
        "%s %d:%02d".format(period, h12, m)
    } catch (_: Exception) {
        actTime
    }
}

@Composable
private fun DayFilterBar(
    days: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val chipShape = RoundedCornerShape(8.dp)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(days) { day ->
            val isSelected = day == selected
            Surface(
                onClick = { onSelect(day) },
                shape = chipShape,
                color = if (isSelected) PrimaryBlue else Color.White,
                tonalElevation = 0.dp,
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun MyRoutineCard(
    routine: RoutineData,
    onToggle: (Boolean) -> Unit,
    onEdit: (RoutineData) -> Unit = {},
    onDelete: (RoutineData) -> Unit = {}
) {
    var enabled by remember(routine.routineId) { mutableStateOf(routine.triggerType) }
    var showMenu by remember { mutableStateOf(false) }

    val textIndent = 56.dp + 14.dp
    val statusTextColor = if (enabled) Color(0xFF22C55E) else BodyColor
    val dayLabels: List<String> =
        ResourceUtils.formatWeekdays(routine.routineWeekday)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    val formattedTime = remember(routine.actTime) { formatKoreanTime(routine.actTime) }

    val cardModifier = if (routine.isAi) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(BorderStroke(2.dp, PrimaryBlue), RoundedCornerShape(12.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {

            // 우측 상단 상태 + 메뉴
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(active = enabled)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (enabled) "활성화" else "비활성화",
                        fontSize = 13.sp,
                        color = statusTextColor
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "more")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                showMenu = false
                                onEdit(routine)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                onDelete(routine)
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            ) {
                // 아이콘은 iconId 매핑이 정해지기 전이라 임시 박스만 유지
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(iconUrl = routine.iconUrl, modifier = Modifier.offset(y = (-5).dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        routine.name?.let {
                            Text(
                                it,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        routine.routineDescription?.let {
                            Text(
                                it,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = BodyColor
                            )
                        }
                    }
                }

                // 실행 요일
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = textIndent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("실행 요일:", fontSize = 14.sp, color = BodyColor)
                    Spacer(Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayLabels.forEach { DayChip(label = it) }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 실행 시간 표시
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = textIndent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("실행 시간:", fontSize = 14.sp, color = BodyColor)
                    Spacer(Modifier.width(8.dp))
                    Text(formattedTime ?: "-", fontSize = 14.sp, color = BodyColor)
                }
            }

            // 스위치: 카드 우하단 고정
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    onToggle(it)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-5).dp)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun IconBox(iconUrl: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val ctx = LocalContext.current
    val absoluteUrl = remember(iconUrl) { toAbsoluteUrl(ApplicationClass.SERVER_URL, iconUrl) }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(shape)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (absoluteUrl.isNullOrBlank()) {
            Text("★", color = Color.White)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(absoluteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
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
private fun StatusDot(active: Boolean) {
    val dotColor = if (active) Color(0xFF22C55E) else Color(0xFF94A3B8)
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

@Composable
private fun DayChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF2563EB))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
