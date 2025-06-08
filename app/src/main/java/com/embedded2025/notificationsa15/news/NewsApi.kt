package com.embedded2025.notificationsa15.news

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {
    @GET("v4/top-headlines")
    suspend fun getTopHeadlines(
        @Query("token") apiKey: String,
        @Query("lang") lang: String = "it",
        @Query("max") max: Int = 1
    ): NewsResponse
}
