package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.device.EnergyDeviceUsage
import com.example.eeum.data.model.response.device.EnergyDeviceUsageData
import com.example.eeum.data.model.response.device.EnergyDeviceUsageDataList
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class EnergyDeviceViewModel : ViewModel() {

    // 디바이스별 에너지 사용량 데이터
    private val _deviceUsageData = MutableLiveData<EnergyDeviceUsageData?>()
    val deviceUsageData: LiveData<EnergyDeviceUsageData?> get() = _deviceUsageData

    // 디바이스 사용량 리스트 (파이차트용)
    private val _deviceItems = MutableLiveData<List<EnergyDeviceUsageDataList>>(emptyList())
    val deviceItems: LiveData<List<EnergyDeviceUsageDataList>> get() = _deviceItems

    // 총 사용량
    private val _totalKwh = MutableLiveData<Double>(0.0)
    val totalKwh: LiveData<Double> get() = _totalKwh

    // 현재 선택된 기간
    private val _selectedRange = MutableLiveData<String?>("day")
    val selectedRange: LiveData<String?> get() = _selectedRange

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 에러 상태
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // 상태 메시지
    private val _status = MutableLiveData<String?>()
    val status: LiveData<String?> get() = _status

    /**
     * 디바이스별 에너지 사용량 데이터 조회
     * @param homeId 집 ID
     * @param range 조회 범위 ("day", "week", "month", "year")
     * @param date 날짜 (yyyy-MM-dd 형식)
     */
    fun fetchDeviceEnergyUsage(
        homeId: Int,
        range: String,
        date: String
    ) {
        _selectedRange.value = range
        _isLoading.value = true
        
        // 요청 파라미터 로그
        Log.d("EnergyDeviceViewModel", "API 요청 - homeId: $homeId, range: $range, date: $date")
        
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.energyService.getEnergyDeviceUsage(
                    homeId = homeId,
                    range = range,
                    date = date
                )
            }.onSuccess { response ->
                _isLoading.value = false
                
                Log.d("EnergyDeviceViewModel", "API 응답 - code: ${response.code()}, message: ${response.message()}")
                
                if (response.isSuccessful) {
                    response.body()?.let { body: EnergyDeviceUsage ->
                        _deviceUsageData.value = body.data
                        _deviceItems.value = body.data.items
                        _totalKwh.value = body.data.totalKwh
                        _status.value = body.status
                        _error.value = null
                        
                        Log.d(
                            "EnergyDeviceViewModel",
                            "디바이스별 에너지 사용량 조회 성공: range=$range, totalKwh=${body.data.totalKwh}, " +
                                    "items=${body.data.items.size}건, from=${body.data.from}, to=${body.data.to}"
                        )
                    } ?: run {
                        _error.value = "응답이 비어있습니다."
                        Log.e("EnergyDeviceViewModel", "디바이스별 에너지 사용량 응답이 비어있습니다.")
                    }
                } else {
                    // 에러 응답 본문도 로그에 출력
                    val errorBody = response.errorBody()?.string()
                    _error.value = "디바이스별 에너지 사용량 조회 실패 ${response.code()}"
                    Log.e("EnergyDeviceViewModel", "디바이스별 에너지 사용량 조회 실패 - code: ${response.code()}, error: $errorBody")
                }
            }.onFailure { e ->
                _isLoading.value = false
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("EnergyDeviceViewModel", "디바이스별 에너지 사용량 조회 실패", e)
            }
        }
    }

    /**
     * 기간별 디바이스 에너지 사용량 조회 (한국어 → 영어 변환)
     * @param koreanPeriod 한국어 기간 ("일간", "주간", "월간", "연간")
     * @param homeId 집 ID (기본값: 1)
     */
    fun fetchDeviceEnergyUsageByPeriod(koreanPeriod: String, homeId: Int = 1) {
        val range = when (koreanPeriod) {
            "일간" -> "day"
            "주간" -> "week"
            "월간" -> "month" 
            "연간" -> "year"
            else -> "day"
        }
        
        // yyyy-MM-dd 형식으로 날짜 생성
        val now = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        val date = when (range) {
            "day" -> {
                // 오늘 날짜
                now.format(dateFormatter)
            }
            "week" -> {
                // 주간 데이터는 오늘 날짜 기준
                now.format(dateFormatter)
            }
            "month" -> {
                // 월간 데이터는 오늘 날짜 기준
                now.format(dateFormatter)
            }
            "year" -> {
                // 연간 데이터는 오늘 날짜 기준
                now.format(dateFormatter)
            }
            else -> now.format(dateFormatter)
        }
        
        Log.d("EnergyDeviceViewModel", "fetchDeviceEnergyUsageByPeriod - koreanPeriod: $koreanPeriod, range: $range, date: $date, homeId: $homeId")
        
        fetchDeviceEnergyUsage(homeId, range, date)
    }

    /**
     * 파이차트용 데이터 변환
     */
    fun getPieChartData(): List<Triple<String, Float, Double>> {
        return _deviceItems.value?.map { item ->
            Triple(item.deviceType, item.kwh.toFloat(), item.percentage)
        } ?: emptyList()
    }

    /**
     * 현재 날짜 기준으로 기간별 조회
     */
    fun fetchCurrentPeriodDeviceData(koreanPeriod: String, homeId: Int = 1) {
        fetchDeviceEnergyUsageByPeriod(koreanPeriod, homeId)
    }

    /**
     * 데이터 초기화
     */
    fun clearData() {
        _deviceUsageData.value = null
        _deviceItems.value = emptyList()
        _totalKwh.value = 0.0
        _error.value = null
        _status.value = null
        _selectedRange.value = "day"
    }

    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 로딩 상태 확인
     */
    fun isDataLoading(): Boolean = _isLoading.value == true

    /**
     * 데이터가 비어있는지 확인
     */
    fun hasData(): Boolean = _deviceItems.value?.isNotEmpty() == true

    /**
     * 특정 디바이스 타입의 데이터 가져오기
     */
    fun getDeviceDataByType(deviceType: String): EnergyDeviceUsageDataList? {
        return _deviceItems.value?.find { it.deviceType == deviceType }
    }

    /**
     * 사용량 순으로 정렬된 디바이스 목록
     */
    fun getDevicesSortedByUsage(): List<EnergyDeviceUsageDataList> {
        return _deviceItems.value?.sortedByDescending { it.kwh } ?: emptyList()
    }
}