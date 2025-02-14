package com.example.myrides.data.api

import retrofit2.http.GET
import retrofit2.Response

interface MyApiService {
    @GET("some_endpoint")
    suspend fun getData(): Response<List<Any>>
}
