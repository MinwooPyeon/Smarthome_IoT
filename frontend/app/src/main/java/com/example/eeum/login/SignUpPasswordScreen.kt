package com.example.eeum.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.ui.theme.Gray300
import com.example.eeum.ui.theme.Gray500

@Composable
fun SignUpPasswordScreen(
    userId: String = "",
    email: String = "",
    onBackClick: () -> Unit = {},
    onNext: (password: String) -> Unit = {}
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    // 비밀번호 유효성 검사
    val isPasswordValid = password.length >= 8 && 
            password.any { it.isUpperCase() || it.isLowerCase() } &&
            password.any { it.isDigit() }
    
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    
    val canProceed = isPasswordValid && passwordsMatch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),            verticalAlignment = Alignment.CenterVertically
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
                progress = { 0.8f }, // 4/5
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Blue600,
                trackColor = Gray300.copy(alpha = 0.3f)
            )
            
            Text(
                text = "4/5",
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
                text = "비밀번호를 설정해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = "안전한 계정을 위해 8자 이상의 비밀번호를 입력해주세요.",
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Password Input Label
            Text(
                text = "비밀번호",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "비밀번호를 입력하세요",
                        color = Gray300,
                        fontSize = 16.sp
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible }
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Info else Icons.Default.Done,
                            contentDescription = if (isPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                            tint = if (!isPasswordVisible && isPasswordValid) Color(0xFF4CAF50) else Gray300 // 유효할 때 초록색
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue600,
                    unfocusedBorderColor = Gray300,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Blue600
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Password requirements
            Text(
                text = "8자 이상",
                fontSize = 12.sp,
                color = if (password.length >= 8) Blue600 else Gray500,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm Password Input Label
            Text(
                text = "비밀번호 확인",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = {
                    Text(
                        text = "비밀번호를 다시 입력하세요",
                        color = Gray300,
                        fontSize = 16.sp
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
                    ) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Info else Icons.Default.Done,
                            contentDescription = if (isConfirmPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                            tint = if (!isConfirmPasswordVisible && passwordsMatch && confirmPassword.isNotEmpty()) Color(0xFF4CAF50) else Gray300 // 일치할 때 초록색
                        )
                    }
                },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmPassword.isNotEmpty() && !passwordsMatch) Color.Red else Blue600,
                    unfocusedBorderColor = if (confirmPassword.isNotEmpty() && !passwordsMatch) Color.Red else Gray300,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Blue600
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch
            )
            
            // Password match indicator
            if (confirmPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (passwordsMatch) "비밀번호가 일치합니다" else "비밀번호가 일치하지 않습니다",
                    fontSize = 12.sp,
                    color = if (passwordsMatch) Blue600 else Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Blue600.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "정보",
                        tint = Blue600,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "보안 팁",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Blue600
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "영문, 숫자, 특수문자를 조합하여 더욱 안전한 비밀번호를 만드세요.",
                            fontSize = 12.sp,
                            color = Gray500,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = { onNext(password) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canProceed) Blue600 else Gray300,
                contentColor = Color.White,
                disabledContainerColor = Gray300,
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = canProceed
        ) {
            Text(
                text = "다음",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun SignUpPasswordScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignUpPasswordScreen()
    }
}
