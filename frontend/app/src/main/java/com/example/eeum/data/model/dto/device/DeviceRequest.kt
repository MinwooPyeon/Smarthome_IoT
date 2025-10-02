package com.example.eeum.data.model.dto.device

data class DeviceRequest(
    val homeId: Int,
    val irDeviceId: String,
    val roomColor: String,
    val model: String,
    val brand: String,
    val deviceType: String,
    val floorplansX: Float,
    val floorplansY: Float
)