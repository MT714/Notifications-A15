package com.embedded2025.notificationsa15.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LiveUpdateService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Stato dell'ordine di consegna.
     */
    object OrderStatus{
        const val ORDER_PLACED = 0
        const val ORDER_ON_THE_WAY = 1
        const val ORDER_COMPLETE = 2
    }

    /**
     * Job per gestire il flusso di aggiornamento live.
     */
    private var liveUpdateJob: Job? = null

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "LiveUpdateService"

        // Azioni per Live Update
        const val ACTION_START_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_START_LIVE_UPDATE"
        const val ACTION_ADVANCE_LIVE_UPDATE = "com.embedded2025.notificationsa15.ACTION_ADVANCE_LIVE_UPDATE"
        const val EXTRA_LIVE_UPDATE_STEP = "extra_live_update_step"

        /**
         * Restituisce un Intent configurato per avviare il servizio di Live Update, con la relativa Action.
         */
        fun getStartLiveUpdateIntent(context: Context): Intent {
            return Intent(context, LiveUpdateService::class.java).apply { action = ACTION_START_LIVE_UPDATE }
        }

        /**
         * Restituisce un Intent per notificare al servizio di passare allo step successivo dell'aggiornamento live.
         *
         * @param currentStep L’indice dello step corrente da elaborare.
         * @return Intent con `ACTION_ADVANCE_LIVE_UPDATE` e `EXTRA_LIVE_UPDATE_STEP` impostati.
         */
        private fun getAdvanceLiveUpdateIntent(context: Context, currentStep: Int): Intent {
            return Intent(context, LiveUpdateService::class.java).apply {
                action = ACTION_ADVANCE_LIVE_UPDATE
                putExtra(EXTRA_LIVE_UPDATE_STEP, currentStep)
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
     * Gestisce l'avvio del servizio a seguito della ricezione di un Intent.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        when (intent?.action) {
            // Avvia la sequenza iniziale di aggiornamento live
            ACTION_START_LIVE_UPDATE -> startLiveUpdateFlow()

            // Gestisce il passaggio allo step successivo dell'aggiornamento live
            ACTION_ADVANCE_LIVE_UPDATE -> {
                val currentStep = intent.getIntExtra(EXTRA_LIVE_UPDATE_STEP, 0)
                handleLiveUpdateAdvancement(currentStep)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Avvia il flusso di aggiornamento live delle notifiche in foreground.
     *
     * Comportamento:
     * 1. Mostra una notifica iniziale con stato `ORDER_PLACED`.
     * 2. Dopo 10 secondi, aggiorna lo stato a `ORDER_ON_THE_WAY`, se la coroutine è ancora attiva.
     * 3. Dopo altri 10 secondi, aggiorna lo stato a `ORDER_COMPLETE`, se la coroutine è ancora attiva
     */
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

    /**
     * Gestisce l'avanzamento del live update:
     *  Calcola lo step successivo (fino a ORDER_COMPLETE).
     *  Mostra la notifica aggiornata.
     *  Se è l’ultimo step, rimuove il servizio in foreground dopo 4 secondi.
     */
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

    /**
     * Crea una notifica per aggiornare lo stato del live update.
     */
    private fun buildLiveUpdateNotification(step: Int): Notification {
        val builder = NotificationHelper.createBasicBuilder(
            ChannelID.SERVICES,
            R.drawable.ic_live,
            getString(R.string.notif_live_update_demo_title)
        )
            .setDestinationFragment(R.id.progressNotificationFragment)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentText(getString(R.string.notif_live_update_demo_text))

        /**
         * Imposta il testo della notifica e il livello di avanzamento in base all'indice dello step corrente.
         */
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
                    .addAction(R.drawable.ic_live, getString(R.string.notif_live_update_demo_order_already_received), pendingAdvanceIntent)
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

    /**
     * Verifica se il servizio deve essere fermato.
     */
    private fun checkAndStopSelf() {
        if (liveUpdateJob?.isActive != true) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        }
    }

    /**
     * Restituisce le flag di PendingIntent in base alla mutabilità specificata.
     */
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