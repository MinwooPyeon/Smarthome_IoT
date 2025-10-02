package com.example.eeum.data.model.response.common

data class ApiResponse<T>(
    val status: String,
    val data: T
)