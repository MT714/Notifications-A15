package com.embedded2025.notificationsa15

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationsHelper {
    const val DEMO_CHANNEL_ID = "channel_demo"
    const val DEMO_SIMPLE_NOTIFICATION_ID = 1

    fun safeNotify(ctx: Context, builder: NotificationCompat.Builder, id: Int) {
        with(NotificationManagerCompat.from(ctx)) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_DENIED) {
                Log.i("NotificationsHelper", "No permission granted")
                // TODO: Consider calling
                // ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                //                                        grantResults: IntArray)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(id, builder.build())
        }
    }

    fun createNotificationChannels(ctx: Context) {
        val channels = mutableListOf<NotificationChannel>()

        channels.add(NotificationChannel(DEMO_CHANNEL_ID, ctx.getString(R.string.channel_demo_name), NotificationManager.IMPORTANCE_HIGH)
                .apply {
            description = ctx.getString(R.string.channel_demo_description)
        })

        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannels(channels)
    }

    fun showSimpleNotificationDemo(ctx: Context) {
        val notif = NotificationCompat.Builder(ctx, DEMO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(ctx.getString(R.string.notif_simple_demo_title))
            .setContentText(ctx.getString(R.string.notif_simple_demo_text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        safeNotify(ctx, notif, DEMO_SIMPLE_NOTIFICATION_ID)
    }
}