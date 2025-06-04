package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap



object NotificationsHelper {
    // ID canali
    const val DEMO_CHANNEL_ID = "channel_demo"
    const val DEFAULT_CHANNEL_ID = "channel_default"

    // ID notifiche
    const val DEMO_SIMPLE_NOTIFICATION_ID = 0
    const val DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID = 1
    const val DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID = 2
    const val DEMO_ACTIONS_NOTIFICATION_ID = 3

    private var notificationIdCounter = 1000
    fun getUniqueId(): Int = notificationIdCounter++

    // Context e NotificationManager
    private var appContext: Context? = null
    private fun getAppContext(): Context = appContext!!
    private fun getNotificationManager(): NotificationManager =
        getAppContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Assegna il contesto dell'applicazione e crea i canali di notifica
    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Create channels
        val channels = listOf<NotificationChannel>(
            NotificationChannel(DEMO_CHANNEL_ID,
                getAppContext().getString(R.string.channel_demo_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getAppContext().getString(R.string.channel_demo_description)
                setShowBadge(true)
            },
            NotificationChannel(DEFAULT_CHANNEL_ID,
                getAppContext().getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getAppContext().getString(R.string.channel_default_description)
                setShowBadge(true)
            }
        )

        getNotificationManager().createNotificationChannels(channels)
    }

    // Pubblica la notifica se l'applicazione ne ha il permesso
    fun safeNotify(id: Int, builder: NotificationCompat.Builder) {
        with(getNotificationManager()) {
            if (ActivityCompat.checkSelfPermission(getAppContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("NotificationsHelper", "No permission granted, requesting now.")
                return
            }
            notify(id, builder.build())
        }
    }

    // Mostra una notifica semplice
    fun showSimpleNotificationDemo() {
        val ctx = getAppContext()
        val notif = NotificationCompat.Builder(ctx, DEMO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_simple_demo_title))
            .setContentText(ctx.getString(R.string.notif_simple_demo_text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(DEMO_SIMPLE_NOTIFICATION_ID, notif)
    }

    // Mostra una notifica espandibile con test
    fun showExpandableTextNotificationDemo() {
        val ctx = getAppContext()
        val notif = NotificationCompat.Builder(ctx, DEMO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_expandable_demo_title))
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(ctx.getString(R.string.notif_expandable_demo_bigtext)))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID, notif)
    }

    // Mostra una notifica espandibile con immagine
    fun showExpandablePictureNotificationDemo() {
        val ctx = getAppContext()
        val notif = NotificationCompat.Builder(ctx, DEMO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_expandable_demo_title))
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(getDrawable(ctx, R.drawable.project_logo)?.toBitmap()))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID, notif)
    }

    // Mostra una notifica con azioni
    fun showNotificationWithActions() {
        val ctx = getAppContext()
        val archivePendingIntent = createBroadcastPendingIntent(DEMO_ACTIONS_NOTIFICATION_ID, "ACTION_ARCHIVE", 1)
        val laterPendingIntent = createBroadcastPendingIntent(DEMO_ACTIONS_NOTIFICATION_ID, "ACTION_LATER", 2)
        val builder = NotificationCompat.Builder(ctx, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_action_demo_title))
            .setContentText(ctx.getString(R.string.notif_action_demo_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(ctx.getString(R.string.notif_action_demo_text)))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(R.drawable.ic_archive, ctx.getString(R.string.notification_action_archive), archivePendingIntent)
            .addAction(R.drawable.ic_later, ctx.getString(R.string.notification_action_later), laterPendingIntent)

        safeNotify(DEMO_ACTIONS_NOTIFICATION_ID, builder)
    }


    private fun createPendingIntent(notificationId: Int, action: String? = null): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this.action = action
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createBroadcastPendingIntent(notificationId: Int, action: String, requestCodeOffset: Int = 0): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + requestCodeOffset,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

// Classe per gestire le azioni delle notifiche
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val action = intent.action

        when (action) {
            "ACTION_ARCHIVE" -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                Toast.makeText(context, "Azione: Archiviato (ID: $notificationId)", Toast.LENGTH_SHORT).show()
            }
            "ACTION_LATER" -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                Toast.makeText(context, "Azione: Più tardi (ID: $notificationId)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}