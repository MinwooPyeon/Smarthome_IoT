package com.example.eeum.data.model.response.common

data class Page<T>(
    val totalCount: Int,
    val items: List<T>
)