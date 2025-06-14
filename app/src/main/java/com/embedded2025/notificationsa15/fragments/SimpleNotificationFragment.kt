package com.embedded2025.notificationsa15.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.weather.WeatherWorker
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController


class SimpleNotificationFragment : Fragment() {
    private var onLocationPermissionGranted: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_simple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.expandableNotificationFragment)
        }

        view.findViewById<Button>(R.id.btnSimple).setOnClickListener {
            NotificationHelper.showSimpleNotification()
        }

        val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)

        val weatherSwitch = view.findViewById<SwitchMaterial>(R.id.swtWeather)
        weatherSwitch.isChecked = prefs.getBoolean(SharedPrefsNames.WEATHER_ENABLED, false)
        weatherSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SharedPrefsNames.WEATHER_ENABLED, isChecked) }
            if (isChecked) {
                checkLocationPermission {
                    startWeatherWorker(prefs.getLong(SharedPrefsNames.WEATHER_NOTIFICATION_INTERVAL_VALUE, 15L))
                }
            } else {
                stopWeatherWorker()
            }
        }

        val spinner = view.findViewById<Spinner>(R.id.spinnerInterval)
        var isSpinnerInitialized = false
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.weather_intervals_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.setSelection(prefs.getInt(SharedPrefsNames.WEATHER_NOTIFICATION_INTERVAL_INDEX, 0))
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val intervalMinutes = when (position) {
                    0 -> 15L
                    1 -> 30L
                    2 -> 60L
                    3 -> 120L
                    else -> 15L
                }

                prefs.edit {
                    putInt(SharedPrefsNames.WEATHER_NOTIFICATION_INTERVAL_INDEX, position)
                    putLong(SharedPrefsNames.WEATHER_NOTIFICATION_INTERVAL_VALUE, intervalMinutes)
                }

                if (prefs.getBoolean(SharedPrefsNames.WEATHER_ENABLED, false)) {
                    stopWeatherWorker()
                    startWeatherWorker(intervalMinutes)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

    }

    private fun startWeatherWorker(intervalMinutes : Long) {
        Log.i("WeatherWorker", "Worker attivato con intervallo di $intervalMinutes minuti")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherRequest = PeriodicWorkRequestBuilder<WeatherWorker>(intervalMinutes, TimeUnit.MINUTES)
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
            onLocationPermissionGranted = onGranted
            foregroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Verifica se il permesso di accesso alla posizione in sfondo è stato concesso. ALtrimenti lancia il dialogo di richiesta
     */
    private fun checkBackgroundLocationPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("Permission", "Permesso permesso di localizzazione in background già concesso")
                onGranted()
            } else {
                Log.i("Permission", "Richiesta permesso di localizzazione in background")
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            Log.i("Permission", "Background location permission is implicitly granted on this Android version (below API 29)")
            onGranted()
        }
    }


    /**
     * Gestisce il permesso di accesso alla posizione e, se concesso, avvia l worker
     */
    private val foregroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LOCATION PERMISSION", "Permesso posizione concesso")
            onLocationPermissionGranted?.invoke()
            onLocationPermissionGranted = null

            checkBackgroundLocationPermission {}
        }
        else Log.d("LCOATION PERMISSION", "Permesso posizione negato")
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("BACKGROUND LOCATION PERMISSION", "Permesso di localizzazione in background concesso.")
            } else {
                Log.w("BACKGROUND LOCATION PERMISSION", "Permesso di localizzazione in background negato.")
            }
        }
}