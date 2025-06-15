package com.embedded2025.notificationsa15.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Worker che gestisce la richiesta di aggiornamenti meteo.
 *
 * @param context Context dell'applicazione.
 * @param params Parametri del worker.
 */
class WeatherWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    /**
     * Prende la posizione corrente se disponibile, altrimenti usa una posizione di default.
     */
    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker partito")

        return try {
            var location = getCurrentLocation() ?: DEFAULT_LOC

            val lat = location.latitude
            val lon = location.longitude

            Log.i(TAG, "Latitudine: $lat, Longitudine: $lon")

            val apiKey = applicationContext.getString(R.string.weather_api_key)
            val response = OWMClient.api.getWeatherByCoords(lat, lon, apiKey)
            val cityName = response.name
            val temp = response.main.temp
            val description = response.weather.firstOrNull()?.description ?: "N/A"
            val weatherText = "$temp°C – $description"
            val iconCode = response.weather.firstOrNull()?.icon ?: "02d"

            NotificationHelper.showWeatherNotification("Meteo a $cityName", weatherText, iconCode)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error fetching weather data", e)
            Result.retry()
        }
    }

    /**
     * Ottiene la posizione corrente se disponibile, altrimenti prova a restituire l'ultima
     * posizione.
     *
     * @return La posizione corrente, l'ultima posizione o null se non è disponibile.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        Log.i(TAG, "Richiesta posizione.")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        // Check for location permissions
        if (!hasLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        // Prova ad ottenere l'ultima posizione
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location: Location? -> cont.resume(location) }
            .addOnFailureListener { exception ->
                // Prova ad ottenere l'ultima posizione
                Log.w(TAG, "Failed to get current location, attempting to get last known location.", exception)
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation: Location? -> cont.resume(lastLocation) }
                        .addOnFailureListener { lastLocationException ->
                            Log.e("LocationHelper", "Failed to get last known location.", lastLocationException)
                            cont.resume(null)
                        }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception when trying to get last known location.", e)
                    cont.resume(null)
                }
            }

        // Handle cancellation of the coroutine
        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    private fun hasLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            return false


        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    companion object {
        const val WORKER_NAME = "weather_worker"
        private const val TAG = "WeatherWorker"

        /**
         * Le coordinate di Padova
         */
        private val DEFAULT_LOC: Location = Location("DefaultLocationProvider").apply {
            latitude = 45.4064
            longitude = 11.8768
        }
    }
}