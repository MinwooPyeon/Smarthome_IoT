package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.device.HubRequest
import com.example.eeum.data.model.response.device.HubResponseData
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class HubViewModel : ViewModel() {

    private val _registrationStatus = MutableLiveData<String>()
    val registrationStatus: LiveData<String> get() = _registrationStatus

    private val _userHomeId = MutableLiveData<Int?>()
    val userHomeId: LiveData<Int?> get() = _userHomeId

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 허브 등록
    fun registerHub(homeId: Int, hubDeviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            runCatching {
                val hubRequest = HubRequest(
                    homeId = homeId,
                    hubDeviceId = hubDeviceId
                )
                RetrofitUtil.hubService.registerHub(hubRequest)
            }.onSuccess { response ->
                _isLoading.value = false
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _userHomeId.value = body.data.userHomeId
                        _registrationStatus.value = body.status
                        _error.value = null
                        Log.d(
                            "HubViewModel",
                            "허브 등록 성공: homeId=$homeId, hubDeviceId=$hubDeviceId, userHomeId=${body.data.userHomeId}, status=${body.status}"
                        )
                    } ?: run {
                        _error.value = "허브 등록 응답이 비어있습니다."
                        Log.e("HubViewModel", "허브 등록 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "허브 등록 실패: ${response.code()}"
                    Log.e("HubViewModel", "허브 등록 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _isLoading.value = false
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HubViewModel", "허브 등록 중 오류 발생", e)
            }
        }
    }

    // 에러 메시지 초기화
    fun clearError() {
        _error.value = null
    }

    // 상태 초기화
    fun clearState() {
        _registrationStatus.value = ""
        _userHomeId.value = null
        _error.value = null
        _isLoading.value = false
    }
}