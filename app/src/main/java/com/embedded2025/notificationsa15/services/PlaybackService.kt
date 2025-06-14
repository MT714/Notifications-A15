package com.embedded2025.notificationsa15.services

import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.AutoPlayForwardingPlayer
import com.embedded2025.notificationsa15.utils.ChannelID
import com.embedded2025.notificationsa15.utils.NotificationID

@UnstableApi
class PlaybackService : MediaSessionService(), Player.Listener {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var forwardingPlayer: AutoPlayForwardingPlayer

    override fun onCreate() {
        super.onCreate()

        val playlist = listOf(
            MediaItem.Builder()
                .setUri("android.resource://$packageName/${R.raw.bohemian_rhapsody}".toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Bohemian Rhapsody")
                        .setArtist("Queen")
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setUri("android.resource://$packageName/${R.raw.roundabout}".toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Roundabout")
                        .setArtist("Yes")
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setUri("android.resource://$packageName/${R.raw.stairway_to_heaven}".toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Stairway to Heaven")
                        .setArtist("Led Zeppelin")
                        .build()
                )
                .build()
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NotificationID.MEDIA_PLAYER)
            .setChannelId(ChannelID.MEDIA_PLAYER)
            .build()
        setMediaNotificationProvider(notificationProvider)

        player = ExoPlayer.Builder(this).build().apply {
            setMediaItems(playlist)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            addListener(this@PlaybackService)
        }

        forwardingPlayer = AutoPlayForwardingPlayer(player)
        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(this@PlaybackService)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        if (!isPlaying) {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }
}