package com.example.eeum.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eeum.R
import com.example.eeum.data.model.dto.ActionItem
import com.example.eeum.data.model.dto.NewActionResult
import com.example.eeum.data.model.dto.ActionUi

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineFirstScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf(0)) } // 0=일 ~ 6=토

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

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

                // 카드 3: 동작 설정 (리스트 + 추가 버튼)
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
            // 선택된 디바이스 아이콘
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
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "delete",
                    tint = Color(0xFFDD4B39)
                )
            }
        }
    }
}

private fun iconForDevice(device: String): ImageVector = when (device) {
    // 실제 아이콘으로 바꾸고 싶으면 둘 다 동일한 매핑 규칙으로 변경하세요.
    "에어컨" -> Icons.Filled.Star
    "선풍기" -> Icons.Filled.Star
    "TV" -> Icons.Filled.Star
    "공기청정기" -> Icons.Filled.Star
    "빔프로젝터" -> Icons.Filled.Star
    "조명" -> Icons.Filled.Star
    else -> Icons.Filled.Star
}

@Preview(showBackground = true)
@Composable
fun Preview_CreatetRoutineFirstScreen() {
    val nav = rememberNavController()
    CreateRoutineFirstScreen(navController = nav)
}
