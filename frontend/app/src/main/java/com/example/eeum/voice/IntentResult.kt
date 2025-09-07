package com.example.eeum.voice

data class IntentResult(
    val intent: String,
    val slots: Map<String, String>
)