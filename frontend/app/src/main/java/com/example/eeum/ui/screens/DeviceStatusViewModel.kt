package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.device.DeviceStatusChangeRequest
import com.example.eeum.data.model.dto.device.DeviceStatusChangeRequestDetail
import com.example.eeum.data.model.response.device.DeviceStatusChangeResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class DeviceStatusViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    private val _status = MutableLiveData<String?>(null)
    val status: LiveData<String?> get() = _status

    private val _lastChangedDeviceId = MutableLiveData<Int?>(null)
    val lastChangedDeviceId: LiveData<Int?> get() = _lastChangedDeviceId

    private val _result = MutableLiveData<DeviceStatusChangeResponse?>(null)
    val result: LiveData<DeviceStatusChangeResponse?> get() = _result

    /**
     * 디바이스 상태 변경
     * @param deviceId 디바이스 ID
     * @param power 전원 상태 (true: 켜짐, false: 꺼짐)
     * @param temperature 온도 설정 (에어컨 등)
     * @param level 레벨/강도 설정 (선풍기 등)
     */
    fun changeDeviceStatus(
        deviceId: Int,
        power: Boolean,
        temperature: Int = 23,
        level: Int = 1
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val requestDetail = DeviceStatusChangeRequestDetail(
                    level = level,
                    power = power,
                    temperature = temperature
                )
                val request = DeviceStatusChangeRequest(deviceDetail = requestDetail)

                Log.d("DeviceStatusViewModel", "상태 변경 요청: deviceId=$deviceId, power=$power, temp=$temperature, level=$level")

                val response = RetrofitUtil.deviceService.changeDeviceStatus(deviceId, request)

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _result.value = body
                        _status.value = body.status
                        _lastChangedDeviceId.value = body.data.deviceId
                        _error.value = null
                        Log.d("DeviceStatusViewModel", "상태 변경 성공: ${body.status}, deviceId=${body.data.deviceId}")
                    } ?: run {
                        _error.value = "상태 변경 실패: 빈 응답"
                        Log.e("DeviceStatusViewModel", "상태 변경 실패: 빈 응답")
                    }
                } else {
                    val errorMsg = "상태 변경 실패: HTTP ${response.code()}"
                    _error.value = errorMsg
                    Log.e("DeviceStatusViewModel", "$errorMsg - ${response.message()}")
                }
            } catch (e: Exception) {
                val errorMsg = "네트워크 오류: ${e.message}"
                _error.value = errorMsg
                Log.e("DeviceStatusViewModel", "상태 변경 실패", e)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 디바이스 전원 토글 (간편 메서드)
     * @param deviceId 디바이스 ID
     * @param currentPower 현재 전원 상태
     */
    fun toggleDevicePower(deviceId: Int, currentPower: Boolean) {
        changeDeviceStatus(deviceId = deviceId, power = !currentPower)
    }

    /**
     * 에어컨 온도 설정
     * @param deviceId 디바이스 ID
     * @param temperature 설정할 온도
     * @param power 전원 상태 (기본값: true)
     */
    fun setAirConditionerTemperature(
        deviceId: Int,
        temperature: Int,
        power: Boolean = true
    ) {
        changeDeviceStatus(
            deviceId = deviceId,
            power = power,
            temperature = temperature,
            level = 1
        )
    }

    /**
     * 선풍기 레벨 설정
     * @param deviceId 디바이스 ID
     * @param level 설정할 레벨 (1-5)
     * @param power 전원 상태 (기본값: true)
     */
    fun setFanLevel(
        deviceId: Int,
        level: Int,
        power: Boolean = true
    ) {
        changeDeviceStatus(
            deviceId = deviceId,
            power = power,
            temperature = 23, // 기본값
            level = level.coerceIn(1, 5) // 1-5 범위로 제한
        )
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 결과 초기화
     */
    fun clearResult() {
        _result.value = null
        _status.value = null
        _lastChangedDeviceId.value = null
    }
}