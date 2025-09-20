package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.routine.RoutineRequestDto
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.routine.IconData
import com.example.eeum.data.model.response.routine.RoomData
import com.example.eeum.data.model.response.routine.RoutineData
import com.example.eeum.data.model.response.routine.RoutineResult
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class RoutineViewModel : ViewModel() {

    private val _routines = MutableLiveData<List<RoutineData>>(emptyList())
    val routines: LiveData<List<RoutineData>> = _routines

    private val _icons = MutableLiveData<List<IconData>>(emptyList())
    val icons: LiveData<List<IconData>> = _icons

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _createdRoutineId = MutableLiveData<Int?>(null)
    val createdRoutineId: LiveData<Int?> = _createdRoutineId

    private val _createResult = MutableLiveData<RoutineResult?>()
    val createResult: LiveData<RoutineResult?> = _createResult

    private val _rooms = MutableLiveData<List<RoomData>>(emptyList())
    val rooms: LiveData<List<RoomData>> = _rooms

    private val _devices = MutableLiveData<List<DeviceItem>>(emptyList())
    val devices: LiveData<List<DeviceItem>> = _devices

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

    // 루틴 생성
    fun generateRoutine(body: RoutineRequestDto) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.generateRoutine(body) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val result = response.body()
                        _createResult.value = result
                        val id = result?.data?.routineId
                        _createdRoutineId.value = id
                        Log.d("RoutineViewModel", "generateRoutine success: routineId=$id")
                    } else {
                        val msg = "generateRoutine failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "generateRoutine exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "generateRoutine exception", e)
                }
        }
    }

    // 홈의 방 목록 조회
    fun fetchRooms(homeId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.homeService.readRooms(homeId) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        _rooms.value = body?.data ?: emptyList()
                        Log.d("RoutineViewModel", "fetchRooms(homeId=$homeId) size=${_rooms.value?.size}")
                    } else {
                        val msg = "readRooms failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readRooms exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "readRooms exception", e)
                }
        }
    }

    // 특정 방의 디바이스 목록 조회 (roomName으로 필터링)
    fun fetchDevicesSimple(roomName: String) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.deviceService.readDevicesSimple(
                    power = null,
                    type = null,
                    roomName = roomName,
                    deviceName = null
                )
            }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val items = body?.data?.items ?: emptyList()
                        _devices.value = items.sortedWith(compareBy({ it.deviceName }, { it.deviceType.toString() }))
                        Log.d("RoutineViewModel", "fetchDevicesSimple(roomName=$roomName) size=${_devices.value?.size}")
                    } else {
                        val msg = "readDevicesSimple failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readDevicesSimple exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "readDevicesSimple exception", e)
                }
        }
    }

    // 디바이스 목록 초기화
    fun clearDevices() {
        _devices.value = emptyList()
    }

}
