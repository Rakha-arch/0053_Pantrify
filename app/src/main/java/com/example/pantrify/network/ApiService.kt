package com.example.pantrify.network

import retrofit2.http.GET

interface ApiService {
    @GET("api/")
    suspend fun getData(): Any
}
