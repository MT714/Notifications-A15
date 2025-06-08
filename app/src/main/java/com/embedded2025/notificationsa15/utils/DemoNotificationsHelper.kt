package com.embedded2025.notificationsa15.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.toBitmap
import com.embedded2025.notificationsa15.NotificationActionReceiver.NotificationAction
import com.embedded2025.notificationsa15.NotificationActionReceiver.IntentExtras
import com.embedded2025.notificationsa15.NotificationService
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationsHelper.ctx
import com.embedded2025.notificationsa15.utils.NotificationsHelper.setBigPicture
import com.embedded2025.notificationsa15.utils.NotificationsHelper.setBigText
import com.embedded2025.notificationsa15.utils.NotificationsHelper.ChannelID

object DemoNotificationsHelper {
     object NotificationID {
        const val SIMPLE = 0
        const val EXPANDABLE_TEXT = 1
        const val EXPANDABLE_PICTURE = 2
        const val ACTIONS = 3
        const val REPLY = 4
        const val PROGRESS = 5
        const val LIVE_UPDATE = 6
        const val MEDIA_PLAYER = 7
    }

    object OrderStatus{
        const val ORDER_PLACED = 0
        const val ORDER_ON_THE_WAY = 1
        const val ORDER_COMPLETE = 2
    }

    /**
     * Pubblica una notifica demo.
     * Differisce da [NotificationsHelper.safeNotify] in quanto se le notifiche non sono abilitate oppure il canale demo
     * non è visibile, allora l'utente viene reindirizzato alle relative impostazioni di sistema.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     *
     * @return true se la notifica è stata pubblicata con successo, false altrimenti
     *
     * @see NotificationsHelper.safeNotify
     */
    private fun safeNotifyDemo(id: Int, builder: NotificationCompat.Builder): Boolean {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("NotificationsHelper", "Permission not granted, opening settings.")
            ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })

            return false
        }
        val notifManager = NotificationsHelper.notifManager
        if (notifManager.getNotificationChannel(ChannelID.DEMO).importance == NotificationManager.IMPORTANCE_NONE
        ) {
            Log.i("NotificationsHelper", "Notification channel is not visible, opening settings.")

            ctx.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, ChannelID.DEMO)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        notifManager.notify(id, builder.build())
        return true
    }

    /**
     * Crea e pubblica una notifica semplice di demo.
     */
    fun showSimpleNotification() {
        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            ctx.getString(R.string.notif_simple_demo_title),
            ctx.getString(R.string.notif_simple_demo_text),
            R.id.simpleNotificationFragment,
        )
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.SIMPLE, notif)
    }

    /**
     * Crea e pubblica una notifica espandibile con testo di demo.
     */
    fun showExpandableTextNotification() {
        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            ctx.getString(R.string.notif_expandable_demo_title),
            ctx.getString(R.string.notif_expandable_demo_text),
            R.id.expandableNotificationFragment,
        )
            .setBigText(ctx.getString(R.string.notif_expandable_demo_text))
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.EXPANDABLE_TEXT, notif)
    }

    /**
     * Crea e pubblica una notifica espandibile con immagine di demo.
     */
    fun showExpandablePictureNotification() {
        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            ctx.getString(R.string.notif_expandable_demo_title),
            ctx.getString(R.string.notif_expandable_demo_text),
            R.id.expandableNotificationFragment,
        )
            .setBigPicture(getDrawable(ctx, R.drawable.project_logo)?.toBitmap())
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.EXPANDABLE_PICTURE, notif)
    }

    /**
     * Crea e pubblica una notifica con azioni di demo.
     */
    fun showActionNotification() {
        val extras = Bundle().apply {
            putInt(IntentExtras.NOTIFICATION_ID, NotificationID.ACTIONS)
        }
        val archivePendingIntent = PendingIntentHelper.createBroadcast(NotificationAction.ARCHIVE, extras)
        val laterPendingIntent = PendingIntentHelper.createBroadcast(NotificationAction.LATER, extras)
        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            ctx.getString(R.string.notif_action_demo_title),
            ctx.getString(R.string.notif_action_demo_text),
            R.id.actionsNotificationFragment
        )
            .addAction(R.drawable.ic_archive, ctx.getString(R.string.notif_action_archive), archivePendingIntent)
            .addAction(R.drawable.ic_later, ctx.getString(R.string.notif_action_later), laterPendingIntent)
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.ACTIONS, notif)
    }

    /**
     * Crea e pubblica una notifica di risposta di demo.
     */
    fun showReplyNotification() {
        val remoteInput = RemoteInput.Builder(IntentExtras.KEY_TEXT_REPLY)
            .setLabel(ctx.getString(R.string.notif_reply_demo_label))
            .build()

        val replyAction = Action.Builder(
            R.drawable.ic_reply_icon,
            ctx.getString(R.string.notif_reply_demo_action),
            PendingIntentHelper.createBroadcast(NotificationAction.REPLY)
        )
            .addRemoteInput(remoteInput)
            .build()

        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            ctx.getString(R.string.notif_reply_demo_title),
            ctx.getString(R.string.notif_reply_demo_text),
            R.id.replyNotificationFragment
        )
            .addAction(replyAction)
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.REPLY, notif)
    }

    //Mostra una notifica con barra di progresso
    fun showProgressNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationsHelper", "Permesso POST_NOTIFICATIONS non concesso. Non avvio il servizio di progresso.")
            Toast.makeText(ctx, ctx.getString(R.string.notif_permission_required_service), Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = NotificationService.getStartProgressIntent(context)
        ctx.startForegroundService(serviceIntent)
        Log.d("NotificationsHelper", "Richiesta di avvio NotificationService per progresso inviata.")
    }

    fun showLiveUpdateNotification(step : Int) {
        val extras = Bundle().apply {
            putInt(IntentExtras.ORDER_STEP, step)
        }
        val updateIntent = PendingIntentHelper.createBroadcast(NotificationAction.NEXT_STEP, extras)

        val notif = NotificationsHelper.createBasicNotificationBuilder(
            ChannelID.DEMO,
            "",
            ctx.getString(R.string.notif_live_update_demo_text),
            R.id.liveUpdateNotificationFragment
        )
            .setProgress(3, step+1, false)

        when (step) {
            OrderStatus.ORDER_PLACED -> {
                notif.setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_placed))

                //Passa allo step successivo dopo 10s
                Handler(Looper.getMainLooper()).postDelayed({
                    showLiveUpdateNotification(OrderStatus.ORDER_ON_THE_WAY)
                }, 10000)
            }
            OrderStatus.ORDER_ON_THE_WAY -> {
                notif
                    .setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_sent))
                    .addAction(R.drawable.ic_later, "Ho già ricevuto l'ordine", updateIntent)
            }
            OrderStatus.ORDER_COMPLETE -> {
                notif
                    .setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_complete))
                    .setProgress(0, 0, false)
                    .setContentText("")
            }
            else -> notif.setContentTitle("Errore") // TODO: chiarisci
        }

        safeNotifyDemo(NotificationID.LIVE_UPDATE, notif)
    }
}