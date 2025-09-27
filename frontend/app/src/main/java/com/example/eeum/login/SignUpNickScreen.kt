package com.example.eeum.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.ui.theme.Gray300
import com.example.eeum.ui.theme.Gray500

@Composable
fun SignUpNickScreen(
    onBackClick: () -> Unit = {},
    onComplete: () -> Unit = {},
    idText: String = "",
    emailText: String = "",
    password: String = "",
    viewModel: SignUpViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var nickname by remember { mutableStateOf("") }
    val suggestions = listOf("스마트홈마스터", "홈매니저", "집지킴이")
    val isValid = isValidNickname(nickname)
    
    // ViewModel 상태 관찰
    val isLoading by viewModel.isLoading.observeAsState(false)
    val isSignUpSuccess by viewModel.isSignUpSuccess.observeAsState(false)
    val error by viewModel.error.observeAsState()
    
    // 회원가입 성공 시 완료 화면으로 이동
    LaunchedEffect(isSignUpSuccess) {
        if (isSignUpSuccess) {
            viewModel.clearSignUpSuccess()
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Gray500
                )
            }

            Text(
                text = "eeum",
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                color = Blue600,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 65.dp)
        ) {
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Blue600,
                trackColor = Gray300.copy(alpha = 0.3f)
            )

            Text(
                text = "5/5",
                fontSize = 12.sp,
                color = Gray500,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "닉네임을 입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "서비스에서 사용할 닉네임을 설정해주세요.",
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Input label
            Text(
                text = "닉네임",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = { Text(text = "닉네임", color = Gray300, fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValid) Color(0xFF4CAF50) else Blue600,
                    unfocusedBorderColor = if (isValid) Color(0xFF4CAF50) else Gray300,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Blue600
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "2-10자의 한글, 영문, 숫자를 사용할 수 있습니다.",
                fontSize = 12.sp,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AI 추천 닉네임",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { s ->
                    FilterChip(
                        selected = nickname == s,
                        onClick = { nickname = s },
                        label = { Text(s) },
                        shape = RoundedCornerShape(12.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            borderWidth = 1.dp,
                            borderColor = Gray300,
                            selectedBorderColor = Gray300,
                            enabled = true,
                            selected = nickname == s,
                            disabledBorderColor = Gray300,
                            disabledSelectedBorderColor = Gray300
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = Blue600,
                            labelColor = MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // 에러 메시지 표시
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { 
                if (isValid && !isLoading) {
                    viewModel.signUp(
                        email = emailText,
                        loginId = idText,
                        nickname = nickname,
                        password = password
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValid) Blue600 else Gray300,
                contentColor = Color.White,
                disabledContainerColor = Gray300,
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = isValid && !isLoading
        ) {
            Text(
                text = if (isLoading) "처리 중..." else "완료",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

private fun isValidNickname(nick: String): Boolean {
    val regex = "^[A-Za-z0-9가-힣]{2,10}$".toRegex()
    return nick.matches(regex)
}

@Preview
@Composable
private fun SignUpNickScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignUpNickScreen()
    }
}
