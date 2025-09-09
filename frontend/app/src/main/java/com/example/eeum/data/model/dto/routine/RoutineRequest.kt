package com.example.eeum.data.model.dto.routine

data class RoutineRequest(
    val name: String,
    val routineWeekday: Int,
    val routineDescription: String? = null,
    val actTime: String,
    val details: List<RoutineDetailRequest>
)