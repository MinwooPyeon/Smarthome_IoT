package com.example.eeum.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.login.EmailRequest
import com.example.eeum.data.model.dto.login.SignUpRequest
import com.example.eeum.data.model.dto.login.VerifyRequest
import com.example.eeum.data.model.response.login.EmailResponse
import com.example.eeum.data.model.response.login.SignUpResponse
import com.example.eeum.data.model.response.login.VerifyResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpViewModel : ViewModel() {

    private val _signUpResponse = MutableLiveData<SignUpResponse?>()
    val signUpResponse: LiveData<SignUpResponse?> get() = _signUpResponse
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error
    
    private val _isSignUpSuccess = MutableLiveData<Boolean>(false)
    val isSignUpSuccess: LiveData<Boolean> get() = _isSignUpSuccess
    
    // 이메일 인증 관련 상태
    private val _emailResponse = MutableLiveData<EmailResponse?>()
    val emailResponse: LiveData<EmailResponse?> get() = _emailResponse
    
    private val _isEmailSending = MutableLiveData<Boolean>(false)
    val isEmailSending: LiveData<Boolean> get() = _isEmailSending
    
    private val _isEmailSent = MutableLiveData<Boolean>(false)
    val isEmailSent: LiveData<Boolean> get() = _isEmailSent
    
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> get() = _emailError
    
    // 인증번호 확인 관련 상태
    private val _verifyResponse = MutableLiveData<VerifyResponse?>()
    val verifyResponse: LiveData<VerifyResponse?> get() = _verifyResponse
    
    private val _isVerifying = MutableLiveData<Boolean>(false)
    val isVerifying: LiveData<Boolean> get() = _isVerifying
    
    private val _isEmailVerified = MutableLiveData<Boolean>(false)
    val isEmailVerified: LiveData<Boolean> get() = _isEmailVerified
    
    private val _verifyError = MutableLiveData<String?>()
    val verifyError: LiveData<String?> get() = _verifyError
    
 //회원가입
    fun signUp(email: String, loginId: String, nickname: String, password: String) {
        _isLoading.value = true
        _error.value = null

        val signUpRequest = SignUpRequest(
            email = email,
            loginId = loginId,
            nickname = nickname,
            password = password
        )

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.authService.signup(signUpRequest)
                    response.execute()
                }
            }
                .onSuccess { response ->
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        response.body()?.let { signUpResponse ->
                            _signUpResponse.value = signUpResponse
                            _isSignUpSuccess.value = true
                            Log.d("SignUpViewModel", "SignUp success: $signUpResponse")
                        } ?: run {
                            _error.value = "회원가입 응답이 비어있습니다."
                            Log.e("SignUpViewModel", "SignUp response body is null")
                        }
                    } else {
                        _error.value = "회원가입 실패: ${response.code()}"
                        Log.e("SignUpViewModel", "SignUp failed with code: ${response.code()}")
                    }
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = "네트워크 오류: ${e.message}"
                    Log.e("SignUpViewModel", "SignUp failed", e)
                }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSignUpSuccess() {
        _isSignUpSuccess.value = false
    }

    // 이메일 인증
    fun sendEmailVerification(email: String) {
        _isEmailSending.value = true
        _emailError.value = null
        _isEmailSent.value = false
        
        val emailRequest = EmailRequest(email = email)
        
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.authService.sendEmailVerification(emailRequest)
                    response.execute()
                }
            }
                .onSuccess { response ->
                    _isEmailSending.value = false
                    if (response.isSuccessful) {
                        response.body()?.let { emailResponse ->
                            _emailResponse.value = emailResponse
                            _isEmailSent.value = emailResponse.sent
                            if (emailResponse.sent) {
                                Log.d("SignUpViewModel", "Email verification sent successfully. Expires in ${emailResponse.expiresInMinutes} minutes")
                            } else {
                                _emailError.value = "이메일 발송에 실패했습니다."
                                Log.e("SignUpViewModel", "Email verification failed to send")
                            }
                        } ?: run {
                            _emailError.value = "이메일 인증 응답이 비어있습니다."
                            Log.e("SignUpViewModel", "Email verification response body is null")
                        }
                    } else {
                        _emailError.value = "이메일 인증 번호 발송 실패: ${response.code()}"
                        Log.e("SignUpViewModel", "Email verification failed with code: ${response.code()}")
                    }
                }
                .onFailure { e ->
                    _isEmailSending.value = false
                    _emailError.value = "네트워크 오류: ${e.message}"
                    Log.e("SignUpViewModel", "Email verification failed", e)
                }
        }
    }

    fun clearEmailError() {
        _emailError.value = null
    }

    fun clearEmailSent() {
        _isEmailSent.value = false
    }
    
    //이메일 인증번호 확인
    fun verifyEmailCode(email: String, code: String) {
        _isVerifying.value = true
        _verifyError.value = null
        _isEmailVerified.value = false
        
        val verifyRequest = VerifyRequest(
            email = email,
            code = code
        )
        
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.authService.verifyEmail(verifyRequest)
                    response.execute()
                }
            }
                .onSuccess { response ->
                    _isVerifying.value = false
                    if (response.isSuccessful) {
                        response.body()?.let { verifyResponse ->
                            _verifyResponse.value = verifyResponse
                            _isEmailVerified.value = verifyResponse.verified
                            if (verifyResponse.verified) {
                                Log.d("SignUpViewModel", "Email verification successful")
                            } else {
                                _verifyError.value = "인증번호가 일치하지 않습니다."
                                Log.e("SignUpViewModel", "Email verification failed - code mismatch")
                            }
                        } ?: run {
                            _verifyError.value = "인증 확인 응답이 비어있습니다."
                            Log.e("SignUpViewModel", "Email verification response body is null")
                        }
                    } else {
                        _verifyError.value = "인증번호 확인 실패: ${response.code()}"
                        Log.e("SignUpViewModel", "Email verification failed with code: ${response.code()}")
                    }
                }
                .onFailure { e ->
                    _isVerifying.value = false
                    _verifyError.value = "네트워크 오류: ${e.message}"
                    Log.e("SignUpViewModel", "Email verification failed", e)
                }
        }
    }
    
    /**
     * 인증 확인 에러 메시지 초기화
     */
    fun clearVerifyError() {
        _verifyError.value = null
    }
    
    /**
     * 이메일 인증 상태 초기화
     */
    fun clearEmailVerified() {
        _isEmailVerified.value = false
    }
}
