package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.google.android.material.textfield.TextInputEditText

class CallNotificationFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_call_notification, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val delayEditText = view.findViewById<TextInputEditText>(R.id.etDelay)
        val scheduleButton = view.findViewById<Button>(R.id.btnScheduleCall)

        scheduleButton.setOnClickListener {
            val delaySeconds = delayEditText.text.toString().toIntOrNull()
            if (delaySeconds != null && delaySeconds > 0) {
                NotificationHelper.showCallNotification(delaySeconds)

                val message = getString(R.string.call_notification_scheduled_toast, delaySeconds)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.call_notification_invalid_delay), Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener(){
            findNavController().navigate(R.id.liveUpdateNotificationFragment)
        }

        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener(){
            findNavController().navigate(R.id.mediaPlayerNotificationFragment)
        }
    }
}