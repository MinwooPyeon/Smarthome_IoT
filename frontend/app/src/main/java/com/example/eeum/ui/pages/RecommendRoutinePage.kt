package com.example.eeum.ui.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.data.model.dto.RecommendRoutine

private val DUMMY_RECOMMENDS = listOf(
    RecommendRoutine(
        icon = Icons.Filled.Star,
        iconBg = Color(0xFF8B5CF6), // Purple
        title = "저녁 휴식 모드",
        description = "취침 준비를 위한 따뜻한 조명, 릴렉스 음악, 아로마 디퓨저",
        time = "매일 오후 10:30"
    ),
    RecommendRoutine(
        icon = Icons.Filled.Star,
        iconBg = Color(0xFFF0524D), // Red
        title = "운동 준비 모드",
        description = "홈짐 조명 켜기, 운동 음악 재생, 환기 시작, 온도 조절",
        time = "월/수/금 오후 7:00"
    ),
    RecommendRoutine(
        icon = Icons.Filled.Star,
        iconBg = Color(0xFF34C759), // Green
        title = "주말 여유 모드",
        description = "늦잠을 위한 블랙아웃 커튼, 따뜻한 조명, 편안한 배경음악",
        time = "토/일 오전 9:30"
    )
)

// Colors
private val TitleColor = Color(0xFF0F172A)
private val BodyColor = Color(0xFF6B7280)
private val PrimaryBlue = Color(0xFF3D6EF7)
private val PrimaryBorderBlue = Color(0xFF007BFF) // 카드 테두리 색

@Preview(showBackground = false)
@Composable
fun RecommendRoutinePage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(DUMMY_RECOMMENDS) { item ->
            RecommendRoutineCard(
                data = item,
                onAddClick = { }
            )
        }
    }
}

@Composable
private fun RecommendRoutineCard(
    data: RecommendRoutine,
    onAddClick: () -> Unit
) {
    val days = parseDaysFrom(data.time)        // 실행 요일 파싱
    val timeText = extractTimeOfDay(data.time) // "오전/오후 HH:MM"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, PrimaryBorderBlue), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 좌측 아이콘
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(data.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            // 우측 내용
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = data.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = BodyColor
                )

                Spacer(Modifier.height(12.dp))

                // 실행 요일 (왼쪽 정렬)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("실행 요일:", fontSize = 14.sp, color = BodyColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.forEach { DayChipSquare(it) }
                    }
                }

                // ⬇ 실행 요일 ↔ 실행 시간 간격을 더 촘촘하게 (2dp)
                Spacer(Modifier.height(2.dp))

                // 실행 시간 (요일 밑줄) | 같은 줄 우측에 버튼
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "실행 시간: $timeText",
                        fontSize = 14.sp,
                        color = BodyColor,
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline() // 텍스트와 버튼을 베이스라인 기준으로 맞춤
                    )
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .alignByBaseline()
                    ) {
                        Text(
                            text = "추가하기",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChipSquare(label: String) {
    val size = 26.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(PrimaryBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

/** "월/수/금 오후 7:00" -> ["월","수","금"],  "매일 오후 10:30" -> ["매일"] */
private fun parseDaysFrom(text: String): List<String> {
    val head = text.substringBefore(' ', missingDelimiterValue = text).trim()
    if (head.isEmpty()) return emptyList()
    return if (head == "매일") listOf("매일") else head.split('/').map { it.trim() }
}

/** "월/수/금 오후 7:00" -> "오후 7:00", "매일 오후 10:30" -> "오전/오후 HH:MM" */
private fun extractTimeOfDay(text: String): String {
    val tail = text.substringAfter(' ', missingDelimiterValue = text).trim()
    return if (tail.isEmpty()) text else tail
}
