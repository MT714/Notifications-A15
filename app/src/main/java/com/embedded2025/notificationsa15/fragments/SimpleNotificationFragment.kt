package com.embedded2025.notificationsa15.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.meteoUtils.MeteoWorker
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
                if (isChecked) startWeatherWorker()
                else stopWeatherWorker()
            }
        }

    private fun startWeatherWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherRequest = PeriodicWorkRequestBuilder<MeteoWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            MeteoWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            weatherRequest
        )

        Log.i("MeteoWorker", "Worker avviato")
    }

    private fun stopWeatherWorker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(MeteoWorker.WORKER_NAME)

        Log.i("MeteoWorker", "Worker interrotto")
    }
}