package com.embedded2025.notificationsa15

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import androidx.core.content.edit
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.SharedPrefsNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Classe che gestisce le azioni delle notifiche.
 */
class NotificationActionReceiver: BroadcastReceiver() {
    /**
     * ID delle azioni da utilizzare che il BroadcastReceiver riconosce.
     */
    object NotificationAction {
        const val SET_RED = "com.embedded2025.notificationsa15.SET_RED"
        const val SET_YELLOW = "com.embedded2025.notificationsa15.SET_YELLOW"
        const val REPLY = "com.embedded2025.notificationsa15.ACTION_REPLY"
    }

    /**
     * ID degli extra da utilizzare nell'intent.
     */
    object IntentExtras {
        const val NOTIFICATION_ID = "notification_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val IS_DEMO = "is_demo"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Dispatcher delle azioni
        when (intent.action) {
            NotificationAction.SET_RED -> handleRed(context, intent)
            NotificationAction.SET_YELLOW -> handleYellow(context, intent)
            NotificationAction.REPLY -> {
                if (intent.getBooleanExtra(IntentExtras.IS_DEMO, false)) handleReply(context, intent)
                else handleChatReply(intent)
            }
            else -> Log.w("NotificationActionReceiver", "Azione sconosciuta: ${intent.action}")
        }
    }

    /**
     * Gestisce l'azione [NotificationAction.SET_RED].
     */
    private fun handleRed(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        Log.d("NotificationActionReceiver", "Azione: Impostato rosso (ID: $notificationId)")

        val prefs = context.getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(SharedPrefsNames.ACTION_COLOR, R.color.red) }
        NotificationHelper.cancel(notificationId)

        Toast.makeText(context, context.getString(R.string.toast_action_color_set_red), Toast.LENGTH_SHORT).show()
    }

    /**
     * Gestisce l'azione [NotificationAction.SET_YELLOW].
     */
    private fun handleYellow(context: Context, intent:Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        Log.d("NotificationActionReceiver", "Azione: Impostato giallo (ID: $notificationId)")

        val prefs = context.getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(SharedPrefsNames.ACTION_COLOR, R.color.yellow) }
        NotificationHelper.cancel(notificationId)

        Toast.makeText(context, context.getString(R.string.toast_action_color_set_yellow), Toast.LENGTH_SHORT).show()
    }

    /**
     * Gestisce l'azione [NotificationAction.REPLY] per la notifica demo.
     */
    private fun handleReply(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(IntentExtras.KEY_TEXT_REPLY)
        if (replyText != null) {
            Log.d("Notification Reply", "User: $replyText (ID: $notificationId)")

            val prefs = context.getSharedPreferences(SharedPrefsNames.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putString(SharedPrefsNames.ACTION_TEXT, replyText.toString()) }
            NotificationHelper.cancel(notificationId)

            Toast.makeText(context, context.getString(R.string.toast_action_string, replyText), Toast.LENGTH_SHORT).show()
        } else Log.w("Notification Reply", "Nessun testo nella risposta.")
    }

    /**
     * Gestisce l'azione [NotificationAction.REPLY] per la chat con l'assistente.
     */
    private fun handleChatReply(intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(IntentExtras.KEY_TEXT_REPLY)
        if (!replyText.isNullOrBlank()) {
            Log.d("Notification Reply", "User: $replyText")
            val chatRepo = NotificationsLabApplication.chatRepository

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val botMsg = chatRepo.processUserMessageAndGetResponse(replyText.toString(), true)
                    if (botMsg != null) {
                        Log.d("Notification Reply", "Bot: ${botMsg.content}")
                    }
                } catch (e: Exception) {
                    Log.e("Notification Reply", "Errore: ${e.message}")
                } finally {
                    NotificationHelper.showMessageNotification(chatRepo.getLastMessages(4))
                    pendingResult.finish()
                }
            }
        }
        else Log.w("Notification Reply", "Nessun testo nella risposta.")
    }
}