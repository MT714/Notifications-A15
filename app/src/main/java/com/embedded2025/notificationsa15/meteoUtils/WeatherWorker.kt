package com.embedded2025.notificationsa15.meteoUtils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            val location = getCurrentLocation() ?: return Result.retry()

            val lat = location.latitude
            val lon = location.longitude

            if (lat.isNaN() || lon.isNaN()) {
                Log.e("WeatherWorker", "Coordinate non valide")
                return Result.failure()
            }
            Log.i("WeatherWorker", "Latitudine: $lat, Longitudine: $lon")

            val apiKey = applicationContext.getString(R.string.weather_api_key)
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

        // Prova con getCurrentLocation (attiva)
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                cont.resume(location)
            } else {
                // Se fallisce, fallback su getLastLocation (passiva)
                fusedClient.lastLocation
                    .addOnSuccessListener { lastLoc ->
                        Log.w("WeatherWorker", "Fallback su lastLocation")
                        cont.resume(lastLoc)
                    }
                    .addOnFailureListener {
                        Log.e("WeatherWorker", "lastLocation fallita")
                        cont.resume(null)
                    }
            }
        }.addOnFailureListener { exception ->
            Log.e("WeatherWorker", "getCurrentLocation fallita", exception)
            // Fallimento diretto, prova lastLocation
            fusedClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    Log.w("WeatherWorker", "Fallback su lastLocation dopo errore")
                    cont.resume(lastLoc)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
    /*@SuppressLint("MissingPermission")
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
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Exception catturata nella richiesta posizione", e)
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }*/


    companion object {
        const val WORKER_NAME = "weather_worker"
    }
}