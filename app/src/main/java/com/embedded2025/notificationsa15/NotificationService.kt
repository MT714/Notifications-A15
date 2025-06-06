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
import android.graphics.Bitmap
import android.os.Build
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

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
        const val ACTION_MEDIA_UPDATE_NOTIFICATION = "com.embedded2025.notificationsa15.ACTION_MEDIA_UPDATE_NOTIFICATION"

        // ID Canali
        const val PROGRESS_CHANNEL_ID = NotificationsHelper.DEMO_CHANNEL_ID
        const val MEDIA_PLAYER_CHANNEL_ID = NotificationsHelper.MEDIA_PLAYER_CHANNEL_ID

        // ID Notifiche
        const val PROGRESS_NOTIFICATION_ID = NotificationsHelper.DEMO_PROGRESS_NOTIFICATION_ID
        const val MEDIA_PLAYER_NOTIFICATION_ID = NotificationsHelper.DEMO_MEDIA_PLAYER_NOTIFICATION_ID

        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply {
                action = ACTION_START_PROGRESS
            }
        }
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
        createNotificationChannels()
        initializeMediaSession()
        Log.d(TAG, "Servizio creato.")
    }

    private fun initializeMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(applicationContext, TAG, mediaButtonReceiver, null).apply {
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
            setCallback(mediaSessionCallback)
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
            startForegroundForMedia()
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "MediaSessionCallback: onPause")
            FakeMediaPlayer.pause()
            updateMediaNotification()
            if (!isProgressTaskRunning) {
                val notification = buildMediaNotification()
                notificationManager.notify(MEDIA_PLAYER_NOTIFICATION_ID, notification)
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
            FakeMediaPlayer.stop()
            isMediaPlayerActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            checkAndStopSelf()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")
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
                //val artistName = intent.getStringExtra("ARTIST_NAME") ?: FakeMediaPlayer.currentArtist
                if (!FakeMediaPlayer.isPlaying || FakeMediaPlayer.currentSong != songTitle) {
                    if (FakeMediaPlayer.currentSong != songTitle && songTitle != "Nessuna canzone"){
                        // Simula la selezione di una canzone specifica se necessario
                    }
                    FakeMediaPlayer.play()
                    Log.d(TAG, "ACTION_MEDIA_PLAY ricevuto con canzone: $songTitle")
                }
                isMediaPlayerActive = true
                startForegroundForMedia()
            }
            ACTION_MEDIA_PAUSE -> {
                FakeMediaPlayer.pause()
                isMediaPlayerActive = FakeMediaPlayer.isPlaying
                updateMediaNotification()
                Log.d(TAG, "ACTION_MEDIA_PAUSE ricevuto")
            }
            ACTION_MEDIA_NEXT -> {
                FakeMediaPlayer.nextTrack()
                isMediaPlayerActive = true
                updateMediaNotification()
                startForegroundForMedia()
                Log.d(TAG, "ACTION_MEDIA_NEXT ricevuto")
            }
            ACTION_MEDIA_PREVIOUS -> {
                FakeMediaPlayer.previousTrack()
                isMediaPlayerActive = true
                updateMediaNotification()
                startForegroundForMedia()
                Log.d(TAG, "ACTION_MEDIA_PREVIOUS ricevuto")
            }
            ACTION_MEDIA_STOP -> {
                FakeMediaPlayer.stop()
                isMediaPlayerActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                checkAndStopSelf()
                Log.d(TAG, "ACTION_MEDIA_STOP ricevuto")
            }
            ACTION_MEDIA_UPDATE_NOTIFICATION -> {
                if (isMediaPlayerActive) {
                    updateMediaNotification()
                    if (!isForegroundServiceRunning()) {
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
        return isProgressTaskRunning || (isMediaPlayerActive && FakeMediaPlayer.isPlaying)
    }


    // Progession notification
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
                    if (!isMediaPlayerActive) {
                        stopForeground(STOP_FOREGROUND_DETACH)
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
        serviceScope.cancel(reason)
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
        val pendingContentIntent = createContentPendingIntent(R.id.progressNotificationFragment)
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


    // Media Player notification
    private fun startForegroundForMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permesso POST_NOTIFICATIONS non concesso per media. Impossibile avviare foreground.")
            isMediaPlayerActive = false;
            checkAndStopSelf()
            return
        }
        val notification = buildMediaNotification()
        startForeground(MEDIA_PLAYER_NOTIFICATION_ID, notification)
        Log.d(TAG, "Servizio avviato in foreground per MEDIA.")
    }


    private fun updateMediaNotification() {
        if (!isMediaPlayerActive && !FakeMediaPlayer.isPlaying) {
            Log.d(TAG, "Media non attivo, non aggiorno la notifica media.")
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
                0,
                1.0f
            )
        mediaSession?.setPlaybackState(playbackStateBuilder.build())

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, FakeMediaPlayer.currentSong)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, FakeMediaPlayer.currentArtist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, FakeMediaPlayer.getAlbumArt(this))
        mediaSession?.setMetadata(metadataBuilder.build())
    }


    private fun buildMediaNotification(): Notification {
        val songTitle = FakeMediaPlayer.currentSong
        val artistName = FakeMediaPlayer.currentArtist
        val albumArt: Bitmap? = FakeMediaPlayer.getAlbumArt(this)
        val isPlaying = FakeMediaPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) getString(R.string.notif_media_player_demo_pause) else getString(R.string.notif_media_player_demo_play)
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingContentIntent = PendingIntent.getActivity(this, 0, contentIntent, getPendingIntentFlags())
        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)


        val builder = NotificationCompat.Builder(this, MEDIA_PLAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(songTitle)
            .setContentText(artistName)
            .setLargeIcon(albumArt)
            .setContentIntent(pendingContentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            // Azioni
            .addAction(R.drawable.ic_previous, getString(R.string.notif_media_player_demo_previous), prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(R.drawable.ic_next, getString(R.string.notif_media_player_demo_next), nextIntent)
            // Stile Media
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
        return builder.build()
    }


    private fun checkAndStopSelf() {
        if (!isProgressTaskRunning && !isMediaPlayerActive) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        } else {
            Log.d(TAG, "Task attivi: Progresso=$isProgressTaskRunning, Media=$isMediaPlayerActive. Non fermo il servizio.")
        }
    }

    private fun createNotificationChannels() {
        val channels = mutableListOf<NotificationChannel>()

        if (notificationManager.getNotificationChannel(PROGRESS_CHANNEL_ID) == null) {
            channels.add(
                NotificationChannel(
                    PROGRESS_CHANNEL_ID,
                    getString(R.string.channel_demo_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_demo_description)
                    setShowBadge(true)
                }
            )
        }

        // Canale Media Player
        if (notificationManager.getNotificationChannel(MEDIA_PLAYER_CHANNEL_ID) == null) {
            channels.add(
                NotificationChannel(
                    MEDIA_PLAYER_CHANNEL_ID,
                    getString(R.string.channel_media_player_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_media_player_description)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        if (channels.isNotEmpty()) {
            notificationManager.createNotificationChannels(channels)
            Log.d(TAG, "Canali di notifica creati/aggiornati dal servizio.")
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

    private fun createContentPendingIntent(destination: Int): PendingIntent =
        NotificationsHelper.createContentPendingIntent(this, destination)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        mediaSession?.release()
        mediaSession = null
        Log.d(TAG, "Servizio distrutto.")
    }
}
