package com.embedded2025.notificationsa15

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class ExpandableNotificationFragment : BaseNotificationFragment() {
    private var pendingNotification: NotificationType? = null

    enum class NotificationType {
        EXPANDABLE_TEXT, EXPANDABLE_PICTURE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_expandable_notification, container, false).apply {
            findViewById<Button>(R.id.btnExpandableText).setOnClickListener {
                pendingNotification = NotificationType.EXPANDABLE_TEXT
                checkAndRequestNotificationPermission()
            }
            findViewById<Button>(R.id.btnExpandablePicture).setOnClickListener {
                pendingNotification = NotificationType.EXPANDABLE_PICTURE
                checkAndRequestNotificationPermission()
            }
        }

    override fun onNotificationPermissionGranted() {
        when (pendingNotification) {
            NotificationType.EXPANDABLE_TEXT ->
                NotificationsHelper.showExpandableTextNotificationDemo(requireContext())
            NotificationType.EXPANDABLE_PICTURE ->
                NotificationsHelper.showExpandablePictureNotificationDemo(requireContext())
            else -> Unit
        }
    }
}