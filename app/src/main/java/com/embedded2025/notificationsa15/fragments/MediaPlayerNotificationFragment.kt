package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.utils.FakeMediaPlayer
import com.embedded2025.notificationsa15.utils.NotificationsHelper

class MediaPlayerNotificationFragment : Fragment() {
    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_media_player_notification, container, false)
        val playButton: Button = view.findViewById(R.id.btnMediaPlayerNotification)
        playButton.setOnClickListener {
            FakeMediaPlayer.play()
            DemoNotificationsHelper.showMediaPlayerNotification(
                songTitle = FakeMediaPlayer.currentSong,
                artistName = FakeMediaPlayer.currentArtist,
                albumArt = FakeMediaPlayer.getAlbumArt(requireContext()),
                isPlaying = FakeMediaPlayer.isPlaying
            )
        }
        return view
    }
}