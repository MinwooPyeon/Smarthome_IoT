package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.menu.UserResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuViewModel : ViewModel() {
    
    private val _userInfo = MutableLiveData<UserResponse?>()
    val userInfo: MutableLiveData<UserResponse?> get() = _userInfo
    
    fun getUserInfo() {
        viewModelScope.launch {
            runCatching { 
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.userService.getUserInfo()
                    response.execute()
                }
            }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        _userInfo.value = userResponse
                        Log.d("MenuViewModel", "getUserInfo success: ${userResponse}")
                        Log.d("MenuViewModel", "User img field: '${userResponse?.data?.img}'")
                    } else {
                        Log.e("MenuViewModel", "getUserInfo failed with code: ${response.code()}, message: ${response.message()}")
                    }
                }
                .onFailure { e ->
                    Log.e("MenuViewModel", "getUserInfo failed", e)
                }
        }
    }
}
