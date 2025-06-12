package com.embedded2025.notificationsa15

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.embedded2025.notificationsa15.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Classe per gestire le azioni delle notifiche
class NotificationActionReceiver : BroadcastReceiver() {
    object NotificationAction {
        const val ARCHIVE = "com.embedded2025.notificationsa15.ACTION_ARCHIVE"
        const val LATER = "com.embedded2025.notificationsa15.ACTION_LATER"
        const val REPLY = "com.embedded2025.notificationsa15.ACTION_REPLY"
    }

    object IntentExtras {
        const val NOTIFICATION_ID = "notification_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val IS_DEMO = "is_demo"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Dispatcher delle azioni
        when (intent.action) {
            NotificationAction.ARCHIVE -> handleArchive(context, intent)
            NotificationAction.LATER -> handleLater(context, intent)
            NotificationAction.REPLY -> handleReply(context, intent)
            else -> Log.w("NotificationActionReceiver", "Azione sconosciuta: ${intent.action}")
        }
    }

    private fun handleArchive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        NotificationHelper.cancel(notificationId)
        Toast.makeText(context, "Azione: Archiviato (ID: $notificationId)", Toast.LENGTH_SHORT).show()
    }

    private fun handleLater(context: Context, intent:Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        NotificationHelper.cancel(notificationId)
        Toast.makeText(context, "Azione: Più tardi (ID: $notificationId)", Toast.LENGTH_SHORT).show()
    }

    private fun handleReply(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(IntentExtras.NOTIFICATION_ID, -1)
        val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(IntentExtras.KEY_TEXT_REPLY)
        if (intent.getBooleanExtra(IntentExtras.IS_DEMO, true)) {
            if (replyText != null) {
                Toast.makeText(context, "Risposta ricevuta: $replyText (ID: $notificationId)", Toast.LENGTH_LONG).show()
                val repliedNotification = NotificationCompat.Builder(context, NotificationHelper.ChannelID.DEMO)
                    .setSmallIcon(R.drawable.ic_action)
                    .setContentText("Risposta inviata: \"$replyText\"")
                NotificationHelper.safeNotify(notificationId, repliedNotification)
            } else Toast.makeText(context, "Nessun testo nella risposta.", Toast.LENGTH_SHORT).show()

        }
        else {
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
            else Toast.makeText(context, "Nessun testo nella risposta.", Toast.LENGTH_SHORT).show()
        }

    }
}