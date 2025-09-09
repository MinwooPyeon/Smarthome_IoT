package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R

// Colors
private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)          // 반투명 화이트
private val BorderGray = Color(0xFFE0E0E0)
private val AccentPill = Color(0xFFEAF2FF)
private val ScreenBg = Color(0x80B8E7FD)        // #B8E7FD, 50% 투명 (0x80 = 50%)

// -------- Entry --------
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CreateRoutineSecondScreen() {
    var selectedRoom by remember { mutableIntStateOf(1) }     // 안방
    var selectedDevice by remember { mutableIntStateOf(1) }   // 조명
    var isOn by remember { mutableStateOf(false) }             // 끄기 선택
    var windLevel by remember { mutableIntStateOf(2) }         // 1~5
    var temperature by remember { mutableIntStateOf(24) }

    val rooms = listOf("거실", "안방", "발코니")
    val devices = listOf("선풍기", "조명", "에어컨")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg) // 배경을 #B8E7FD 50% 투명으로
    ) {
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
                        tint = Color.Black
                    )
                    Text("루틴 만들기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(" ", fontSize = 16.sp) // 우측 정렬용
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 방 선택
                SectionCard(title = "방 선택") {
                    rooms.forEachIndexed { idx, title ->
                        SelectRow(
                            title = title,
                            selected = selectedRoom == idx,
                            onClick = { selectedRoom = idx }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 디바이스 선택
                SectionCard(title = "디바이스 선택") {
                    devices.forEachIndexed { idx, title ->
                        SelectRow(
                            title = title,
                            selected = selectedDevice == idx,
                            onClick = { selectedDevice = idx }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 상태 설정 (켜기/끄기)
                SectionCard(title = "상태 설정") {
                    SelectRow(
                        title = "켜기",
                        badge = "ON",
                        selected = isOn,
                        onClick = { isOn = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    SelectRow(
                        title = "끄기",
                        badge = "OFF",
                        selected = !isOn,
                        onClick = { isOn = false }
                    )
                }

                // 바람 세기
                SectionCard(title = "바람 세기") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { lvl ->
                            LevelChip(
                                label = lvl.toString(),
                                selected = windLevel == lvl,
                                onClick = { windLevel = lvl }
                            )
                        }
                    }
                }

                // 에어컨 온도
                SectionCard(title = "에어컨 온도") {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${temperature}°C", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            ControlButton("▲") { temperature++ }
                            ControlButton("▼") { temperature-- }
                        }
                    }
                }

                // 저장 버튼
                Button(
                    onClick = { /* TODO: 저장 처리 */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue, contentColor = Color.White
                    )
                ) {
                    Text("…   동작 저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// -------- Reusable UI --------
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, Color(0x22000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SelectRow(
    title: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val borderColor = if (selected) TextBlue else BorderGray
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable { onClick() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측 아이콘/배지 캡슐
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentPill),
                contentAlignment = Alignment.Center
            ) {
                Text(badge ?: "📍", fontSize = if (badge == null) 14.sp else 10.sp, fontWeight = FontWeight.SemiBold, color = TextBlue)
            }
            Spacer(Modifier.width(12.dp))

            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // 우측 라디오 점
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = TextBlue,
                    unselectedColor = BorderGray
                )
            )
        }
    }
}

@Composable
private fun LevelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Color(0xFFF7FAFF),
        border = BorderStroke(1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier
            .height(40.dp)
            .width(48.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (selected) TextBlue else Color(0xFF6B7280))
        }
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier
            .size(width = 72.dp, height = 44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextBlue)
        }
    }
}
