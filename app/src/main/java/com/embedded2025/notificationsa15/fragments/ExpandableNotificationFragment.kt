package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.NotificationsHelper

class ExpandableNotificationFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_expandable_notification, container, false).apply {
            findViewById<Button>(R.id.btnExpandableText).setOnClickListener {
                DemoNotificationsHelper.showExpandableTextNotification()
            }
            findViewById<Button>(R.id.btnExpandablePicture).setOnClickListener {
                DemoNotificationsHelper.showExpandablePictureNotification()
            }
        }
}