package com.embedded2025.notificationsa15.meteoUtils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.*
import com.embedded2025.notificationsa15.utils.NotificationsHelper.ctx

class MeteoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            //TODO da fare selezionabile
            val city = "Padova"
            val apiKey = ctx.getString(R.string.weather_api_key)
            val response = RetrofitClient.api.getWeather(city, apiKey)

            Log.i("MeteoWorker", "MeteoWorker called")

            val temp = response.main.temp
            val description = response.weather.firstOrNull()?.description ?: "N/A"
            val weatherText = "$temp°C – $description"

            NotificationsHelper.showWeatherNotification("Aggiornamento Meteo a $city", weatherText)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORKER_NAME = "meteo_worker"
    }
}