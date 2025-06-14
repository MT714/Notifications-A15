package com.embedded2025.notificationsa15.fragments

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.services.PlaybackService
import com.embedded2025.notificationsa15.R
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MediaPlayerFragment : Fragment() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var playerView: PlayerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerView = view.findViewById(R.id.player_view)

        val serviceIntent = Intent(requireContext(), PlaybackService::class.java)
        requireContext().startForegroundService(serviceIntent)

        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()

        controllerFuture.addListener({
            playerView!!.player = controllerFuture.get()
        },
            ContextCompat.getMainExecutor(requireContext()))

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            findNavController().navigate(R.id.callNotificationFragment)
        }
        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.finalFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        playerView?.player = null
        MediaController.releaseFuture(controllerFuture)
        playerView = null
    }
}