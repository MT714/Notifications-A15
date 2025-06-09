package com.embedded2025.notificationsa15.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.embedded2025.notificationsa15.R

object NotificationsHelper {
    object ChannelID {
        const val DEMO = "channel_demo"
        const val DEFAULT = "channel_default"
        const val MEDIA_PLAYER = "channel_media_player"
        const val WEATHER = "channel_weather"
        const val CALLS = "channel_call"
    }

    private object NotificationID {
        const val METEO = 1001
    }

    private lateinit var appContext: Context
    val ctx: Context
        get() = appContext

    val notifManager: NotificationManager
        get() = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var notificationIdCounter = 1000

    /**
     * Inizializza l'oggetto NotificationsHelper.
     *
     * @param context il contesto dell'applicazione.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Create channels
        val channels = listOf<NotificationChannel>(
            NotificationChannel(ChannelID.DEMO,
                ctx.getString(R.string.channel_demo_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_demo_description)
                setShowBadge(true)
            },
            NotificationChannel(ChannelID.DEFAULT,
                ctx.getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_default_description)
                setShowBadge(true)
            },
            NotificationChannel(ChannelID.MEDIA_PLAYER,
                ctx.getString(R.string.channel_media_player_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_media_player_description)
                setSound(null, null)
            },
            NotificationChannel(ChannelID.WEATHER,
                ctx.getString(R.string.channel_weather_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_weather_description)
                setShowBadge(true)
            },
            NotificationChannel(ChannelID.CALLS,
                ctx.getString(R.string.channel_calls_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_calls_description)
                setShowBadge(false)
            }
        )

        notifManager.createNotificationChannels(channels)
    }

    fun getUniqueId(): Int = notificationIdCounter++

    /**
     * Pubblica una notifica.
     *
     * @param id l'ID della notifica.
     * @param builder il builder della notifica.
     *
     * @return true se la notifica è stata pubblicata con successo, false altrimenti.
     */
    fun safeNotify(id: Int, builder: NotificationCompat.Builder): Boolean {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("NotificationsHelper", "Permission not granted.")

            return false
        }

        notifManager.notify(id, builder.build())
        return true
    }

    /**
     * Crea un builder di notifica con le impostazioni di base usate da ogni notifica.
     *
     * @param channelId l'ID del canale della notifica.
     * @param title il titolo della notifica.
     * @param text il testo della notifica.
     * @param destinationId l'ID della destinazione (fragment) della notifica.
     *
     * @return un builder di notifica con le impostazioni di base.
     */
    fun createBasicNotificationBuilder(channelId: String, title: String, text: String, destinationId: Int): NotificationCompat.Builder =
        NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(PendingIntentHelper.createWithDestination(destinationId))
            .setSmallIcon(R.drawable.ic_simple)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

    /**
     * Imposta il testo della notifica quando espansa.
     *
     * @param text il testo della notifica.
     *
     * @return il builder della notifica con il testo della notifica espansa.
     */
    fun NotificationCompat.Builder.setBigText(text: String): NotificationCompat.Builder =
        setStyle(NotificationCompat.BigTextStyle()
            .bigText(text))
            .setSmallIcon(R.drawable.ic_expandable)

    /**
     * Imposta l'immagine della notifica quando espansa.
     *
     * @param bitmap l'immagine della notifica.
     *
     * @return il builder della notifica con l'immagine della notifica espansa.
     */
    fun NotificationCompat.Builder.setBigPicture(bitmap: Bitmap?): NotificationCompat.Builder =
        setStyle(NotificationCompat.BigPictureStyle()
            .bigPicture(bitmap))
            .setSmallIcon(R.drawable.ic_expandable)

    /**
     * Cancella una notifica.
     *
     * @param notificationId l'ID della notifica da cancellare.
     */
    fun cancel(notificationId: Int) = notifManager.cancel(notificationId)

    /**
     * Pubblica una notifica di aggiornamento meteo.
     *
     * @param titolo il titolo -> Aggiornamento meteo a "" .
     * @param contenuto -> temperatura e meteo.
     * */
    fun showWeatherNotification(titolo: String, contenuto: String, iconCode: String = "01d") {
        val notif = createBasicNotificationBuilder(
            ChannelID.WEATHER,
            titolo,
            contenuto,
            R.id.simpleNotificationFragment
        )

        val smallIconRes = getWeatherIconRes(iconCode)
        notif.setSmallIcon(smallIconRes)
        safeNotify(NotificationID.METEO, notif)
    }

    fun getWeatherIconRes(iconCode: String): Int {
        //TODO reperire icone on rispettivo codice da https://openweathermap.org/weather-conditions#Icon-list
        /*return when (iconCode) {
            "01d" -> R.drawable.ic_weather_01d
            "01n" -> R.drawable.ic_weather_01n
            "02d" -> R.drawable.ic_weather_02d
            "02n" -> R.drawable.ic_weather_02n

        }*/
        return 1;
    }
}