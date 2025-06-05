package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.app.RemoteInput



object NotificationsHelper {
    // ID canali
    const val DEMO_CHANNEL_ID = "channel_demo"
    const val DEFAULT_CHANNEL_ID = "channel_default"

    // ID notifiche
    const val DEMO_SIMPLE_NOTIFICATION_ID = 0
    const val DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID = 1
    const val DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID = 2
    const val DEMO_ACTIONS_NOTIFICATION_ID = 3
    const val DEMO_REPLY_NOTIFICATION_ID = 4


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
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getAppContext().getString(R.string.channel_demo_description)
                setShowBadge(true)
            },
            NotificationChannel(DEFAULT_CHANNEL_ID,
                getAppContext().getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getAppContext().getString(R.string.channel_default_description)
                setShowBadge(true)
            }
        )

        getNotificationManager().createNotificationChannels(channels)
    }

    // Pubblica la notifica se l'applicazione ne ha il permesso
    fun safeNotify(id: Int, builder: NotificationCompat.Builder, channelId: String) {
        with(getNotificationManager()) {
            val ctx = getAppContext()
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("NotificationsHelper", "Permission not granted, opening settings.")
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
                return
            } else if (getNotificationManager().getNotificationChannel(channelId).importance
                    == NotificationManager.IMPORTANCE_NONE
            ) {
                Log.i("NotificationsHelper", "Notification channel is not visible, opening settings.")
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
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

        safeNotify(DEMO_SIMPLE_NOTIFICATION_ID, notif, DEMO_CHANNEL_ID)
    }

    // Mostra una notifica espandibile con testo
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

        safeNotify(DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID, notif, DEMO_CHANNEL_ID)
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

        safeNotify(DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID, notif, DEMO_CHANNEL_ID)
    }

    // Mostra una notifica con azioni
    fun showActionNotificationDemo() {
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
            .addAction(R.drawable.ic_archive, ctx.getString(R.string.notif_action_archive), archivePendingIntent)
            .addAction(R.drawable.ic_later, ctx.getString(R.string.notif_action_later), laterPendingIntent)

        safeNotify(DEMO_ACTIONS_NOTIFICATION_ID, builder, DEMO_CHANNEL_ID)
    }

    //Mostra una notifica di risposta
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY = "com.embedded2025.notificationsa15.ACTION_REPLY" //Nome completo per prevenire conflitti con altre azioni
    fun showReplyNotificationDemo() {
        val ctx = getAppContext()
        val channelForReply = DEMO_CHANNEL_ID
        val replyLabel = ctx.getString(R.string.notif_reply_demo_label)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(replyLabel)
            build()
        }
        val replyIntent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra("notification_id", DEMO_REPLY_NOTIFICATION_ID)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            ctx,
            DEMO_REPLY_NOTIFICATION_ID + 3,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_reply_icon,
            ctx.getString(R.string.notif_reply_demo_action),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()
        val builder = NotificationCompat.Builder(ctx, channelForReply)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_reply_demo_title))
            .setContentText(ctx.getString(R.string.notif_reply_demo_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)

        safeNotify(DEMO_REPLY_NOTIFICATION_ID, builder, channelForReply)

    }

    private fun createPendingIntent(notificationId: Int, action: String? = null): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this.action = action // L'action sull'intent per l'Activity può essere utile se MainActivity deve comportarsi diversamente
            putExtra("notification_id", notificationId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )
    }

    private fun createBroadcastPendingIntent(notificationId: Int, action: String, requestCodeOffset: Int = 0): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("notification_id", notificationId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + requestCodeOffset,
            intent,
            flags
        )
    }
}

// Classe per gestire le azioni delle notifiche
class NotificationActionReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
            NotificationsHelper.ACTION_REPLY -> {
                val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(NotificationsHelper.KEY_TEXT_REPLY)
                if (replyText != null) {
                    Toast.makeText(context, "Risposta ricevuta: $replyText (ID: $notificationId)", Toast.LENGTH_LONG).show()
                    // Qui puoi:
                    // 1. Salvare la risposta
                    // 2. Inviarla a un server
                    // 3. Aggiornare la notifica originale
                    val notificationManager = NotificationManagerCompat.from(context)
                    val repliedNotification = NotificationCompat.Builder(context, NotificationsHelper.DEMO_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_actions)
                        .setContentText("Risposta inviata: \"$replyText\"")
                        .build()
                    notificationManager.notify(notificationId, repliedNotification)
                } else {
                    Toast.makeText(context, "Nessun testo nella risposta.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}