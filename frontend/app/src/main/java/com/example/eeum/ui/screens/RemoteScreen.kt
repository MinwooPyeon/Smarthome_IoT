package com.example.eeum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.eeum.R
import com.example.eeum.ui.theme.EeumTheme

@Composable
fun RemoteScreen(navController: NavHostController? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBFE6FF)) // 연한 하늘색 배경
            .padding(horizontal = 16.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 타이틀
        Text(
            text = "거실 에어컨",
            style = TextStyle(
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                color = Color(0xFF0F172A)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 버튼 그리드 (3행 × 4열)
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) { // 3행
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { // 4열
                        Button(
                            onClick = { /* TODO: 버튼 기능 */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(56.dp) // 원 크기
                        ) { }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RemoteScreenPreview() {
    EeumTheme(dynamicColor = false) {
        RemoteScreen()
    }
}
