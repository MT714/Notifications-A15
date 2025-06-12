package com.embedded2025.notificationsa15.utils

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.embedded2025.notificationsa15.utils.NotificationHelper.ctx
import androidx.core.net.toUri

object PendingIntentHelper {

    fun createOpenUrlIntent(url: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(ctx, 0, intent, flags)
    }
}
