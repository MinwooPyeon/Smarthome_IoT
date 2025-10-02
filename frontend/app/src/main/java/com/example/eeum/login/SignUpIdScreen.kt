package com.example.eeum.login

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.ui.theme.Blue600
import com.example.eeum.ui.theme.EeumTheme
import com.example.eeum.ui.theme.Gray300
import com.example.eeum.ui.theme.Gray500

@Composable
fun SignUpIdScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (String) -> Unit = {},
    onDuplicateCheck: (String) -> Unit = {}
) {
    var idText by remember { mutableStateOf("") }
    
    // 아이디 유효성 검사: 영문, 숫자 조합 5~10자
    val isValidId = remember(idText) {
        idText.length in 5..10 && 
        idText.matches(Regex("^[a-zA-Z0-9]+$")) &&
        idText.any { it.isLetter() } && 
        idText.any { it.isDigit() }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with back button and title
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
                progress = { 0.2f }, // 1/5
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Blue600,
                trackColor = Gray300.copy(alpha = 0.3f)
            )
            
            Text(
                text = "1/5",
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
                text = "아이디를 입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                text = "로그인 시 사용할 아이디를 설정해주세요.",
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ID Input Field
            OutlinedTextField(
                value = idText,
                onValueChange = { idText = it },
                placeholder = {
                    Text(
                        text = "아이디 입력",
                        color = Gray300,
                        fontSize = 16.sp
                    )
                },
                trailingIcon = {
                    if (isValidId) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "유효한 아이디",
                            tint = Color(0xFF4CAF50), // 초록색
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValidId) Color(0xFF4CAF50) else Blue600,
                    unfocusedBorderColor = if (isValidId) Color(0xFF4CAF50) else Gray300,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = Blue600
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Helper text
            Text(
                text = "영문, 숫자 조합 5~10자",
                fontSize = 12.sp,
                color = Gray500,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))

        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = { onNextClick(idText) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValidId) Blue600 else Gray300,
                contentColor = Color.White,
                disabledContainerColor = Gray300,
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = isValidId // 유효한 아이디일 때만 활성화
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
private fun SignUpIdScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignUpIdScreen()
    }
}
