package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper

class EmailNotificationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout per questo frammento
        return inflater.inflate(R.layout.fragment_email_notification, container, false).apply {
            findViewById<Button>(R.id.btnEmailNotification).setOnClickListener {
                DemoNotificationsHelper.showGroupedInboxNotifications()
            }
        }
    }
}