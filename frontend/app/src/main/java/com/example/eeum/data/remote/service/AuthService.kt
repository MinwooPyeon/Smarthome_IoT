package com.example.eeum.data.remote.service

import com.example.eeum.data.model.dto.login.EmailRequest
import com.example.eeum.data.model.dto.login.SignUpRequest
import com.example.eeum.data.model.dto.login.VerifyRequest
import com.example.eeum.data.model.response.common.RefreshResponse
import com.example.eeum.data.model.response.login.EmailResponse
import com.example.eeum.data.model.response.login.SignUpResponse
import com.example.eeum.data.model.response.login.VerifyResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/refresh")
    fun refresh(
        @Header("Authorization") refreshBearer: String
    ): retrofit2.Call<RefreshResponse>

    // 회원가입
    @POST("auth/signup")
    fun signup(
        @Body signUpRequest: SignUpRequest
    ): retrofit2.Call<SignUpResponse>
    
    // 이메일 인증 번호 발송
    @POST("auth/send")
    fun sendEmailVerification(
        @Body emailRequest: EmailRequest
    ): retrofit2.Call<EmailResponse>
    
    // 이메일 인증 번호 확인
    @POST("auth/verify-email")
    fun verifyEmail(
        @Body verifyRequest: VerifyRequest
    ): retrofit2.Call<VerifyResponse>
}
