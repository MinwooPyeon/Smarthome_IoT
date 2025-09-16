package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.home.AllUserHome
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _homes = MutableLiveData<List<Home>>()
    val homes: LiveData<List<Home>> get() = _homes

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

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

    /** 에러 초기화 */
    fun clearError() {
        _error.value = null
    }
}
