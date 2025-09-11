package com.example.eeum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eeum.R
import com.example.eeum.ui.theme.*
import com.example.eeum.ui.components.CheckPasswordDialog

@Composable
fun PasswordChangeScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val isValid = remember(newPassword) { isPasswordValid(newPassword) }
    val matches = newPassword.isNotEmpty() && newPassword == confirmPassword
    val enableButton = isValid && matches

    var showCheckDialog by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 60.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header: back icon and centered title
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_page_move_left),
                contentDescription = "뒤로가기",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { navController?.popBackStack() }
            )
            Text(
                text = "비밀번호 변경",
                color = Gray900,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "새 비밀번호 설정",
            color = Gray900,
            style = TextStyle(
                fontSize = 18.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold))
            )
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "안전한 비밀번호로 변경해주세요.",
            color = Gray600,
            style = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )

        Spacer(Modifier.height(24.dp))

        // New password field
        LabeledPasswordField(
            label = "새 비밀번호",
            placeholder = "새 비밀번호를 입력하세요",
            value = newPassword,
            onValueChange = { newPassword = it },
            visible = newVisible,
            onToggleVisible = { newVisible = !newVisible }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "8자 이상, 영문, 숫자, 특수문자 포함",
            color = if (newPassword.isNotEmpty() && !isValid) Red500 else Gray600,
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.goormsansmedium))
            )
        )

        Spacer(Modifier.height(40.dp))

        // Confirm password field
        LabeledPasswordField(
            label = "새 비밀번호 확인",
            placeholder = "새 비밀번호를 다시 입력하세요",
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            visible = confirmVisible,
            onToggleVisible = { confirmVisible = !confirmVisible }
        )

        Spacer(Modifier.weight(1f))

        PrimaryWideButton(
            text = "비밀번호 변경",
            enabled = enableButton,
            onClick = { /* TODO: API 연동 */ }
        )

        Spacer(Modifier.height(12.dp))
    }

    if (showCheckDialog) {
        // 현재 비밀번호 확인 다이얼로그: 항상 화면 진입 시 표시
        CheckPasswordDialog(
            onConfirm = { _ ->
                // TODO: API로 현재 비밀번호 확인 후 성공 시 닫기, 실패 시 에러 메시지 전달
                showCheckDialog = false
            },
            onCancel = { navController?.popBackStack() }
        )
    }
}

@Composable
private fun LabeledPasswordField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Gray900,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.goormsansbold))
            )
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = Color(0xFFADAEBC), fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_eye_blind),
                    contentDescription = if (visible) "비밀번호 숨기기" else "비밀번호 보이기",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onToggleVisible() }
                )
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Blue500,
                unfocusedIndicatorColor = Gray50,
                disabledIndicatorColor = Gray50,
                cursorColor = Blue500
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

private fun isPasswordValid(pw: String): Boolean {
    if (pw.length < 8) return false
    val hasLetter = pw.any { it.isLetter() }
    val hasDigit = pw.any { it.isDigit() }
    val hasSpecial = pw.any { !it.isLetterOrDigit() }
    return hasLetter && hasDigit && hasSpecial
}

@Composable
private fun PrimaryWideButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    // Simple full-width primary button following the app style
    val bg = if (enabled) Blue500 else Gray50
    val fg = if (enabled) Color.White else Gray600
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .let { base -> if (enabled) base.clickable { onClick() } else base }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = fg,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansbold))
                )
            )
        }
    }
}

@Preview
@Composable
private fun PasswordChangeScreenPreview() {
    EeumTheme(dynamicColor = false) {
        PasswordChangeScreen()
    }
}
