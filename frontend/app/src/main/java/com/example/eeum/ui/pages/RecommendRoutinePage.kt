package com.example.eeum.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
private val CardStroke = Color(0xFFE6F1FF)
private val TitleColor = Color(0xFF0F172A)
private val BodyColor = Color(0xFF6B7280)
private val PrimaryBlue = Color(0xFF3D6EF7)
private val BadgeBg = Color(0xFFFFEBCD)
private val BadgeFg = Color(0xFFF59E0B)
@Preview(showBackground = false)
@Composable
fun RecommendRoutinePage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(), // transparent background
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(DUMMY_RECOMMENDS) { item ->
            RecommendRoutineCard(
                data = item,
                onAddClick = { /* TODO: Add routine */ }
            )
        }
    }
}
@Composable
private fun RecommendRoutineCard(
    data: RecommendRoutine,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        // 아이콘 왼쪽, 나머지는 모두 오른쪽 컬럼에 배치
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 아이콘
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

            // 타이틀 · 설명 · 시간 · 버튼이 같은 시작선에서 세로로 정렬
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 상단: 타이틀(+배지 공간)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = data.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TitleColor
                    )

                    // 배지 필요하면 주석 해제
                    // Text(
                    //     text = "추천",
                    //     color = BadgeFg,
                    //     fontSize = 12.sp,
                    //     fontWeight = FontWeight.Medium,
                    //     modifier = Modifier
                    //         .clip(RoundedCornerShape(8.dp))
                    //         .background(BadgeBg)
                    //         .padding(horizontal = 8.dp, vertical = 4.dp)
                    // )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = data.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = BodyColor
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.time,
                        fontSize = 12.sp,
                        color = BodyColor
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
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
