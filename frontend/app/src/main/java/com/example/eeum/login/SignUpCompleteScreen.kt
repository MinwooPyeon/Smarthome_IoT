package com.example.eeum.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.ui.components.CustomButton
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.ui.theme.Gray500
import com.example.eeum.ui.theme.Green500

@Composable
fun SignUpCompleteScreen(
    onStart: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        // 완료 아이콘 (초록 원 + 체크)
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(15.dp, shape = CircleShape)
                .background(Green500, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "가입 완료",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "회원가입 완료!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "이음 스마트홈 서비스에\n성공적으로 가입되었습니다.",
            fontSize = 16.sp,
            color = Gray500,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // 시작하기 버튼 (프로젝트 공통 커스텀 버튼)
        CustomButton(
            text = "시작하기",
            onClick = onStart,
            modifier = Modifier
                .padding(bottom = 24.dp)
        )

        Text(
            text = "eeum",
            fontSize = 30.sp,
            color = Blue600,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun SignUpCompleteScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignUpCompleteScreen()
    }
}
