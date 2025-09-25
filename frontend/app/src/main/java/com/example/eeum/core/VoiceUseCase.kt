package com.example.eeum.core

import android.util.Log
import com.example.eeum.data.model.dto.routine.RoutineDetailRequest
import com.example.eeum.data.model.dto.routine.RoutineRequest
import com.example.eeum.data.model.dto.voice.IntentResult
import com.example.eeum.data.model.response.routine.RoutineResponse
import com.example.eeum.data.remote.repository.DeviceRepository
import com.example.eeum.data.remote.repository.RoutineRepository
import com.example.eeum.util.Payload
import com.example.eeum.util.ResourceUtils.friendlyFail
import com.example.eeum.util.ResourceUtils.optBool
import com.example.eeum.util.ResourceUtils.optInt
import com.example.eeum.util.ResourceUtils.parseTime
import com.example.eeum.util.ResourceUtils.parseWeekdays
import com.google.gson.JsonObject

private const val TAG = "EEUM_VoiceUseCase"

class VoiceUseCase(
    private val repo: DeviceRepository,
    private val routineRepo: RoutineRepository
) {
    suspend fun run(intents: List<IntentResult>, raw: String): List<AppEffect> {
        if (intents.isEmpty()) return emptyList()

        // 이름 기반 조회/삭제는 반복 필요 없음 (expectReply=false)
        intents.firstOrNull {
            it.intent == "ROUTINE_SHOW_BY_NAME" || it.intent == "ROUTINE_DELETE_BY_NAME"
        }?.let { hit ->
            val speech = handleRoutineByName(hit)
            return listOf(AppEffect.Speak(speech, /* expectReply = */ false))
        }

        // ── 루틴 생성 시작 ──
        intents.firstOrNull { it.intent == "ROUTINE_CREATE_START" }?.let {
            routineSession = RoutineSession() // 세션 초기화
            return listOf(AppEffect.Speak("루틴 생성을 시작할게요. 루틴 이름을 말씀해 주세요.", true))
        }

        // ── 루틴 세션 중이면 우선 처리 ──
        routineSession?.let { sess ->
            val say = handleRoutineFlow(sess, intents, raw)
            return listOf(AppEffect.Speak(say.text, say.expectReply))
        }

        // ── 일반 제어/질의: 반복 필요 없음 ──
        val effects = mutableListOf<AppEffect>()
        for (block in coalesceControlBlocks(intents)) {
            when (block) {
                is ControlBlock -> {
                    val speech = applyComposite(block)
                    if (speech.isNotBlank()) {
                        effects += AppEffect.Speak(speech, false)
                        effects += AppEffect.DevicesChanged
                    }
                }
                is ScopeBlock -> {
                    val speech = applyScope(block)
                    if (speech.isNotBlank()) {
                        effects += AppEffect.Speak(speech, false)
                        effects += AppEffect.DevicesChanged
                    }
                }
                is QueryItem -> {
                    val speech = handleQuery(block.intent)
                    if (speech.isNotBlank()) effects += AppEffect.Speak(speech, false)
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
        return r.fold(
            onSuccess = {
                AppEventBus.tryEmit(AppEffect.DevicesChanged)
                buildSuccessSpeech(b) },
            onFailure = { e -> friendlyFail(e) }
        )
    }

    private suspend fun applyScope(b: ScopeBlock): String {
        // 1) "침실 전부 켜줘" 같은 방 계열 전체 (number 미지정)
        if (!b.roomName.isNullOrBlank() && b.number == null) {
            val r = repo.bulkSetPowerRoomFamily(
                baseRoom = b.roomName!!,
                deviceType = b.deviceType,
                on = b.power
            )
            return r.fold(
                onSuccess = { cnt ->
                    AppEventBus.tryEmit(AppEffect.DevicesChanged)
                    val what = b.deviceType ?: "모든 기기"
                    val act  = if (b.power) "켰어요" else "껐어요"
                    if (cnt > 0) "${b.roomName} 계열의 $what ${cnt}대 $act." else "대상이 없어요."
                },
                onFailure = { e -> friendlyFail(e) }
            )
        }

        // 2) 방+번호가 명시되면 → 정확 일치 스코프
        if (!b.roomName.isNullOrBlank()) {
            val r = repo.bulkSetPower(
                roomName = b.roomName!!,
                number = b.number,
                deviceType = b.deviceType,
                on = b.power
            )
            return r.fold(
                onSuccess = { cnt ->
                    AppEventBus.tryEmit(AppEffect.DevicesChanged)
                    val what = b.deviceType ?: "모든 기기"
                    val act  = if (b.power) "켰어요" else "껐어요"
                    val room = buildRoomKey(b.roomName, b.number)
                    if (cnt > 0) "${room}의 $what ${cnt}대 $act." else "대상이 없어요."
                },
                onFailure = { e -> friendlyFail(e) }
            )
        }

        // 3) 방 지정이 없으면 → 전체/타입 일괄
        val r = repo.bulkSetPower(deviceType = b.deviceType, on = b.power)
        return r.fold(
            onSuccess = { cnt ->
                AppEventBus.tryEmit(AppEffect.DevicesChanged)
                val what = b.deviceType ?: "모든 기기"
                val verb = if (b.power) "켰어요" else "껐어요"
                if (cnt > 0) "$what ${cnt}대 $verb." else "대상이 없어요."
            },
            onFailure = { e -> friendlyFail(e) }
        )
    }

    private fun buildSuccessSpeech(b: ControlBlock): String {
        val parts = mutableListOf<String>()
        b.power?.let { parts += if (it) "켰어요" else "껐어요" }
        b.temperature?.let { parts += "온도 ${it}도로 맞췄어요" }
        b.level?.let { parts += "바람 ${it}단으로 바꿨어요" }
        return if (parts.isEmpty()) "완료했어요." else parts.joinToString(" 그리고 ")
    }

    // ───────────────────────── Routine flow ─────────────────────────
    private suspend fun handleRoutineFlow(
        sess: RoutineSession,
        intents: List<IntentResult>,
        raw: String
    ): Speech {
        fun next(step: RoutineStep, prompt: String): Speech {
            sess.step = step
            return Speech(prompt, expectReply = true)
        }

        // — 루틴 중단/건너뛰기/저장 같은 “메타” 인텐트
        val wantSave  = raw.contains("저장", true) || raw.contains("완료", true) || raw.contains("끝", true)
        val wantSkip  = raw.contains("건너뛰", true) || raw.contains("넘겨", true) || raw.contains("패스", true) || raw.contains("넘어가", true)
        val wantStop  = raw.contains("그만", true) || raw.contains("정지", true) || raw.contains("취소", true) || raw.contains("안할래", true)

        // — 명령형 인텐트 플래그 (macros 반영)
        val wantList   = intents.any { it.intent == "ROUTINE_LIST_ACTIONS" }
        val wantUndo   = intents.any { it.intent == "ROUTINE_UNDO_LAST" }
        val wantClear  = intents.any { it.intent == "ROUTINE_CLEAR_ACTIONS" }
        val wantRename = intents.any { it.intent == "ROUTINE_RENAME" }
        val wantRedesc = intents.any { it.intent == "ROUTINE_REDESC" }

        // — 즉시 단계 전환 (이름/설명 재설정)
        if (wantRename) {
            sess.step = RoutineStep.NAME
            return Speech("새 루틴 이름을 말씀해 주세요.", true)
        }
        if (wantRedesc) {
            sess.step = RoutineStep.DESCRIPTION
            return Speech("새 루틴 설명을 말씀해 주세요. 생략하려면 '건너뛰기'라고 말해 주세요.", true)
        }

        // — 세션 전체 중단
        if (wantStop) {
            routineSession = null
            return Speech("루틴 생성을 취소했어요.", false) // 종료 → 재청취 X
        }

        when (sess.step) {
            RoutineStep.NAME -> {
                val hasNameIntent = intents.any { it.intent == "ROUTINE_SET_NAME" }
                val name = if (hasNameIntent) extractAfter(raw, NAME_PREFIXES) else raw.trim()
                if (name.isEmpty()) return Speech("루틴 이름을 다시 말씀해 주세요. 예) '루틴 이름은 아침 루틴이야'", true)
                sess.name = name
                return next(RoutineStep.DESCRIPTION, "루틴 설명을 말씀해 주세요. 생략하시려면 '건너뛰기'라고 말해 주세요.")
            }

            RoutineStep.DESCRIPTION -> {
                if (wantSkip) {
                    sess.description = null
                } else {
                    val hasDescIntent = intents.any { it.intent == "ROUTINE_SET_DESC" }
                    val desc = if (hasDescIntent) extractAfter(raw, DESC_PREFIXES) else raw.trim()
                    if (desc.isEmpty()) return Speech("설명을 이해하지 못했어요. 예) '루틴 설명은 평일 아침 준비야' 또는 '건너뛰기'", true)
                    sess.description = desc
                }
                return next(RoutineStep.WEEKDAY, "요일을 말씀해 주세요. 예: 월 화 수, 또는 평일, 주말.")
            }

            RoutineStep.WEEKDAY -> {
                val weekText = if (intents.any { it.intent == "ROUTINE_SET_WEEK" }) {
                    extractAfter(raw, WEEK_PREFIXES)
                } else raw
                val mask = parseWeekdays(weekText)
                if (mask == null || mask == 0) return Speech("요일을 이해하지 못했어요. 예: 월 화, 평일, 주말.", true)
                sess.weekdayMask = mask
                return next(RoutineStep.TIME, "시간을 말씀해 주세요. 예: 오전 7시 30분, 밤 10시.")
            }

            RoutineStep.TIME -> {
                val timeText = if (intents.any { it.intent == "ROUTINE_SET_TIME" }) {
                    extractAfter(raw, TIME_PREFIXES)
                } else raw
                val hhmm = parseTime(timeText)
                if (hhmm == null) return Speech("시간을 이해하지 못했어요. 예: 오전 7시, 오후 9시 15분.", true)
                sess.actTime = hhmm
                return next(RoutineStep.ACTIONS, "이제 동작을 말씀해 주세요. 예: 거실 전등 켜줘. 끝내려면 '저장'이라고 말해 주세요.")
            }

            RoutineStep.ACTIONS -> {
                // 리스트/되돌리기/초기화 등 관리 명령
                if (wantList) {
                    val n = sess.details.size
                    if (n == 0) return Speech("아직 추가된 동작이 없어요.", true)
                    val preview = sess.details.take(3).mapIndexed { idx, d ->
                        val name = repo.getDeviceName(d.deviceId).getOrNull() ?: "디바이스 ${d.deviceId}"
                        val p = d.deviceDetail
                        val bits = buildList {
                            p.get("power")?.let { add(if (it.asBoolean) "켜기" else "끄기") }
                            p.get("temperature")?.let { add("온도 ${it.asInt}도") }
                            p.get("level")?.let { add("바람 ${it.asInt}단") }
                        }.joinToString(", ")
                        "${idx + 1}. $name${if (bits.isNotEmpty()) " ($bits)" else ""}"
                    }.joinToString(" ")
                    return if (n > 3) Speech("동작은 총 ${n}개예요. 예: $preview …", true)
                    else Speech("동작은 총 ${n}개예요. $preview", true)
                }
                if (wantUndo) {
                    if (sess.details.isEmpty()) return Speech("되돌릴 동작이 없어요.", true)
                    sess.details.removeAt(sess.details.lastIndex)
                    return Speech("마지막 동작을 취소했어요. 더 추가하시겠어요, 아니면 '저장'할까요?", true)
                }
                if (wantClear) {
                    if (sess.details.isEmpty()) return Speech("이미 동작이 비어 있어요.", true)
                    sess.details.clear()
                    return Speech("동작을 모두 지웠어요. 새로 추가하시겠어요?", true)
                }

                // 저장 의사
                if (wantSave) {
                    val n = sess.name
                    val w = sess.weekdayMask
                    val t = sess.actTime
                    if (n.isNullOrBlank() || w == null || t.isNullOrBlank() || sess.details.isEmpty()) {
                        return Speech("입력이 부족해요. 동작을 최소 1개 이상 추가해 주세요.", true)
                    }

                    val normalizedDesc = sess.description?.takeIf { it.isNotBlank() } ?: " "

                    val req = RoutineRequest(
                        name = n,
                        routineWeekday = w,
                        routineDescription = normalizedDesc,
                        actTime = com.example.eeum.util.ResourceUtils.toIsoActTime(t),
                        detail = sess.details.toList(),
                        isAi = false
                    )

                    Log.d(TAG, "handleRoutineFlow: $req")
                    val r = routineRepo.createRoutine(req)
                    routineSession = null
                    return r.fold(
                        onSuccess = {
                            AppEventBus.tryEmit(AppEffect.RoutinesChanged)
                            Speech("루틴이 등록되었어요.", false) }, // 완료 → 재청취 종료
                        onFailure = { Speech(friendlyFail(it), true) }       // 실패 → 계속 듣기
                    )
                }

                // 이번 발화에서 제어 인텐트를 동작으로 수집
                val controls = coalesceControlBlocks(intents).filterIsInstance<ControlBlock>()
                if (controls.isEmpty()) {
                    return Speech("동작을 이해하지 못했어요. 다른 표현으로 말씀해 주세요. 저장하려면 '저장'이라고 말해 주세요.", true)
                }

                var added = 0
                for (b in controls) {
                    val device = canonicalDevice(b.deviceType)
                    if (b.roomName.isNullOrBlank() || device.isNullOrBlank()) continue
                    val devRes = repo.readDeviceBySlots(b.roomName, b.number, device)
                    if (!devRes.isSuccess) continue
                    val dev = devRes.getOrNull() ?: continue

                    val detail = Payload.deviceDetail(
                        power = b.power,
                        temperature = b.temperature,
                        level = b.level
                    )
                    sess.details += RoutineDetailRequest(
                        deviceId = dev.deviceId,
                        deviceDetail = detail
                    )
                    added++
                }
                return if (added > 0) {
                    Speech("동작 ${added}개를 추가했어요. 더 추가하시겠어요? 아니면 '저장'이라고 말해 주세요.", true)
                } else {
                    Speech("동작을 이해하지 못했어요. 예: 침실1 에어컨 24도로 맞춰줘.", true)
                }
            }
        }
    }

    // ───────────────────────── Query path ─────────────────────────
    private suspend fun handleQuery(nlu: IntentResult): String {
        val place  = nlu.slots["place"]
        val device = canonicalDevice(nlu.slots["device"])
        val number = nlu.slots["num"]?.toIntOrNull()

        return when (nlu.intent) {
            // 전원 켜짐/꺼짐 묻기: deviceDetail.power만 보고 응답
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

            // 상태 요약(타입별): 에어컨 → power/temperature/level, 선풍기 → power/level, 기타 → power만
            "QUERY_IS_STATE" -> {
                if (place.isNullOrBlank() || device.isNullOrBlank()) return "대상 기기를 특정할 수 없어요."
                val r = repo.readDeviceBySlots(place, number, device)
                r.fold(
                    onSuccess = { dev ->
                        val type = canonicalDevice(dev.deviceType) ?: device
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

            // 어디가 켜/꺼져 있나: 서버 power + type 필터 사용(방 이름만)
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
            val hasScope = r.slots.containsKey("scope")

            if (isControlIntent(r.intent)) {
                val room   = r.slots["place"]
                val num    = r.slots["num"]?.toIntOrNull()
                val device = canonicalDevice(r.slots["device"])

                // ① 범위 명령이면 → ScopeBlock으로 분리 (방/타입/전체)
                if (hasScope && (r.intent == "SET_TURN_ON" || r.intent == "SET_TURN_OFF")) {
                    if (cur != null) { out += cur; cur = null }
                    val power = (r.intent == "SET_TURN_ON")
                    out += ScopeBlock(roomName = room, number = num, deviceType = device, power = power)
                    continue
                }

                // ② 개별 타깃 제어 병합
                val (pwr, tmp, lvl) = when (r.intent) {
                    "SET_TURN_ON",  "TURN_ON_DEVICE"    -> Triple(true, null, null)
                    "SET_TURN_OFF", "TURN_OFF_DEVICE"   -> Triple(false, null, null)
                    "SET_TEMPERATURE"                   -> Triple(null, r.slots["tmp"]?.toIntOrNull()?.coerceIn(16, 30), null)
                    "SET_FAN_LEVEL","SET_LEVEL_DEVICE"  -> Triple(null, null, normalizeLevel(r.slots["level"])?.coerceIn(1, 5))
                    else -> Triple(null, null, null)
                }

                val mention = targetMention(r)
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
            "공기 청정기"-> "공기청정기"
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

    private fun buildDeviceName(place: String?, number: Int?, device: String?): String {
        val rn = place?.trim().orEmpty()
        val dn = device?.trim().orEmpty()
        val nn = number?.toString().orEmpty()
        return if (rn.isEmpty() || dn.isEmpty()) "해당 기기"
        else if (nn.isEmpty()) "$rn $dn" else "$rn$nn $dn"
    }

    private fun buildRoomKey(place: String?, number: Int?): String {
        val rn = place?.trim().orEmpty()
        val nn = number?.toString().orEmpty()
        return if (nn.isEmpty()) rn else rn + nn
    }

    private suspend fun handleRoutineByName(nlu: IntentResult): String {
        val name = nlu.slots["name"]?.trim().orEmpty()
        if (name.isBlank()) return "루틴 이름을 이해하지 못했어요."

        val id = routineRepo.findIdByName(name)
            ?: return "루틴 '${name}'을 찾을 수 없어요."

        return when (nlu.intent) {
            "ROUTINE_SHOW_BY_NAME" -> {
                val r = routineRepo.readRoutineById(id)
                r.fold(
                    onSuccess = { rr -> summarizeRoutine(rr) },
                    onFailure = { e -> friendlyFail(e) }
                )
            }
            "ROUTINE_DELETE_BY_NAME" -> {
                val r = routineRepo.deleteRoutineById(id)
                r.fold(
                    onSuccess = {
                        AppEventBus.tryEmit(AppEffect.RoutinesChanged)
                        "루틴 '${name}'을 삭제했어요." },
                    onFailure = { e -> friendlyFail(e) }
                )
            }
            else -> "아직 지원하지 않는 루틴 명령이에요."
        }
    }

    private fun summarizeRoutine(rr: RoutineResponse): String {
        val days = weekdayMaskToText(rr.routineWeekday).ifBlank { "매일" }
        val time = formatActTime(rr.actTime)
        val actions = rr.details.size
        return "‘${rr.name}’ 요약: 요일은 ${days}, 시간은 ${time}, 동작은 ${actions}개입니다."
    }

    private fun weekdayMaskToText(mask: Int): String {
        val names = listOf("월","화","수","목","금","토","일")
        return names.mapIndexedNotNull { i, n -> if (mask and (1 shl i) != 0) n else null }
            .joinToString("")
    }

    private fun formatActTime(raw: String): String {
        val s = raw.trim()

        // 1) "H:mm" / "HH:mm[:ss]"
        Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?$""").matchEntire(s)?.let { m ->
            val h = m.groupValues[1].toInt().coerceIn(0, 23)
            val mm = m.groupValues[2].toInt().coerceIn(0, 59)
            return "%02d:%02d".format(h, mm)
        }

        // 2) ISO_INSTANT (…Z)
        runCatching {
            val zdt = org.threeten.bp.Instant.parse(s).atZone(org.threeten.bp.ZoneId.systemDefault())
            return "%02d:%02d".format(zdt.hour, zdt.minute)
        }

        // 3) ISO_OFFSET_DATE_TIME (…+09:00 / …-05:00)
        runCatching {
            val odt = org.threeten.bp.OffsetDateTime.parse(
                s, org.threeten.bp.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
            val zdt = odt.atZoneSameInstant(org.threeten.bp.ZoneId.systemDefault())
            return "%02d:%02d".format(zdt.hour, zdt.minute)
        }

        // 4) ISO_LOCAL_DATE_TIME (타임존 없음)
        runCatching {
            val ldt = org.threeten.bp.LocalDateTime.parse(
                s, org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )
            return "%02d:%02d".format(ldt.hour, ldt.minute)
        }

        // 5) epoch millis 문자열
        s.toLongOrNull()?.let { ep ->
            val zdt = org.threeten.bp.Instant.ofEpochMilli(ep)
                .atZone(org.threeten.bp.ZoneId.systemDefault())
            return "%02d:%02d".format(zdt.hour, zdt.minute)
        }

        // 6) 실패 시 원문 반환
        return s
    }

    // ───────────────────────── Internal exec model ─────────────────────────
    private sealed interface ExecItem

    private data class Speech(val text: String, val expectReply: Boolean)

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

    private data class ScopeBlock(
        val roomName: String?,
        val number: Int?,
        val deviceType: String?,
        val power: Boolean
    ) : ExecItem

    private data class QueryItem(val intent: IntentResult) : ExecItem

    private var routineSession: RoutineSession? = null

    private enum class RoutineStep { NAME, DESCRIPTION, WEEKDAY, TIME, ACTIONS }

    private data class RoutineSession(
        var step: RoutineStep = RoutineStep.NAME,
        var name: String? = null,
        var description: String? = null,
        var weekdayMask: Int? = null,
        var actTime: String? = null,                 // "HH:mm"
        val details: MutableList<RoutineDetailRequest> = mutableListOf()
    )

    private val NAME_PREFIXES = listOf(
        "루틴 이름은", "이름은", "루틴 이름", "이름", "이름을", "이름을 말하자면"
    )

    private val DESC_PREFIXES = listOf(
        "루틴 설명은", "설명은", "루틴 설명", "설명", "설명을", "설명을 말하자면"
    )

    private val WEEK_PREFIXES = listOf(
        "요일은", "요일이", "요일", "날짜는", "날짜가", "날짜",
        "루틴 요일은", "루틴 요일이", "루틴 요일"
    )

    private val TIME_PREFIXES = listOf(
        "시간은", "시간이", "시간", "시기는", "시기가", "시기",
        "루틴 시간은", "루틴 시간이", "루틴 시간"
    )

    private fun extractAfter(raw: String, prefixes: List<String>): String {
        val t = raw.trim()
        for (p in prefixes) {
            if (t.startsWith(p)) {
                return t.removePrefix(p).trim()
                    .removePrefix("은").removePrefix("는")
                    .removePrefix("이").removePrefix("가")
                    .trim()
            }
        }
        return t
    }
}
