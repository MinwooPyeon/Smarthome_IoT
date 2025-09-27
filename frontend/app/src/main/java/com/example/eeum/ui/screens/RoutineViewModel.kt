package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.routine.RoutineRequestDto
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.device.DeviceResponse
import com.example.eeum.data.model.response.routine.IconData
import com.example.eeum.data.model.response.routine.RoomData
import com.example.eeum.data.model.response.routine.RoutineData
import com.example.eeum.data.model.response.routine.RoutineResult
import com.example.eeum.data.remote.RetrofitUtil
import com.example.eeum.data.model.response.routine.RoutineResponse
import com.example.eeum.data.model.response.routine.DataInRoutine
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

    private val _deleteMessage = MutableLiveData<String?>(null)
    val deleteMessage: LiveData<String?> = _deleteMessage

    private val _routineDetail = MutableLiveData<RoutineResponse?>(null)
    val routineDetail: LiveData<RoutineResponse?> = _routineDetail

    private val _routineDetailV2 = MutableLiveData<DataInRoutine?>(null)
    val routineDetailV2: LiveData<DataInRoutine?> = _routineDetailV2
    
    // 개별 디바이스 정보를 저장하기 위한 Map
    private val _deviceInfoMap = MutableLiveData<Map<Int, DeviceResponse>>(emptyMap())
    val deviceInfoMap: LiveData<Map<Int, DeviceResponse>> = _deviceInfoMap
    
    // 방 정보를 저장하기 위한 Map (roomId -> roomName)
    private val _roomInfoMap = MutableLiveData<Map<Int, String>>(emptyMap())
    val roomInfoMap: LiveData<Map<Int, String>> = _roomInfoMap

    fun fetchAllRoutines() {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.readAllRoutines() }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val routineList = body?.data ?: emptyList()
                        _routines.value = routineList
                        
                        // 각 루틴의 isAi 값 로그
                        routineList.forEach { routine ->
                            Log.d("RoutineViewModel", "Routine ${routine.routineId}: name=${routine.name}, isAi=${routine.isAi}")
                        }
                        
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
                        _devices.value = items.sortedWith(compareBy({ it.deviceName }, { it.deviceType?.toString() ?: "" }))
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

    // 모든 방의 디바이스 목록 조회 (roomName 필터 없이)
    fun fetchDevicesAll() {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.deviceService.readDevicesSimple(
                    power = null,
                    type = null,
                    roomName = null,
                    deviceName = null
                )
            }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val items = body?.data?.items ?: emptyList()
                        _devices.value = items.sortedWith(compareBy({ it.deviceName }, { it.deviceType?.toString() ?: "" }))
                        Log.d("RoutineViewModel", "fetchDevicesAll size=${_devices.value?.size}")
                    } else {
                        val msg = "readDevicesSimple(all) failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readDevicesSimple(all) exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "readDevicesSimple(all) exception", e)
                }
        }
    }

    // 루틴 수정
    fun updateRoutine(routineId: Int, body: RoutineRequestDto) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.updateRoutine(routineId, body) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val result = response.body()
                        _createResult.value = result
                        Log.d("RoutineViewModel", "updateRoutine success: routineId=$routineId")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val msg = "updateRoutine failed: code=${response.code()} msg=${response.message()}"
                        _error.value = "$msg\nError: $errorBody"
                        Log.e("RoutineViewModel", msg)
                        Log.e("RoutineViewModel", "Error body: $errorBody")
                    }
                }
                .onFailure { e ->
                    val msg = "updateRoutine exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "updateRoutine exception", e)
                }
        }
    }

    // 루틴 삭제
    fun removeRoutine(routineId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.removeRoutine(routineId) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val message = body?.data?.message ?: "루틴이 삭제되었어요."
                        _deleteMessage.value = message
                        Log.d("RoutineViewModel", "removeRoutine success: id=$routineId, msg=$message")

                        // 현재 메모리 목록에서도 제거 (또는 fetchAllRoutines() 재조회로 대체 가능)
                        _routines.value = _routines.value?.filterNot { it.routineId == routineId }
                    } else {
                        val msg = "removeRoutine failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "removeRoutine exception: ${e.message}"
                    _error.value = msg
                    Log.e("RoutineViewModel", "removeRoutine exception", e)
                }
        }
    }
    //루틴 단건 조회
    fun fetchRoutine(routineId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.readRoutine(routineId) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val data = body?.data
                        _routineDetail.value = data
                        Log.d("RoutineViewModel", "fetchRoutine success: id=$routineId, name=${data?.name}")
                    } else {
                        val msg = "readRoutine failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        _routineDetail.value = null
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readRoutine exception: ${e.message}"
                    _error.value = msg
                    _routineDetail.value = null
                    Log.e("RoutineViewModel", "readRoutine exception", e)
                }
        }
    }

    fun fetchRoutineDetail(routineId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.routineService.readRoutineDetail(routineId) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        _routineDetailV2.value = data
                        Log.d(
                            "RoutineViewModel",
                            "fetchRoutineDetail success: id=$routineId, name=${data?.name}, details=${data?.details?.size}"
                        )
                    } else {
                        val msg = "readRoutineDetail failed: code=${response.code()} msg=${response.message()}"
                        _error.value = msg
                        _routineDetailV2.value = null
                        Log.e("RoutineViewModel", msg)
                    }
                }
                .onFailure { e ->
                    val msg = "readRoutineDetail exception: ${e.message}"
                    _error.value = msg
                    _routineDetailV2.value = null
                    Log.e("RoutineViewModel", "readRoutineDetail exception", e)
                }
        }
    }

    // 새 상세 초기화
    fun clearRoutineDetailV2() { _routineDetailV2.value = null }
    
    // 개별 디바이스 정보 조회
    fun fetchDeviceInfo(deviceId: Int) {
        viewModelScope.launch {
            runCatching { RetrofitUtil.deviceService.readDevice(deviceId) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        val deviceInfo = response.body()?.data
                        deviceInfo?.let {
                            val currentDeviceMap = _deviceInfoMap.value ?: emptyMap()
                            _deviceInfoMap.value = currentDeviceMap + (deviceId to it)
                            
                            // 방 정보도 함께 저장 (실제로는 방 API를 따로 호출해야 하지만 임시로 처리)
                            val currentRoomMap = _roomInfoMap.value ?: emptyMap()
                            if (!currentRoomMap.containsKey(it.roomId)) {
                                _roomInfoMap.value = currentRoomMap + (it.roomId to "방 ${it.roomId}")
                            }
                            
                            Log.d("RoutineViewModel", "fetchDeviceInfo success: deviceId=$deviceId, name=${it.deviceName}, roomId=${it.roomId}")
                        }
                    } else {
                        Log.e("RoutineViewModel", "fetchDeviceInfo failed: deviceId=$deviceId, code=${response.code()}")
                    }
                }
                .onFailure { e ->
                    Log.e("RoutineViewModel", "fetchDeviceInfo exception: deviceId=$deviceId", e)
                }
        }
    }
    
    // 디바이스 정보 초기화
    fun clearDeviceInfoMap() {
        _deviceInfoMap.value = emptyMap()
        _roomInfoMap.value = emptyMap()
    }

    // 디바이스 목록 초기화
    fun clearDevices() {
        _devices.value = emptyList()
    }
    
    // 메시지 초기화 함수들
    fun clearDeleteMessage() {
        _deleteMessage.value = null
    }
    
    fun clearError() {
        _error.value = null
    }

}
