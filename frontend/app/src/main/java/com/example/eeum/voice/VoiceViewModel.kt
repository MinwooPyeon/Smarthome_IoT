package com.example.eeum.voice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.base.DeviceDirectoryCache
import com.example.eeum.data.model.dto.voice.IntentResult
import com.example.eeum.data.model.response.common.ApiResponse
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.remote.repository.DeviceRepository
import com.example.eeum.data.remote.service.DeviceService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class VoiceViewModel(
    private val repo: DeviceRepository,
    private val deviceService: DeviceService,
    private val directory: DeviceDirectoryCache
) : ViewModel() {

    // ---- UI State ----
    data class UiState(
        val isProcessing: Boolean = false,
        val lastSummary: String? = null
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    // ---- One-shot Effects (TTS/Toast 등) ----
    sealed class Effect {
        data class Say(val text: String) : Effect()
        data class Toast(val text: String) : Effect()
    }
    class Event<out T>(val content: T) { private var handled=false; fun get(): T? = if (handled) null else { handled=true; content } }

    private val _effect = MutableLiveData<Event<Effect>>()
    val effect: LiveData<Event<Effect>> = _effect

    /** NLU 결과 실행: 절(클라우즈) 순차 처리 + 같은 타깃은 복합으로 병합 */
    fun execute(intents: List<IntentResult>) {
        if (intents.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isProcessing = true, lastSummary = null)

            val summaries = mutableListOf<String>()
            for (block in coalesceControlBlocks(intents)) {
                val s = when (block) {
                    is ControlBlock -> applyComposite(block)
                    is QueryItem    -> handleQuery(block.intent)
                }
                if (s.isNotBlank()) summaries += s
            }

            val summary = summaries.joinToString(" 그리고 ").ifBlank { null }
            summary?.let { _effect.value = Event(Effect.Say(it)) }
            _uiState.value = UiState(isProcessing = false, lastSummary = summary)
        }
    }

    // ───────────── Control path ─────────────
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

    // ───────────── Query path ─────────────
    private suspend fun handleQuery(nlu: IntentResult): String = withContext(Dispatchers.IO) {
        val place  = nlu.slots["place"]
        val device = canonicalDevice(nlu.slots["device"])
        val number = nlu.slots["num"]?.toIntOrNull()

        val id = directory.findDeviceId(place, number, device) ?: return@withContext "대상 기기를 찾을 수 없어요."
        val res: Response<ApiResponse<DeviceResponse>> = deviceService.readDevice(id)
        if (!res.isSuccessful) return@withContext "조회에 실패했어요(HTTP ${res.code()})."
        val api = res.body() ?: return@withContext "조회 응답이 비었어요."
        if (api.status != "SUCCES") return@withContext "조회에 실패했어요."

        val detail: JsonObject = api.data.deviceDetail
        return@withContext when (nlu.intent) {
            "QUERY_IS_ON" -> {
                val on = detail.optBool("power") ?: return@withContext "전원 상태를 알 수 없어요."
                if (on) "켜져 있어요." else "꺼져 있어요."
            }
            "QUERY_TEMPERATURE" -> {
                val t = detail.optInt("temperature") ?: return@withContext "온도를 알 수 없어요."
                "현재 온도는 ${t}도예요."
            }
            "QUERY_FAN_LEVEL" -> {
                val lv = detail.optInt("level") ?: return@withContext "바람 세기를 알 수 없어요."
                "바람은 ${lv}단이에요."
            }
            else -> "아직 지원하지 않는 질의예요."
        }
    }

    // ───────────── Coalesce control intents ─────────────
    private fun coalesceControlBlocks(input: List<IntentResult>): List<ExecItem> {
        val out = mutableListOf<ExecItem>()
        var cur: ControlBlock? = null

        for (r in input) {
            if (isControlIntent(r.intent)) {
                val room   = r.slots["place"]
                val num    = r.slots["num"]?.toIntOrNull()
                val device = canonicalDevice(r.slots["device"])

                val (pwr, tmp, lvl) = when (r.intent) {
                    "SET_TURN_ON",  "TURN_ON_DEVICE"   -> Triple(true, null, null)
                    "SET_TURN_OFF", "TURN_OFF_DEVICE"  -> Triple(false, null, null)
                    "SET_TEMPERATURE"                  -> Triple(null, r.slots["tmp"]?.toIntOrNull()?.coerceIn(16, 30), null)
                    "SET_FAN_LEVEL","SET_LEVEL_DEVICE" -> Triple(null, null, normalizeLevel(r.slots["level"])?.coerceIn(1, 5))
                    else -> Triple(null, null, null)
                }

                if (cur != null && cur.sameTarget(room, num, device)) {
                    cur = cur.copy(
                        power = pwr ?: cur.power,
                        temperature = tmp ?: cur.temperature,
                        level = lvl ?: cur.level
                    )
                } else {
                    if (cur != null) out += cur
                    cur = ControlBlock(room, num, device, pwr, tmp, lvl)
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

    // ───────────── Helpers ─────────────
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

    private fun JsonObject.optBool(key: String): Boolean? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asBoolean }.getOrNull() else null

    private fun JsonObject.optInt(key: String): Int? =
        if (has(key) && !get(key).isJsonNull) runCatching { get(key).asInt }.getOrNull() else null

    // ───────────── Internal exec model ─────────────
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