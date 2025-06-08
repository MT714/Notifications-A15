package com.embedded2025.notificationsa15

import android.app.Service
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.embedded2025.notificationsa15.utils.AutoPlayForwardingPlayer

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var forwardingPlayer: AutoPlayForwardingPlayer
    private lateinit var playerListener: Player.Listener

    private val playlist = listOf(
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

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItems(playlist)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }

        playerListener = PlayerListener()
        player.addListener(playerListener)

        forwardingPlayer = AutoPlayForwardingPlayer(player)

        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        player.removeListener(playerListener)
        mediaSession?.run {
            this.player.release()
            this.release()
            mediaSession = null
        }
        super.onDestroy()
    }
}