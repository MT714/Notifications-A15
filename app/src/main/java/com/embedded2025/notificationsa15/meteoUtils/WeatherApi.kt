package com.embedded2025.notificationsa15.meteoUtils

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    /**
     * Ottiene le informazioni meteo in base alla città
     */
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "it"
    ): WeatherResponse

    /**
     * Ottiene le informazioni meteo in base alle coordinate geografiche
     */
    @GET("data/2.5/weather")
    suspend fun getWeatherByCoords(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",   // per avere i °C
        @Query("lang") lang: String = "it"          // per avere le descrizioni in italiano
    ): WeatherResponse
}