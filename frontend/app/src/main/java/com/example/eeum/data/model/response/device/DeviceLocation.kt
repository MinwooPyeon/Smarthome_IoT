package com.example.eeum.data.model.response.device

data class DeviceLocation(
    val `data`: List<LocationData>,
    val status: String
)