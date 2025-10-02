package com.example.eeum.data.model.response.device

data class LocationData(
    val deviceId: Int,
    val homeId: Int,
    val positionId: Int,
    val roomId: Int,
    val x: Double,
    val y: Double
)