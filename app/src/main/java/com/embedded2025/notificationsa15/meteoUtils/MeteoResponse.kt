package com.embedded2025.notificationsa15.meteoUtils

data class MeteoResponse(
    val weather: List<Weather>,
    val main: Main,
    val name: String
)

data class Weather(val description: String)
data class Main(val temp: Float)
