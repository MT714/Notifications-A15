package com.embedded2025.notificationsa15.meteoUtils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.*
import com.embedded2025.notificationsa15.utils.NotificationsHelper.ctx
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.i("WeatherWorker", "Inizio Worker")

            val location = getCurrentLocation() ?: return Result.retry()

            Log.i("WeatherWorker", "Post getCurrentLocation()")
            val lat = location.latitude
            val lon = location.longitude

            if (lat.isNaN() || lon.isNaN()) {
                Log.e("WeatherWorker", "Coordinate non valide")
                return Result.failure()
            }
            Log.i("WeatherWorker", "Latitudine: $lat, Longitudine: $lon")

            val apiKey = ctx.getString(R.string.weather_api_key)
            val response = RetrofitClient.api.getWeatherByCoords(lat, lon, apiKey)
            val cityName = response.name
            val temp = response.main.temp
            val description = response.weather.firstOrNull()?.description ?: "N/A"
            val weatherText = "$temp°C – $description"

            NotificationsHelper.showWeatherNotification("Aggiornamento Meteo a $cityName", weatherText)
            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }


    /**
     * Ottiene la posizione CORRENTE in modo attivo.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val cancellationTokenSource = CancellationTokenSource()

        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                cont.resume(location)
            }.addOnFailureListener { exception ->
                cont.resume(null)
            }

            Log.i("WeatherWorker", "Fine try")
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Exception catturata nella richiesta posizione", e)
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }


    companion object {
        const val WORKER_NAME = "weather_worker"

        fun createInputData(lat: Double, lon: Double): Data {
            return workDataOf(
                "latitude" to lat,
                "longitude" to lon
            )
        }
    }
}