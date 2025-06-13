package com.embedded2025.notificationsa15

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.embedded2025.notificationsa15.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.media.AudioManager
import com.embedded2025.notificationsa15.utils.NotificationHelper.setDestinationFragment
import com.embedded2025.notificationsa15.utils.NotificationID
import com.embedded2025.notificationsa15.utils.ChannelID

class NotificationService : Service() {
    object OrderStatus{
        const val ORDER_PLACED = 0
        const val ORDER_ON_THE_WAY = 1
        const val ORDER_COMPLETE = 2
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    @Volatile private var isCancellationRequested = false

    private var progressJob: Job? = null
    private var liveUpdateJob: Job? = null
    private var ringtone: Ringtone? = null

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "NotificationService"

        // Azioni per Progress
        const val ACTION_START_PROGRESS = "com.embedded2025.notificationsa15.ACTION_START_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.embedded2025.notificationsa15.ACTION_CANCEL_PROGRESS"

        // Azioni per Live Update
        const val ACTION_START_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_START_LIVE_UPDATE"
        const val ACTION_ADVANCE_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_ADVANCE_LIVE_UPDATE"

        // Azioni per Call Notification
        const val ACTION_START_CALL = "com.embedded2025.notificationsa15.ACTION_START_CALL"
        const val ACTION_ANSWER_CALL = "com.embedded2025.notificationsa15.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.embedded2025.notificationsa15.ACTION_DECLINE_CALL"

        const val EXTRA_LIVE_UPDATE_STEP = "extra_live_update_step"
        const val EXTRA_CALL_DELAY_SECONDS = "extra_call_delay_seconds"

        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply { action = ACTION_START_PROGRESS }
        }

        fun getStartLiveUpdateIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply { action = ACTION_START_LIVE_UPDATE }
        }

        fun getStartCallIntent(context: Context, delayInSeconds: Int): Intent {
            return Intent(context, NotificationService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CALL_DELAY_SECONDS, delayInSeconds)
            }
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
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "Servizio creato.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_PROGRESS -> startForegroundWithProgress()
            ACTION_CANCEL_PROGRESS -> handleProgressCancellation()
            ACTION_START_LIVE_UPDATE -> startLiveUpdateFlow()
            ACTION_ADVANCE_LIVE_UPDATE -> {
                val currentStep = intent.getIntExtra(EXTRA_LIVE_UPDATE_STEP, 0)
                handleLiveUpdateAdvancement(currentStep)
            }
            ACTION_START_CALL -> {
                val delay = intent.getIntExtra(EXTRA_CALL_DELAY_SECONDS, 0)
                scheduleCallNotification(delay)
            }
            ACTION_ANSWER_CALL -> handleCallAnswer()
            ACTION_DECLINE_CALL -> handleCallDecline()
        }
        return START_NOT_STICKY
    }

    private fun startLiveUpdateFlow() {
        liveUpdateJob?.cancel()
        liveUpdateJob = serviceScope.launch {
            val initialNotification = buildLiveUpdateNotification(OrderStatus.ORDER_PLACED)
            startForeground(NotificationID.LIVE_UPDATE, initialNotification)
            Log.d(TAG, "Servizio in foreground per LIVE UPDATE.")

            delay(10000)

            if (isActive) {
                val onTheWayNotification = buildLiveUpdateNotification(OrderStatus.ORDER_ON_THE_WAY)
                notificationManager.notify(NotificationID.LIVE_UPDATE, onTheWayNotification)
            }

            delay(10000)

            if (isActive) {
                val completeNotification = buildLiveUpdateNotification(OrderStatus.ORDER_COMPLETE)
                notificationManager.notify(NotificationID.LIVE_UPDATE, completeNotification)
            }
        }
    }

    private fun handleLiveUpdateAdvancement(currentStep: Int) {
        liveUpdateJob?.cancel()
        val nextStep = (currentStep + 1).coerceAtMost(OrderStatus.ORDER_COMPLETE)
        val finalNotification = buildLiveUpdateNotification(nextStep)
        notificationManager.notify(NotificationID.LIVE_UPDATE, finalNotification)

        if (nextStep == OrderStatus.ORDER_COMPLETE) {
            handler.postDelayed({
                stopForeground(STOP_FOREGROUND_REMOVE)
                checkAndStopSelf()
            }, 4000)
        }
    }

    private fun buildLiveUpdateNotification(step: Int): Notification {
        val builder = NotificationHelper.createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_live,
            getString(R.string.notif_live_update_demo_title)
        )
            .setDestinationFragment(R.id.progressNotificationFragment)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentText(getString(R.string.notif_live_update_demo_text))

        when (step) {
            OrderStatus.ORDER_PLACED -> {
                builder
                    .setContentTitle(getString(R.string.notif_live_update_demo_order_placed))
                    .setProgress(3, 1, false)
            }
            OrderStatus.ORDER_ON_THE_WAY -> {
                val advanceIntent = getAdvanceLiveUpdateIntent(this, step)
                val pendingAdvanceIntent = PendingIntent.getService(this, 201, advanceIntent, getPendingIntentFlags())
                builder
                    .setContentTitle(getString(R.string.notif_live_update_demo_order_sent))
                    .setProgress(3, 2, false)
                    .addAction(R.drawable.ic_live, "Ho già ricevuto l'ordine", pendingAdvanceIntent)
            }
            OrderStatus.ORDER_COMPLETE -> {
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

    private fun startForegroundWithProgress() {
        progressJob?.cancel()
        isCancellationRequested = false

        progressJob = serviceScope.launch {
            val initialNotification =
                buildProgressNotification(0, "Avvio operazione…")
            if (ActivityCompat.checkSelfPermission(
                    this@NotificationService,
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

            stopForeground(STOP_FOREGROUND_DETACH)
            if (isCancellationRequested) {
                val cancelledNotification = buildFinalProgressNotification(getString(R.string.notif_progress_demo_cancelled))
                notificationManager.notify(NotificationID.PROGRESS, cancelledNotification)
            } else {
                val finalNotification =
                    buildFinalProgressNotification(getString(R.string.notif_progress_demo_complete))
                notificationManager.notify(NotificationID.PROGRESS, finalNotification)
            }
            checkAndStopSelf()
        }
    }

    private fun handleProgressCancellation() {
        Log.i(TAG, "Gestione cancellazione utente")
        isCancellationRequested = true
    }

    private fun buildProgressNotification(progress: Int, contentText: String): Notification {
        val cancelIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_CANCEL_PROGRESS }
        val pendingCancelIntent = PendingIntent.getService(this, 101, cancelIntent, getPendingIntentFlags())
        return NotificationHelper.createBasicBuilder(
            ChannelID.DEMO,
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

    private fun buildFinalProgressNotification(contentText: String): Notification {
        return NotificationHelper.createBasicBuilder(
            ChannelID.DEMO,
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

    private fun scheduleCallNotification(delayInSeconds: Int) {
        serviceScope.launch {
            delay(delayInSeconds * 1000L)
            if (isActive) {
                buildCallNotification()
            }
        }
    }
    private fun buildCallNotification() {
        val callerName = "Mario Rossi"

        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val answerIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_ANSWER_CALL }
        val answerPendingIntent = PendingIntent.getService(this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_DECLINE_CALL }
        val declinePendingIntent = PendingIntent.getService(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val caller = Person.Builder()
            .setName(callerName)
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_call))
            .build()

        val notification = NotificationCompat.Builder(this, ChannelID.CALLS)
            .setSmallIcon(R.drawable.ic_call)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent))
            .setContentTitle("Chiamata in arrivo")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDestinationFragment(R.id.callNotificationFragment)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        try {
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    // Suoneria e vibrazione
                    val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.isLooping = true
                    }
                    ringtone?.play()

                    @Suppress("DEPRECATION")
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    val vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                    val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, 0)
                    vibrator.vibrate(vibrationEffect)
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    @Suppress("DEPRECATION")
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    val vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                    val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, 0)
                    vibrator.vibrate(vibrationEffect)
                }
                AudioManager.RINGER_MODE_SILENT -> {
                    // Niente suoneria né vibrazione
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante la riproduzione della suoneria", e)
        }

        startForeground(NotificationID.CALL, notification)
    }

    private fun handleCallAnswer() {
        stopRingtoneAndVibration()
        Toast.makeText(this, "Risposto", Toast.LENGTH_SHORT).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleCallDecline() {
        stopRingtoneAndVibration()
        Toast.makeText(this, "Chiamata rifiutata", Toast.LENGTH_SHORT).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun checkAndStopSelf() {
        if (progressJob?.isActive != true && liveUpdateJob?.isActive != true) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        }
    }

    private fun stopRingtoneAndVibration() {
        ringtone?.stop()
        ringtone = null

        @Suppress("DEPRECATION")
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    private fun getPendingIntentFlags(mutable: Boolean = false): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        flags = if (mutable) flags or PendingIntent.FLAG_MUTABLE else flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
        serviceJob.cancel()
    }
}