package com.example.eeum.login
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eeum.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.example.eeum.ui.components.CustomButton
import com.example.eeum.ui.theme.EeumTheme

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    var idText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
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
                .shadow(elevation = 2.dp, spotColor = Color(0x0D000000), ambientColor = Color(0x0D000000))
                .width(330.dp)
                .height(65.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xCCFFFFFF),
                unfocusedContainerColor = Color(0xCCFFFFFF),
                unfocusedIndicatorColor = Color(0xFFE5E7EB),
                focusedIndicatorColor = Color(0xFF03a9f4)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text(
                text = "비밀번호",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.goormsansmedium)),
                    color = Color(0xFFADAEBC),
                )
            ) },
            modifier = Modifier
                .shadow(elevation = 2.dp, spotColor = Color(0x0D000000), ambientColor = Color(0x0D000000))
                .width(330.dp)
                .height(65.dp),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xCCFFFFFF),
                unfocusedContainerColor = Color(0xCCFFFFFF),
                unfocusedIndicatorColor = Color(0xFFE5E7EB),
                focusedIndicatorColor = Color(0xFF03a9f4)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 로그인 버튼
        CustomButton(
            text = "로그인",
            onClick = { 
                // TODO: 실제 로그인 로직 구현 (현재는 바로 성공 처리)
                onLoginSuccess()
            },
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Google 로그인 버튼
        CustomButton(
            text = "Google 로그인",
            onClick = { /* TODO: Google 로그인 로직 구현 */ },
            icon = painterResource(id = R.drawable.ic_google),
            iconTint = Color(0xff374151),
            backgroundColor = Color.White,
            textColor = Color(0xff374151),
        )

        Spacer(modifier = Modifier.height(70.dp))

        // 회원가입 텍스트
        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xff4b5563), fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)))) {
                append("\'이음\'이 처음이신가요? ")
            }
            pushStringAnnotation(tag = "SIGNUP", annotation = "signup")
            withStyle(style = SpanStyle(color = Color(0xff03a9f4), fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.goormsansmedium)))) {
                append("회원가입")
            }
            pop()
        }
        
        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "SIGNUP", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onSignUpClick()
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            style = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center
            )
        )
        }
    }

@Preview
@Composable
private fun SignInScreenPreview() {
    EeumTheme(dynamicColor = false) {
        SignInScreen()
    }
}
