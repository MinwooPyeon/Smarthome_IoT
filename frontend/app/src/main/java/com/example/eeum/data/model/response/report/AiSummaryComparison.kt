package com.example.eeum.data.model.response.report

data class AiSummaryComparison(
    val lastMonth: AiSummaryLastMonth,
    val lastYearSameMonth: AiSummaryLastYearSameMonth,
    val thisMonth: AiSummaryThisMonth
)