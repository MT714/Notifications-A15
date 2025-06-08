package com.embedded2025.notificationsa15

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.NotificationsHelper
import com.embedded2025.notificationsa15.utils.PendingIntentHelper

class NotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private var progressJob: Job? = null
    private var liveUpdateJob: Job? = null

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "NotificationService"

        // Azioni
        const val ACTION_START_PROGRESS = "com.embedded2025.notificationsa15.ACTION_START_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.embedded2025.notificationsa15.ACTION_CANCEL_PROGRESS"
        const val ACTION_START_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_START_LIVE_UPDATE"
        const val ACTION_ADVANCE_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_ADVANCE_LIVE_UPDATE"

        // ID
        const val PROGRESS_CHANNEL_ID = NotificationsHelper.ChannelID.DEMO
        const val PROGRESS_NOTIFICATION_ID = DemoNotificationsHelper.NotificationID.PROGRESS
        const val LIVE_UPDATE_NOTIFICATION_ID = DemoNotificationsHelper.NotificationID.LIVE_UPDATE

        const val EXTRA_LIVE_UPDATE_STEP = "extra_live_update_step"

        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply { action = ACTION_START_PROGRESS }
        }

        fun getStartLiveUpdateIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply { action = ACTION_START_LIVE_UPDATE }
        }

        private fun getAdvanceLiveUpdateIntent(context: Context, currentStep: Int): Intent {
            return Intent(context, NotificationService::class.java).apply {
                action = ACTION_ADVANCE_LIVE_UPDATE
                putExtra(EXTRA_LIVE_UPDATE_STEP, currentStep)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        Log.d(TAG, "Servizio creato.")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_PROGRESS -> startForegroundWithProgress()
            ACTION_CANCEL_PROGRESS -> handleProgressCancellation("Azione di cancellazione utente")
            ACTION_START_LIVE_UPDATE -> startLiveUpdateFlow()
            ACTION_ADVANCE_LIVE_UPDATE -> {
                val currentStep = intent.getIntExtra(EXTRA_LIVE_UPDATE_STEP, 0)
                handleLiveUpdateAdvancement(currentStep)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithProgress() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            val initialNotification =
                buildProgressNotification(0, 100, false, "Avvio operazione...")
            if (ActivityCompat.checkSelfPermission(
                    this@NotificationService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permesso negato, impossibile avviare progresso.")
                return@launch
            }
            startForeground(PROGRESS_NOTIFICATION_ID, initialNotification)
            Log.d(TAG, "Servizio in foreground per PROGRESSO.")

            var currentProgress = 0
            val maxProgress = 100
            while (currentProgress <= maxProgress && isActive) {
                val progressText = getString(R.string.notif_progress_demo_det, currentProgress)
                val notificationUpdate =
                    buildProgressNotification(currentProgress, maxProgress, false, progressText)
                notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationUpdate)
                delay(500)
                currentProgress += 5
            }
            if (isActive) {
                val finalNotification =
                    buildFinalProgressNotification(getString(R.string.notif_progress_demo_complete))
                notificationManager.notify(PROGRESS_NOTIFICATION_ID, finalNotification)
                handler.postDelayed({checkAndStopSelf()},200)
            }
        }
    }

    private fun handleProgressCancellation(reason: String) {
        progressJob?.cancel()
        Log.i(TAG, "Gestione cancellazione progresso: $reason")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val cancelledNotification = buildFinalProgressNotification(getString(R.string.notif_progress_demo_cancelled))
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, cancelledNotification)
        }
        checkAndStopSelf()
    }

    private fun buildProgressNotification(progress: Int, max: Int, indeterminate: Boolean, contentText: String): Notification {
        val pendingContentIntent = PendingIntentHelper.createWithDestination(R.id.progressNotificationFragment)
        val cancelIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_CANCEL_PROGRESS }
        val pendingCancelIntent = PendingIntent.getService(this, 101, cancelIntent, getPendingIntentFlags())
        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(getString(R.string.progress_notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, indeterminate)
            .setContentIntent(pendingContentIntent)
            .addAction(R.drawable.ic_cancel, getString(R.string.action_cancel), pendingCancelIntent).build()
    }

    private fun buildFinalProgressNotification(contentText: String): Notification {
        val pendingContentIntent = PendingIntentHelper.createWithDestination(R.id.progressNotificationFragment)
        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(getString(R.string.progress_notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true).build()
    }

    private fun checkAndStopSelf() {
        if (progressJob?.isActive != true && liveUpdateJob?.isActive != true) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        }
    }

    private fun startLiveUpdateFlow() {
        liveUpdateJob?.cancel()
        liveUpdateJob = serviceScope.launch {
            val initialNotification = buildLiveUpdateNotification(DemoNotificationsHelper.OrderStatus.ORDER_PLACED)
            startForeground(LIVE_UPDATE_NOTIFICATION_ID, initialNotification)
            Log.d(TAG, "Servizio in foreground per LIVE UPDATE.")

            delay(10000)

            if (isActive) {
                val onTheWayNotification = buildLiveUpdateNotification(DemoNotificationsHelper.OrderStatus.ORDER_ON_THE_WAY)
                notificationManager.notify(LIVE_UPDATE_NOTIFICATION_ID, onTheWayNotification)
            }
        }
    }

    private fun handleLiveUpdateAdvancement(currentStep: Int) {
        liveUpdateJob?.cancel()
        val nextStep = (currentStep + 1).coerceAtMost(DemoNotificationsHelper.OrderStatus.ORDER_COMPLETE)
        val finalNotification = buildLiveUpdateNotification(nextStep)
        notificationManager.notify(LIVE_UPDATE_NOTIFICATION_ID, finalNotification)

        if (nextStep == DemoNotificationsHelper.OrderStatus.ORDER_COMPLETE) {
            handler.postDelayed({
                stopForeground(STOP_FOREGROUND_REMOVE)
                checkAndStopSelf()
            }, 4000)
        }
    }

    private fun buildLiveUpdateNotification(step: Int): Notification {
        val pendingContentIntent = PendingIntentHelper.createWithDestination(R.id.liveUpdateNotificationFragment)
        val builder = NotificationCompat.Builder(this, NotificationsHelper.ChannelID.DEMO)
            .setSmallIcon(R.drawable.ic_notification_actions)
            .setContentIntent(pendingContentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentText(getString(R.string.notif_live_update_demo_text))

        when (step) {
            DemoNotificationsHelper.OrderStatus.ORDER_PLACED -> {
                builder
                    .setContentTitle(getString(R.string.notif_live_update_demo_order_placed))
                    .setProgress(3, step + 1, false)
            }
            DemoNotificationsHelper.OrderStatus.ORDER_ON_THE_WAY -> {
                val advanceIntent = getAdvanceLiveUpdateIntent(this, step)
                val pendingAdvanceIntent = PendingIntent.getService(this, 201, advanceIntent, getPendingIntentFlags())
                builder
                    .setContentTitle(getString(R.string.notif_live_update_demo_order_sent))
                    .setProgress(3, step + 1, false)
                    .addAction(R.drawable.ic_later, "Ho già ricevuto l'ordine", pendingAdvanceIntent)
            }
            DemoNotificationsHelper.OrderStatus.ORDER_COMPLETE -> {
                builder
                    .setContentTitle(getString(R.string.notif_live_update_demo_order_complete))
                    .setProgress(0, 0, false)
                    .setContentText("")
                    .setOngoing(false)
                    .setAutoCancel(true)
            }
        }
        return builder.build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(PROGRESS_CHANNEL_ID, getString(R.string.channel_demo_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannels(channels)
    }
    private fun getPendingIntentFlags(mutable: Boolean = false): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = if (mutable) flags or PendingIntent.FLAG_MUTABLE else flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
