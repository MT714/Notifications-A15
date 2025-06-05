package com.embedded2025.notificationsa15

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class ProgressNotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_progress_notification, container, false).apply {
            findViewById<Button>(R.id.btnProgressNotification).setOnClickListener {
                NotificationsHelper.showProgressNotificationDemo()
            }
        }
}