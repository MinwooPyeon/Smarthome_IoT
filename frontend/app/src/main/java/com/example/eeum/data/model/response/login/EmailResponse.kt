package com.example.eeum.data.model.response.login

data class EmailResponse(
    val expiresInMinutes: Int,
    val sent: Boolean
)