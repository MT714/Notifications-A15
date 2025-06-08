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
import com.embedded2025.notificationsa15.utils.FakeMediaPlayer
import com.embedded2025.notificationsa15.utils.NotificationsHelper
import com.embedded2025.notificationsa15.utils.PendingIntentHelper

class NotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private var progressJob: Job? = null

    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    companion object {
        private const val TAG = "NotificationService"

        // Azioni
        const val ACTION_START_PROGRESS = "com.embedded2025.notificationsa15.ACTION_START_PROGRESS"
        const val ACTION_CANCEL_PROGRESS = "com.embedded2025.notificationsa15.ACTION_CANCEL_PROGRESS"
        const val ACTION_MEDIA_PLAY = "com.embedded2025.notificationsa15.ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.embedded2025.notificationsa15.ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.embedded2025.notificationsa15.ACTION_MEDIA_NEXT"
        const val ACTION_MEDIA_PREVIOUS = "com.embedded2025.notificationsa15.ACTION_MEDIA_PREVIOUS"
        const val ACTION_MEDIA_STOP = "com.embedded2025.notificationsa15.ACTION_MEDIA_STOP"

        // ID
        const val PROGRESS_CHANNEL_ID = NotificationsHelper.ChannelID.DEMO
        const val MEDIA_PLAYER_CHANNEL_ID = NotificationsHelper.ChannelID.MEDIA_PLAYER
        const val PROGRESS_NOTIFICATION_ID = DemoNotificationsHelper.NotificationID.PROGRESS
        const val MEDIA_PLAYER_NOTIFICATION_ID = DemoNotificationsHelper.NotificationID.MEDIA_PLAYER

        fun getStartProgressIntent(context: Context): Intent {
            return Intent(context, NotificationService::class.java).apply { action = ACTION_START_PROGRESS }
        }
        fun getMediaControlIntent(context: Context, mediaAction: String): Intent {
            return Intent(context, NotificationService::class.java).apply { action = mediaAction }
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
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_STOP
                ).setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f).build()
            setPlaybackState(initialState)
            setCallback(mediaSessionCallback)
            isActive = true
        }
        Log.d(TAG, "MediaSession inizializzata.")
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { super.onPlay(); Log.d(TAG, "Callback: onPlay"); FakeMediaPlayer.play(); updateMediaPlaybackState() }
        override fun onPause() { super.onPause(); Log.d(TAG, "Callback: onPause"); FakeMediaPlayer.pause(); updateMediaPlaybackState() }
        override fun onSkipToNext() { super.onSkipToNext(); Log.d(TAG, "Callback: onSkipToNext"); FakeMediaPlayer.nextTrack(); updateMediaPlaybackState() }
        override fun onSkipToPrevious() { super.onSkipToPrevious(); Log.d(TAG, "Callback: onSkipToPrevious"); FakeMediaPlayer.previousTrack(); updateMediaPlaybackState() }
        override fun onStop() {
            super.onStop()
            Log.d(TAG, "Callback: onStop")
            FakeMediaPlayer.stop()
            updateMediaPlaybackState()
            checkAndStopSelf()
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

    private fun updateMediaPlaybackState() {
        if (mediaSession == null) return
        val isPlaying = FakeMediaPlayer.isPlaying
        val isStopped = FakeMediaPlayer.currentTrackIndex == -1 //TODO Controlla l'utilitá

        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_STOP
            ).setState(
                when {
                    isPlaying -> PlaybackStateCompat.STATE_PLAYING
                    !isStopped -> PlaybackStateCompat.STATE_PAUSED
                    else -> PlaybackStateCompat.STATE_STOPPED
                }, 0, 1.0f
            )
        mediaSession!!.setPlaybackState(playbackStateBuilder.build())

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, FakeMediaPlayer.currentSong)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, FakeMediaPlayer.currentArtist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, FakeMediaPlayer.getAlbumArt(this))
        mediaSession!!.setMetadata(metadataBuilder.build())

        if (!isStopped) {
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
        }
    }

    private fun buildMediaNotification(): Notification {
        val controller = mediaSession!!.controller
        val description = controller.metadata?.description
        val isPlaying = controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING

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
            .addAction(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, if (isPlaying) "Pausa" else "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
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
                if (!FakeMediaPlayer.isPlaying) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
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
        if (!FakeMediaPlayer.isPlaying) {
            stopForeground(STOP_FOREGROUND_DETACH)
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
        val isMediaRunning = FakeMediaPlayer.currentTrackIndex != -1
        if (progressJob?.isActive != true && !isMediaRunning) {
            Log.d(TAG, "Nessun task attivo. Fermo il servizio.")
            stopSelf()
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(PROGRESS_CHANNEL_ID, getString(R.string.channel_demo_name), NotificationManager.IMPORTANCE_DEFAULT), // Aumentato a DEFAULT
            NotificationChannel(MEDIA_PLAYER_CHANNEL_ID, getString(R.string.channel_media_player_name), NotificationManager.IMPORTANCE_LOW)
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
        mediaSession?.release()
    }
}
