package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.floorplans.HouseItem
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class FloorplansViewModel : ViewModel() {

    private val _houses = MutableLiveData<List<HouseItem>>()
    val houses: LiveData<List<HouseItem>> get() = _houses

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun searchHouses(keyword: String? = null) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.floorplansService.searchHouses(keyword)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _houses.value = body.data.items
                        _status.value = body.status
                        _error.value = null
                        Log.d("FloorplansViewModel", "검색 성공: ${body.data.items.size}건")
                    }
                } else {
                    _error.value = "검색 실패: ${response.code()}"
                    Log.e("FloorplansViewModel", "검색 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("FloorplansViewModel", "검색 실패", e)
            }
        }
    }
}
