package com.example.eeum.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.example.eeum.MainActivity
import com.example.eeum.ui.theme.EeumTheme

class LoginActivity : ComponentActivity() {

    private val signUpViewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
                        setContent {
            var currentScreen by remember { mutableStateOf("signin") }
            var signUpData by remember { mutableStateOf(mapOf<String, String>()) }
            var isTransitioning by remember { mutableStateOf(false) }
            
            EeumTheme(dynamicColor = false) {
                when (currentScreen) {
                    "signin" -> SignInScreen(
                        onLoginSuccess = {
                            // 로그인 성공 시 MainActivity로 이동
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish() // LoginActivity 종료
                        },
                        onSignUpClick = {
                            // 회원가입 화면으로 이동
                            currentScreen = "signup_id"
                        }
                    )
                    "signup_id" -> SignUpIdScreen(
                        onBackClick = {
                            // 뒤로가기 - 로그인 화면으로
                            currentScreen = "signin"
                        },
                        onNextClick = { userId ->
                            // 아이디 저장 후 이메일 화면으로 이동
                            signUpData = signUpData + ("userId" to userId)
                            currentScreen = "signup_email"
                        },
                        onDuplicateCheck = { userId ->
                            // TODO: 중복 확인 로직 구현
                        }
                    )
                    "signup_email" -> SignUpEmailScreen(
                        onBackClick = {
                            // 뒤로가기 - 아이디 화면으로
                            currentScreen = "signup_id"
                        },
                        onSendVerificationCode = { email ->
                            if (!isTransitioning) {
                                isTransitioning = true
                                // 이메일 인증 번호 발송
                                signUpViewModel.sendEmailVerification(email)
                                signUpData = signUpData + ("email" to email)
                                // 안전한 화면 전환을 위해 지연 추가
                                currentScreen = "signup_verify"
                                isTransitioning = false
                            }
                        }
                    )
                    "signup_verify" -> SignUpVerifyScreen(
                        userId = signUpData["userId"] ?: "",
                        email = signUpData["email"] ?: "",
                        signUpViewModel = signUpViewModel,
                        onBackClick = {
                            // 뒤로가기 - 이메일 화면으로
                            currentScreen = "signup_email"
                        },
                        onVerify = { verificationCode ->
                            // 인증 성공 시 비밀번호 설정 화면으로 이동
                            signUpData = signUpData + ("verificationCode" to verificationCode)
                            currentScreen = "signup_password"
                        },
                        onResendCode = {
                            // 인증 번호 재발송
                            signUpData["email"]?.let { email ->
                                signUpViewModel.sendEmailVerification(email)
                            }
                        }
                    )
                    "signup_password" -> SignUpPasswordScreen(
                        userId = signUpData["userId"] ?: "",
                        email = signUpData["email"] ?: "",
                        onBackClick = {
                            // 뒤로가기 - 인증 화면으로
                            currentScreen = "signup_verify"
                        },
                        onNext = { password ->
                            // 비밀번호 저장 후 닉네임 설정 화면으로 이동
                            signUpData = signUpData + ("password" to password)
                            currentScreen = "signup_nick"
                        }
                    )
                    "signup_nick" -> SignUpNickScreen(
                        onBackClick = {
                            // 뒤로가기 - 비밀번호 화면으로
                            currentScreen = "signup_password"
                        },
                        onComplete = {
                            // 회원가입 완료 후 완료 화면으로 이동
                            currentScreen = "signup_complete"
                        },
                        idText = signUpData["userId"] ?: "",
                        emailText = signUpData["email"] ?: "",
                        password = signUpData["password"] ?: "",
                        viewModel = signUpViewModel
                    )
                    "signup_complete" -> SignUpCompleteScreen(
                        onStart = {
                            // 로그인 화면으로 이동
                            currentScreen = "signin"
                        }
                    )
                }
            }
        }
    }
}
