package com.embedded2025.notificationsa15

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button



class ActionsNotificationFragment : BaseNotificationFragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_actions_notification, container, false).apply {
            findViewById<Button>(R.id.btnShowNotification).setOnClickListener {
                checkAndRequestNotificationPermission()
            }
        }

    override fun onNotificationPermissionGranted() {
        NotificationsHelper.showSimpleNotificationDemo(requireContext())
    }
}