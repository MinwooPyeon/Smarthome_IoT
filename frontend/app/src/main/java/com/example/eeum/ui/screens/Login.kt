package com.example.eeum.ui.screens
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.example.eeum.ui.components.CustomButton

@Composable
fun SimpleLoginScreen(modifier: Modifier = Modifier) {
    var idText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFB4E3FD),
                        Color(0xFFCCFCFF)
                    )
                )
            )
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 로고 이미지
        Image(
            painter = painterResource(id = R.drawable.ic_mainlogo),
            contentDescription = "Logo",
            modifier = Modifier.size(215.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        // 아이디 입력 필드
        OutlinedTextField(
            value = idText,
            onValueChange = { idText = it },
            label = { Text(
                text = "아이디",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                    color = Color(0xFFADAEBC),
                )
            ) },
            modifier = Modifier
                .border(width = 0.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(size = 12.dp))
                .width(326.dp)
                .height(50.dp)
                .background(color = Color.Transparent, shape = RoundedCornerShape(size = 12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent, // 포커스 됐을 때 테두리 색상 (선택 사항)
                unfocusedBorderColor = Color.Transparent // 포커스 안됐을 때 테두리 색상 (선택 사항)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text(text = "비밀번호", color = Color(0xffadaebc)) },
            modifier = Modifier
                .border(width = 0.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(size = 12.dp))
                .width(326.dp)
                .height(50.dp)
                .background(color = Color.Transparent, shape = RoundedCornerShape(size = 12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent, // 포커스 됐을 때 테두리 색상 (선택 사항)
                unfocusedBorderColor = Color.Transparent // 포커스 안됐을 때 테두리 색상 (선택 사항)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 로그인 버튼
        CustomButton(
            text = "로그인",
            onClick = { /* TODO: 로그인 로직 구현 */ },
            width = 330.dp,
            height = 50.dp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Google 로그인 버튼
        CustomButton(
            text = "Google 계정 로그인",
            onClick = { /* TODO: Google 로그인 로직 구현 */ },
            icon = painterResource(id = R.drawable.ic_google),
            iconTint = Color(0xff374151),
            backgroundColor = Color.White,
            textColor = Color(0xff374151),
            modifier = Modifier
                .shadow(elevation = 2.dp, spotColor = Color(0x0D000000), ambientColor = Color(0x0D000000))
                .border(width = 1.dp, color = Color(0xFFE5E7EB), shape = RoundedCornerShape(size = 12.dp))
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 회원가입 텍스트
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xff4b5563), fontSize = 14.sp)) {
                    append("‘이음’이 처음이신가요? ")
                }
                withStyle(style = SpanStyle(color = Color(0xff03a9f4), fontSize = 14.sp)) {
                    append("회원가입")
                }
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun SimpleLoginScreenPreview() {
    SimpleLoginScreen()
}
