package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



object NotificationsHelper {
    // ID canali
    const val DEMO_CHANNEL_ID = "channel_demo"
    const val DEFAULT_CHANNEL_ID = "channel_default"
    const val MEDIA_PLAYER_CHANNEL_ID = "channel_media_player"

    // ID notifiche
    const val DEMO_SIMPLE_NOTIFICATION_ID = 0
    const val DEMO_EXPANDABLE_NOTIFICATION_TEXT_ID = 1
    const val DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID = 2
    const val DEMO_ACTIONS_NOTIFICATION_ID = 3
    const val DEMO_REPLY_NOTIFICATION_ID = 4
    const val DEMO_PROGRESS_NOTIFICATION_ID = 5
    const val DEMO_MEDIA_PLAYER_NOTIFICATION_ID = 6


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
            },
            NotificationChannel(MEDIA_PLAYER_CHANNEL_ID,
                getAppContext().getString(R.string.channel_media_player_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getAppContext().getString(R.string.channel_media_player_description)
                setShowBadge(false)
                setSound(null, null)
            }
        )

        getNotificationManager().createNotificationChannels(channels)
    }

    /**
     * Pubblica una notifica.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     */
    fun safeNotify(id: Int, builder: NotificationCompat.Builder) {
        with(getNotificationManager()) {
            if (ActivityCompat.checkSelfPermission(getAppContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("NotificationsHelper", "Permission not granted.")

                return
            }
            notify(id, builder.build())
        }
    }

    /**
     * Pubblica una notifica demo.
     * Differisce da [safeNotify] in quanto se le notifiche non sono abilitate oppure il canale demo
     * non è visibile, allora l'utente viene reindirizzato alle relative impostazioni di sistema.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     *
     * @see safeNotify
     */
    private fun safeNotifyDemo(id: Int, builder: NotificationCompat.Builder) {
        with(getNotificationManager()) {
            val ctx = getAppContext()
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("NotificationsHelper", "Permission not granted, opening settings.")
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })

                return
            } else if (getNotificationManager().getNotificationChannel(DEMO_CHANNEL_ID).importance
                    == NotificationManager.IMPORTANCE_NONE
            ) {
                Log.i("NotificationsHelper", "Notification channel is not visible, opening settings.")

                ctx.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, DEMO_CHANNEL_ID)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })

                return
            }
            notify(id, builder.build())
        }
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

        safeNotifyDemo(DEMO_EXPANDABLE_NOTIFICATION_PICTURE_ID, notif)
    }

    // Mostra una notifica con azioni
    const val ACTION_ARCHIVE = "com.embedded2025.notificationsa15.ACTION_ARCHIVE"
    const val ACTION_LATER = "com.embedded2025.notificationsa15.ACTION_LATER"
    fun showActionNotificationDemo() {
        val ctx = getAppContext()
        val archivePendingIntent = createBroadcastPendingIntent(DEMO_ACTIONS_NOTIFICATION_ID, ACTION_ARCHIVE, 1)
        val laterPendingIntent = createBroadcastPendingIntent(DEMO_ACTIONS_NOTIFICATION_ID, ACTION_LATER,2)
        val builder = NotificationCompat.Builder(ctx, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_action_demo_title))
            .setContentText(ctx.getString(R.string.notif_action_demo_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(ctx.getString(R.string.notif_action_demo_text)))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(R.drawable.ic_archive, ctx.getString(R.string.notif_action_archive), archivePendingIntent)
            .addAction(R.drawable.ic_later, ctx.getString(R.string.notif_action_later), laterPendingIntent)

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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)

        safeNotify(notificationId, initialBuilder)

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
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setProgress(maxProgress, currentProgress, false)
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
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setOngoing(false)
                        .setOnlyAlertOnce(false)
                        .setProgress(0, 0, false)

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
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(false)
                        .setProgress(0, 0, false)
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
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(false)
                    .setProgress(0, 0, false)
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

    // Mostra una notifica di riproduzione multimediale
    const val ACTION_MEDIA_PLAY_PAUSE = "com.embedded2025.notificationsa15.ACTION_MEDIA_PLAY_PAUSE"
    const val ACTION_MEDIA_NEXT = "com.embedded2025.notificationsa15.ACTION_MEDIA_NEXT"
    const val ACTION_MEDIA_PREVIOUS = "com.embedded2025.notificationsa15.ACTION_MEDIA_PREVIOUS"
    const val ACTION_MEDIA_STOP = "com.embedded2025.notificationsa15.ACTION_MEDIA_STOP"
    fun showMediaPlayerNotification(
        songTitle: String,
        artistName: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        mediaSessionToken: android.support.v4.media.session.MediaSessionCompat.Token? = null
    ) {
        val ctx = getAppContext()
        val notificationId = DEMO_MEDIA_PLAYER_NOTIFICATION_ID
        val playPauseIntent = createBroadcastPendingIntent(
            notificationId,
            ACTION_MEDIA_PLAY_PAUSE,
            requestCodeOffset = 10
        )
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) ctx.getString(R.string.notif_media_player_demo_pause) else ctx.getString(R.string.notif_media_player_demo_play)
        val nextIntent = createBroadcastPendingIntent(
            notificationId,
            ACTION_MEDIA_NEXT,
            requestCodeOffset = 11
        )
        val previousIntent = createBroadcastPendingIntent(
            notificationId,
            ACTION_MEDIA_PREVIOUS,
            requestCodeOffset = 12
        )
        val stopIntent = createBroadcastPendingIntent(
            notificationId,
            ACTION_MEDIA_STOP,
            requestCodeOffset = 13
        )
        val builder = NotificationCompat.Builder(ctx, MEDIA_PLAYER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(songTitle)
            .setContentText(artistName)
            .setLargeIcon(albumArt)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            // Azioni
            .addAction(R.drawable.ic_previous, ctx.getString(R.string.notif_media_player_demo_previous), previousIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(R.drawable.ic_next, ctx.getString(R.string.notif_media_player_demo_next), nextIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )

        safeNotifyDemo(notificationId, builder)
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

    private fun createBroadcastPendingIntent(notificationId: Int, action: String, requestCodeOffset: Int = 0): PendingIntent {
        val context = getAppContext()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("notification_id", notificationId)
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
            NotificationsHelper.ACTION_ARCHIVE -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                Toast.makeText(context, "Azione: Archiviato (ID: $notificationId)", Toast.LENGTH_SHORT).show()
            }
            NotificationsHelper.ACTION_LATER -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
                Toast.makeText(context, "Azione: Più tardi (ID: $notificationId)", Toast.LENGTH_SHORT).show()
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
            NotificationsHelper.ACTION_MEDIA_PLAY_PAUSE -> {
                Toast.makeText(context, "Azione: Play/Pausa (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Play/Pause Toggled")
                FakeMediaPlayer.togglePlayPause()
                NotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )

            }
            NotificationsHelper.ACTION_MEDIA_NEXT -> {
                Toast.makeText(context, "Azione: Successivo (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Next Track")
                FakeMediaPlayer.nextTrack()
                NotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )
            }
            NotificationsHelper.ACTION_MEDIA_PREVIOUS -> {
                Toast.makeText(context, "Azione: Precedente (ID: $notificationId)", Toast.LENGTH_SHORT).show()
                Log.i("MediaPlayerAction", "Previous Track")
                FakeMediaPlayer.previousTrack()
                NotificationsHelper.showMediaPlayerNotification(
                    songTitle = FakeMediaPlayer.currentSong,
                    artistName = FakeMediaPlayer.currentArtist,
                    albumArt = FakeMediaPlayer.getAlbumArt(context),
                    isPlaying = FakeMediaPlayer.isPlaying
                )
            }
            else -> {
                Log.w("NotificationAction", "Azione sconosciuta: $action")
            }
        }
    }
}

/**
 * Oggetto Singleton fittizio per simulare un Media Player.
 */
object FakeMediaPlayer {
    var isPlaying = false
        private set
    var currentSong = "Nessuna canzone"
        private set
    var currentArtist = "Sconosciuto"
        private set

    private val playlist = listOf(
        Triple("Bohemian Rhapsody", "Queen", R.drawable.album_art_1),
        Triple("Stairway to Heaven", "Led Zeppelin", R.drawable.album_art_2),
        Triple("Hotel California", "Eagles", R.drawable.album_art_3)
    )
    private var currentTrackIndex = -1

    fun togglePlayPause() {
        if (playlist.isEmpty()) return
        if (currentTrackIndex == -1) {
            currentTrackIndex = 0
            updateTrackInfo()
        }
        isPlaying = !isPlaying
        Log.d("FakeMediaPlayer", "isPlaying: $isPlaying")
    }

    fun play() {
        if (playlist.isEmpty()) return
        if (currentTrackIndex == -1) {
            currentTrackIndex = 0
        }
        updateTrackInfo()
        isPlaying = true
        Log.d("FakeMediaPlayer", "Playing: $currentSong")
    }

    fun pause() {
        isPlaying = false
        Log.d("FakeMediaPlayer", "Paused: $currentSong")
    }

    fun nextTrack() {
        if (playlist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        updateTrackInfo()
        isPlaying = true
        Log.d("FakeMediaPlayer", "Next Track: $currentSong")
    }

    fun previousTrack() {
        if (playlist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) playlist.size - 1 else currentTrackIndex - 1
        updateTrackInfo()
        isPlaying = true
        Log.d("FakeMediaPlayer", "Previous Track: $currentSong")
    }

    private fun updateTrackInfo() {
        if (currentTrackIndex in playlist.indices) {
            val track = playlist[currentTrackIndex]
            currentSong = track.first
            currentArtist = track.second
        }
    }

    fun getAlbumArt(context: Context): Bitmap? {
        if (currentTrackIndex in playlist.indices) {
            val drawableId = playlist[currentTrackIndex].third
            return try {
                BitmapFactory.decodeResource(context.resources, drawableId)
            } catch (e: Exception) {
                Log.e("FakeMediaPlayer", "Error loading album art for drawable ID: $drawableId", e)
                try { BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album_art) }
                catch (e2: Exception) { null }
            }
        }
        return try { BitmapFactory.decodeResource(context.resources, R.drawable.ic_default_album_art) }
        catch (e: Exception) { null }
    }
}