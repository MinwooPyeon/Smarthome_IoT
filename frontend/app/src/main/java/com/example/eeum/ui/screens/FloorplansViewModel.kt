package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.floorplans.FloorPlansList
import com.example.eeum.data.model.response.floorplans.HouseItem
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class FloorplansViewModel : ViewModel() {

    // 주소 검색 결과
    private val _houses = MutableLiveData<List<HouseItem>>()
    val houses: LiveData<List<HouseItem>> get() = _houses

    //주소별 평면도 목록
    private val _floorplans = MutableLiveData<List<FloorPlansList>>()
    val floorplans: LiveData<List<FloorPlansList>> get() = _floorplans

    // 공통 상태/에러
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    //주소 검색
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
                    } ?: run {
                        _error.value = "검색 실패: 빈 응답"
                        Log.e("FloorplansViewModel", "검색 실패: 빈 응답")
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

    //주소별 평면도 목록 조회
    fun getFloorplans(addressId: Int) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.floorplansService.getFloorplans(addressId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _floorplans.value = body.data.items
                        _status.value = body.status
                        _error.value = null
                        Log.d("FloorplansViewModel",
                            "평면도 조회 성공: addressId=$addressId, ${body.data.items.size}건")
                    } ?: run {
                        _error.value = "평면도 조회 실패: 빈 응답"
                        Log.e("FloorplansViewModel", "평면도 조회 실패: 빈 응답")
                    }
                } else {
                    _error.value = "평면도 조회 실패: ${response.code()}"
                    Log.e("FloorplansViewModel", "평면도 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("FloorplansViewModel", "평면도 조회 실패", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
