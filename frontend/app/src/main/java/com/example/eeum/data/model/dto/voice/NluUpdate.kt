package com.example.eeum.data.model.dto.voice

data class NluUpdate (
    val raw: String,
    val intents: List<IntentResult>) {
}