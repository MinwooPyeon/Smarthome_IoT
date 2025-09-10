package com.example.eeum.base

import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.common.Page
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.remote.service.DeviceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceDirectoryCache(
    private val deviceService: DeviceService
) {
    @Volatile private var byName: Map<String, Int> = emptyMap()

    suspend fun loadAllDevices(): Int = withContext(Dispatchers.IO) {
        val res = deviceService.readDevices() // Response<ApiResponse<Page<DeviceResponse>>>
        if (!res.isSuccessful) throw IllegalStateException("readDevices HTTP ${res.code()}")
        val api = res.body() ?: throw IllegalStateException("readDevices empty body")

        if (api.status != "SUCCES") throw IllegalStateException("readDevices status=${api.status}")

        val page: Page<DeviceResponse> = api.data
        val devices: List<DeviceResponse> = page.items

        val map = HashMap<String, Int>(devices.size)
        for (d in devices) {
            val key = normalizeName(d.deviceName)
            if (key.isNotEmpty()) map[key] = d.deviceId
        }
        byName = map
        devices.size
    }

    /**
     * 음성 슬롯으로 만든 "방1 에어컨" 같은 키로 deviceId를 조회.
     * - roomName/number/deviceType을 결합해 동일 규칙으로 정규화 후 사용
     */
    fun findDeviceId(roomName: String?, number: Int?, deviceType: String?): Int? {
        val key = buildName(roomName, number, deviceType) ?: return null
        return byName[normalizeName(key)]
    }

    /**
     * 이미 완성된 name("방1 에어컨")으로 바로 조회하고 싶을 때 사용.
     */
    fun findDeviceIdByName(name: String): Int? = byName[normalizeName(name)]

    // ───────────────────────── 내부 유틸 ─────────────────────────

    /** 음성 슬롯을 조합해 "방1 에어컨" 형태의 이름을 만든다. */
    fun buildName(roomName: String?, number: Int?, deviceType: String?): String? {
        val r = roomName?.trim().orEmpty()
        val d = deviceType?.trim().orEmpty()
        if (r.isEmpty() || d.isEmpty()) return null
        val n = number?.toString().orEmpty()
        // 규칙: "방" + "1" + " " + "에어컨" → "방1 에어컨"
        return if (n.isEmpty()) "$r $d" else "$r$n $d"
    }

    /**
     * 서버의 deviceName과 클라이언트가 만든 name을 동일하게 맞추는 정규화.
     * - 공백 정리
     * - "번" 제거
     * - "방 1"/"방 1번" → "방1" 로 합치기
     * - 주요 동의어 통일(필요 최소한만)
     */
    private fun normalizeName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var t = raw.trim()
            .replace(Regex("\\s+"), " ")      // 다중 공백 → 1칸
            .replace(" 번", "번")              // "  번" → "번" (후속 처리 편의)
            .replace("번", "")                // "번" 제거

        // 방 + 숫자 붙이기: "방 01", "방1", "방 1 " → "방1 "
        t = Regex("""^([가-힣A-Za-z]+)\s*0*([0-9]+)\s*""")
            .replace(t) { mr -> "${mr.groupValues[1]}${mr.groupValues[2]} " }

        // 동의어 최소 정규화(서버/클라 간 표현 차이 대비)
        t = t.replace("침실", "방")
            .replace("부엌", "주방")
            .replace("발코니", "베란다")
            .replace("작업실", "서재")
            .replace("냉방기", "에어컨")
            .replace("불", "전등")
            .replace("조명", "전등")
            .replace("빔프로젝터", "프로젝터")
            .replace("빔", "프로젝터")
            .replace("팬", "선풍기")

        return t
    }

    companion object {
        /**
         * 디버그용 시드로 바로 캐시를 만든다.
         */
        fun withSeed(deviceService: DeviceService, seed: Map<String, Int>): DeviceDirectoryCache {
            val c = DeviceDirectoryCache(deviceService)
            val map = HashMap<String, Int>(seed.size)
            seed.forEach { (name, id) ->
                val key = c.normalizeName(name)
                if (key.isNotEmpty()) map[key] = id
            }
            c.byName = map
            return c
        }
    }
}