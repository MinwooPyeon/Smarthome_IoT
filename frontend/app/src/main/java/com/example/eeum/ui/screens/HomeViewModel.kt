package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.floorplans.FloorPlan
import com.example.eeum.data.model.response.floorplans.FloorPlansList
import com.example.eeum.data.model.response.home.AllUserHome
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.data.model.response.home.PrimaryHome
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _floorplans = MutableLiveData<List<FloorPlansList>>()
    val floorplans: LiveData<List<FloorPlansList>> get() = _floorplans

    // (선택) 현재 선택된 홈 ID
    private val _selectedHomeId = MutableLiveData<Int?>()
    val selectedHomeId: LiveData<Int?> get() = _selectedHomeId

    private val _homes = MutableLiveData<List<Home>>()
    val homes: LiveData<List<Home>> get() = _homes

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _primaryHomeId = MutableLiveData<Int?>()
    val primaryHomeId: LiveData<Int?> get() = _primaryHomeId

    private val _primaryMessage = MutableLiveData<String?>()
    val primaryMessage: LiveData<String?> get() = _primaryMessage

    //특정 집 평면도 조회
    fun fetchUserHomeFloorplans(homeId: Int) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.floorplansService.getUserHomeFloorplans(homeId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: FloorPlan ->
                        _floorplans.value = body.data.items
                        _status.value = body.status
                        _error.value = null
                        Log.d(
                            "HomeViewModel",
                            "홈($homeId) 평면도 조회 성공: ${body.data.items.size}건"
                        )
                    } ?: run {
                        _floorplans.value = emptyList()
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "홈($homeId) 평면도 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "평면도 조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "평면도 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "평면도 조회 실패", e)
            }
        }
    }

    //홈 선택 시 자동 조회
    fun selectHome(homeId: Int) {
        if (_selectedHomeId.value != homeId) {
            _selectedHomeId.value = homeId
            fetchUserHomeFloorplans(homeId)
        }
    }

    //평면도 초기화
    fun clearFloorplans() {
        _floorplans.value = emptyList()
    }

    //유저 집 목록 조회
    fun fetchUserHomes() {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.homeService.getUserHomes()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: AllUserHome ->
                        _homes.value = body.data.homes
                        _status.value = body.status
                        _error.value = null
                        Log.d("HomeViewModel", "유저 집 목록 조회 성공: ${body.data.homes}")
                    } ?: run {
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "유저 집 목록 조회 실패", e)
            }
        }
    }

    // 대표 집 주소 변경
    fun setPrimaryHome(homeId: Int) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.homeService.setPrimaryHome(homeId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: PrimaryHome ->
                        _primaryHomeId.value = body.data.homeId
                        _primaryMessage.value = body.data.massage // 서버 스펙 그대로 사용
                        _status.value = body.status
                        _error.value = null
                        _selectedHomeId.value = body.data.homeId
                        Log.d(
                            "HomeViewModel",
                            "대표 집 변경 성공: homeId=${body.data.homeId}, message=${body.data.massage}"
                        )
                        // 최신 상태 반영을 위해 집 목록 재조회
                        fetchUserHomes()
                    } ?: run {
                        _error.value = "대표 집 변경 응답이 비어있습니다."
                        Log.e("HomeViewModel", "대표 집 변경 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "대표 집 변경 실패: ${response.code()}"
                    Log.e("HomeViewModel", "대표 집 변경 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "대표 집 변경 실패", e)
            }
        }
    }

    //에러 초기화
    fun clearError() {
        _error.value = null
    }

    fun clearPrimaryMessage() {
        _primaryMessage.value = null
    }
}
