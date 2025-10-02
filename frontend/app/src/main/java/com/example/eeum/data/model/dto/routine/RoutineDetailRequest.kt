package com.example.eeum.data.model.dto.routine

import com.google.gson.JsonObject

data class RoutineDetailRequest(
    val deviceId: Int,
    val deviceDetail: JsonObject
)