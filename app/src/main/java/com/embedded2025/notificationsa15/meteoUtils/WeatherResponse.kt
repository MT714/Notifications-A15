package com.embedded2025.notificationsa15.meteoUtils

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val name: String,
)


data class Weather(
    val description: String,
    val icon: String
    )
data class Main(val temp: Float)
