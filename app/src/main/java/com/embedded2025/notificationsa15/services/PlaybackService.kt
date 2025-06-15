package com.embedded2025.notificationsa15.services

import android.content.ComponentName
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.navigation.NavDeepLinkBuilder
import com.embedded2025.notificationsa15.MainActivity
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.AutoPlayPlayer
import com.embedded2025.notificationsa15.utils.ChannelID
import com.embedded2025.notificationsa15.utils.NotificationID

/**
 * Servizio in foreground dedicato al media player.
 */
@UnstableApi
class PlaybackService : MediaSessionService(), Player.Listener {
    /**
     * La sessione media
     */
    private var mediaSession: MediaSession? = null
    /**
     * Il player utilizzato.
     */
    private lateinit var player: AutoPlayPlayer

    /**
     * Crea la sessione ed il player, aggiungendoci la playlist.
     */
    override fun onCreate() {
        super.onCreate()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NotificationID.MEDIA_PLAYER)
            .setChannelId(ChannelID.MEDIA_PLAYER)
            .build()
        setMediaNotificationProvider(notificationProvider)

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

        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItems(playlist)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            addListener(this@PlaybackService)
        }

        player = AutoPlayPlayer(exoPlayer)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(NavDeepLinkBuilder(this)
                .setComponentName(ComponentName(this, MainActivity::class.java))
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.mediaPlayerNotificationFragment)
                .createPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    /**
     * Rilascia sessione e player.
     */
    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(this@PlaybackService)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

}