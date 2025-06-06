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
            Log.d(TAG, "Callback: onPlay")
            FakeMediaPlayer.play()
            isMediaPlayerActive = true
            updateMediaPlaybackState()
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "Callback: onPause")
            FakeMediaPlayer.pause()
            updateMediaPlaybackState()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.d(TAG, "Callback: onSkipToNext")
            FakeMediaPlayer.nextTrack()
            updateMediaPlaybackState()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            Log.d(TAG, "Callback: onSkipToPrevious")
            FakeMediaPlayer.previousTrack()
            updateMediaPlaybackState()
        }

        override fun onStop() {
            super.onStop()
            Log.d(TAG, "Callback: onStop")
            FakeMediaPlayer.stop()
            isMediaPlayerActive = false
            updateMediaPlaybackState()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ricevuto con azione: ${intent?.action}")

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startForeground(MEDIA_PLAYER_NOTIFICATION_ID, buildMediaNotification())
            } else {
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_START_PROGRESS -> startForegroundWithProgress()
            ACTION_CANCEL_PROGRESS -> handleProgressCancellation("Azione di cancellazione utente")
            ACTION_MEDIA_PLAY -> mediaSession?.controller?.transportControls?.play()
            ACTION_MEDIA_PAUSE -> mediaSession?.controller?.transportControls?.pause()
            ACTION_MEDIA_NEXT -> mediaSession?.controller?.transportControls?.skipToNext()
            ACTION_MEDIA_PREVIOUS -> mediaSession?.controller?.transportControls?.skipToPrevious()
            ACTION_MEDIA_STOP -> mediaSession?.controller?.transportControls?.stop()
        }
        return START_NOT_STICKY
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
            .setSmallIcon(R.drawable.ic_launcher_background)
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
    private fun updateMediaPlaybackState() {
        if (mediaSession == null) return
        val isPlaying = FakeMediaPlayer.isPlaying

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
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                0, 1.0f
            )
        mediaSession!!.setPlaybackState(playbackStateBuilder.build())

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, FakeMediaPlayer.currentSong)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, FakeMediaPlayer.currentArtist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, FakeMediaPlayer.getAlbumArt(this))
        mediaSession!!.setMetadata(metadataBuilder.build())

        if (isMediaPlayerActive) {
            val notification = buildMediaNotification()
            if (isPlaying) {
                startForeground(MEDIA_PLAYER_NOTIFICATION_ID, notification)
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(MEDIA_PLAYER_NOTIFICATION_ID, notification)
                }
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(MEDIA_PLAYER_NOTIFICATION_ID)
        }
    }


    private fun buildMediaNotification(): Notification {
        val controller = mediaSession!!.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata?.description
        val playbackState = controller.playbackState

        val isPlaying = playbackState?.state == PlaybackStateCompat.STATE_PLAYING
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) getString(R.string.notif_media_player_demo_pause) else getString(R.string.notif_media_player_demo_play)

        val builder = NotificationCompat.Builder(this, MEDIA_PLAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(description?.title ?: "Nessun Titolo")
            .setContentText(description?.subtitle ?: "Nessun Artista")
            .setLargeIcon(description?.iconBitmap)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_previous, getString(R.string.notif_media_player_demo_previous), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(playPauseIcon, playPauseTitle, MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
            .addAction(R.drawable.ic_next, getString(R.string.notif_media_player_demo_next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
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
