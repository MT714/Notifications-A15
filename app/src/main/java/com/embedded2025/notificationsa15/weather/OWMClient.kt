package com.embedded2025.notificationsa15.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Client per l'API OpenWeatherMap.
 */
object OWMClient {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: OWMApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OWMApi::class.java)
    }
}

/**
 * Interfaccia API di OpenWeatherMap
 */
interface OWMApi {
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
    ): OWMResponse
}

data class OWMResponse(
    val weather: List<OWMWeather>,
    val main: OWMMain,
    val name: String,
)

data class OWMWeather(
    val description: String,
    val icon: String
)

data class OWMMain(
    val temp: Float
)
