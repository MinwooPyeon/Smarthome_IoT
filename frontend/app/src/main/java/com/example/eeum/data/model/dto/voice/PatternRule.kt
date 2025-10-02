package com.example.eeum.data.model.dto.voice

data class PatternRule(
    val regex: Regex,
    val slotNames: Set<String>
)