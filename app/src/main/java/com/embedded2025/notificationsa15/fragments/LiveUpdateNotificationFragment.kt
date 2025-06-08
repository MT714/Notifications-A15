package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper


class LiveUpdateNotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_live_update_notification, container, false).apply {
            findViewById<Button>(R.id.btnLiveUpdate_create).setOnClickListener {
                DemoNotificationsHelper.showLiveUpdateNotification(requireContext())
            }
        }

}