package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import com.embedded2025.notificationsa15.NotificationService
import com.embedded2025.notificationsa15.R

class MediaPlayerNotificationFragment : Fragment() {
    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_media_player_notification, container, false)
        val playButton: Button = view.findViewById(R.id.btnMediaPlayerNotification)

        playButton.setOnClickListener {
            //val currentSong = FakeMediaPlayer.currentSong
            //val currentArtist = FakeMediaPlayer.currentArtist
            DemoNotificationsHelper.showMediaPlayerNotification(
                context = requireContext(),
                mediaAction = NotificationService.ACTION_MEDIA_PLAY,
                //songTitle = if (currentSong == "Nessuna canzone") null else currentSong,
                //artistName = if (currentArtist == "Sconosciuto") null else currentArtist
            )
        }
        return view
    }
}