package com.example.eeum.data.model.dto.login

data class VerifyRequest(
    val code: String,
    val email: String
)