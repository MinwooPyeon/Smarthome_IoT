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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val PageBg = Color(0xFFE8F3FF) // 연한 하늘색 배경
private val BorderGray = Color(0xFFE0E0E0)
private val IconBg = Color(0xFFEAF2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSecondScreen(navController: NavController) {
    // 상태값들
    var selectedRoomIdx by remember { mutableIntStateOf(1) }     // 방 선택(거실/안방/발코니) - 기본 "안방"
    var selectedDeviceIdx by remember { mutableIntStateOf(1) }   // 디바이스 - 기본 "조명"
    var selectedStateIdx by remember { mutableIntStateOf(1) }    // 상태 설정 - 0=켜기, 1=끄기 (기본 끄기)
    var windLevel by remember { mutableIntStateOf(2) }           // 바람 세기 - 1~5 (기본 2)
    var acTemp by remember { mutableIntStateOf(24) }             // 에어컨 온도 (기본 24℃)

    val roomItems = listOf(
        RowItem(Icons.Filled.LocationOn, "거실"),
        RowItem(Icons.Filled.LocationOn, "안방"),
        RowItem(Icons.Filled.LocationOn, "발코니")
    )

    val deviceItems = listOf(
        RowItem(Icons.Filled.Star, "선풍기"),
        RowItem(Icons.Filled.Star, "조명"),
        RowItem(Icons.Filled.Star, "에어컨")
    )

    val stateItems = listOf(
        // 칩 텍스트 + 제목
        StateItem("ON", "켜기"),
        StateItem("OFF", "끄기")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
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
                        tint = Color.Black,
                        modifier = Modifier.clickable { navController.popBackStack() }
                    )
                    Text("루틴 만들기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    // 오른쪽 공간 맞춤용
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 방 선택
                SectionCard(title = "방 선택") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        roomItems.forEachIndexed { idx, item ->
                            RadioListRow(
                                title = item.title,
                                icon = item.icon,
                                selected = selectedRoomIdx == idx,
                                onClick = { selectedRoomIdx = idx }
                            )
                        }
                    }
                }

                // 디바이스 선택
                SectionCard(title = "디바이스 선택") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        deviceItems.forEachIndexed { idx, item ->
                            RadioListRow(
                                title = item.title,
                                icon = item.icon,
                                selected = selectedDeviceIdx == idx,
                                onClick = { selectedDeviceIdx = idx }
                            )
                        }
                    }
                }

                // 상태 설정 (켜기/끄기) — 칩 + 라디오
                SectionCard(title = "상태 설정") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stateItems.forEachIndexed { idx, item ->
                            RadioListRowWithChip(
                                chipText = item.chip,
                                title = item.title,
                                selected = selectedStateIdx == idx,
                                onClick = { selectedStateIdx = idx }
                            )
                        }
                    }
                }

                // 바람 세기 (세그먼트)
                SectionCard(title = "바람 세기") {
                    SegmentedNumberSelector(
                        count = 5,
                        selected = windLevel,
                        onSelect = { windLevel = it }
                    )
                }

                // 에어컨 온도
                SectionCard(title = "에어컨 온도") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 96.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${acTemp}°C", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SquareIconButton(
                                icon = Icons.Filled.KeyboardArrowUp,
                                onClick = { if (acTemp < 30) acTemp++ }
                            )
                            SquareIconButton(
                                icon = Icons.Filled.KeyboardArrowDown,
                                onClick = { if (acTemp > 16) acTemp-- }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(6.dp))
                        Text("동작 저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8F3FF)
@Composable
private fun Preview_CreateRoutineSecondScreen() {
    val nav = rememberNavController()
    CreateRoutineSecondScreen(navController = nav)
}

/* ---------- UI 구성요소들 ---------- */

private data class RowItem(val icon: ImageVector, val title: String)
private data class StateItem(val chip: String, val title: String)

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Column(content = content)
        }
    }
}

@Composable
private fun RadioListRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TextBlue)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
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
private fun RadioListRowWithChip(
    chipText: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 칩
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, TextBlue),
                color = Color.Transparent,
                modifier = Modifier
                    .height(28.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chipText, color = TextBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
private fun SegmentedNumberSelector(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        (1..count).forEach { i ->
            val isSelected = i == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) TextBlue else Color.White,
                border = BorderStroke(1.dp, if (isSelected) TextBlue else BorderGray),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onSelect(i) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$i",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun SquareIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = TextBlue,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}
