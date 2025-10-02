package com.example.eeum.data.model.dto.voice

data class IntentResult(
    val intent: String,
    val slots: Map<String, String>
)