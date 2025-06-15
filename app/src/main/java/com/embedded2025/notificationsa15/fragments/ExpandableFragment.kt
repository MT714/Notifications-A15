package com.embedded2025.notificationsa15.fragments

import android.content.Context
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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.news.NewsWorker
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.TimeUnit

/**
 * Classe del fragment relativo alle notifiche espandibli.
 * Si occupa anche del worker relativo alle notizie di cronaca.
 */
class ExpandableFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_expandable, container, false)
    }

    /**
     * Gestisce l'interazione con gli elementi dell'interfaccia utente con le notifiche demo e
     * con le notifiche delle notizie di cronaca.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnExpandableText).setOnClickListener {
            NotificationHelper.showExpandableTextNotification()
        }
        view.findViewById<Button>(R.id.btnExpandablePicture).setOnClickListener {
            NotificationHelper.showExpandablePictureNotification()
        }

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            findNavController().navigate(R.id.simpleNotificationFragment)
        }
        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.emailNotificationFragment)
        }

        val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)

        val newsSwitch = view.findViewById<SwitchMaterial>(R.id.swtNews)
        newsSwitch.isChecked = prefs.getBoolean(SharedPrefsNames.NEWS_ENABLED, false)
        newsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SharedPrefsNames.NEWS_ENABLED, isChecked) }
            if (isChecked) startNewsWorker(prefs.getLong(SharedPrefsNames.NEWS_NOTIFICATION_INTERVAL_VALUE,15L))
            else stopNewsWorker()
        }

        val spinner = view.findViewById<Spinner>(R.id.spinnerInterval)
        var isSpinnerInitialized = false
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.news_intervals_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.setSelection(prefs.getInt(SharedPrefsNames.NEWS_NOTIFICATION_INTERVAL_INDEX, 0))
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
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
                    putInt(SharedPrefsNames.NEWS_NOTIFICATION_INTERVAL_INDEX, position)
                    putLong(SharedPrefsNames.NEWS_NOTIFICATION_INTERVAL_VALUE, intervalMinutes)
                }

                Log.i(
                    "NewsWorker",
                    "Intervallo di aggiornamento news impostato a $intervalMinutes minuti"
                )

                /**
                 * Quando modifico l'intrevallo, se c'è già un worker lo cancello e ne creo uno con il nuovo intervallo
                 */
                if (prefs.getBoolean(SharedPrefsNames.WEATHER_ENABLED, false)) {
                    Log.i("WeatherWorker", "Worker già attivo, modifico l'intervallo")
                    stopNewsWorker()
                    startNewsWorker(intervalMinutes)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun startNewsWorker(intervalMinutes : Long) {
        Log.i("NewsWorker", "Worker attivato con intervallo di $intervalMinutes minuti")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsRequest = PeriodicWorkRequestBuilder<NewsWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            NewsWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            newsRequest
        )

        Log.i("NewsWorker", "Worker avviato")
    }

    private fun stopNewsWorker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(NewsWorker.WORKER_NAME)

        Log.i("NewsWorker", "Worker interrotto")
    }
}