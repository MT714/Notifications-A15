package com.embedded2025.notificationsa15.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.embedded2025.notificationsa15.R

/**
 * Classe del fragment relativa alla schermata home dell'app.
 */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageButton>(R.id.btn_previous).visibility = View.GONE
        view.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            findNavController().navigate(R.id.simpleNotificationFragment)
        }
    }
}