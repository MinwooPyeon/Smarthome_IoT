package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.dto.device.DeviceRequest
import com.example.eeum.data.model.response.common.BaseResponse
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class DeviceRegistrationViewModel : ViewModel() {

    data class Draft(
        var kind: String? = null,        // deviceType
        var serial: String? = null,      // irDeviceId (시리얼번호)
        var brand: String? = null,
        var model: String? = null,
        var posX: Float? = null,         // floorplansX
        var posY: Float? = null,         // floorplansY 
        var colorHex: String? = null,    // roomColor
        var homeId: Int? = null
    )

    private val _draft = MutableLiveData(Draft())
    val draft: LiveData<Draft> get() = _draft

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> get() = _loading

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _result = MutableLiveData<Boolean?>()
    val result: LiveData<Boolean?> get() = _result

    fun resetDraft() {
        _draft.value = Draft()
        _status.value = ""
        _result.value = null
        _error.value = null
        _loading.value = false
    }

    fun setKind(kind: String) { update { it.kind = kind } }
    fun setSerial(serial: String) { update { it.serial = serial } }
    fun setBrandModel(brand: String, model: String) { update { it.brand = brand; it.model = model } }
    fun setHomeId(id: Int) { update { it.homeId = id } }
    fun setPositionAndColor(x: Float, y: Float, color: androidx.compose.ui.graphics.Color?) {
        val hex = color?.let {
            val r = (it.red   * 255f).toInt().coerceIn(0, 255)
            val g = (it.green * 255f).toInt().coerceIn(0, 255)
            val b = (it.blue  * 255f).toInt().coerceIn(0, 255)
            // 서버는 #RRGGBB 6자리만 허용
            String.format("#%02X%02X%02X", r, g, b)
        }
        update {
            it.posX = x; it.posY = y; it.colorHex = hex
        }
        Log.d("DeviceRegistrationVM", "pos=($x,$y), color=$hex")
    }
    
    // 평면도 상에서 선택한 픽셀 좌표를 0.0~1.0 정규화된 좌표로 변환
    fun setNormalizedPositionAndColor(pixelX: Float, pixelY: Float, imageWidth: Int, imageHeight: Int, color: androidx.compose.ui.graphics.Color?) {
        val normalizedX = if (imageWidth > 0) (pixelX / imageWidth.toFloat()).coerceIn(0.0f, 1.0f) else 0.5f
        val normalizedY = if (imageHeight > 0) (pixelY / imageHeight.toFloat()).coerceIn(0.0f, 1.0f) else 0.5f
        setPositionAndColor(normalizedX, normalizedY, color)
        Log.d("DeviceRegistrationVM", "normalized pos=($normalizedX,$normalizedY) from pixel=($pixelX,$pixelY) imageSize=($imageWidth,$imageHeight)")
    }

    private inline fun update(block: (Draft) -> Unit) {
        val d = _draft.value ?: Draft()
        block(d)
        _draft.value = d
    }

    private fun buildRequest(): DeviceRequest {
        val d = _draft.value ?: Draft()
        val homeId = d.homeId ?: 0
        val irDeviceId = d.serial ?: ""
        // 서버가 요구하는 색상 포맷: #RRGGBB 또는 RRGGBB
        val roomColor = d.colorHex?.let { v ->
            val s = v.trim()
            when {
                s.startsWith("#") && s.length == 9 -> "#" + s.substring(3) // #AARRGGBB -> #RRGGBB
                s.startsWith("#") && s.length == 7 -> s
                s.length == 6 -> "#$s"
                else -> "#FFFFFF"
            }
        } ?: "#FFFFFF"
        val model = (d.model ?: "UNKNOWN").ifBlank { "UNKNOWN" }
        val brand = (d.brand ?: "UNKNOWN").ifBlank { "UNKNOWN" }
        // 영어 디바이스 타입을 한국어로 변환하여 서버에 전송
        val deviceType = convertDeviceTypeToKorean(d.kind ?: "UNKNOWN")
        val floorplansX = d.posX ?: 0.5f
        val floorplansY = d.posY ?: 0.5f
        
        val request = DeviceRequest(
            homeId = homeId,
            irDeviceId = irDeviceId,
            roomColor = roomColor,
            model = model,
            brand = brand,
            deviceType = deviceType,
            floorplansX = floorplansX,
            floorplansY = floorplansY
        )
        
        Log.d("DeviceRegistrationVM", "buildRequest: homeId=$homeId, irDeviceId=$irDeviceId, roomColor=$roomColor, model=$model, brand=$brand, deviceType=$deviceType, pos=($floorplansX, $floorplansY)")
        
        return request
    }

    /**
     * 디바이스 등록 API 호출
     */
    fun registerDevice(request: DeviceRequest) {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                RetrofitUtil.deviceService.createDevice(request)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _result.value = body.data
                        _status.value = body.status
                        _error.value = null
                        Log.d(
                            "DeviceRegistrationVM",
                            "등록 성공: status=${body.status}, data=${body.data}"
                        )
                    } ?: run {
                        _error.value = "등록 실패: 빈 응답"
                        Log.e("DeviceRegistrationVM", "등록 실패: 빈 응답")
                    }
                } else {
                    _error.value = "등록 실패: ${response.code()}"
                    Log.e(
                        "DeviceRegistrationVM",
                        "등록 실패 code=${response.code()} message=${response.message()}"
                    )
                }
            }.onFailure { e ->
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("DeviceRegistrationVM", "등록 실패", e)
            }
            _loading.value = false
        }
    }

    fun registerCurrentDraft() {
        Log.d("DeviceRegistrationVM", "registerCurrentDraft called")
        val req = buildRequest()
        registerDevice(req)
    }

    fun clearError() {
        _error.value = null
    }
    
    /**
     * 영어 디바이스 타입을 한국어로 변환
     */
    private fun convertDeviceTypeToKorean(deviceType: String): String {
        return when (deviceType.uppercase()) {
            "HUB" -> "허브"
            "AIR_CONDITIONER" -> "에어컨"
            "FAN" -> "선풍기"
            "TV" -> "텔레비전"
            "BEAM_PROJECTOR" -> "빔프로젝터"
            "AIR_PURIFIER" -> "공기청정기"
            "LIGHT" -> "조명"
            else -> deviceType
        }
    }
}
