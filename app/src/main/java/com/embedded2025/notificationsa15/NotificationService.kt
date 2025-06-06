package com.embedded2025.notificationsa15 // Assicurati che sia il tuo package corretto

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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver // Importante per i pulsanti media
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob) // IO per operazioni di rete/disco/lunghe

    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    // Tracciamento dello stato per decidere quando fermare il servizio
    private var isProgressTaskRunning = false
    private var isMediaPlayerActive = false


    companion object {
        private const val TAG = "NotificationService"

        // Azioni per il servizio
        const val ACTION_START_PROGRESS = "com.embedded2025.notificationsa15.ACTION_START_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.embedded2025.notificationsa15.ACTION_CANCEL_PROGRESS"

        const val ACTION_MEDIA_PLAY = "com.embedded2025.notificationsa15.ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.embedded2025.notificationsa15.ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.embedded2025.notificationsa15.ACTION_MEDIA_NEXT"
        const val ACTION_MEDIA_PREVIOUS = "com.embedded2025.notificationsa15.ACTION_MEDIA_PREVIOUS"
        const val ACTION_MEDIA_STOP = "com.embedded2025.notificationsa15.ACTION_MEDIA_STOP"
        const val ACTION_MEDIA_UPDATE_NOTIFICATION = "com.embedded2025.notificationsa15.ACTION_MEDIA_UPDATE_NOTIFICATION" // Azione per aggiornare la notifica media

        // ID Canali (devono corrispondere a quelli in NotificationsHelper se usi entrambi)
        const val PROGRESS_CHANNEL_ID = NotificationsHelper.DEMO_CHANNEL_ID // Riutilizza se appropriato
        const val MEDIA_PLAYER_CHANNEL_ID = NotificationsHelper.MEDIA_PLAYER_CHANNEL_ID

        // ID Notifiche (devono essere univoci all'interno del servizio)
        const val PROGRESS_NOTIFICATION_ID = NotificationsHelper.DEMO_PROGRESS_NOTIFICATION_ID // Riutilizza se appropriato
        const val MEDIA_PLAYER_NOTIFICATION_ID = NotificationsHelper.DEMO_MEDIA_PLAYER_NOTIFICATION_ID

        // Funzioni helper per creare Intent per questo servizio
        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply {
                action = ACTION_START_PROGRESS
            }
        }
        // Aggiungi altre funzioni helper per gli intent media se necessario
        // Esempio:
        fun getMediaControlIntent(context: Context, mediaAction: String, songTitle: String? = null, artistName: String? = null): Intent {
            return Intent(context, NotificationService::class.java).apply {
                action = mediaAction
                putExtra("SONG_TITLE", songTitle)
                putExtra("ARTIST_NAME", artistName)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels() // Assicurati che i canali siano creati
        initializeMediaSession()
        Log.d(TAG, "Servizio creato.")
    }

    private fun initializeMediaSession() {
        // Il nome del component per MediaButtonReceiver
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(applicationContext, TAG, mediaButtonReceiver, null).apply {
            // Imposta i flag per indicare che questo media session gestisce i controlli di trasporto
            // e può ricevere comandi dai media button.
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            // Imposta uno stato di playback iniziale (es. Paused)
            val initialState = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
            setPlaybackState(initialState)

            // Imposta il callback per gestire gli eventi del media session
            setCallback(mediaSessionCallback)

            // Rendi la sessione attiva per ricevere gli eventi dei media button
            isActive = true
        }
        Log.d(TAG, "MediaSession inizializzata.")
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "MediaSessionCallback: onPlay")
            FakeMediaPlayer.play()
            isMediaPlayerActive = true
            updateMediaNotification()
            startForegroundForMedia() // Assicurati che il servizio sia in foreground
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "MediaSessionCallback: onPause")
            FakeMediaPlayer.pause()
            updateMediaNotification()
            // Non fermare il foreground se la pausa è temporanea, ma aggiorna l'ongoing della notifica
            if (!isProgressTaskRunning) { // Se solo il media player era attivo
                val notification = buildMediaNotification()
                notificationManager.notify(MEDIA_PLAYER_NOTIFICATION_ID, notification)
                // Potresti voler chiamare stopForeground(false) per mantenere la notifica ma non più foreground
                // Oppure, se la pausa significa che l'utente non sta più ascoltando attivamente,
                // potresti chiamare stopForeground(STOP_FOREGROUND_DETACH) e potenzialmente stopSelf() dopo un timeout.
                // Per ora, manteniamo la notifica attiva ma non ongoing
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.d(TAG, "MediaSessionCallback: onSkipToNext")
            FakeMediaPlayer.nextTrack()
            updateMediaNotification()
            startForegroundForMedia()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            Log.d(TAG, "MediaSessionCallback: onSkipToPrevious")
            FakeMediaPlayer.previousTrack()
            updateMediaNotification()
            startForegroundForMedia()
        }

        override fun onStop() {
            super.onStop()
            Log.d(TAG, "MediaSessionCallback: onStop")
            FakeMediaPlayer.pause() // O una vera funzione stop se disponibile
            isMediaPlayerActive = false
            stopForeground(STOP_FOREGROUND_REMOVE) // Rimuovi notifica
            checkAndStopSelf()
        }

        // Potresti voler gestire onPlayFromMediaId, onPlayFromSearch, onPlayFromUri etc.
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        // Gestione degli intent dei MediaButton
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_START_PROGRESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permesso POST_NOTIFICATIONS non concesso. Impossibile avviare il task di progresso.")
                    checkAndStopSelf()
                    return START_NOT_STICKY
                }
                isProgressTaskRunning = true
                startForegroundWithProgress()
            }
            ACTION_CANCEL_PROGRESS -> {
                Log.i(TAG, "Azione di cancellazione progresso ricevuta.")
                handleProgressCancellation("Azione di cancellazione progresso dall'utente")
            }
            ACTION_MEDIA_PLAY -> {
                val songTitle = intent.getStringExtra("SONG_TITLE") ?: FakeMediaPlayer.currentSong
                val artistName = intent.getStringExtra("ARTIST_NAME") ?: FakeMediaPlayer.currentArtist
                // Se non sta già suonando o l'info è diversa, aggiorna e suona
                if (!FakeMediaPlayer.isPlaying || FakeMediaPlayer.currentSong != songTitle) {
                    if (FakeMediaPlayer.currentSong != songTitle && songTitle != "Nessuna canzone"){
                        // Simula la selezione di una canzone specifica se necessario
                        // Per ora FakeMediaPlayer gestisce la sua playlist interna
                    }
                    FakeMediaPlayer.play() // Assicura che parta
                }
                isMediaPlayerActive = true
                startForegroundForMedia() // Inizia o aggiorna la notifica media in foreground
            }
            ACTION_MEDIA_PAUSE -> {
                FakeMediaPlayer.pause()
                isMediaPlayerActive = FakeMediaPlayer.isPlaying // Potrebbe essere già in pausa
                updateMediaNotification() // Aggiorna lo stato nella notifica
                // Non fermare il servizio qui, la notifica di pausa rimane
            }
            ACTION_MEDIA_NEXT -> {
                FakeMediaPlayer.nextTrack()
                isMediaPlayerActive = true
                updateMediaNotification()
                startForegroundForMedia()
            }
            ACTION_MEDIA_PREVIOUS -> {
                FakeMediaPlayer.previousTrack()
                isMediaPlayerActive = true
                updateMediaNotification()
                startForegroundForMedia()
            }
            ACTION_MEDIA_STOP -> {
                FakeMediaPlayer.pause() // o una vera funzione stop
                isMediaPlayerActive = false
                stopForeground(STOP_FOREGROUND_REMOVE) // Rimuove la notifica media
                checkAndStopSelf() // Ferma il servizio se non ci sono altri task
            }
            ACTION_MEDIA_UPDATE_NOTIFICATION -> { // Azione esplicita per aggiornare la notifica media
                if (isMediaPlayerActive) {
                    updateMediaNotification()
                    if (!isForegroundServiceRunning()) { // Se per qualche motivo non è foreground
                        startForegroundForMedia()
                    }
                }
            }
            else -> {
                Log.w(TAG, "Azione non riconosciuta o intent nullo: ${intent?.action}")
                checkAndStopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun isForegroundServiceRunning(): Boolean {
        // Questo è un modo approssimativo. Android non offre un API diretto per verificarlo.
        // Si basa sul fatto che se un task che richiede foreground è attivo, allora dovrebbe esserlo.
        return isProgressTaskRunning || (isMediaPlayerActive && FakeMediaPlayer.isPlaying)
    }


    // --- Logica Notifica Progresso ---
    private fun startForegroundWithProgress() {
        val initialNotification = buildProgressNotification(0, 100, false, "Avvio operazione...")
        startForeground(PROGRESS_NOTIFICATION_ID, initialNotification)
        Log.d(TAG, "Servizio avviato in foreground per PROGRESSO.")

        serviceScope.launch {
            val maxProgress = 100
            var currentProgress = 0
            try {
                while (currentProgress <= maxProgress && isActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(
                            this@NotificationService, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.w(TAG, "Permesso POST_NOTIFICATIONS perso durante l'aggiornamento progresso.")
                        handleProgressCancellation("Permesso notifiche perso")
                        break
                    }

                    val progressText = getString(R.string.notif_progress_demo_det, currentProgress)
                    notificationManager.notify(
                        PROGRESS_NOTIFICATION_ID,
                        buildProgressNotification(currentProgress, maxProgress, false, progressText)
                    )
                    delay(500)
                    currentProgress += 5
                }

                if (isActive) { // Completato
                    Log.d(TAG, "Progresso completato.")
                    notificationManager.notify(
                        PROGRESS_NOTIFICATION_ID,
                        buildFinalProgressNotification(getString(R.string.notif_progress_demo_complete))
                    )
                    isProgressTaskRunning = false
                    // Se il media player non è attivo, possiamo rimuovere il foreground per il progresso
                    if (!isMediaPlayerActive) {
                        stopForeground(STOP_FOREGROUND_DETACH) // Lascia la notifica finale di progresso
                    }
                    checkAndStopSelf()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i(TAG, "Coroutine di progresso cancellata: ${e.message}")
                } else {
                    Log.e(TAG, "Errore durante l'operazione di progresso", e)
                    if (isActive) {
                        notificationManager.notify(
                            PROGRESS_NOTIFICATION_ID,
                            buildFinalProgressNotification(getString(R.string.notif_progress_demo_fail))
                        )
                        isProgressTaskRunning = false
                        if (!isMediaPlayerActive) {
                            stopForeground(STOP_FOREGROUND_DETACH)
                        }
                        checkAndStopSelf()
                    }
                }
            }
        }
    }

    private fun handleProgressCancellation(reason: String) {
        Log.i(TAG, "Gestione cancellazione progresso: $reason")
        serviceScope.cancel(reason) // Cancella le coroutine di questo scope (selettivamente se necessario)
        isProgressTaskRunning = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(
                PROGRESS_NOTIFICATION_ID,
                buildFinalProgressNotification(getString(R.string.notif_progress_demo_cancelled))
            )
        }
        if (!isMediaPlayerActive) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        checkAndStopSelf()
    }

    private fun buildProgressNotification(progress: Int, max: Int, indeterminate: Boolean, contentText: String): Notification {
        val pendingContentIntent = createContentPendingIntent(R.id.progressNotificationFragment) // Da NotificationsHelper
        val cancelIntent = Intent(this, NotificationService::class.java).apply { action = ACTION_CANCEL_PROGRESS }
        val pendingCancelIntent = PendingIntent.getService(this, 101, cancelIntent, getPendingIntentFlags())

        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background) // Sostituisci con icona appropriata
            .setContentTitle(getString(R.string.progress_notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, progress, indeterminate)
            .setContentIntent(pendingContentIntent)
            .addAction(R.drawable.ic_cancel, getString(R.string.action_cancel), pendingCancelIntent)
            .build()
    }

    private fun buildFinalProgressNotification(contentText: String): Notification {
        val pendingContentIntent = createContentPendingIntent(R.id.progressNotificationFragment)
        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(getString(R.string.progress_notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true)
            .build()
    }


    // --- Logica Notifica Media Player ---
    private fun startForegroundForMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permesso POST_NOTIFICATIONS non concesso per media. Impossibile avviare foreground.")
            isMediaPlayerActive = false; // Non può operare senza notifica
            checkAndStopSelf()
            return
        }
        val notification = buildMediaNotification()
        startForeground(MEDIA_PLAYER_NOTIFICATION_ID, notification)
        Log.d(TAG, "Servizio avviato in foreground per MEDIA.")
    }


    private fun updateMediaNotification() {
        if (!isMediaPlayerActive && !FakeMediaPlayer.isPlaying) { // Se è stato fermato o non è mai partito
            Log.d(TAG, "Media non attivo, non aggiorno la notifica media.")
            // Potresti voler cancellare la notifica qui se non è più rilevante
            // notificationManager.cancel(MEDIA_PLAYER_NOTIFICATION_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permesso POST_NOTIFICATIONS perso, impossibile aggiornare notifica media.")
            return
        }
        val notification = buildMediaNotification()
        notificationManager.notify(MEDIA_PLAYER_NOTIFICATION_ID, notification)
        Log.d(TAG, "Notifica media aggiornata.")

        // Aggiorna lo stato della MediaSession
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (FakeMediaPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                0, // position, non simulato qui
                1.0f // playback speed
            )
        mediaSession?.setPlaybackState(playbackStateBuilder.build())

        // Aggiorna i metadati della MediaSession
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, FakeMediaPlayer.currentSong)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, FakeMediaPlayer.currentArtist)
        // Aggiungi album art se disponibile e FakeMediaPlayer lo fornisce come Bitmap
        // .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, FakeMediaPlayer.getAlbumArt(this))
        // Se hai durata: .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, FakeMediaPlayer.getDuration())
        mediaSession?.setMetadata(metadataBuilder.build())
    }


    private fun buildMediaNotification(): Notification {
        val songTitle = FakeMediaPlayer.currentSong
        val artistName = FakeMediaPlayer.currentArtist
        val albumArt: Bitmap? = FakeMediaPlayer.getAlbumArt(this) // Assumendo che questa funzione esista in FakeMediaPlayer
        val isPlaying = FakeMediaPlayer.isPlaying

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) getString(R.string.notif_media_player_demo_pause) else getString(R.string.notif_media_player_demo_play)

        // Intent per l'activity principale quando si clicca sulla notifica
        val contentIntent = Intent(this, MainActivity::class.java) // Sostituisci MainActivity
        val pendingContentIntent = PendingIntent.getActivity(this, 0, contentIntent, getPendingIntentFlags())

        // Azioni per i controlli media
        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)


        val builder = NotificationCompat.Builder(this, MEDIA_PLAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note) // Icona per la notifica media
            .setContentTitle(songTitle)
            .setContentText(artistName)
            .setLargeIcon(albumArt)
            .setContentIntent(pendingContentIntent) // Cosa succede al click sulla notifica
            .setDeleteIntent(stopIntent) // Cosa succede quando la notifica viene scartata (swipe via)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying) // La notifica è ongoing (non scartabile) se sta suonando
            // Azioni
            .addAction(R.drawable.ic_previous, getString(R.string.notif_media_player_demo_previous), prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(R.drawable.ic_next, getString(R.string.notif_media_player_demo_next), nextIntent)
            // Stile Media
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken) // COLLEGA LA MEDIA SESSION
                    .setShowActionsInCompactView(0, 1, 2) // Indici delle azioni da mostrare in vista compatta
                    .setShowCancelButton(true) // Mostra il pulsante 'stop' (X) quando espansa
                    .setCancelButtonIntent(stopIntent) // Intent per il pulsante 'stop'
            )
        return builder.build()
    }

    // --- Gestione Generale Servizio ---
    private fun checkAndStopSelf() {
        if (!isProgressTaskRunning && !isMediaPlayerActive) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            // serviceJob.cancel() // Cancella tutte le coroutine rimanenti prima di fermare
            // mediaSession?.release() // Rilascia la media session
            // mediaSession = null
            stopSelf()
        } else {
            Log.d(TAG, "Task attivi: Progresso=$isProgressTaskRunning, Media=$isMediaPlayerActive. Non fermo il servizio.")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = mutableListOf<NotificationChannel>()

            // Canale Progresso (potrebbe già esistere se NotificationsHelper.initialize è chiamato prima)
            if (notificationManager.getNotificationChannel(PROGRESS_CHANNEL_ID) == null) {
                channels.add(
                    NotificationChannel(
                        PROGRESS_CHANNEL_ID,
                        getString(R.string.channel_demo_name), // Usa le tue stringhe
                        NotificationManager.IMPORTANCE_LOW // Progresso di solito è LOW per non essere invasivo
                    ).apply {
                        description = getString(R.string.channel_demo_description)
                        setShowBadge(true) // O false per progresso
                    }
                )
            }

            // Canale Media Player
            if (notificationManager.getNotificationChannel(MEDIA_PLAYER_CHANNEL_ID) == null) {
                channels.add(
                    NotificationChannel(
                        MEDIA_PLAYER_CHANNEL_ID,
                        getString(R.string.channel_media_player_name),
                        NotificationManager.IMPORTANCE_LOW // Anche media di solito LOW, non suona ad ogni cambio traccia
                    ).apply {
                        description = getString(R.string.channel_media_player_description)
                        setShowBadge(false) // Media player di solito non ha badge
                        setSound(null, null) // Nessun suono per aggiornamenti
                        enableVibration(false)
                    }
                )
            }
            if (channels.isNotEmpty()) {
                notificationManager.createNotificationChannels(channels)
                Log.d(TAG, "Canali di notifica creati/aggiornati dal servizio.")
            }
        }
    }

    private fun getPendingIntentFlags(mutable: Boolean = false): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (mutable) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }
        }
        return flags
    }

    // Wrapper per la creazione del PendingIntent per il contenuto (da NotificationsHelper)
    private fun createContentPendingIntent(destination: Int): PendingIntent =
        NotificationsHelper.createContentPendingIntent(this, destination)


    override fun onBind(intent: Intent?): IBinder? {
        return null // Non forniamo binding per ora
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        mediaSession?.release()
        mediaSession = null
        Log.d(TAG, "Servizio distrutto.")
    }
}
