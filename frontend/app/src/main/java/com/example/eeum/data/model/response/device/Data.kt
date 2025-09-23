package com.example.eeum.data.model.response.device

data class Data(
    val from: String,
    val items: List<Item>,
    val range: String,
    val to: String,
    val totalKwh: Double
)