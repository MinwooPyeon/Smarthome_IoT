package com.example.eeum.data.remote.service

import com.example.eeum.data.model.response.device.EnergyTotalUsage
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface EnergyService {
    @GET("api/energy/series")
    suspend fun getEnergyTotalUsage(
        @Query("homeId") homeId: Int, // 집 ID
        @Query("range") range: String, // "day", "week", "month", "year"
        @Query("date") date: String // 날짜 (yyyy-MM-dd format)
    ): Response<EnergyTotalUsage>
}
