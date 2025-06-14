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
class MediaPlayerNotificationFragment : Fragment() {
    private var controller: MediaController? = null
    private lateinit var playerView: PlayerView // Use lateinit for non-nullable views initialized in onCreateView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_media_player, container, false)
        playerView = view.findViewById(R.id.player_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serviceIntent = Intent(requireContext(), PlaybackService::class.java)
        requireContext().startService(serviceIntent)

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

        view.findViewById<ImageButton>(R.id.btn_previous).setOnClickListener(){
           findNavController().navigate(R.id.callNotificationFragment)
        }
        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener(){
            findNavController().navigate(R.id.finalFragment)
        }
    }

    override fun onStop() {
        super.onStop()
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