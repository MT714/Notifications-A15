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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import com.embedded2025.notificationsa15.weather.WeatherWorker
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.TimeUnit

/**
 * Fragment che gestisce le notifiche semplici e le notifiche meteo.
 */
class SimpleFragment : Fragment() {
    /**
     * Launcher per il permesso di accesso alla posizione in foreground.
     * Se ha successo richiede anche il permesso in background.
     */
    private val fgLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseLocGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocGranted || coarseLocGranted) {
                Log.d(TAG, "Foreground location permessa.")
                checkAndRequestBackgroundLocation()
            } else Log.d(TAG, "Foreground location negata.")
        }

    /**
     * Launcher per il permesso di accesso alla posizione in background.
     */
    private val bgLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isgranted ->
            if (isgranted) Log.d(TAG, "Background location permessa.")
            else Log.d(TAG, "Background location negata")
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_simple, container, false)
    }


    /**
     * Gestisce l'interazione con gli elementi dell'interfaccia utente.
     */
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
                startLocationPermissionRequest()
                startWeatherWorker(prefs.getLong(SharedPrefsNames.WEATHER_NOTIFICATION_INTERVAL_VALUE, 15L))
            } else stopWeatherWorker()
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

                if (prefs.getBoolean(SharedPrefsNames.WEATHER_ENABLED, false))
                    startWeatherWorker(intervalMinutes)
            }

            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

    }

    /**
     * Avvia il worker di aggiornamento meteo.
     */
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

    /**
     * Interrompe il worker di aggiornamento meteo.
     */
    private fun stopWeatherWorker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(WeatherWorker.WORKER_NAME)

        Log.i("WeatherWorker", "Worker interrotto")
    }

    /**
     * Richiede il permesso di accesso alla posizione in foreground ed in background.
     */
    fun startLocationPermissionRequest() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        ) {
            checkAndRequestBackgroundLocation()
        } else {
            fgLocationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /**
     * Richiede il permesso di accesso alla posizione in background se necessario.
     */
    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    companion object {
        private const val TAG = "LocationAccess"
    }
}