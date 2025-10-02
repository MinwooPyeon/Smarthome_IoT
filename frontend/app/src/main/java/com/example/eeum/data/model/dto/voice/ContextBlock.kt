package com.example.eeum.data.model.dto.voice

data class ContextBlock(
    val expressions: Map<String, List<String>>,
    val slots: Map<String, List<String>> = emptyMap(),
    val macros: Map<String, List<String>> = emptyMap()
)