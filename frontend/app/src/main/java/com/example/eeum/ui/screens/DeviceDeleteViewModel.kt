package com.example.eeum.ui.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.device.DeviceDeleteResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class DeviceDeleteViewModel : ViewModel() {
    private val _deleteResult = MutableLiveData<DeviceDeleteResponse?>(null)
    val deleteResult: LiveData<DeviceDeleteResponse?> get() = _deleteResult
    
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error
    
    private val _isDeleted = MutableLiveData(false)
    val isDeleted: LiveData<Boolean> get() = _isDeleted

    /**
     * 디바이스 삭제 요청
     * @param deviceId 삭제할 디바이스 ID
     */
    fun deleteDevice(deviceId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            runCatching { 
                RetrofitUtil.deviceService.deleteDevice(deviceId) 
            }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val body = response.body()
                    _deleteResult.value = body
                    
                    // 삭제 성공 여부 확인
                    val deleted = body?.data?.deleted ?: false
                    _isDeleted.value = deleted
                    
                    if (!deleted) {
                        _error.value = "디바이스 삭제에 실패했습니다."
                    }
                } else {
                    _error.value = "HTTP ${response.code()}: 서버 오류가 발생했습니다."
                }
            }
            .onFailure { exception ->
                _error.value = exception.message ?: "네트워크 오류가 발생했습니다."
            }
            
            _loading.value = false
        }
    }
    
    /**
     * 삭제 상태 초기화
     */
    fun resetDeleteState() {
        _deleteResult.value = null
        _isDeleted.value = false
        _error.value = null
    }
    
    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _error.value = null
    }
}