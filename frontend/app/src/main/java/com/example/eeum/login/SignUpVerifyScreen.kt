package com.example.eeum.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.ui.theme.Gray300
import com.example.eeum.ui.theme.Gray500
import kotlinx.coroutines.delay

@Composable
fun SignUpVerifyScreen(
    userId: String = "",
    email: String = "",
    signUpViewModel: SignUpViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onVerify: (String) -> Unit = {},
    onResendCode: () -> Unit = {}
) {
    var verificationCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(300) }  // 5분
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    // ViewModel 상태 관찰
    val isVerifying by signUpViewModel.isVerifying.observeAsState(false)
    val isEmailVerified by signUpViewModel.isEmailVerified.observeAsState(false)
    val verifyError by signUpViewModel.verifyError.observeAsState()

    // 타이머 효과
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }
    
    // 인증 성공 시 처리
    LaunchedEffect(isEmailVerified) {
        if (isEmailVerified) {
            onVerify(verificationCode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

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
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }
        
        // Progress indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 65.dp)
        ) {
            LinearProgressIndicator(
                progress = { 0.6f }, // 3/5
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Blue600,
                trackColor = Gray300.copy(alpha = 0.3f)
            )
            
            Text(
                text = "3/5",
                fontSize = 12.sp,
                color = Gray500,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Title
            Text(
                text = "인증번호를 입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = "${email}으로 발송된\n6자리 인증번호를 입력해주세요",
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 인증번호 입력 박스들
            Column {
                // 개별 입력 박스들
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                focusRequester.requestFocus()
                            } catch (e: Exception) {
                                // 포커스 요청 실패 시 무시
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(6) { index ->
                        VerificationCodeBox(
                            value = verificationCode.getOrNull(index)?.toString() ?: "",
                            isFocused = verificationCode.length == index,
                            isFilled = index < verificationCode.length,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    try {
                                        focusRequester.requestFocus()
                                    } catch (e: Exception) {
                                        // 포커스 요청 실패 시 무시
                                    }
                                }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 숨겨진 입력 필드
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { newValue ->
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            verificationCode = newValue
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.Transparent,
                        unfocusedTextColor = Color.Transparent,
                        cursorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "남은 시간: ${String.format("%02d:%02d", timeLeft / 60, timeLeft % 60)}",
                fontSize = 14.sp,
                color = if (timeLeft <= 30) Color.Red else Gray500,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = if (timeLeft <= 30) FontWeight.Medium else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resend button
            TextButton(
                onClick = {
                    onResendCode()
                    timeLeft = 180 // 타이머 리셋
                    verificationCode = "" // 인증번호 초기화
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "인증번호를 받지 못하셨나요? 재전송",
                    fontSize = 14.sp,
                    color = Blue600,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 에러 메시지 표시
            verifyError?.let { error ->
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Verify Button
            Button(
                onClick = { 
                    if (verificationCode.length == 6 && !isVerifying) {
                        signUpViewModel.verifyEmailCode(email, verificationCode)
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (verificationCode.length == 6 && !isVerifying) Blue600 else Gray300,
                    contentColor = Color.White,
                    disabledContainerColor = Gray300,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = verificationCode.length == 6 && !isVerifying
            ) {
                Text(
                    text = if (isVerifying) "인증 중..." else "인증 확인",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom helper text
        Text(
            text = "인증번호가 도착하지 않은 분 스팩함을 확인하거나\n잠시 후 다시 시도해주세요",
            fontSize = 12.sp,
            color = Gray500,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 50.dp),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun VerificationCodeBox(
    value: String,
    isFocused: Boolean,
    isFilled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 2.dp,
                color = when {
                    isFilled -> Blue600 // 채워진 칸은 파란색
                    isFocused -> Blue600 // 현재 입력 위치도 파란색
                    else -> Gray300 // 빈 칸은 회색
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFilled) Blue600 else Gray500,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun SignUpVerifyScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignUpVerifyScreen(
            email = "example@email.com"
        )
    }
}
