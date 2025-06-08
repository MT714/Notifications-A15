package com.embedded2025.notificationsa15.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.meteoUtils.WeatherWorker
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.TimeUnit
import androidx.core.content.edit


class SimpleNotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_simple_notification, container, false).apply {
            findViewById<Button>(R.id.btnSimple).setOnClickListener {
                DemoNotificationsHelper.showSimpleNotification()
            }

            val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)

            val weatherSwitch = findViewById<SwitchMaterial>(R.id.swtWeather)
            weatherSwitch.isChecked = prefs.getBoolean(SharedPrefsNames.WEATHER_ENABLED, false)
            weatherSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit { putBoolean(SharedPrefsNames.WEATHER_ENABLED, isChecked) }
                if (isChecked) {
                    checkLocationPermission {
                        startWeatherWorker()
                    }
                } else {
                    stopWeatherWorker()
                }
            }
        }

    private fun startWeatherWorker() {
        Log.i("WeatherWorker", "Worker avviato")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherRequest = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            WeatherWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            weatherRequest
        )
    }

    private fun stopWeatherWorker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(WeatherWorker.WORKER_NAME)

        Log.i("WeatherWorker", "Worker interrotto")
    }

    /**
     * Verifica se il permesso di accesso alla posizione è stato concesso. ALtrimenti lancia il dialogo di richiesta
     */
    private fun checkLocationPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else{
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Gestisce il permesso di accesso alla posizione e, se concesso, avvia l worker
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LOCATION PERMISSION", "Permesso posizione concesso")
             startWeatherWorker()
        }
        else Log.d("LCOATION PERMISSION", "Permesso posizione negato")
    }
}