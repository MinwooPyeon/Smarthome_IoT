package com.example.eeum.util

import com.google.gson.JsonObject

object Payload {
    fun deviceDetail(
        power: Boolean? = null,
        temperature: Int? = null,
        level: Int? = null
    ): JsonObject {
        return JsonObject().apply {
            power?.let        { addProperty("power", it) }
            temperature?.let  { addProperty("temperature", it) }
            level?.let        { addProperty("level", it) }
        }
    }
}