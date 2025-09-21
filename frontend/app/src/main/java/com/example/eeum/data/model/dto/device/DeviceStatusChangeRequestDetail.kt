package com.example.eeum.data.model.dto.device

data class DeviceStatusChangeRequestDetail(
    val level: Int,
    val power: Boolean,
    val temperature: Int
)