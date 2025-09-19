package com.example.eeum.data.model.response.floorplans

data class FloorPlansList(
    val createdAt: String,
    val floorplanId: Int,
    val floorplansX: Float,
    val floorplansY: Float,
    val homeId: Int,
    val imageUrl: String,
    val square: Double,
    val homeName: String
)
