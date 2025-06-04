package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap



object NotificationsHelper{
    const val DEMO_CHANNEL_ID = "channel_demo"
    const val DEMO_SIMPLE_NOTIFICATION_ID = 1
    const val DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID = 2
    const val DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID = 3

    private var appContext: Context? = null
    private var internalNotificationManager: NotificationManagerCompat? = null

    // ID dei canali di notifica
    const val CHANNEL_ID_DEFAULT = "channel_default"
    const val CHANNEL_ID_HIGH_PRIORITY = "channel_high_priority"

    private var notificationIdCounter = 1000
    fun getNextNotificationId(): Int = notificationIdCounter++

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            internalNotificationManager = NotificationManagerCompat.from(appContext!!)
            createNotificationChannels(appContext!!)
        }
    }

    private fun getContext(): Context {
        return appContext ?: throw IllegalStateException("NotificationHelper not initialized. Call initialize(context) first.")
    }

    private fun getManager(): NotificationManagerCompat {
        return internalNotificationManager ?: throw IllegalStateException("NotificationHelper not initialized.")
    }

    fun requestPostPermission(ctx: Context): Boolean {
        // TODO: da implementare

        return false
    }

    private fun createPendingIntent(notificationId: Int, action: String? = null): PendingIntent {
        val context = getContext()
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
        val context = getContext()
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

     fun createNotificationChannels(ctx: Context) {
        val systemNotificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val defaultChannel = NotificationChannel(
            CHANNEL_ID_DEFAULT,
            ctx.getString(R.string.channel_name_default),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ctx.getString(R.string.channel_description_default)
            setShowBadge(true)
        }
        systemNotificationManager.createNotificationChannel(defaultChannel)

        val highPriorityChannel = NotificationChannel(
            CHANNEL_ID_HIGH_PRIORITY,
            ctx.getString(R.string.channel_name_high_priority),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ctx.getString(R.string.channel_description_high_priority)
            setShowBadge(true)
        }
        systemNotificationManager.createNotificationChannel(highPriorityChannel)
    }

    fun safeNotify(ctx: Context, builder: NotificationCompat.Builder, id: Int) {
        with(NotificationManagerCompat.from(ctx)) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_DENIED) {
                Log.i("NotificationsHelper", "No permission granted, requesting now.")
                if (!requestPostPermission(ctx)) return
            }
            notify(id, builder.build())
        }
    }

    //Simple notification
    fun showSimpleNotificationDemo() {
        val ctx = getContext()
        val notificationId = getNextNotificationId()
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_simple_demo_title))
            .setContentText(ctx.getString(R.string.notif_simple_demo_text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(ctx, notif, notificationId)
    }

    //Expand notification with text
    fun showExpandableTextNotificationDemo() {
        val ctx = getContext()
        val notificationId = getNextNotificationId()
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_expandable_demo_title))
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(ctx.getString(R.string.notif_expandable_demo_bigtext)))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(ctx, notif, notificationId)
    }

    //Expand notification with picture
    fun showExpandablePictureNotificationDemo() {
        val ctx = getContext()
        val notificationId = getNextNotificationId()
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_expandable_demo_title))
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(getDrawable(ctx, R.drawable.project_logo)?.toBitmap()))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(ctx, notif, notificationId)
    }

    //Actions notification
    fun showNotificationWithActions() {
        val ctx = getContext()
        val notificationId = getNextNotificationId()
        val archivePendingIntent = createBroadcastPendingIntent(notificationId, "ACTION_ARCHIVE", 1)
        val laterPendingIntent = createBroadcastPendingIntent(notificationId, "ACTION_LATER", 2)
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT)
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
        safeNotify(ctx, builder, notificationId)
    }

}

//Classe per gestire le azioni delle notifiche
class NotificationActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val action = intent.action

        when (action) {
            "ACTION_ARCHIVE" -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                android.widget.Toast.makeText(context, "Azione: Archiviato (ID: $notificationId)", android.widget.Toast.LENGTH_SHORT).show()
            }
            "ACTION_LATER" -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                android.widget.Toast.makeText(context, "Azione: Più tardi (ID: $notificationId)", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}