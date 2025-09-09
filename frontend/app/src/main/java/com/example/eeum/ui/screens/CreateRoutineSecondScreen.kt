package com.example.eeum.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R

private val TextBlue = Color(0xFF3B82F6)
private val CardBg = Color(0x80FFFFFF)
private val BorderGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSecondScreen(navController: NavController) {
    var selectedDeviceIdx by remember { mutableIntStateOf(1) } // 기본 선택: "조명"
    var selectedDetailIdx by remember { mutableIntStateOf(1) } // 기본 선택: "조명이"
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("22:00") }

    val deviceItems = listOf(
        DeviceItem(R.drawable.ic_device, "선풍기"),
        DeviceItem(R.drawable.ic_light, "조명"),
        DeviceItem(R.drawable.ic_device, "에어컨"),
        DeviceItem(R.drawable.ic_device, "공기청정기"),
        DeviceItem(R.drawable.ic_device, "프로젝터")
    )

    val detailItems = listOf(
        DeviceItem(R.drawable.ic_light, "예배"),
        DeviceItem(R.drawable.ic_light, "조명이")
    )

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 디바이스 선택 카드
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("디바이스 선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            deviceItems.forEachIndexed { idx, item ->
                                RadioListRow(
                                    title = item.title,
                                    iconRes = item.iconRes,
                                    selected = selectedDeviceIdx == idx,
                                    onClick = { selectedDeviceIdx = idx }
                                )
                            }
                        }
                    }
                }

                // 세부 선택 카드
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("세부 선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            detailItems.forEachIndexed { idx, item ->
                                RadioListRow(
                                    title = item.title,
                                    iconRes = item.iconRes,
                                    selected = selectedDetailIdx == idx,
                                    onClick = { selectedDetailIdx = idx }
                                )
                            }
                        }
                    }
                }

                // 시간 설정 카드
                Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("시간 설정", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                        TimeRow(label = "시작 시간", time = startTime)
                        TimeRow(label = "종료 시간", time = endTime)
                    }
                }

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextBlue, contentColor = Color.White
                    )
                ) {
                    Text("동작 저장", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_CreateRoutineSecondScreen() {
    val nav = rememberNavController()
    CreateRoutineSecondScreen(navController = nav)
}

private data class DeviceItem(val iconRes: Int, val title: String)

@Composable
private fun RadioListRow(
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
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
private fun TimeRow(label: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Text(
                text = time,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
