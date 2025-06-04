package com.embedded2025.notificationsa15

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class SimpleNotificationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_simple_notification, container, false)

        val button = view.findViewById<Button>(R.id.btnSimpleNotification)
        button.setOnClickListener {
            NotificationsHelper.showSimpleNotificationDemo(requireContext())
            //showSimpleNotification(requireContext())
        }

        return view
    }

}