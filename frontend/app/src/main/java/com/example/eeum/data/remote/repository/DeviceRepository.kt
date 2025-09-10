package com.example.eeum.data.remote.repository

import com.example.eeum.base.DeviceDirectoryCache
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.model.response.device.DeviceResponse
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
    // ───────── 제어 (이미 있던 것 + 유지) ─────────
    suspend fun setPower(roomName: String, number: Int?, deviceType: String, on: Boolean): Result<Unit> =
        execute(roomName, number, deviceType, power = on)

    suspend fun setTemperature(roomName: String, number: Int?, deviceType: String, temperature: Int): Result<Unit> =
        execute(roomName, number, deviceType, temperature = temperature.coerceIn(16, 30))

    suspend fun setFanLevel(roomName: String, number: Int?, deviceType: String, level: Int): Result<Unit> =
        execute(roomName, number, deviceType, level = level.coerceIn(1, 5))

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

        val res: Response<ApiResponse<BaseResponse>> = deviceService.updateDeviceStatus(deviceId, body)
        if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code()}"))
        val api = res.body() ?: return@withContext Result.failure(IllegalStateException("빈 응답"))
        if (api.status != "SUCCES") return@withContext Result.failure(IllegalStateException("API status=${api.status}"))

        Result.success(Unit)
    }

    // ───────── 조회 (QUERY_*) ─────────
    suspend fun queryIsOn(roomName: String, number: Int?, deviceType: String): Result<Boolean> =
        readDetail(roomName, number, deviceType).mapCatching { detail ->
            detail.optBool("power") ?: error("power 없음")
        }

    suspend fun queryTemperature(roomName: String, number: Int?, deviceType: String): Result<Int> =
        readDetail(roomName, number, deviceType).mapCatching { detail ->
            detail.optInt("temperature") ?: error("temperature 없음")
        }

    suspend fun queryFanLevel(roomName: String, number: Int?, deviceType: String): Result<Int> =
        readDetail(roomName, number, deviceType).mapCatching { detail ->
            detail.optInt("level") ?: error("level 없음")
        }

    private suspend fun readDetail(roomName: String, number: Int?, deviceType: String): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            val deviceId = directory.findDeviceId(roomName, number, deviceType)
                ?: return@withContext Result.failure(IllegalArgumentException("대상 기기를 찾을 수 없습니다: ${directory.buildName(roomName, number, deviceType)}"))

            val res: Response<ApiResponse<DeviceResponse>> = deviceService.readDevice(deviceId)
            if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code()}"))
            val api = res.body() ?: return@withContext Result.failure(IllegalStateException("빈 응답"))
            if (api.status != "SUCCES") return@withContext Result.failure(IllegalStateException("API status=${api.status}"))

            Result.success(api.data.deviceDetail)
        }

    // ───────── JsonObject helpers ─────────
    private fun JsonObject.optBool(key: String): Boolean? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asBoolean }.getOrNull() else null

    private fun JsonObject.optInt(key: String): Int? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asInt }.getOrNull() else null
}