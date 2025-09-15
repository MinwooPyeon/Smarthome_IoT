package com.example.eeum.data.remote.repository

import com.example.eeum.base.DeviceDirectoryCache
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
    // ───────── 제어: 합성만 ─────────
    suspend fun applyComposite(
        roomName: String,
        number: Int?,
        deviceType: String,
        power: Boolean? = null,
        temperature: Int? = null,
        level: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val deviceId = directory.findDeviceId(roomName, number, deviceType)
            ?: return@withContext fail("대상 기기를 찾을 수 없습니다: ${directory.buildName(roomName, number, deviceType)}")

        val detail = Payload.deviceDetail(
            power = power,
            temperature = temperature?.coerceIn(16, 30),
            level = level?.coerceIn(1, 5)
        )
        val body = JsonObject().apply { add("deviceDetail", detail) }

        val res = deviceService.updateDeviceStatus(deviceId, body)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "API status=${api.status}"))

        Result.success(Unit)
    }

    // ───────── 일괄 제어: 전체/타입별 ─────────
    suspend fun bulkSetPower(
        deviceType: String?,   // null → 전체 기기
        on: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        val res = deviceService.readDevices(type = deviceType)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "API status=${api.status}"))

        val items = api.data.items
        var ok = 0
        val body = JsonObject().apply { add("deviceDetail", Payload.deviceDetail(power = on)) }
        for (d in items) {
            val up = deviceService.updateDeviceStatus(d.deviceId, body)
            if (up.isSuccessful && up.body()?.status == "SUCCESS") ok++
        }
        Result.success(ok)
    }

    // ───────── 일괄 제어: 방(+번호) / 타입별 ─────────
    suspend fun bulkSetPower(
        roomName: String,
        number: Int?,
        deviceType: String?,   // null → 해당 방의 모든 기기
        on: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        val roomKey = number?.let { "$roomName$it" } ?: roomName
        val res = deviceService.readDevices(type = deviceType, roomName = roomKey)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "API status=${api.status}"))

        val items = api.data.items
        var ok = 0
        val body = JsonObject().apply { add("deviceDetail", Payload.deviceDetail(power = on)) }
        for (d in items) {
            val up = deviceService.updateDeviceStatus(d.deviceId, body)
            if (up.isSuccessful && up.body()?.status == "SUCCESS") ok++
        }
        Result.success(ok)
    }

    // ───────── 조회: 단건 + WHERE 목록 ─────────
    /** 슬롯(장소/번호/기기)로 deviceId 찾고 단건 조회 */
    suspend fun readDeviceBySlots(
        roomName: String,
        number: Int?,
        deviceType: String
    ): Result<DeviceResponse> = withContext(Dispatchers.IO) {
        val deviceId = directory.findDeviceId(roomName, number, deviceType)
            ?: return@withContext fail("대상 기기를 찾을 수 없습니다: ${directory.buildName(roomName, number, deviceType)}")

        val res = deviceService.readDevice(deviceId)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "API status=${api.status}"))

        Result.success(api.data)
    }

    /** power(on/off) + type으로 조회해 '방 이름'만 추출(중복 제거) */
    suspend fun listRoomsByActiveAndType(
        active: Boolean,
        deviceType: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val res = deviceService.readDevices(power = active, type = deviceType)
        if (!res.isSuccessful) return@withContext fail(httpMsg(res))
        val api = res.body() ?: return@withContext fail("빈 응답")
        if (api.status != "SUCCESS") return@withContext fail(apiError(res, "API status=${api.status}"))

        val rooms = api.data.items
            .mapNotNull { it.deviceName?.trim() }
            .map { name -> name.substringBefore(' ').trim() } // "방1 에어컨" -> "방1"
            .filter { it.isNotEmpty() }
            .distinct()

        Result.success(rooms)
    }

    // ───────── helpers ─────────
    private fun <T> fail(msg: String): Result<T> = Result.failure(IllegalStateException(msg))

    private fun httpMsg(res: Response<*>): String =
        buildString {
            append("HTTP ").append(res.code())
            val m = res.message().orEmpty()
            if (m.isNotBlank()) append(" ").append(m)
            val apiErr = apiError(res, "")
            if (apiErr.isNotBlank()) append(" - ").append(apiErr)
        }

    /** 서버가 {"status":"FAIL","error":"..."} 를 내려줄 때 error 메시지 뽑기 */
    private fun apiError(res: Response<*>, fallback: String): String {
        val raw = res.errorBody()?.string().orEmpty()
        return try {
            val err = org.json.JSONObject(raw).optString("error")
            if (err.isNullOrBlank()) fallback else err
        } catch (_: Exception) { fallback }
    }
}
