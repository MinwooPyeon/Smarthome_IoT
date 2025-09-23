package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.menu.LogData
import com.example.eeum.data.model.response.menu.LogResponse
import com.example.eeum.data.remote.RetrofitUtil
import com.example.eeum.base.ApplicationClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class LogViewModel : ViewModel() {

    private val _logs = MutableLiveData<List<LogRecord>>(emptyList())
    val logs: LiveData<List<LogRecord>> get() = _logs

    private val _displayDate = MutableLiveData<String>(nowKst().format(koreanDateFormatter()))
    val displayDate: LiveData<String> get() = _displayDate

    fun fetchLogs() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val homeId = ApplicationClass.sharedPreferencesUtil.getSelectedHomeId() ?: 1
                    Log.d("LogViewModel", "Fetching IR logs, homeId=$homeId")
                    RetrofitUtil.authService.getIrLogs(homeId).execute()
                }
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val body: LogResponse? = response.body()
                    val items: List<LogData> = body?.data ?: emptyList()
                    val mapped = items.map { it.toRecord() }
                        .sortedByDescending { it.timestamp }
                    _logs.value = mapped

                    // 최신 로그의 날짜로 헤더 설정 (없으면 현재 날짜)
                    val headerZdt = mapped.firstOrNull()?.let {
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.of("Asia/Seoul"))
                    } ?: nowKst()
                    _displayDate.value = headerZdt.format(koreanDateFormatter())

                    Log.d("LogViewModel", "IR logs fetched: ${mapped.size} items")
                } else {
                    Log.e("LogViewModel", "getIrLogs failed: code=${response.code()} msg=${response.message()}")
                    _logs.value = emptyList()
                    _displayDate.value = nowKst().format(koreanDateFormatter())
                }
            }.onFailure { e ->
                Log.e("LogViewModel", "getIrLogs error", e)
                _logs.value = emptyList()
                _displayDate.value = nowKst().format(koreanDateFormatter())
            }
        }
    }

    private fun LogData.toRecord(): LogRecord {
        val zdt = parseToKst(eventTime)
        val hour = zdt.hour
        val minute = zdt.minute
        val period = if (hour < 12) "오전" else "오후"
        val hour12 = when {
            hour == 0 -> 12
            hour <= 12 -> hour
            else -> hour - 12
        }
        val timeStr = String.format("%d:%02d", hour12, minute)

        val statusText = when {
            kind.startsWith("temperature_", ignoreCase = true) -> {
                val v = kind.substringAfter("temperature_")
                "온도 ${v}°C"
            }
            kind.startsWith("power_", ignoreCase = true) -> {
                when (kind.substringAfter("power_").lowercase()) {
                    "on", "true", "1" -> "전원 켜짐"
                    "off", "false", "0" -> "전원 꺼짐"
                    else -> "전원 ${kind.substringAfter("power_")}"
                }
            }
            else -> kind
        }

        return LogRecord(
            id = "${eventTime}_${deviceName}_${roomId}",
            timestamp = zdt.toInstant().toEpochMilli(),
            period = period,
            time = timeStr,
            device = deviceName,      // deviceName → record.device
            location = roomName,      // roomName   → record.location
            status = statusText       // kind       → record.status (규칙 적용)
        )
    }

    private fun parseToKst(iso: String): ZonedDateTime {
        return try {
            OffsetDateTime.parse(iso).toZonedDateTime().withZoneSameInstant(ZoneId.of("Asia/Seoul"))
        } catch (e: Exception) {
            try {
                val instant = Instant.parse(iso)
                instant.atZone(ZoneId.of("Asia/Seoul"))
            } catch (e2: Exception) {
                Log.w("LogViewModel", "Failed to parse eventTime='$iso', default now", e2)
                nowKst()
            }
        }
    }

    private fun nowKst(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
    private fun koreanDateFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("M. d. EEEE", Locale.KOREAN)
}
