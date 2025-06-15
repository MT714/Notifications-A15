package com.embedded2025.notificationsa15.services

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.ChannelID
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.NotificationHelper.setDestinationFragment
import com.embedded2025.notificationsa15.utils.NotificationID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servizio dedicato alla notifica con barra di progresso.
 */
class ProgressService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var progressJob: Job? = null

    @Volatile private var isCancellationRequested = false

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "ProgressService"

        // Azioni per Progress
        const val ACTION_START_PROGRESS = "com.embedded2025.notificationsa15.ACTION_START_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.embedded2025.notificationsa15.ACTION_CANCEL_PROGRESS"

        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, ProgressService::class.java).apply { action = ACTION_START_PROGRESS }
        }

    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "Servizio creato.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_PROGRESS -> startForegroundWithProgress()
            ACTION_CANCEL_PROGRESS -> handleProgressCancellation()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithProgress() {
        progressJob?.cancel()
        isCancellationRequested = false

        progressJob = serviceScope.launch {
            val initialNotification =
                buildProgressNotification(0, getString(R.string.notif_progress_demo_start))
            if (ActivityCompat.checkSelfPermission(
                    this@ProgressService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permesso negato, impossibile avviare progresso.")
                return@launch
            }
            startForeground(NotificationID.PROGRESS, initialNotification)
            Log.d(TAG, "Servizio in foreground per PROGRESSO.")

            var currentProgress = 0
            while (currentProgress <= 100 && !isCancellationRequested) {
                val progressText = getString(R.string.notif_progress_demo_det, currentProgress)
                val notificationUpdate =
                    buildProgressNotification(currentProgress, progressText)
                notificationManager.notify(NotificationID.PROGRESS, notificationUpdate)
                delay(500)
                currentProgress += 5
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            if (isCancellationRequested) {
                val cancelledNotification = buildFinalProgressNotification(getString(R.string.notif_progress_demo_cancelled))
                notificationManager.notify(NotificationID.PROGRESS, cancelledNotification)
            } else {
                val finalNotification =
                    buildFinalProgressNotification(getString(R.string.notif_progress_demo_complete))
                notificationManager.notify(NotificationID.PROGRESS, finalNotification)
            }

            delay(100)
            checkAndStopSelf()
        }
    }

    private fun buildProgressNotification(progress: Int, contentText: String): Notification {
        val cancelIntent = Intent(this, ProgressService::class.java).apply { action =
            ACTION_CANCEL_PROGRESS
        }
        val pendingCancelIntent = PendingIntent.getService(this, 101, cancelIntent, getPendingIntentFlags())
        return NotificationHelper.createBasicBuilder(
            ChannelID.SERVICES,
            R.drawable.ic_progress,
            getString(R.string.progress_notification_title)
        )
            .setContentText(contentText)
            .setDestinationFragment(R.id.progressNotificationFragment)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .addAction(R.drawable.ic_cancel, getString(R.string.action_cancel), pendingCancelIntent)
            .build()
    }

    private fun getPendingIntentFlags(mutable: Boolean = false): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = if (mutable) flags or PendingIntent.FLAG_MUTABLE else flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    private fun buildFinalProgressNotification(contentText: String): Notification {
        return NotificationHelper.createBasicBuilder(
            ChannelID.SERVICES,
            R.drawable.ic_progress,
            getString(R.string.progress_notification_title)
        )
            .setContentText(contentText)
            .setDestinationFragment(R.id.progressNotificationFragment)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .build()
    }

    private fun handleProgressCancellation() {
        Log.i(TAG, "Gestione cancellazione utente")
        isCancellationRequested = true
    }

    private fun checkAndStopSelf() {
        if (progressJob?.isActive != true) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}