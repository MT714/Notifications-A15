package com.embedded2025.notificationsa15.utils

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.navigation.NavDeepLinkBuilder
import com.embedded2025.notificationsa15.MainActivity
import com.embedded2025.notificationsa15.NotificationActionReceiver
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationsHelper.ctx
import androidx.core.net.toUri

object PendingIntentHelper {

    /**
     * Crea un PendingIntent per aprire una destinazione specifica della navigazione.
     *
     * @param destination l'ID della destinazione da aprire.
     */
    fun createWithDestination(destination: Int): PendingIntent =
        NavDeepLinkBuilder(ctx)
            .setComponentName(ComponentName(ctx, MainActivity::class.java))
            .setGraph(R.navigation.nav_graph)
            .setDestination(destination)
            .createPendingIntent()

    /**
     * Crea un PendingIntent per un'azione di notifica.
     *
     * @param action l'azione da eseguire.
     * @param extras eventuali dati aggiuntivi da passare con l'azione.
     */
    fun createBroadcast(action: String, extras: Bundle? = null): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            this.action = action
            if (extras != null) putExtras(extras)
            setPackage("com.embedded2025.notificationsa15")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(ctx, 0, intent, flags)
    }

    fun createOpenUrlIntent(url: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(ctx, 0, intent, flags)
    }
}
