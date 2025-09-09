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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.data.model.dto.MyRoutine

private val DUMMY_MY_ROUTINES = listOf(
    MyRoutine(Icons.Filled.Star, Color(0xFFFF8A3D), "모닝 루틴", "싸피 안가는 날 아침 루틴", listOf("월","화","수"), false),
    MyRoutine(Icons.Filled.Star, Color(0xFF7C83FF), "저녁 루틴", "조명 어둡게, 온도 낮추기, 보안 시스템 활성화", listOf("금"), true),
    MyRoutine(Icons.Filled.Star, Color(0xFF2DD07F), "주말 휴식 루틴", "늦잠 모드, 따뜻한 조명, 편안한 음악", listOf("토","일"), false),
    MyRoutine(Icons.Filled.Star, Color(0xFFF0524D), "운동 루틴", "홈짐 조명 켜기, 운동 음악 재생, 환기 시작", listOf("화"), true)
)

private val TitleColor = Color(0xFF0F172A)
private val BodyColor  = Color(0xFF6B7280)
private val PrimaryBlue = Color(0xFF3D6EF7)

@Preview(showBackground = false)
@Composable
fun MyRoutinePage() {
    var selected by remember { mutableStateOf("전체") }
    val days = listOf("전체","월","화","수","목","금","토","일")

    val filtered = remember(selected) {
        if (selected == "전체") DUMMY_MY_ROUTINES
        else DUMMY_MY_ROUTINES.filter { it.days.contains(selected) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(), // 배경 투명
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 상단 요일 선택 바
        item {
            DayFilterBar(
                days = days,
                selected = selected,
                onSelect = { selected = it }
            )
            Spacer(Modifier.height(4.dp))
        }

        items(filtered) { routine ->
            MyRoutineCard(routine = routine, onToggle = { _, _ -> })
        }
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
                // 테두리는 선택 안 됐을 때만(Surface의 border로)
                border = if (isSelected) null
                else BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                        color = if (isSelected) Color.White else TitleColor
                    )
                }
            }
        }
    }
}


@Composable
private fun MyRoutineCard(
    routine: MyRoutine,
    onToggle: (MyRoutine, Boolean) -> Unit
) {
    var enabled by remember { mutableStateOf(routine.enabled) }

    // description 시작선 = 아이콘(56) + 간격(14)
    val textIndent = 56.dp + 14.dp
    val statusTextColor = if (enabled) Color(0xFF22C55E) else BodyColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {

            // ⬅️ 우측 상단: 상태(점+텍스트) + 메뉴 아이콘
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
                IconButton(onClick = { /* menu */ }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "more")
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 12.dp,
                    bottom = 16.dp
                )
            ) {
                // 아이콘 + 제목/설명
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(icon = routine.icon, bg = routine.iconBg)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            routine.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TitleColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            routine.description,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = BodyColor
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = textIndent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("실행 요일:", fontSize = 14.sp, color = BodyColor)
                    Spacer(Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        routine.days.forEach { DayChip(label = it) } // 정사각 칩 유지
                    }

                    Spacer(Modifier.weight(1f))

                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            onToggle(routine, it)
                        }
                    )
                }
            }
        }
    }
}




@Composable
private fun IconBox(icon: ImageVector, bg: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White) }
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
