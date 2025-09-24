package com.example.eeum.data.model.dto.login

data class SignUpRequest(
    val email: String,
    val loginId: String,
    val nickname: String,
    val password: String
)