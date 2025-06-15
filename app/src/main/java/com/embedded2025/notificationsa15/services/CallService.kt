package com.embedded2025.notificationsa15.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.embedded2025.notificationsa15.MainActivity
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.ChannelID
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.NotificationHelper.setDestinationFragment
import com.embedded2025.notificationsa15.utils.NotificationID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servizio dedicato alla notifica della chiamata in arrivo.
 */
class CallService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var ringtone: Ringtone? = null

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "CallService"

        /**
         *  Azioni per la notifica di chiamata
         */
        const val ACTION_START_CALL = "com.embedded2025.notificationsa15.ACTION_START_CALL"
        const val ACTION_ANSWER_CALL = "com.embedded2025.notificationsa15.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.embedded2025.notificationsa15.ACTION_DECLINE_CALL"
        const val EXTRA_CALL_DELAY_SECONDS = "extra_call_delay_seconds"

        /**
         * Restituisce un Intent per notificare al servizio di far partire la chiamata dopo un certo ritardo.
         *
         * @param delayInSeconds ritardo in secondi prima di far partire la chiamata.
         * @return Intent con `ACTION_START_CALL` e `EXTRA_CALL_DELAY_SECONDS` impostati.
         */
        fun getStartCallIntent(context: Context, delayInSeconds: Int): Intent {
            return Intent(context, CallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CALL_DELAY_SECONDS, delayInSeconds)
            }
        }
    }

    /**
    * Gestisce la creazione del servizio.
    */
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "Servizio creato.")

    }


    /**
     * Entry point del servizio. Viene chiamato quando il servizio viene avviato tramite `startService()`.
     * Gestisce le azioni in base all'Intent ricevuto.
     *
     * @param intent L'Intent che ha avviato il servizio, contenente l'azione da eseguire.
     * @param flags Flag aggiuntivi sull'avvio.
     * @param startId Un ID univoco per questa richiesta di avvio.
     * @return Il comportamento del servizio in caso di terminazione. `START_NOT_STICKY` indica che
     * il servizio non verrà riavviato automaticamente se terminato dal sistema.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_CALL -> {
                val delay = intent.getIntExtra(EXTRA_CALL_DELAY_SECONDS, 0)
                scheduleCallNotification(delay)
            }
            ACTION_ANSWER_CALL -> handleCallAnswer()
            ACTION_DECLINE_CALL -> handleCallDecline()
        }
        return START_NOT_STICKY
    }

    /**
     * Pianifica la visualizzazione di una notifica di chiamata dopo un ritardo specificato.
     * Utilizza una coroutine per attendere il ritardo prima di costruire e mostrare la notifica.
     *
     * @param delayInSeconds Il numero di secondi da attendere prima di mostrare la notifica.
     */
    private fun scheduleCallNotification(delayInSeconds: Int) {
        serviceScope.launch {
            delay(delayInSeconds * 1000L)
            if (isActive) {
                buildCallNotification()
            }
        }
    }

    /**
     * Costruisce e mostra la notifica di chiamata in arrivo.
     * Configura lo stile `CallStyle`, i PendingIntent per le azioni e avvia il servizio in foreground.
     * Gestisce anche la riproduzione della suoneria e della vibrazione in base alla modalità suoneria del dispositivo.
     */
    private fun buildCallNotification() {
        val callerName = getString(R.string.notif_call_caller)

        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val answerIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_ANSWER_CALL
        }
        val answerPendingIntent = PendingIntent.getService(this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_DECLINE_CALL
        }
        val declinePendingIntent = PendingIntent.getService(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val caller = Person.Builder()
            .setName(callerName)
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_call))
            .build()

        val notification = NotificationHelper.createBasicBuilder(
            ChannelID.CALLS,
            R.drawable.ic_call,
            getString(R.string.notif_chat_title)
        )
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent))
            .setContentText(callerName)
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

    /**
     * Gestisce l'azione di risposta alla chiamata.
     * Interrompe suoneria e vibrazione, mostra un Toast e termina il servizio.
     */
    private fun handleCallAnswer() {
        stopRingtoneAndVibration()
        Toast.makeText(this, getString(R.string.toast_call_accepted), Toast.LENGTH_SHORT).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Gestisce l'azione di rifiuto della chiamata.
     * Interrompe suoneria e vibrazione, mostra un Toast e termina il servizio.
     */
    private fun handleCallDecline() {
        stopRingtoneAndVibration()
        Toast.makeText(this, getString(R.string.toast_call_denied), Toast.LENGTH_SHORT).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Interrompe la riproduzione della suoneria e cancella la vibrazione.
     */
    private fun stopRingtoneAndVibration() {
        ringtone?.stop()
        ringtone = null

        @Suppress("DEPRECATION")
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    /**
     * Questo metodo non è utilizzato per i servizi avviati (Started Services),
     * quindi restituisce null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Metodo di cleanup del servizio.
     * Viene chiamato quando il servizio sta per essere distrutto.
     * Interrompe suoneria/vibrazione e cancella la coroutine scope per evitare memory leak.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
        serviceJob.cancel()
    }
}