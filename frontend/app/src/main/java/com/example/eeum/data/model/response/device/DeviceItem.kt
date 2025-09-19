package com.example.eeum.data.model.response.device

data class DeviceItem(
    val brand: String,
    val deviceDetail: DevicePwTemp,
    val deviceId: Int,
    val deviceName: String,
    val deviceType: Any,
    val irDeviceId: String,
    val model: String,
    val registeredAt: String,
    val roomId: Int,
    val x: Double,
    val y: Double,
)