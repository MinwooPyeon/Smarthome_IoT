package com.example.eeum.data.model.response.report

data class AiSummaryStats(
    val peakHour: Int,
    val peakWeekday: Int,
    val period_30d: AiSummaryPeriod30d,
    val period_90d: AiSummaryPeriod90d,
    val topTypes90d: List<String>
)