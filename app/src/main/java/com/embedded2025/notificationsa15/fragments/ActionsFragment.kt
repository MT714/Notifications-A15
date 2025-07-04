package com.embedded2025.notificationsa15.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames

/**
 * Classe del fragment relativo alle notifiche con azioni.
 */
class ActionsFragment: Fragment() {
    /**
     * Listener sui cambiamenti delle shared preferences, utilizzato per aggiornare l'interfaccia
     * utente in caso di modifica di uno dei parametri mentre l'app è ancora aperta.
     */
    private val listener: SharedPreferences.OnSharedPreferenceChangeListener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == SharedPrefsNames.ACTION_COLOR) {
                val view = requireView()
                val color = prefs.getInt(SharedPrefsNames.ACTION_COLOR, R.color.grey)
                view.findViewById<TextView>(R.id.colorView)
                    .setBackgroundColor(resources.getColor(color, requireContext().theme))
            }
            else if (key == SharedPrefsNames.ACTION_TEXT) {
                val view = requireView()
                val text = prefs.getString(SharedPrefsNames.ACTION_TEXT, "")
                view.findViewById<TextView>(R.id.actionsText).text =
                    if (!text.isNullOrBlank()) getString(R.string.actions_string, text)
                    else getString(R.string.actions_not_text_received)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_actions, container, false)
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

        view.findViewById<Button>(R.id.btn_clear_actions).setOnClickListener {
            requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(SharedPrefsNames.ACTION_COLOR)
                remove(SharedPrefsNames.ACTION_TEXT)
            }
        }
    }

    /**
     * Aggiorna l'interfaccia utente in caso di cambiamenti delle shared preferences ed aggiunge
     * un listener in caso di ulteriori modifiche.
     */
    override fun onResume() {
        super.onResume()

        val view = requireView()

        val prefs = requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        val color = prefs.getInt(SharedPrefsNames.ACTION_COLOR, R.color.grey)
        view.findViewById<TextView>(R.id.colorView)
            .setBackgroundColor(resources.getColor(color, requireContext().theme))
        val text = prefs.getString(SharedPrefsNames.ACTION_TEXT, "")
        view.findViewById<TextView>(R.id.actionsText).text =
            if (!text.isNullOrBlank()) getString(R.string.actions_string, text)
            else getString(R.string.actions_not_text_received)
    }

    /**
     * Rimuove il listener delle shared preferences.
     */
    override fun onPause() {
        super.onPause()

        requireContext().getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }
}