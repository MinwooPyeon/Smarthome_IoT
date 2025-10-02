package com.example.eeum.data.model.response.device

data class EnergyDeviceUsageData(
    val from: String,
    val items: List<EnergyDeviceUsageDataList>,
    val range: String,
    val to: String,
    val totalKwh: Double
)