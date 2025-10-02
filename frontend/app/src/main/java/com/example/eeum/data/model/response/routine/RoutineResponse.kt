package com.example.eeum.data.model.response.routine

import com.example.eeum.data.model.response.common.BaseResponse

data class RoutineResponse (
    val routineId: Int,
    val name: String,
    val triggerType: Boolean,
    val routineWeekday: Int,
    val routineDescription: String?,
    val actTime: String,
    val createdAt: String,
    val updatedAt: String,
    val details: List<BaseResponse>
)
