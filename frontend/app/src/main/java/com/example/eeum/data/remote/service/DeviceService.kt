package com.example.eeum.data.remote.service

import com.example.eeum.data.model.dto.device.DeviceRequest
import com.example.eeum.data.model.dto.device.DeviceStatusChangeRequest
import com.example.eeum.data.model.dto.device.DevicesLocation
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.model.response.common.Page
import com.example.eeum.data.model.response.device.DeviceChangeResponse
import com.example.eeum.data.model.response.device.DeviceDeleteResponse
import com.example.eeum.data.model.response.device.DeviceIcon
import com.example.eeum.data.model.response.device.DeviceLocation
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.model.response.device.DeviceStatusChangeResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DeviceService {

    // 디바이스 등록
    @POST ("/api/devices")
    suspend fun createDevice(
        @Body body: DeviceRequest
    ): Response<ApiResponse<Boolean>>

    // 디바이스 조회
    @GET("/api/devices")
    suspend fun readDevices(
        @Query("power") power: Boolean? = null,
        @Query("type") type: String? = null,
        @Query("roomName") roomName: String? = null,
        @Query("deviceName") deviceName: String? = null
    ): Response<ApiResponse<Page<DeviceResponse>>>

    // 디바이스 목록 조회
    @GET("api/devices")
    suspend fun readDevicesSimple(
        @Query("power") power: Boolean? = null,
        @Query("type") type: String? = null,
        @Query("roomName") roomName: String? = null,
        @Query("deviceName") deviceName: String? = null
    ): Response<DeviceIcon>

    // 디바이스 단건 조회
    @GET("/api/devices/{deviceId}")
    suspend fun readDevice(
        @Path("deviceId") id: Int,
    ): Response<ApiResponse<DeviceResponse>>

    // 디바이스 상태 업데이트 (기존)
    @PUT("/api/devices/{deviceId}/status")
    suspend fun updateDeviceStatus(
        @Path("deviceId") id: Int,
        @Body body: JsonObject
    ): Response<ApiResponse<BaseResponse>>

    // 디바이스 상태 변경 (새로운 DTO 사용)
    @PUT("/api/devices/{deviceId}/status")
    suspend fun changeDeviceStatus(
        @Path("deviceId") deviceId: Int,
        @Body request: DeviceStatusChangeRequest
    ): Response<DeviceStatusChangeResponse>

    // 디바이스 삭제
    @DELETE("/api/devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("deviceId") id: Int,
    ): Response<DeviceDeleteResponse>

    // 디바이스 위치 조회
    @GET("api/devices/locations")
    suspend fun readDeviceLocations(
        @Query("homeId") homeId: Int? = null,
        @Query("roomId") roomId: Int? = null,
    ): Response<DeviceLocation>

    // 디바이스 위치 배치 전체 수정
    @PUT("/api/devices/locations")
    suspend fun updateDeviceLocations(
        @Body body: DevicesLocation
    ): Response<DeviceChangeResponse>
}
