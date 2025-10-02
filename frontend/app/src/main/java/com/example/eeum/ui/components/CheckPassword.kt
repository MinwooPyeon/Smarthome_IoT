package com.example.eeum.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.eeum.R
import com.example.eeum.ui.theme.*

@Composable
fun CheckPasswordDialog(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "현재 비밀번호 확인",
                        color = Gray900,
                        style = TextStyle(fontSize = 18.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)))
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "보안을 위해 현재 비밀번호를 입력해주세요.",
                    color = Gray600,
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium))),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "현재 비밀번호",
                    color = Gray900,
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)))
                )
                Spacer(Modifier.height(8.dp))

                var password by remember { mutableStateOf("") }
                var visible by remember { mutableStateOf(false) }
                val isError = errorMessage != null

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            text = errorMessage ?: "현재 비밀번호를 입력하세요",
                            color = if (isError) Red500 else Color(0xFFADAEBC),
                            style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)))
                        )
                    },
                    singleLine = true,
                    isError = isError,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_eye_blind),
                            contentDescription = if (visible) "비밀번호 숨기기" else "비밀번호 보이기",
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { visible = !visible }
                        )
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = if (isError) Red500 else Blue500,
                        unfocusedIndicatorColor = if (isError) Red500 else Gray50,
                        errorIndicatorColor = Red500,
                        cursorColor = if (isError) Red500 else Blue500
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Confirm button
                Surface(
                    color = Blue500,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onConfirm(password) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "확인",
                            color = Color.White,
                            style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.goormsansbold)))
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Cancel button
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gray50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onCancel() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "취소",
                            color = Gray600,
                            style = TextStyle(fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)))
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CheckPasswordDialogPreview() {
    androidx.compose.material3.Surface(modifier = Modifier.background(Color(0x33000000))) {
        CheckPasswordDialog(onConfirm = {}, onCancel = {})
    }
}
