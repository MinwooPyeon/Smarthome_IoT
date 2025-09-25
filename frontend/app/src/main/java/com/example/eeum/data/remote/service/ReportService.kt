package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.report.AiSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportService {
    @GET("api/reports/ai/energy")
    suspend fun getAiEnergyReport(
        @Query("homeId") homeId: Int
    ): Response<AiSummary>
}
