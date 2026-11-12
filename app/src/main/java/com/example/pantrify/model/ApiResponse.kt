package com.example.pantrify.model

data class ApiResponse<T>(
    val data: T? = null,
    val message: String? = null
)
