package com.example.eeum.core

sealed class AppEffect {
    data class Speak(val text: String) : AppEffect()

    data class Toast(
        val text: String,
        val long: Boolean = false
    ) : AppEffect()

    data class Navigate(
        val route: String,
        val params: Map<String, String> = emptyMap()
    ) : AppEffect()
}