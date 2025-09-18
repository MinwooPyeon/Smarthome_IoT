package com.example.eeum.data.model.response.routine

data class RoutineData(
    val actTime: String,
    val createdAt: String,
    val details: List<RoutineDetail>,
    val iconId: Int,
    val isAi: Boolean,
    val name: String,
    val routineDescription: String,
    val routineId: Int,
    val routineWeekday: Int,
    val triggerType: Boolean,
    val updatedAt: String,
    val iconUrl: String
)