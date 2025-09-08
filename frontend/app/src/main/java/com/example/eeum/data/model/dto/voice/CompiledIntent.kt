package com.example.eeum.data.model.dto.voice

data class CompiledIntent(
    val intent: String,
    val rules: List<PatternRule>
)