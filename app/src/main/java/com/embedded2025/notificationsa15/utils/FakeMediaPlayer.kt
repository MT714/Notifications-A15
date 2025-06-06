package com.embedded2025.notificationsa15.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.embedded2025.notificationsa15.R

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