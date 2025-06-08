package com.embedded2025.notificationsa15.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.news.NewsWorker
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.TimeUnit

class ExpandableNotificationFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_expandable_notification, container, false)

        view.findViewById<Button>(R.id.btnExpandableText).setOnClickListener {
            DemoNotificationsHelper.showExpandableTextNotification()
        }
        view.findViewById<Button>(R.id.btnExpandablePicture).setOnClickListener {
            DemoNotificationsHelper.showExpandablePictureNotification()
        }

        val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)

        val newsSwitch = view.findViewById<SwitchMaterial>(R.id.swtNews)
        newsSwitch.isChecked = prefs.getBoolean(SharedPrefsNames.NEWS_ENABLED, false)
        newsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(SharedPrefsNames.NEWS_ENABLED, isChecked) }
            if (isChecked) startNewsWorker()
            else stopNewsWorker()
        }

        return view
    }

    private fun startNewsWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsRequest = PeriodicWorkRequestBuilder<NewsWorker>(15, TimeUnit.MINUTES)
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