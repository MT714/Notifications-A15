package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R

class FinalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_final, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_previous_final).setOnClickListener {
            findNavController().navigate(R.id.mediaPlayerNotificationFragment)
        }
/*
        view.findViewById<ImageButton>(R.id.btn_exit_app).setOnClickListener {
            activity?.finishAffinity()
        }*/
    }
}