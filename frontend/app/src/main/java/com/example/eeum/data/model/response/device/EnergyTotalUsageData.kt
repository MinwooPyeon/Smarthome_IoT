package com.example.eeum.data.model.response.device

data class EnergyTotalUsageData(
    val from: String,
    val range: String,
    val series: List<EnergyTotalUsageDataList>,
    val to: String,
    val totalKwh: Double
)