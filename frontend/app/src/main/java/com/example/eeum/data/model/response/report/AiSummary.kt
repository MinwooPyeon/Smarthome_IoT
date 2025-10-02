package com.example.eeum.data.model.response.report

data class AiSummary(
    val comparison: AiSummaryComparison,
    val generatedAt: String,
    val highlights: List<AiSummaryHighlight>,
    val insights: List<String>,
    val stats: AiSummaryStats,
    val suggestions: List<String>,
    val summary: String
)