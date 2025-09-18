package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.routine.IconData
import com.example.eeum.data.model.response.routine.RoutineData
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class RoutineViewModel : ViewModel() {

    private val _routines = MutableLiveData<List<RoutineData>>(emptyList())
    val routines: LiveData<List<RoutineData>> = _routines

    private val _icons = MutableLiveData<List<IconData>>(emptyList())
    val icons: LiveData<List<IconData>> = _icons

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun fetchAllRoutines() {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.readAllRoutines() }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        _routines.value = body?.data ?: emptyList()
                        Log.d("RoutineViewModel", "fetchAllRoutines size=${_routines.value?.size}")
                    } else {
                        val msg = "readAllRoutines failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readAllRoutines exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "readAllRoutines exception", e)
                }
        }
    }

    fun fetchRoutineIcons() {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.readRoutineIcons() }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        _icons.value = body?.data ?: emptyList()
                        Log.d("RoutineViewModel", "fetchRoutineIcons size=${_icons.value?.size}")
                    } else {
                        val msg = "readRoutineIcons failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readRoutineIcons exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "readRoutineIcons exception", e)
                }
        }
    }
}
