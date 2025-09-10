package com.example.eeum.data.model.dto.device

data class DeviceRequest(
    val roomId: Int,
    val irDeviceId: Int,
    val deviceConsumption: Int,
    val deviceName: String,
    val type: String,
    val brand: String,
    val model: String
)