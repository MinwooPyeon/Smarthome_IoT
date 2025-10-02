package com.example.eeum.data.model.dto.routine

data class RoutineRequestDto(
    val actTime: String,
    val detail: List<Detail>,
    val iconId: Int,
    val isAi: Boolean = false,
    val name: String,
    val routineDescription: String,
    val routineWeekday: Int
)