package com.example.eeum.ui.screens

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeum.data.model.response.report.AiSummary
import com.example.eeum.data.model.response.report.AiSummaryComparison
import com.example.eeum.data.model.response.report.AiSummaryHighlight
import com.example.eeum.data.model.response.report.AiSummaryStats
import com.example.eeum.data.remote.RetrofitUtil
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {

    // AI 요약 리포트 데이터
    private val _aiSummaryData = MutableLiveData<AiSummary?>(null)
    val aiSummaryData: LiveData<AiSummary?> get() = _aiSummaryData

    // 요약 텍스트
    private val _summary = MutableLiveData<String>("")
    val summary: LiveData<String> get() = _summary

    // 하이라이트 정보
    private val _highlights = MutableLiveData<List<AiSummaryHighlight>>(emptyList())
    val highlights: LiveData<List<AiSummaryHighlight>> get() = _highlights

    // 인사이트 목록
    private val _insights = MutableLiveData<List<String>>(emptyList())
    val insights: LiveData<List<String>> get() = _insights

    // 제안 목록
    private val _suggestions = MutableLiveData<List<String>>(emptyList())
    val suggestions: LiveData<List<String>> get() = _suggestions

    // 통계 정보
    private val _stats = MutableLiveData<AiSummaryStats?>(null)
    val stats: LiveData<AiSummaryStats?> get() = _stats

    // 비교 정보
    private val _comparison = MutableLiveData<AiSummaryComparison?>(null)
    val comparison: LiveData<AiSummaryComparison?> get() = _comparison

    // 생성 시간
    private val _generatedAt = MutableLiveData<String>("")
    val generatedAt: LiveData<String> get() = _generatedAt

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 에러 상태
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> get() = _error

    /**
     * AI 에너지 요약 리포트 조회
     * @param homeId 집 ID
     */
    fun fetchAiEnergyReport(homeId: Int) {
        _isLoading.value = true
        _error.value = null

        Log.d("ReportViewModel", "AI 에너지 리포트 조회 요청 - homeId: $homeId")

        viewModelScope.launch {
            runCatching {
                RetrofitUtil.reportService.getAiEnergyReport(homeId)
            }.onSuccess { response ->
                _isLoading.value = false

                Log.d("ReportViewModel", "API 응답 - code: ${response.code()}, message: ${response.message()}")

                if (response.isSuccessful) {
                    response.body()?.let { aiSummary ->
                        _aiSummaryData.value = aiSummary
                        _summary.value = aiSummary.summary
                        _highlights.value = aiSummary.highlights
                        _insights.value = aiSummary.insights
                        _suggestions.value = aiSummary.suggestions
                        _stats.value = aiSummary.stats
                        _comparison.value = aiSummary.comparison
                        _generatedAt.value = aiSummary.generatedAt

                        Log.d(
                            "ReportViewModel",
                            "AI 에너지 리포트 조회 성공: summary=${aiSummary.summary}, " +
                                    "highlights=${aiSummary.highlights.size}건, " +
                                    "insights=${aiSummary.insights.size}건, " +
                                    "suggestions=${aiSummary.suggestions.size}건"
                        )
                    } ?: run {
                        _error.value = "응답이 비어있습니다."
                        Log.e("ReportViewModel", "AI 에너지 리포트 응답이 비어있습니다.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = "리포트 조회 실패 ${response.code()}"
                    Log.e("ReportViewModel", "AI 에너지 리포트 조회 실패 - code: ${response.code()}, error: $errorBody")
                }
            }.onFailure { e ->
                _isLoading.value = false
                _error.value = "네트워크 오류: ${e.message}"
                Log.e("ReportViewModel", "AI 에너지 리포트 조회 실패", e)
            }
        }
    }

    /**
     * 30일 기간 통계 데이터 가져오기
     */
    fun getPeriod30dStats() = _stats.value?.period_30d

    /**
     * 90일 기간 통계 데이터 가져오기
     */
    fun getPeriod90dStats() = _stats.value?.period_90d

    /**
     * 피크 시간 정보 가져오기
     */
    fun getPeakHour() = _stats.value?.peakHour

    /**
     * 피크 요일 정보 가져오기
     */
    fun getPeakWeekday() = _stats.value?.peakWeekday

    /**
     * 상위 기기 타입 목록 가져오기
     */
    fun getTopDeviceTypes() = _stats.value?.topTypes90d

    /**
     * 이번 달 사용량 비교 정보
     */
    fun getThisMonthUsage() = _comparison.value?.thisMonth

    /**
     * 지난 달 사용량 비교 정보
     */
    fun getLastMonthUsage() = _comparison.value?.lastMonth

    /**
     * 작년 같은 달 사용량 비교 정보
     */
    fun getLastYearSameMonthUsage() = _comparison.value?.lastYearSameMonth

    /**
     * 데이터 초기화
     */
    fun clearData() {
        _aiSummaryData.value = null
        _summary.value = ""
        _highlights.value = emptyList()
        _insights.value = emptyList()
        _suggestions.value = emptyList()
        _stats.value = null
        _comparison.value = null
        _generatedAt.value = ""
        _error.value = null
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
     * 데이터가 있는지 확인
     */
    fun hasData(): Boolean = _aiSummaryData.value != null

    /**
     * 하이라이트 데이터가 있는지 확인
     */
    fun hasHighlights(): Boolean = _highlights.value?.isNotEmpty() == true

    /**
     * 인사이트 데이터가 있는지 확인
     */
    fun hasInsights(): Boolean = _insights.value?.isNotEmpty() == true

    /**
     * 제안 데이터가 있는지 확인
     */
    fun hasSuggestions(): Boolean = _suggestions.value?.isNotEmpty() == true
}