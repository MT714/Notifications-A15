package com.embedded2025.notificationsa15.meteoUtils

import retrofit2.http.GET
import retrofit2.http.Query

interface MeteoApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): MeteoResponse
}