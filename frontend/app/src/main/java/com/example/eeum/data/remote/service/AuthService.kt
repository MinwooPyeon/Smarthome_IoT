package com.example.eeum.data.remote.service

import com.example.eeum.data.model.dto.login.EmailRequest
import com.example.eeum.data.model.dto.login.SignUpRequest
import com.example.eeum.data.model.dto.login.VerifyRequest
import com.example.eeum.data.model.response.common.RefreshResponse
import com.example.eeum.data.model.response.login.EmailResponse
import com.example.eeum.data.model.response.login.SignUpResponse
import com.example.eeum.data.model.response.login.VerifyResponse
import com.example.eeum.data.model.response.menu.LogResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface AuthService {
    @POST("api/auth/refresh")
    fun refresh(
        @Header("Authorization") refreshBearer: String
    ): retrofit2.Call<RefreshResponse>

    // IR 이벤트 로그 조회
    @GET("/api/ir/logs")
    fun getIrLogs(
        @Query("homeId") homeId: Int
    ): retrofit2.Call<LogResponse>
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
