package com.example.eeum.ui.screens

data class LogRecord(
    val id: String,
    val timestamp: Long,
    val period: String,
    val time: String,
    val device: String,
    val location: String,
    val status: String
)
