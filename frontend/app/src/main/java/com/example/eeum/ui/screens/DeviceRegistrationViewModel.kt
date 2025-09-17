package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.device.DeviceRequest
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class DeviceRegistrationViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _result = MutableLiveData<BaseResponse?>()
    val result: LiveData<BaseResponse?> get() = _result

    /**
     * 디바이스 등록 API 호출
     */
    fun registerDevice(request: DeviceRequest) {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                RetrofitUtil.deviceService.createDevice(request)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _result.value = body.data
                        _status.value = body.status
                        _error.value = null
                        Log.d(
                            "DeviceRegistrationVM",
                            "등록 성공: id=${body.data.id}, status=${body.status}"
                        )
                    } ?: run {
                        _error.value = "등록 실패: 빈 응답"
                        Log.e("DeviceRegistrationVM", "등록 실패: 빈 응답")
                    }
                } else {
                    _error.value = "등록 실패: ${response.code()}"
                    Log.e(
                        "DeviceRegistrationVM",
                        "등록 실패 code=${response.code()} message=${response.message()}"
                    )
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("DeviceRegistrationVM", "등록 실패", e)
            }
            _loading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
