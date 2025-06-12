package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper


class LiveUpdateNotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_live_update_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnLiveUpdate_create).setOnClickListener {
            NotificationHelper.showLiveUpdateNotification()
        }

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener(){
            findNavController().navigate(R.id.progressNotificationFragment)
        }

        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener(){
            findNavController().navigate(R.id.callNotificationFragment)
        }
    }
}