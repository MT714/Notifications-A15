package com.embedded2025.notificationsa15.fragments

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.embedded2025.notificationsa15.PlaybackService
import com.embedded2025.notificationsa15.R
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MediaPlayerNotificationFragment : Fragment() {
    private var controller: MediaController? = null
    private lateinit var playerView: PlayerView // Use lateinit for non-nullable views initialized in onCreateView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_media_player_notification, container, false)
        playerView = view.findViewById(R.id.player_view) // Initialize playerView here
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionToken = SessionToken(
            requireContext(),
            ComponentName(requireContext(), PlaybackService::class.java)
        )

        val controllerFuture: ListenableFuture<MediaController> =
            MediaController.Builder(requireContext(), sessionToken).buildAsync()

        controllerFuture.addListener({
                controller = controllerFuture.get()
                playerView.player = controller
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    override fun onStop() {
        super.onStop()
        // Release the controller when the fragment is stopped
        // to prevent leaks and ensure proper resource management.
        controller?.let {
            it.release()
            controller = null
        }
    }


    override fun onDestroyView() {
        playerView.player = null
        super.onDestroyView()
    }
}