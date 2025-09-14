package com.example.eeum.core

import com.example.eeum.data.model.dto.voice.IntentResult
import com.example.eeum.data.remote.repository.DeviceRepository
import com.google.gson.JsonObject

class VoiceUseCase(
    private val repo: DeviceRepository
) {
    suspend fun run(intents: List<IntentResult>): List<AppEffect> {
        if (intents.isEmpty()) return emptyList()

        val effects = mutableListOf<AppEffect>()
        for (block in coalesceControlBlocks(intents)) {
            when (block) {
                is ControlBlock -> {
                    val speech = applyComposite(block)
                    if (speech.isNotBlank()) effects += AppEffect.Speak(speech)
                }
                is QueryItem -> {
                    val speech = handleQuery(block.intent)
                    if (speech.isNotBlank()) effects += AppEffect.Speak(speech)
                }
            }
        }
        return effects
    }

    // ───────────────────────── Control path ─────────────────────────
    private suspend fun applyComposite(b: ControlBlock): String {
        if (b.roomName.isNullOrBlank() || b.deviceType.isNullOrBlank()) {
            return "대상 기기를 특정할 수 없어요."
        }
        val r = repo.applyComposite(
            roomName = b.roomName,
            number = b.number,
            deviceType = b.deviceType,
            power = b.power,
            temperature = b.temperature,
            level = b.level
        )
        return if (r.isSuccess) buildSuccessSpeech(b) else "실패했어요."
    }

    private fun buildSuccessSpeech(b: ControlBlock): String {
        val parts = mutableListOf<String>()
        b.power?.let { parts += if (it) "켰어요" else "껐어요" }
        b.temperature?.let { parts += "온도 ${it}도로 맞췄어요" }
        b.level?.let { parts += "바람 ${it}단으로 바꿨어요" }
        return if (parts.isEmpty()) "완료했어요." else parts.joinToString(" 그리고 ")
    }

    // ───────────────────────── Query path ─────────────────────────
    private suspend fun handleQuery(nlu: IntentResult): String {
        val place  = nlu.slots["place"]
        val device = canonicalDevice(nlu.slots["device"])
        val number = nlu.slots["num"]?.toIntOrNull()

        return when (nlu.intent) {

            // 전원 켜짐/꺼짐 묻기: power만 보고 응답
            "QUERY_IS_ON" -> {
                if (place.isNullOrBlank() || device.isNullOrBlank()) return "대상 기기를 특정할 수 없어요."
                val r = repo.readDeviceBySlots(place, number, device)
                r.fold(
                    onSuccess = { dev ->
                        when (dev.deviceDetail.optBool("power")) {
                            true  -> "${dev.deviceName}은 켜져 있습니다."
                            false -> "${dev.deviceName}은 꺼져 있습니다."
                            null  -> "전원 상태를 알 수 없어요."
                        }
                    },
                    onFailure = { e -> friendlyFail(e) }
                )
            }

            // 상태 요약(타입별)
            "QUERY_IS_STATE" -> {
                if (place.isNullOrBlank() || device.isNullOrBlank()) return "대상 기기를 특정할 수 없어요."
                val r = repo.readDeviceBySlots(place, number, device)
                r.fold(
                    onSuccess = { dev ->
                        val type = canonicalDevice(dev.type) ?: device
                        val on   = dev.deviceDetail.optBool("power")
                        val t    = dev.deviceDetail.optInt("temperature")
                        val lv   = dev.deviceDetail.optInt("level")

                        when (on) {
                            false -> "${dev.deviceName}은 꺼져 있습니다."
                            true  -> when (type) {
                                "에어컨" -> {
                                    val tempPart = t?.let { "온도는 ${it}도" }
                                    val lvlPart  = lv?.let { "바람은 ${it}단" }
                                    val extra = listOfNotNull(tempPart, lvlPart).joinToString(", ")
                                    if (extra.isBlank()) "${dev.deviceName}은 켜져 있습니다."
                                    else "${dev.deviceName}은 켜져 있고, $extra 입니다."
                                }
                                "선풍기" -> {
                                    val lvlPart = lv?.let { "바람은 ${it}단" }
                                    if (lvlPart == null) "${dev.deviceName}은 켜져 있습니다."
                                    else "${dev.deviceName}은 켜져 있고, $lvlPart 입니다."
                                }
                                else -> "${dev.deviceName}은 켜져 있습니다."
                            }
                            null  -> "전원 상태를 알 수 없어요."
                        }
                    },
                    onFailure = { e -> friendlyFail(e) }
                )
            }

            // 어디가 켜/꺼져 있나: 서버 power 필터 사용 (방 이름만)
            "QUERY_WHERE_ON" -> {
                val devType = device ?: return "어떤 기기를 말해줄래요?"
                val r = repo.listRoomsByActiveAndType(active = true, deviceType = devType)
                r.fold(
                    onSuccess = { rooms -> speakRoomsOnly(devType, on = true, rooms) },
                    onFailure = { e -> friendlyFail(e) }
                )
            }
            "QUERY_WHERE_OFF" -> {
                val devType = device ?: return "어떤 기기를 말해줄래요?"
                val r = repo.listRoomsByActiveAndType(active = false, deviceType = devType)
                r.fold(
                    onSuccess = { rooms -> speakRoomsOnly(devType, on = false, rooms) },
                    onFailure = { e -> friendlyFail(e) }
                )
            }

            // 온도: OFF면 수치 차단
            "QUERY_TEMPERATURE" -> {
                if (place.isNullOrBlank() || device.isNullOrBlank()) return "대상 기기를 특정할 수 없어요."
                val r = repo.readDeviceBySlots(place, number, device)
                r.fold(
                    onSuccess = { dev ->
                        val on = dev.deviceDetail.optBool("power")
                        if (on == false) return@fold "${dev.deviceName}은 꺼져 있어서 온도를 알 수 없어요."
                        val t  = dev.deviceDetail.optInt("temperature")
                        if (t != null) "${dev.deviceName}의 온도는 ${t}도입니다." else "온도를 알 수 없어요."
                    },
                    onFailure = { e -> friendlyFail(e) }
                )
            }

            // 바람세기: OFF면 수치 차단
            "QUERY_FAN_LEVEL" -> {
                if (place.isNullOrBlank() || device.isNullOrBlank()) return "대상 기기를 특정할 수 없어요."
                val r = repo.readDeviceBySlots(place, number, device)
                r.fold(
                    onSuccess = { dev ->
                        val on = dev.deviceDetail.optBool("power")
                        if (on == false) return@fold "${dev.deviceName}은 꺼져 있어서 바람 세기를 알 수 없어요."
                        val lv = dev.deviceDetail.optInt("level")
                        if (lv != null) "${dev.deviceName}의 바람 세기는 ${lv}단입니다." else "바람 세기를 알 수 없어요."
                    },
                    onFailure = { e -> friendlyFail(e) }
                )
            }

            else -> "아직 지원하지 않는 질의예요."
        }
    }

    // ───────────────────────── Coalesce control intents ─────────────────────────
    private fun coalesceControlBlocks(input: List<IntentResult>): List<ExecItem> {
        val out = mutableListOf<ExecItem>()
        var cur: ControlBlock? = null

        for (r in input) {
            if (isControlIntent(r.intent)) {
                val room   = r.slots["place"]
                val num    = r.slots["num"]?.toIntOrNull()
                val device = canonicalDevice(r.slots["device"])
                val mention = targetMention(r)

                val (pwr, tmp, lvl) = when (r.intent) {
                    "SET_TURN_ON",  "TURN_ON_DEVICE"   -> Triple(true, null, null)
                    "SET_TURN_OFF", "TURN_OFF_DEVICE"  -> Triple(false, null, null)
                    "SET_TEMPERATURE"                  -> Triple(null, r.slots["tmp"]?.toIntOrNull()?.coerceIn(16, 30), null)
                    "SET_FAN_LEVEL","SET_LEVEL_DEVICE" -> Triple(null, null, normalizeLevel(r.slots["level"])?.coerceIn(1, 5))
                    else -> Triple(null, null, null)
                }

                val sameTarget = cur?.sameTarget(room, num, device) == true

                val someTargetPresent = (room != null) || (device != null) || (num != null)
                val shouldSplit = when {
                    cur == null -> false
                    mention.any && !sameTarget -> true
                    !sameTarget && someTargetPresent -> true
                    (room == null && device == null) -> true
                    else -> false
                }

                if (shouldSplit) {
                    out += cur!!
                    cur = null
                }

                if (cur == null) {
                    cur = ControlBlock(room, num, device, pwr, tmp, lvl)
                } else {

                    cur = cur.copy(
                        power = pwr ?: cur.power,
                        temperature = tmp ?: cur.temperature,
                        level = lvl ?: cur.level
                    )
                }
            } else {
                if (cur != null) { out += cur; cur = null }
                out += QueryItem(r)
            }
        }
        if (cur != null) out += cur
        return out
    }

    private fun isControlIntent(name: String) = when (name) {
        "SET_TURN_ON","TURN_ON_DEVICE",
        "SET_TURN_OFF","TURN_OFF_DEVICE",
        "SET_TEMPERATURE",
        "SET_FAN_LEVEL","SET_LEVEL_DEVICE" -> true
        else -> false
    }

    // ───────────────────────── Helpers ─────────────────────────
    private fun canonicalDevice(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return null
        return when (t) {
            "냉방기" -> "에어컨"
            "불", "조명" -> "전등"
            "빔", "빔프로젝터" -> "프로젝터"
            "팬" -> "선풍기"
            else -> t
        }
    }

    private fun normalizeLevel(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        raw.toIntOrNull()?.let { return it }
        return when (raw.trim()) {
            "최강","제일 세게","최대로","맥스","풀로" -> 5
            "강","강하게","세게" -> 4
            "중","중간","보통","적당하게" -> 3
            "약","약하게","살짝" -> 2
            "최소","제일 약하게","가장 약하게" -> 1
            else -> null
        }
    }

    private data class TargetMention(val place: Boolean, val device: Boolean, val num: Boolean) {
        val any get() = place || device || num
    }
    private fun targetMention(r: IntentResult) = TargetMention(
        place  = "place"  in r.slots,
        device = "device" in r.slots,
        num    = "num"    in r.slots
    )

    private fun speakRoomsOnly(deviceType: String, on: Boolean, rooms: List<String>): String {
        if (rooms.isEmpty()) return if (on) "$deviceType 이(가) 켜져 있는 곳은 없어요." else "$deviceType 이(가) 꺼져 있는 곳은 없어요."
        val shown = rooms.take(5)
        val rest  = rooms.size - shown.size
        val head  = if (on) "$deviceType 이(가) 켜져 있는 곳은" else "$deviceType 이(가) 꺼져 있는 곳은"
        return if (rest > 0) "$head ${shown.joinToString(", ")} 외 ${rest}곳입니다."
        else "$head ${shown.joinToString(", ")}입니다."
    }


    private fun friendlyFail(e: Throwable): String {
        val msg = e.message?.trim().orEmpty()
        return if (msg.isNotEmpty()) msg else "요청을 처리하지 못했어요."
    }

    private fun JsonObject.optBool(key: String): Boolean? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asBoolean }.getOrNull() else null

    private fun JsonObject.optInt(key: String): Int? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asInt }.getOrNull() else null

    // ───────────────────────── Internal exec model ─────────────────────────
    private sealed interface ExecItem
    private data class ControlBlock(
        val roomName: String?,
        val number: Int?,
        val deviceType: String?,
        val power: Boolean? = null,
        val temperature: Int? = null,
        val level: Int? = null
    ) : ExecItem {
        fun sameTarget(room: String?, num: Int?, device: String?) =
            roomName == room && number == num && deviceType == device
    }
    private data class QueryItem(val intent: IntentResult) : ExecItem
}