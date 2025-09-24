package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.device.EnergyTotalUsage
import com.example.eeum.data.model.response.device.EnergyTotalUsageData
import com.example.eeum.data.model.response.device.EnergyTotalUsageDataList
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class EnergyViewModel : ViewModel() {

    // 전체 에너지 사용량 데이터
    private val _energyUsageData = MutableLiveData<EnergyTotalUsageData?>()
    val energyUsageData: LiveData<EnergyTotalUsageData?> get() = _energyUsageData

    // 시리즈 데이터 (차트용)
    private val _energySeries = MutableLiveData<List<EnergyTotalUsageDataList>>(emptyList())
    val energySeries: LiveData<List<EnergyTotalUsageDataList>> get() = _energySeries

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
     * 전력 사용량 데이터 조회
     * @param homeId 집 ID
     * @param range 조회 범위 ("day", "week", "month", "year")
     * @param date 날짜 (yyyy-MM-dd 형식)
     */
    fun fetchEnergyTotalUsage(
        homeId: Int,
        range: String,
        date: String
    ) {
        _selectedRange.value = range
        _isLoading.value = true
        
        // 요청 파라미터 로그
        Log.d("EnergyViewModel", "API 요청 - homeId: $homeId, range: $range, date: $date")
        Log.d("EnergyViewModel", "Server URL: ${com.example.eeum.base.ApplicationClass.SERVER_URL}")
        
        viewModelScope.launch {
            runCatching {
                RetrofitUtil.energyService.getEnergyTotalUsage(
                    homeId = homeId,
                    range = range,
                    date = date
                )
            }.onSuccess { response ->
                _isLoading.value = false
                
                Log.d("EnergyViewModel", "API 응답 - code: ${response.code()}, message: ${response.message()}")
                
                if (response.isSuccessful) {
                    response.body()?.let { body: EnergyTotalUsage ->
                        _energyUsageData.value = body.data
                        _energySeries.value = body.data.series
                        _totalKwh.value = body.data.totalKwh
                        _status.value = body.status
                        _error.value = null
                        
                        Log.d(
                            "EnergyViewModel",
                            "전력 사용량 조회 성공: range=$range, totalKwh=${body.data.totalKwh}, " +
                                    "series=${body.data.series.size}건, from=${body.data.from}, to=${body.data.to}"
                        )
                    } ?: run {
                        _error.value = "응답이 비어있습니다."
                        Log.e("EnergyViewModel", "전력 사용량 응답이 비어있습니다.")
                    }
                } else {
                    // 에러 응답 본문도 로그에 출력
                    val errorBody = response.errorBody()?.string()
                    _error.value = "전력 사용량 조회 실패 ${response.code()}"
                    Log.e("EnergyViewModel", "전력 사용량 조회 실패 - code: ${response.code()}, error: $errorBody")
                }
            }.onFailure { e ->
                _isLoading.value = false
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("EnergyViewModel", "전력 사용량 조회 실패", e)
            }
        }
    }

    /**
     * 기간별 전력 사용량 조회 (한국어 → 영어 변환)
     * @param koreanPeriod 한국어 기간 ("일간", "주간", "월간", "연간")
     * @param homeId 집 ID (기본값: 1)
     */
    fun fetchEnergyUsageByPeriod(koreanPeriod: String, homeId: Int = 1) {
        val range = when (koreanPeriod) {
            "일간" -> "day"
            "주간" -> "week"
            "월간" -> "month" 
            "연간" -> "year"
            else -> "day"
        }
        
        // yyyy-MM-dd 형식으로 날짜 생성
        val now = org.threeten.bp.LocalDateTime.now()
        val dateFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
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
        
        Log.d("EnergyViewModel", "fetchEnergyUsageByPeriod - koreanPeriod: $koreanPeriod, range: $range, date: $date, homeId: $homeId")
        
        fetchEnergyTotalUsage(homeId, range, date)
    }

    /**
     * 차트용 데이터 변환
     */
    fun getChartData(): List<Pair<String, Float>> {
        return _energySeries.value?.map { item ->
            item.label to item.kwh.toFloat()
        } ?: emptyList()
    }


    /**
     * 데이터 초기화
     */
    fun clearData() {
        _energyUsageData.value = null
        _energySeries.value = emptyList()
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
    fun hasData(): Boolean = _energySeries.value?.isNotEmpty() == true
}