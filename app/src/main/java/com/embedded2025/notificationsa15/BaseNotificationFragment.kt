package com.embedded2025.notificationsa15

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

abstract class BaseNotificationFragment : Fragment() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onNotificationPermissionGranted()
        } else {
            Log.i("NotificationsHelper", "Permesso notifiche negato.")
        }
    }

    fun checkAndRequestNotificationPermission(){
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNotificationPermissionGranted()
        }
    }

    abstract fun onNotificationPermissionGranted()
}