package com.example.eeum.data.model.dto.routine

data class RoutineRequest(
    val name: String,
    val routineWeekday: Int,
    val routineDescription: String? = null,
    val actTime: String,
    val detail: List<RoutineDetailRequest>,
    val iconId: Int = 1,
    val isAi: Boolean = false
)