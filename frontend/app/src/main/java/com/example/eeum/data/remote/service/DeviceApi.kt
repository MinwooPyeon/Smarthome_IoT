package com.example.eeum.data.remote.service

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApi {

    @POST("/api/devices/{deviceId}/status")
    suspend fun updateDeviceStatus(
        @Path("deviceId") id: Int,
        )

}