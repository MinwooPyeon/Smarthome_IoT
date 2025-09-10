package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.common.RefreshResponse
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/refresh")
    fun refresh(
        @Header("Authorization") refreshBearer: String
    ): retrofit2.Call<RefreshResponse>
}