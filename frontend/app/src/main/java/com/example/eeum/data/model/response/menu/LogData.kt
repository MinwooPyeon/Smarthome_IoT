package com.example.eeum.data.model.response.menu

data class LogData(
    val deviceName: String,
    val eventTime: String,
    val kind: String,
    val roomId: Int,
    val roomName: String
)