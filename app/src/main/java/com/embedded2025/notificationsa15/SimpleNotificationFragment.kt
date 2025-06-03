package com.embedded2025.notificationsa15

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class SimpleNotificationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_simple_notification, container, false)

        val button = view.findViewById<Button>(R.id.btnSimpleNotification)
        button.setOnClickListener {

            showSimpleNotification(requireContext())
        }

        return view
    }

    private fun showSimpleNotification(context: Context){
        val builder = NotificationCompat.Builder(context, "default_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(context.getString(R.string.simple_notification_title))
            .setContentText(context.getString(R.string.simple_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)


        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){

            with(NotificationManagerCompat.from(context)) {
                notify(1, builder.build())
            }

        }

    }

}