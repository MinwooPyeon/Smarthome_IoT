package com.example.eeum.data.model.response.floorplans

data class FloorPlansList(
    val createdAt: String,
    val floorplanId: Int,
    val floorplansX: Double,
    val floorplansY: Double,
    val homeId: Int,
    val imageUrl: String,
    val square: Double,
    val homeName: String
)