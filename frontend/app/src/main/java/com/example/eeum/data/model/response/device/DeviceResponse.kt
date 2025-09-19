package com.example.eeum.data.model.response.device

import com.google.gson.JsonObject

data class DeviceResponse (
    val deviceId: Int,
    val roomId: Int,
    val remoteId: Int,
    val irDeviceId: String,
    val brand: String,
    val model: String,
    val deviceName: String,
    val type: String,
    val registeredAt: String,
    val deviceDetail: JsonObject
)