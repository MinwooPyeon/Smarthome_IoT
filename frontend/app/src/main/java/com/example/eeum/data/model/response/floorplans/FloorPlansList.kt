package com.example.eeum.data.model.response.floorplans

data class FloorPlansList(
    val createdAt: String,
    val floorplanId: Int,
    val floorplansX: Int,
    val floorplansY: Int,
    val homeId: Int,
    val imageUrl: String,
    val square: Double,
    val homeName: String
)