package com.example.eeum.ui.screens

import android.os.Parcelable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eeum.data.model.dto.NewActionResult
import kotlinx.parcelize.Parcelize

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val PageBg = Color(0xFFE8F3FF)
private val BorderGray = Color(0xFFE0E0E0)
private val IconBg = Color(0xFFEAF2FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSecondScreen(navController: NavController) {
    val roomItems = listOf(
        RowItem(Icons.Filled.LocationOn, "거실"),
        RowItem(Icons.Filled.LocationOn, "안방"),
        RowItem(Icons.Filled.LocationOn, "발코니")
    )
    val deviceItems = listOf(
        RowItem(Icons.Filled.Star, "에어컨"),
        RowItem(Icons.Filled.Star, "선풍기"),
        RowItem(Icons.Filled.Star, "TV"),
        RowItem(Icons.Filled.Star, "공기청정기"),
        RowItem(Icons.Filled.Star, "빔프로젝터"),
        RowItem(Icons.Filled.Star, "조명")
    )
    val stateItems = listOf(
        StateItem("ON", "켜기"),
        StateItem("OFF", "끄기")
    )

    var selectedRoomIdx by remember { mutableIntStateOf(1) } // 안방
    val defaultDeviceIndex = remember { deviceItems.indexOfFirst { it.title == "조명" }.let { if (it == -1) 0 else it } }
    var selectedDeviceIdx by remember { mutableIntStateOf(defaultDeviceIndex) } // 기본 조명
    var selectedStateIdx by remember { mutableIntStateOf(1) } // 끄기
    var windLevel by remember { mutableIntStateOf(2) }
    var acTemp by remember { mutableIntStateOf(24) }

    val selectedDeviceTitle = deviceItems.getOrNull(selectedDeviceIdx)?.title ?: ""
    val showWind = selectedDeviceTitle == "선풍기"
    val showAcTemp = selectedDeviceTitle == "에어컨"

    Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
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
                                title = item.title, icon = item.icon,
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
                                title = item.title, icon = item.icon,
                                selected = selectedDeviceIdx == idx,
                                onClick = { selectedDeviceIdx = idx }
                            )
                        }
                    }
                }

                // 상태 설정
                SectionCard(title = "상태 설정") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stateItems.forEachIndexed { idx, item ->
                            RadioListRowWithChip(
                                chipText = item.chip, title = item.title,
                                selected = selectedStateIdx == idx,
                                onClick = { selectedStateIdx = idx }
                            )
                        }
                    }
                }

                // 조건부 섹션
                if (showWind) {
                    SectionCard(title = "바람 세기") {
                        SegmentedNumberSelector(
                            count = 5, selected = windLevel,
                            onSelect = { windLevel = it }
                        )
                    }
                }
                if (showAcTemp) {
                    SectionCard(title = "에어컨 온도") {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("${acTemp}°C", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                SquareIconButton(icon = Icons.Filled.KeyboardArrowUp) { if (acTemp < 30) acTemp++ }
                                SquareIconButton(icon = Icons.Filled.KeyboardArrowDown) { if (acTemp > 16) acTemp-- }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 동작 저장 → 결과 전달 후 이전 화면으로
                Button(
                    onClick = {
                        val result = NewActionResult(
                            room = roomItems[selectedRoomIdx].title,
                            device = selectedDeviceTitle,
                            state = if (selectedStateIdx == 0) "켜기" else "끄기",
                            windLevel = if (showWind) windLevel else null,
                            acTemp = if (showAcTemp) acTemp else null
                        )
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("new_action", result)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextBlue, contentColor = Color.White)
                ) {
                    Text("동작 저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

private data class RowItem(val icon: ImageVector, val title: String)
private data class StateItem(val chip: String, val title: String)

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Column(content = content)
        }
    }
}

@Preview
@Composable
private fun CreateRoutineSecondScreenPreview() {
    val nav = androidx.navigation.compose.rememberNavController()
    com.example.eeum.ui.theme.EeumTheme(dynamicColor = false) {
        CreateRoutineSecondScreen(nav)
    }
}

@Composable
private fun RadioListRow(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(IconBg),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = TextBlue) }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TextBlue, unselectedColor = BorderGray)
            )
        }
    }
}

@Composable
private fun RadioListRowWithChip(chipText: String, title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TextBlue else BorderGray),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, TextBlue), color = Color.Transparent) {
                Box(Modifier.height(28.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(chipText, color = TextBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            RadioButton(
                selected = selected, onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TextBlue, unselectedColor = BorderGray)
            )
        }
    }
}

@Composable
private fun SegmentedNumberSelector(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..count).forEach { i ->
            val isSelected = i == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) TextBlue else Color.White,
                border = BorderStroke(1.dp, if (isSelected) TextBlue else BorderGray),
                modifier = Modifier.weight(1f).height(44.dp).clickable { onSelect(i) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$i", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else Color.Black)
                }
            }
        }
    }
}

@Composable
private fun SquareIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(color = TextBlue, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color.White) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8F3FF)
@Composable
private fun Preview_CreateRoutineSecondScreen() {
    val nav = rememberNavController()
    CreateRoutineSecondScreen(navController = nav)
}
