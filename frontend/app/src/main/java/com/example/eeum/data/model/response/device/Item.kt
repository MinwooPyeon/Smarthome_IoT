package com.example.eeum.data.model.response.device

data class Item(
    val deviceType: String,
    val kwh: Double,
    val percentage: Int
)