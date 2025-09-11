package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import com.example.eeum.ui.theme.EeumTheme

private val BrandBlue = Color(0xFF007AFF)

@Composable
fun SelectFloorplanScreen() {
    var selectedSize by remember { mutableStateOf("79m² (24평형)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 60.dp)
    ) {
        // 상단바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { /* TODO: 뒤로가기 */ }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "평면도 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // 안내 문구
        Text(
            text = "평면도를 선택해주세요.",
            fontSize = 16.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 아파트명
        Text(
            text = "비스타캐슬",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 평수 선택 Row (단일 선택)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            FloorplanChip(
                label = "43m² (13평형)",
                selected = selectedSize == "43m² (13평형)",
                onClick = { selectedSize = "43m² (13평형)" }
            )
            FloorplanChip(
                label = "79m² (24평형)",
                selected = selectedSize == "79m² (24평형)",
                onClick = { selectedSize = "79m² (24평형)" }
            )
        }

        // 도면 이미지
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.examplefloor),
                contentDescription = "평면도",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 이전 / 다음 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 🔹 테두리 없는 버튼 (흰 배경 + 파란 텍스트)
            Button(
                onClick = { /* TODO: 이전 */ },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BrandBlue
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp) // 그림자도 제거
            ) {
                Text(text = "이전", fontSize = 14.sp)
            }

            // 저장 버튼 (파란 배경 + 흰 텍스트)
            Button(
                onClick = { /* TODO: 다음 */ },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(text = "다음", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FloorplanChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandBlue else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) Color.White else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun SelectFloorplanScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SelectFloorplanScreen()
    }
}
