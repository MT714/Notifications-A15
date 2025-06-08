package com.embedded2025.notificationsa15

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

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
    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setMediaItems(playlist)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}