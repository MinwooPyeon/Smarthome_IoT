package com.example.eeum.ui.screens

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class DeviceListViewModel : ViewModel() {
    private val _items = MutableLiveData<List<DeviceResponse>>(emptyList())
    val items: LiveData<List<DeviceResponse>> get() = _items

    private val _totalCount = MutableLiveData(0)
    val totalCount: LiveData<Int> get() = _totalCount

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    /**
     * 디바이스 목록 조회 (필터 지원)
     */
    fun load(
        power: Boolean? = null,
        type: String? = null,
        roomName: String? = null,
        deviceName: String? = null
    ) {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                RetrofitUtil.deviceService.readDevices(
                    power = power,
                    type = type,
                    roomName = roomName,
                    deviceName = deviceName
                )
            }
                .onSuccess { res ->
                    if (res.isSuccessful) {
                        val body = res.body()
                        _items.value = body?.data?.items ?: emptyList()
                        _totalCount.value = body?.data?.totalCount ?: 0
                        _error.value = null
                    } else {
                        _error.value = "HTTP ${res.code()}"
                    }
                }
                .onFailure { e -> _error.value = e.message }
            _loading.value = false
        }
    }
}
