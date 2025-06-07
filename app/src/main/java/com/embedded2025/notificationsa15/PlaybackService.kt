package com.embedded2025.notificationsa15

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            addMediaItems(listOf(
                MediaItem.fromUri("android.resource://$packageName/${R.raw.bohemian_rhapsody}".toUri()),
                MediaItem.fromUri("android.resource://$packageName/${R.raw.roundabout}".toUri()),
                MediaItem.fromUri("android.resource://$packageName/${R.raw.stairway_to_heaven}".toUri())
            ))
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