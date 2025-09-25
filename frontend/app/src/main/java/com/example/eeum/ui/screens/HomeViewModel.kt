package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.device.DeviceIcon
import com.example.eeum.data.model.response.device.DeviceItem
import com.example.eeum.data.model.response.device.DeviceLocation
import com.example.eeum.data.model.response.device.LocationData
import com.example.eeum.data.model.dto.device.DevicesLocation
import com.example.eeum.data.model.dto.device.LocationItem
import com.example.eeum.data.model.response.floorplans.FloorPlan
import com.example.eeum.data.model.response.floorplans.FloorPlansList
import com.example.eeum.data.model.response.home.AllUserHome
import com.example.eeum.data.model.response.home.Home
import com.example.eeum.data.model.response.home.PrimaryHome
import com.example.eeum.data.model.response.home.GetPrimaryHome
import com.example.eeum.data.model.response.routine.AllRoom
import com.example.eeum.data.model.response.routine.RoomData
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _floorplans = MutableLiveData<List<FloorPlansList>>()
    val floorplans: LiveData<List<FloorPlansList>> get() = _floorplans

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

    private val _primaryHomeName = MutableLiveData<String?>()
    val primaryHomeName: LiveData<String?> get() = _primaryHomeName

    private val _primaryMessage = MutableLiveData<String?>()
    val primaryMessage: LiveData<String?> get() = _primaryMessage

    private val _devices = MutableLiveData<List<DeviceItem>>(emptyList())
    val devices: LiveData<List<DeviceItem>> get() = _devices

    private val _deviceTotalCount = MutableLiveData<Int>(0)
    val deviceTotalCount: LiveData<Int> get() = _deviceTotalCount

    // 방 목록 (roomColor -> roomId 매핑용)
    private val _rooms = MutableLiveData<List<RoomData>>(emptyList())
    val rooms: LiveData<List<RoomData>> get() = _rooms

    //디바이스 위치 목록
    private val _deviceLocations = MutableLiveData<List<LocationData>>(emptyList())
    val deviceLocations: LiveData<List<LocationData>> get() = _deviceLocations

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
                        Log.d("HomeViewModel", "홈($homeId) 평면도 조회 성공: ${body.data.items.size}건")
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

    // 방 목록 조회 (roomColor 매핑용)
    fun fetchRooms(homeId: Int) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.homeService.readRooms(homeId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: AllRoom ->
                        _rooms.value = body.data
                        _status.value = body.status
                        _error.value = null
                        Log.d("HomeViewModel", "방 목록 조회 성공: ${body.data.size}건")
                    } ?: run {
                        _rooms.value = emptyList()
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "방 목록 응답 비어있음")
                    }
                } else {
                    _error.value = "방 목록 조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "방 목록 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "방 목록 조회 실패", e)
            }
        }
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

    // 대표 집 조회
    fun fetchPrimaryHome() {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.homeService.getPrimaryHome()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: GetPrimaryHome ->
                        _primaryHomeId.value = body.data.homeId
                        _primaryHomeName.value = body.data.homeName
                        _status.value = body.status
                        _error.value = null
                        // 선택된 집도 대표 집으로 맞춰 둠
                        _selectedHomeId.value = body.data.homeId
                        Log.d(
                            "HomeViewModel",
                            "대표 집 조회 성공: homeId=${body.data.homeId}, name=${body.data.homeName}"
                        )
                        
                        // 대표 집 조회 성공 시 자동으로 평면도와 디바이스 조회
                        fetchUserHomeFloorplans(body.data.homeId)
                        fetchDevicesIcon()
                    } ?: run {
                        _error.value = "대표 집 조회 응답이 비어있습니다."
                        Log.e("HomeViewModel", "대표 집 조회 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "대표 집 조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "대표 집 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "대표 집 조회 실패", e)
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

    // 디바이스 목록 조회
    fun fetchDevicesIcon(
        power: Boolean? = null,
        type: String? = null,
        roomName: String? = null,
        deviceName: String? = null
    ) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.deviceService.readDevicesSimple(
                    power = power,
                    type = type,
                    roomName = roomName,
                    deviceName = deviceName
                )
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val items = body.data.items
                            // UI 안정화를 위해 정렬(방->이름)
                            .sortedWith(compareBy<DeviceItem>({ it.roomId }, { it.deviceName }))

                        _devices.value = items
                        _deviceTotalCount.value = body.data.totalCount
                        _status.value = body.status
                        _error.value = null

                        // 샘플 로그: 좌표/타입까지 확인
                        val sample = items.firstOrNull()
                        Log.d(
                            "HomeViewModel",
                            buildString {
                                append("디바이스 목록 조회 성공: total=${body.data.totalCount}, items=${items.size}")
                                sample?.let {
                                    append(", sample={id=${it.deviceId}, name=${it.deviceName}, room=${it.roomId}, x=${it.x}, y=${it.y}, type=${it.deviceType}}")
                                }
                            }
                        )
                    } ?: run {
                        _devices.value = emptyList()
                        _deviceTotalCount.value = 0
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "디바이스 목록 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "디바이스 목록 조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "디바이스 목록 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "디바이스 목록 조회 실패", e)
            }
        }
    }

    fun clearDevices() {
        _devices.value = emptyList()
        _deviceTotalCount.value = 0
    }

    // 디바이스 위치 조회
    fun fetchDeviceLocations(
        homeId: Int? = null,
        roomId: Int? = null
    ) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.deviceService.readDeviceLocations(
                    homeId = homeId,
                    roomId = roomId
                )
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body: DeviceLocation ->
                        _deviceLocations.value = body.data
                        _status.value = body.status
                        _error.value = null
                        Log.d(
                            "HomeViewModel",
                            "디바이스 위치 조회 성공: count=${body.data.size}, homeId=$homeId, roomId=$roomId"
                        )
                    } ?: run {
                        _deviceLocations.value = emptyList()
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "디바이스 위치 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "디바이스 위치 조회 실패: ${response.code()}"
                    Log.e("HomeViewModel", "디바이스 위치 조회 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "디바이스 위치 조회 실패", e)
            }
        }
    }

    fun clearDeviceLocations() {
        _deviceLocations.value = emptyList()
    }

    // 디바이스 위치 배치 수정
    fun updateDeviceLocations(locations: List<LocationItem>) {
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.deviceService.updateDeviceLocations(
                    DevicesLocation(items = locations)
                )
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _status.value = body.status
                        _error.value = null
                        Log.d(
                            "HomeViewModel",
                            "디바이스 위치 배치 수정 성공: count=${locations.size}"
                        )
                    } ?: run {
                        _error.value = "응답이 비어있습니다."
                        Log.e("HomeViewModel", "디바이스 위치 배치 수정 응답이 비어있습니다.")
                    }
                } else {
                    _error.value = "디바이스 위치 배치 수정 실패: ${response.code()}"
                    Log.e("HomeViewModel", "디바이스 위치 배치 수정 실패 code=${response.code()}")
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("HomeViewModel", "디바이스 위치 배치 수정 실패", e)
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
