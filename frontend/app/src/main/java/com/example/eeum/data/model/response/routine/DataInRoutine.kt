package com.example.eeum.data.model.response.routine

data class Data(
    val actTime: String,
    val createdAt: String,
    val details: List<Detail>,
    val iconId: Int,
    val iconUrl: String,
    val isAi: Boolean,
    val name: String,
    val routineDescription: String,
    val routineId: Int,
    val routineWeekday: Int,
    val triggerType: Boolean,
    val updatedAt: String
)