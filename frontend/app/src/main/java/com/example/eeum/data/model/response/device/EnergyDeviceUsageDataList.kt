package com.example.eeum.data.model.response.device

data class EnergyDeviceUsageDataList(
    val deviceType: String,
    val kwh: Double,
    val percentage: Double
)
