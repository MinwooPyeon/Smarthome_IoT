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
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { RetrofitUtil.deviceService.readDevices() }
                .onSuccess { res ->
                    if (res.isSuccessful) {
                        val body = res.body()
                        _items.value = body?.data?.items ?: emptyList()
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