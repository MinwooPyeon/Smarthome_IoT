package com.example.eeum.data.remote.service

import com.example.eeum.data.model.dto.device.HubRequest
import com.example.eeum.data.model.response.device.HubResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HubService {
    
    // 허브 등록
    @POST("/api/hubs/register")
    suspend fun registerHub(
        @Body body: HubRequest
    ): Response<HubResponse>
}