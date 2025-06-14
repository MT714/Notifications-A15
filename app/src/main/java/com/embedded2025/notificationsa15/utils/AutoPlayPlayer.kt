package com.embedded2025.notificationsa15.utils

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Questo Player personalizzato inoltra tutti i comandi all'istanza originale di ExoPlayer,
 * ma intercetta i comandi 'seekToNext' e 'seekToPrevious' per forzare la riproduzione.
 */
@UnstableApi
class AutoPlayForwardingPlayer(player: ExoPlayer): ForwardingPlayer(player) {

    override fun seekToNext() {
        super.seekToNext()

        if (!playWhenReady) play()
    }

    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()

        if (!playWhenReady) play()
    }

    override fun seekToPrevious() {
        super.seekToPrevious()

        if (!playWhenReady) play()
    }

    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()

        if (!playWhenReady) play()
    }
}