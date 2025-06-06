package com.embedded2025.notificationsa15

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper.OrderStatus
import com.embedded2025.notificationsa15.utils.FakeMediaPlayer
import com.embedded2025.notificationsa15.utils.NotificationsHelper

object NotificationAction {
    const val ARCHIVE = "com.embedded2025.notificationsa15.ACTION_ARCHIVE"
    const val LATER = "com.embedded2025.notificationsa15.ACTION_LATER"
    const val NEXT_STEP = "com.embedded2025.notificationsa15.ACTION_NEXT_STEP"
    const val REPLY = "com.embedded2025.notificationsa15.ACTION_REPLY"
    const val MEDIA_PLAY_PAUSE = "com.embedded2025.notificationsa15.ACTION_MEDIA_PLAY_PAUSE"
    const val MEDIA_NEXT = "com.embedded2025.notificationsa15.ACTION_MEDIA_NEXT"
    const val MEDIA_PREVIOUS = "com.embedded2025.notificationsa15.ACTION_MEDIA_PREVIOUS"
    const val MEDIA_STOP = "com.embedded2025.notificationsa15.ACTION_MEDIA_STOP"
}

// Classe per gestire le azioni delle notifiche
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        NotificationsHelper.initialize(context)

        when (intent.action) {
            NotificationAction.ARCHIVE -> {
                NotificationsHelper.cancel(notificationId)
                Toast.makeText(context, "Azione: Archiviato (ID: $notificationId)", Toast.LENGTH_SHORT).show()
            }
            NotificationAction.LATER -> {
                NotificationsHelper.cancel(notificationId)
                Toast.makeText(context, "Azione: Più tardi (ID: $notificationId)", Toast.LENGTH_SHORT).show()
            }
            NotificationAction.NEXT_STEP -> {
                val currentStep = intent.getIntExtra("order_step", 0)
                val nextStep = (currentStep + 1).coerceAtMost(OrderStatus.ORDER_COMPLETE)
                DemoNotificationsHelper.showLiveUpdateNotification(nextStep)
            }
            NotificationAction.REPLY -> {
                val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("key_text_reply")
                if (replyText != null) {
                    Toast.makeText(context, "Risposta ricevuta: $replyText (ID: $notificationId)", Toast.LENGTH_LONG).show()
                    val repliedNotification = NotificationCompat.Builder(context, NotificationsHelper.ChannelID.DEMO)
                        .setSmallIcon(R.drawable.ic_notification_actions)
                        .setContentText("Risposta inviata: \"$replyText\"")
                    NotificationsHelper.safeNotify(notificationId, repliedNotification)
                } else {
                    Toast.makeText(context, "Nessun testo nella risposta.", Toast.LENGTH_SHORT).show()
                }
            }
            NotificationAction.MEDIA_PLAY_PAUSE -> {
                Toast.makeText(context, "Azione: Play/Pausa (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Play/Pause Toggled")
                FakeMediaPlayer.togglePlayPause()
                DemoNotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )

            }
            NotificationAction.MEDIA_NEXT -> {
                Toast.makeText(context, "Azione: Successivo (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Next Track")
                FakeMediaPlayer.nextTrack()
                DemoNotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )
            }
            NotificationAction.MEDIA_PREVIOUS -> {
                Toast.makeText(context, "Azione: Precedente (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Previous Track")
                FakeMediaPlayer.previousTrack()
                DemoNotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )
            }
            NotificationAction.MEDIA_STOP -> {

            }
            else -> {
                Log.w("NotificationActionReceiver", "Azione sconosciuta: ${intent.action}")
            }
        }
    }
}