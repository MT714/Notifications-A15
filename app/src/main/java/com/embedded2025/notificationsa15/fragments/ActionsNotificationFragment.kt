package com.embedded2025.notificationsa15.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames


class ActionsNotificationFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_actions_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnActionNotification).setOnClickListener {
            NotificationHelper.showActionNotification()
        }

        view.findViewById<Button>(R.id.btnReplyNotification).setOnClickListener {
            NotificationHelper.showReplyNotification()
        }

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            findNavController().navigate(R.id.emailNotificationFragment)
        }

        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.chatNotificationFragment)
        }
    }

    override fun onResume() {
        super.onResume()

        val view = requireView()

        val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
        val color = prefs.getInt(SharedPrefsNames.ACTION_COLOR, 0)
        view.findViewById<TextView>(R.id.colorView).setBackgroundColor(getColor(color))
        val text = prefs.getString(SharedPrefsNames.ACTION_TEXT, "")
        if (!text.isNullOrBlank())
            view.findViewById<TextView>(R.id.actionsText).text = getString(R.string.action_string, text)
    }

    private fun getColor(color: Int): Int {
        val theme = requireContext().theme

        return when (color) {
            1 -> resources.getColor(R.color.red, theme)
            2 -> resources.getColor(R.color.yellow, theme)
            else -> resources.getColor(R.color.grey, theme)
        }
    }
}