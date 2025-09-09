package com.example.eeum.data.remote.repository

import com.example.eeum.base.DeviceDirectoryCache
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.remote.service.DeviceService
import com.example.eeum.util.Payload
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class DeviceRepository(
    private val deviceService: DeviceService,
    private val directory: DeviceDirectoryCache
) {

    suspend fun setPower(
        roomName: String,
        number: Int?,
        deviceType: String,
        on: Boolean
    ): Result<Unit> = execute(roomName, number, deviceType, power = on)

    /** 온도 설정 (기본 클램프: 16..30) */
    suspend fun setTemperature(
        roomName: String,
        number: Int?,
        deviceType: String,
        temperature: Int
    ): Result<Unit> = execute(roomName, number, deviceType, temperature = temperature.coerceIn(16, 30))

    /** 바람 세기 설정 (기본 클램프: 1..5) */
    suspend fun setFanLevel(
        roomName: String,
        number: Int?,
        deviceType: String,
        level: Int
    ): Result<Unit> = execute(roomName, number, deviceType, level = level.coerceIn(1, 5))

    /**
     * 복합 설정도 한 번에 가능: (켜고 26도로, 바람 3단 등)
     * 필요 값만 채워서 호출하면, body에는 해당 필드만 포함됨.
     */
    suspend fun applyComposite(
        roomName: String,
        number: Int?,
        deviceType: String,
        power: Boolean? = null,
        temperature: Int? = null,
        level: Int? = null
    ): Result<Unit> = execute(
        roomName, number, deviceType,
        power = power,
        temperature = temperature?.coerceIn(16, 30),
        level = level?.coerceIn(1, 5)
    )

    // ───────────────────────── internal ─────────────────────────

    private suspend fun execute(
        roomName: String,
        number: Int?,
        deviceType: String,
        power: Boolean? = null,
        temperature: Int? = null,
        level: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val deviceId = directory.findDeviceId(roomName, number, deviceType)
            ?: return@withContext Result.failure(IllegalArgumentException("대상 기기를 찾을 수 없습니다: ${directory.buildName(roomName, number, deviceType)}"))

        val body: JsonObject = Payload.deviceDetail(
            power = power,
            temperature = temperature,
            level = level
        )

        val res: Response<ApiResponse<BaseResponse>> =
            deviceService.updateDeviceStatus(deviceId, body)

        if (!res.isSuccessful) {
            return@withContext Result.failure(IllegalStateException("HTTP ${res.code()}"))
        }

        val api = res.body()
            ?: return@withContext Result.failure(IllegalStateException("실패 했습니다."))

        if (api.status != "SUCCES") {
            return@withContext Result.failure(IllegalStateException("API status=${api.status}"))
        }

        Result.success(Unit)
    }
}