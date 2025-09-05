package com.example.eeum.voice

data class IntentResult(
    val name: String,
    val slots: Map<String, String>
)