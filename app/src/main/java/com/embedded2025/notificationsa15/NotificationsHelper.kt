package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.app.RemoteInput
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


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
    const val DEMO_PROGRESS_NOTIFICATION_ID = 5
    const val DEMO_LIVE_UPDATE_NOTIFICATION_ID = 6


    private var notificationIdCounter = 1000
    fun getUniqueId(): Int = notificationIdCounter++

    // Context e NotificationManager
    private var appContext: Context? = null
    private fun getAppContext(): Context = appContext!!
    private fun getNotificationManager(): NotificationManager =
        getAppContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Inizializza l'oggetto NotificationsHelper.
     */
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

    /**
     * Pubblica una notifica.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     *
     * @return true se la notifica è stata pubblicata con successo, false altrimenti
     */
    fun safeNotify(id: Int, builder: NotificationCompat.Builder): Boolean {
        if (ActivityCompat.checkSelfPermission(getAppContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("NotificationsHelper", "Permission not granted.")

            return false
        }

        getNotificationManager().notify(id, builder.build())
        return true
    }

    /**
     * Pubblica una notifica demo.
     * Differisce da [safeNotify] in quanto se le notifiche non sono abilitate oppure il canale demo
     * non è visibile, allora l'utente viene reindirizzato alle relative impostazioni di sistema.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     *
     * @return true se la notifica è stata pubblicata con successo, false altrimenti
     *
     * @see safeNotify
     */
    private fun safeNotifyDemo(id: Int, builder: NotificationCompat.Builder): Boolean {
        val ctx = getAppContext()
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

        val manager = getNotificationManager()
        if (manager.getNotificationChannel(DEMO_CHANNEL_ID).importance == NotificationManager.IMPORTANCE_NONE
        ) {
            Log.i("NotificationsHelper", "Notification channel is not visible, opening settings.")

            ctx.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, DEMO_CHANNEL_ID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })

            return false
        }

        manager.notify(id, builder.build())
        return true
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
            .setContentIntent(createContentPendingIntent(R.id.simpleNotificationFragment))
            .setAutoCancel(true)

        safeNotifyDemo(DEMO_SIMPLE_NOTIFICATION_ID, notif)
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
            .setContentIntent(createContentPendingIntent(R.id.expandableNotificationFragment))
            .setAutoCancel(true)

        safeNotifyDemo(DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID, notif)
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
            .setContentIntent(createContentPendingIntent(R.id.expandableNotificationFragment))
            .setAutoCancel(true)

        safeNotifyDemo(DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID, notif)
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
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(ctx.getString(R.string.notif_action_demo_text)))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(R.drawable.ic_archive, ctx.getString(R.string.notif_action_archive), archivePendingIntent)
            .addAction(R.drawable.ic_later, ctx.getString(R.string.notif_action_later), laterPendingIntent)
            .setContentIntent(createContentPendingIntent(R.id.actionsNotificationFragment))
            .setAutoCancel(true)

        safeNotifyDemo(DEMO_ACTIONS_NOTIFICATION_ID, builder)
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(createContentPendingIntent(R.id.replyNotificationFragment))
            .setAutoCancel(true)
            .addAction(action)

        safeNotifyDemo(DEMO_REPLY_NOTIFICATION_ID, builder)
    }

    //Mostra una notifica con barra di progresso
    private val helperJob = SupervisorJob()
    private val helperScope = CoroutineScope(Dispatchers.Default + helperJob)
    fun showProgressNotificationDemo() {
        val ctx = getAppContext()
        val channelForProgress = DEMO_CHANNEL_ID
        val notificationId = DEMO_PROGRESS_NOTIFICATION_ID
        val initialBuilder = NotificationCompat.Builder(ctx, channelForProgress)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.progress_notification_title))
            .setContentText(
                ctx.getString(
                    R.string.notif_progress_demo_det,
                    0
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)
            .setContentIntent(createContentPendingIntent(R.id.progressNotificationFragment))

        if (!safeNotifyDemo(notificationId, initialBuilder)) return
        // Qua la notifica è stata pubblicata con successo
        // Avvia la coroutine per simulare il progresso
        helperScope.launch {
            val maxProgress = 100
            var currentProgress = 0
            try {
                while (currentProgress <= maxProgress && isActive) {
                    val updateBuilder = NotificationCompat.Builder(ctx, channelForProgress)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(ctx.getString(R.string.progress_notification_title))
                        .setContentText(
                            String.format(
                                ctx.getString(R.string.notif_progress_demo_det),
                                currentProgress
                            )
                        )
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setProgress(maxProgress, currentProgress, false)
                        .setContentIntent(createContentPendingIntent(R.id.progressNotificationFragment))
                    if (ActivityCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getNotificationManager().notify(notificationId, updateBuilder.build())
                    } else {
                        Log.w(
                            "ProgressNotification",
                            "Permesso per le notifiche perso durante l'aggiornamento del progresso."
                        )
                        break
                    }

                    delay(500)
                    currentProgress += 5
                }
                if (isActive) {
                    val finalBuilder = NotificationCompat.Builder(ctx, channelForProgress)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(ctx.getString(R.string.progress_notification_title))
                        .setContentText(ctx.getString(R.string.notif_progress_demo_complete))
                        .setOngoing(false)
                        .setOnlyAlertOnce(false)
                        .setProgress(0, 0, false)
                        .setContentIntent(createContentPendingIntent(R.id.progressNotificationFragment))
                        .setAutoCancel(true)
                    if (ActivityCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getNotificationManager().notify(notificationId, finalBuilder.build())
                    }
                } else {
                    Log.i("ProgressNotification", "Operazione di progresso cancellata.")
                    val cancelledBuilder = NotificationCompat.Builder(ctx, channelForProgress)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(ctx.getString(R.string.progress_notification_title))
                        .setContentText("Operazione annullata.")
                        .setOngoing(false)
                        .setProgress(0, 0, false)
                        .setContentIntent(createContentPendingIntent(R.id.progressNotificationFragment))
                        .setAutoCancel(true)
                    if (ActivityCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getNotificationManager().notify(notificationId, cancelledBuilder.build())
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ProgressNotification",
                    "Errore o cancellazione durante l'operazione di progresso",
                    e
                )
                val errorBuilder = NotificationCompat.Builder(ctx, channelForProgress)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(ctx.getString(R.string.progress_notification_title))
                    .setContentText(ctx.getString(R.string.notif_progress_demo_fail))
                    .setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentIntent(createContentPendingIntent(R.id.progressNotificationFragment))
                    .setAutoCancel(true)
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    getNotificationManager().notify(notificationId, errorBuilder.build())
                }
            }
        }
    }

    fun showLiveUpdateNotificationDemo(step : Int){
        val ctx = getAppContext()

        val stepBundle = Bundle().apply {
            putInt("order_step", step)
        }
        val updateIntent = createBroadcastPendingIntent(DEMO_LIVE_UPDATE_NOTIFICATION_ID, "ACTION_NEXT_STEP", currentStep = stepBundle)

        val builder = NotificationCompat.Builder(ctx, DEMO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentText(ctx.getString(R.string.notif_live_update_demo_text))
            .setProgress(3, step + 1, false)
            .setContentIntent(createContentPendingIntent(R.id.liveUpdateNotificationFragment))

        when (step) {
            OrderStatus.ORDER_PLACED -> {
                builder.setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_placed))

                //Passa allo step successivo dopo 10s
                Handler(Looper.getMainLooper()).postDelayed({
                    showLiveUpdateNotificationDemo(OrderStatus.ORDER_ON_THE_WAY)
                }, 10000)
            }
            OrderStatus.ORDER_ON_THE_WAY -> {
                builder
                    .setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_sent))
                    .addAction(R.drawable.ic_later, "Ho già ricevuto l'ordine", updateIntent)
            }
            OrderStatus.ORDER_COMPLETE -> {
                builder
                    .setContentTitle(ctx.getString(R.string.notif_live_update_demo_order_complete))
                    .setProgress(0, 0, false)
                    .setContentText("")
            }
            else -> builder.setContentTitle("Errore")//TODO chiarisci
        }

        safeNotifyDemo(DEMO_LIVE_UPDATE_NOTIFICATION_ID, builder)

    }

    private fun createPendingIntent(notificationId: Int, action: String? = null): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this.action = action
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
    private fun createContentPendingIntent(destination: Int): PendingIntent =
        NavDeepLinkBuilder(getAppContext())
            .setComponentName(ComponentName(getAppContext(), MainActivity::class.java))
            .setGraph(R.navigation.nav_graph)
            .setDestination(destination)
            .createPendingIntent()


    private fun createBroadcastPendingIntent(notificationId: Int, action: String, requestCodeOffset: Int = 0, currentStep : Bundle? = null): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("notification_id", notificationId)
            currentStep?.let { putExtras(it) }
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
            "ACTION_NEXT_STEP" -> {
                val currentStep = intent.getIntExtra("order_step", 0)
                val nextStep = (currentStep + 1).coerceAtMost(OrderStatus.ORDER_COMPLETE)
                NotificationsHelper.showLiveUpdateNotificationDemo(nextStep)
            }
            NotificationsHelper.ACTION_REPLY -> {
                val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(NotificationsHelper.KEY_TEXT_REPLY)
                if (replyText != null) {
                    Toast.makeText(context, "Risposta ricevuta: $replyText (ID: $notificationId)", Toast.LENGTH_LONG).show()
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

object OrderStatus{
    const val ORDER_PLACED = 0
    const val ORDER_ON_THE_WAY = 1
    const val ORDER_COMPLETE = 2


}