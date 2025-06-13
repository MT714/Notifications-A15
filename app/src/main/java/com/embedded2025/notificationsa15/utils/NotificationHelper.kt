package com.embedded2025.notificationsa15.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkBuilder
import com.embedded2025.notificationsa15.MainActivity
import com.embedded2025.notificationsa15.NotificationActionReceiver
import com.embedded2025.notificationsa15.NotificationActionReceiver.IntentExtras
import com.embedded2025.notificationsa15.NotificationActionReceiver.NotificationAction
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.chat.Message
import com.embedded2025.notificationsa15.news.GNewsArticle
import com.embedded2025.notificationsa15.services.CallService.Companion.getStartCallIntent
import com.embedded2025.notificationsa15.services.LiveUpdateService.Companion.getStartLiveUpdateIntent
import com.embedded2025.notificationsa15.services.ProgressService.Companion.getStartProgressIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.collections.forEach

object ChannelID {
    const val DEMO = "channel_demo"
    const val ACTIONS = "channel_actions"
    const val WEATHER = "channel_weather"
    const val NEWS = "channel_news"
    const val CHAT = "channel_chat"
    const val SERVICES = "channel_services"
    const val MEDIA_PLAYER = "channel_media_player"
    const val CALLS = "channel_call"
}

object ChannelGroupID {
    const val DEMO = "group_demo"
    const val UPDATES = "group_updates"
    const val MULTIMEDIA = "group_multimedia"
}

object NotificationID {
    const val CHAT = 0
    const val WEATHER = 1
    const val PROGRESS = 2
    const val LIVE_UPDATE = 3
    const val CALL = 4
    const val MEDIA_PLAYER = 5

    object Demo {
        const val SIMPLE = 100
        const val EXPANDABLE_TEXT = 101
        const val EXPANDABLE_PICTURE = 102
        const val ACTIONS = 103
        const val REPLY = 104
        const val INBOX_1 = 105
        const val INBOX_2 = 106
        const val INBOX_3 = 107
        const val INBOX_SUMMARY = 108
    }
}

object NotificationHelper {
    private lateinit var appContext: Context
    val ctx get() = appContext

    val notifManager: NotificationManager by lazy {
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var notificationIdCounter = 1000

    /**
     * Inizializza l'oggetto NotificationHelper.
     *
     * @param context il contesto dell'applicazione.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Crea i gruppi di canali
        notifManager.createNotificationChannelGroups(listOf(
            NotificationChannelGroup(ChannelGroupID.DEMO, ctx.getString(R.string.group_demo)),
            NotificationChannelGroup(ChannelGroupID.UPDATES, ctx.getString(R.string.group_updates)),
            NotificationChannelGroup(ChannelGroupID.MULTIMEDIA, ctx.getString(R.string.group_multimedia))
        ))

        // Crea i canali
        notifManager.createNotificationChannels(listOf(
            NotificationChannel(
                ChannelID.DEMO,
                ctx.getString(R.string.channel_demo_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_demo_description)
                group = ChannelGroupID.DEMO
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.ACTIONS,
                ctx.getString(R.string.channel_actions_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_actions_description)
                group = ChannelGroupID.DEMO
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.WEATHER,
                ctx.getString(R.string.channel_weather_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_weather_description)
                group = ChannelGroupID.UPDATES
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.NEWS,
                ctx.getString(R.string.channel_news_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_news_description)
                group = ChannelGroupID.UPDATES
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.SERVICES,
                ctx.getString(R.string.channel_services_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_services_description)
                group = ChannelGroupID.DEMO
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.CHAT,
                ctx.getString(R.string.channel_chat_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_chat_description)
                group = ChannelGroupID.MULTIMEDIA
                setShowBadge(true)
            },
            NotificationChannel(
                ChannelID.MEDIA_PLAYER,
                ctx.getString(R.string.channel_media_player_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_media_player_description)
                group = ChannelGroupID.MULTIMEDIA
            },
            NotificationChannel(
                ChannelID.CALLS,
                ctx.getString(R.string.channel_calls_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_calls_description)
                group = ChannelGroupID.MULTIMEDIA
            }
        ))
    }

    /**
     * Ottiene un ID univoco per una notifica, a partire da 1000.
     *
     * @return un ID univoco per una notifica.
     */
    fun getUniqueId() = notificationIdCounter++

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
            Log.i("NotificationHelper", "Permission not granted.")

            return false
        }

        notifManager.notify(id, builder.build())
        return true
    }

    /**
     * Pubblica una notifica demo.
     * Differisce da [safeNotify] in quanto se le notifiche non sono abilitate oppure il canale demo
     * non è visibile, allora l'utente viene reindirizzato alle relative impostazioni di sistema.
     *
     * @param id l'ID della notifica
     * @param builder il builder della notifica
     *
     * @return true se la notifica è stata pubblicata con successo, false altrimenti
     *
     * @see safeNotify
     */
    fun safeNotifyDemo(id: Int, builder: NotificationCompat.Builder): Boolean {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("NotificationHelper", "Permission not granted, opening settings.")
            ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })

            return false
        }
        if (notifManager.getNotificationChannel(ChannelID.DEMO).importance
            == NotificationManager.IMPORTANCE_NONE
        ) {
            Log.i("NotificationHelper", "Notification channel is not visible, opening settings.")

            ctx.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, ChannelID.DEMO)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        notifManager.notify(id, builder.build())
        return true
    }

    /**
     * Crea un builder di notifica con le impostazioni di base usate da ogni notifica.
     *
     * @param channelId l'ID del canale della notifica.
     * @param iconId l'ID dell'icona piccola.
     * @param title il titolo della notifica.
     *
     * @return un builder di notifica con le impostazioni di base.
     */
    fun createBasicBuilder(channelId: String, iconId: Int, title: String) =
        NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(title)
            .setSmallIcon(iconId)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    /**
     * Imposta il testo della notifica quando espansa.
     *
     * @param text il testo della notifica.
     *
     * @return il builder della notifica con il testo della notifica espansa.
     */
    fun NotificationCompat.Builder.setBigText(text: String) =
        setStyle(NotificationCompat.BigTextStyle().bigText(text))

    /**
     * Imposta l'immagine della notifica quando espansa.
     *
     * @param bitmap l'immagine della notifica.
     *
     * @return il builder della notifica con l'immagine della notifica espansa.
     */
    fun NotificationCompat.Builder.setBigPicture(bitmap: Bitmap?) =
        setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))

    /**
     * Imposta il fragment di destinazione della notifica quando cliccata.
     *
     * @param destinationId l'ID del fragment di destinazione.
     *
     * @return il builder della notifica con il fragment di destinazione impostato.
     */
    fun NotificationCompat.Builder.setDestinationFragment(destinationId: Int) =
        setContentIntent(
            NavDeepLinkBuilder(ctx)
                .setComponentName(ComponentName(ctx, MainActivity::class.java))
                .setGraph(R.navigation.nav_graph)
                .setDestination(destinationId)
                .createPendingIntent()
        )

    fun NotificationCompat.Builder.setDestinationUrl(url: String): NotificationCompat.Builder {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return setContentIntent(PendingIntent.getActivity(ctx, 0, intent, flags))
    }

    /**
     * Crea un PendingIntent per un'azione di notifica, con le impostazioni specificate.
     *
     * @param action l'azione da eseguire.
     * @param extras eventuali dati aggiuntivi da passare con l'azione.
     */
    fun createBroadcastIntent(action: String, extras: Bundle? = null): PendingIntent {
        val intent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            setAction(action)
            if (extras != null) putExtras(extras)
            setPackage("com.embedded2025.notificationsa15")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(ctx, 0, intent, flags)
    }

    fun NotificationCompat.Builder.addReplyAction(extras: Bundle?): NotificationCompat.Builder {
        val remoteInput = RemoteInput.Builder(IntentExtras.KEY_TEXT_REPLY)
            .setLabel(ctx.getString(R.string.notif_reply_label))
            .build()

        val replyAction = Action.Builder(
            R.drawable.ic_reply,
            ctx.getString(R.string.notif_reply_action),
            createBroadcastIntent(NotificationAction.REPLY, extras)
        )
            .addRemoteInput(remoteInput)
            .build()

        return addAction(replyAction)
    }

    /**
     * Cancella una notifica.
     *
     * @param notificationId l'ID della notifica da cancellare.
     */
    fun cancel(notificationId: Int) = notifManager.cancel(notificationId)

    /**
     * Crea e pubblica una notifica demo semplice.
     */
    fun showSimpleNotification() {
        val builder = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_simple,
            ctx.getString(R.string.notif_simple_demo_title),
        )
            .setDestinationFragment(R.id.simpleNotificationFragment)
            .setContentText(ctx.getText(R.string.notif_simple_demo_text))
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.SIMPLE, builder)
    }

    /**
     * Crea e pubblica una notifica demo espandibile con testo.
     */
    fun showExpandableTextNotification() {
        val builder = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_expandable,
            ctx.getString(R.string.notif_expandable_demo_title)
        )
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setDestinationFragment(R.id.expandableNotificationFragment)
            .setBigText(ctx.getString(R.string.notif_expandable_demo_bigtext))
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.EXPANDABLE_TEXT, builder)
    }

    /**
     * Crea e pubblica una notifica demo espandibile con immagine.
     */
    fun showExpandablePictureNotification() {
        val builder = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_expandable,
            ctx.getString(R.string.notif_expandable_demo_title)
        )
            .setContentText(ctx.getString(R.string.notif_expandable_demo_text))
            .setDestinationFragment(R.id.expandableNotificationFragment)
            .setBigPicture(getDrawable(ctx, R.drawable.project_logo)?.toBitmap())
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.EXPANDABLE_PICTURE, builder)
    }

    /**
     * Crea e pubblica una notifica demo di aggiornamento in gruppo.
     */
    fun showGroupedInboxNotifications() {
        val groupKey = "com.embedded2025.notificationsa15.EMAIL_GROUP"
        // Notifica 1
        val builder1 = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_email,
            ctx.getString(R.string.notif_inbox_1_title)
        )
            .setDestinationFragment(R.id.emailNotificationFragment)
            .setContentText(ctx.getString(R.string.notif_inbox_1_content))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(ctx.getString(R.string.notif_inbox_1_line_1))
                    .addLine(ctx.getString(R.string.notif_inbox_1_line_2))
            )
            .setGroup(groupKey)
            .setAutoCancel(true)

        // Notifica 2
        val builder2 = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_email,
            ctx.getString(R.string.notif_inbox_2_title)
        )
            .setDestinationFragment(R.id.emailNotificationFragment)
            .setContentText(ctx.getString(R.string.notif_inbox_2_content))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(ctx.getString(R.string.notif_inbox_2_line_1))
                    .setSummaryText(ctx.getString(R.string.notif_inbox_2_line_2))
            )
            .setGroup(groupKey)
            .setAutoCancel(true)

        // Notifica 3
        val builder3 = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_email,
            ctx.getString(R.string.notif_inbox_3_title)
        )
            .setDestinationFragment(R.id.emailNotificationFragment)
            .setContentText(ctx.getString(R.string.notif_inbox_3_content))
            .setGroup(groupKey)
            .setAutoCancel(true)

        val summaryBuilder = createBasicBuilder(
            ChannelID.DEMO,
            R.drawable.ic_email,
            ctx.getString(R.string.notif_inbox_summary_title)
        )
            .setDestinationFragment(R.id.emailNotificationFragment)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(ctx.getString(R.string.notif_inbox_summary_line_1))
                    .addLine(ctx.getString(R.string.notif_inbox_summary_line_2))
                    .addLine(ctx.getString(R.string.notif_inbox_summary_line_3))
                    .setBigContentTitle(ctx.getString(R.string.notif_inbox_summary_big_content))
                    .setSummaryText(ctx.getString(R.string.notif_inbox_summary_text))
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.INBOX_1, builder1)
        safeNotifyDemo(NotificationID.Demo.INBOX_2, builder2)
        safeNotifyDemo(NotificationID.Demo.INBOX_3, builder3)
        safeNotifyDemo(NotificationID.Demo.INBOX_SUMMARY, summaryBuilder)
    }

    /**
     * Crea e pubblica una notifica demo di risposta.
     */
    fun showReplyNotification() {
        val extras = Bundle().apply {
            putInt(IntentExtras.NOTIFICATION_ID, NotificationID.Demo.REPLY)
            putBoolean(IntentExtras.IS_DEMO, true)
        }

        val builder = createBasicBuilder(
            ChannelID.ACTIONS,
            R.drawable.ic_reply,
            ctx.getString(R.string.notif_reply_demo_title)
        )
            .addReplyAction(extras)
            .setDestinationFragment(R.id.actionsNotificationFragment)
            .setContentText(ctx.getString(R.string.notif_reply_demo_text))
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.REPLY, builder)
    }

    /**
     * Crea e pubblica una notifica demo con azioni.
     */
    fun showActionNotification() {
        val extras = Bundle().apply {
            putInt(IntentExtras.NOTIFICATION_ID, NotificationID.Demo.ACTIONS)
            putBoolean(IntentExtras.IS_DEMO, true)
        }
        val redIntent = createBroadcastIntent(NotificationAction.SET_RED, extras)
        val yellowIntent = createBroadcastIntent(NotificationAction.SET_YELLOW, extras)

        val builder = createBasicBuilder(
            ChannelID.ACTIONS,
            R.drawable.ic_action,
            ctx.getString(R.string.notif_action_demo_title)
        )
            .setDestinationFragment(R.id.actionsNotificationFragment)
            .setContentText(ctx.getString(R.string.notif_action_demo_text))
            .addAction(R.drawable.ic_red, ctx.getString(R.string.notif_action_set_red), redIntent)
            .addAction(R.drawable.ic_yellow, ctx.getString(R.string.notif_action_set_yellow), yellowIntent)
            .setAutoCancel(true)

        safeNotifyDemo(NotificationID.Demo.ACTIONS, builder)
    }

    /**
     * Crea e pubblica una notifica demo di progresso.
     */
    fun showProgressNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("NotificationHelper", "Permesso POST_NOTIFICATIONS non concesso. Non avvio il servizio di progresso.")
            Toast.makeText(ctx, ctx.getString(R.string.toast_permission_required_service), Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = getStartProgressIntent(ctx)
        ctx.startForegroundService(serviceIntent)
        Log.d("NotificationHelper", "Richiesta di avvio NotificationService per progresso inviata.")
    }

    /**
     * Crea e pubblica una notifica demo di aggiornamenti live.
     */
    fun showLiveUpdateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
        {
            Log.w("NotificationHelper", "Permesso POST_NOTIFICATIONS non concesso. Non avvio il servizio.")
            Toast.makeText(ctx, ctx.getString(R.string.toast_permission_required_service), Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = getStartLiveUpdateIntent(ctx)
        ctx.startForegroundService(serviceIntent)
        Log.d("NotificationHelper", "Richiesta di avvio NotificationService per live update inviata.")
    }

    /**
     * Crea e pubblica una notifica demo di chiamata fittizia.
     */
    fun showCallNotification(delayInSeconds: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("NotificationHelper", "Permesso POST_NOTIFICATIONS non concesso. Non avvio il servizio di chiamata.")
            Toast.makeText(ctx, ctx.getString(R.string.toast_permission_required_service), Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = getStartCallIntent(ctx, delayInSeconds)
        ctx.startForegroundService(serviceIntent)
        Log.d("NotificationHelper", "Richiesta di avvio del service per chiamata fittizia inviata.")
    }

    /**
     * Pubblica una notifica di aggiornamento meteo.
     *
     * @param titolo il titolo -> Aggiornamento meteo a "" .
     * @param contenuto -> temperatura e meteo.
     * */
    fun showWeatherNotification(titolo: String, contenuto: String, iconCode: String = "01d") {
        val builder = createBasicBuilder(
            ChannelID.WEATHER,
            getWeatherIconRes(iconCode),
            titolo
        )
            .setDestinationFragment(R.id.simpleNotificationFragment)
            .setContentText(contenuto)
            .setLargeIcon(getDrawable(ctx, getWeatherIconRes(iconCode))?.toBitmap())
            .setAutoCancel(true)

        safeNotify(NotificationID.WEATHER, builder)
    }

    /**
     * Ottiene la risorsa dell'icona meteo in base al codice dell'icona.
     * Non comprende le icone relative alle meteo di notte, ma solo di giorno
     *
     * @param iconCode il codice dell'icona.
     * @return la risorsa dell'icona.
     */
    private fun getWeatherIconRes(iconCode: String): Int {
        return when (iconCode) {
            "01d" -> R.drawable.ic_weather_01d
            "02d" -> R.drawable.ic_weather_02d
            "03d" -> R.drawable.ic_weather_03d
            "04d" -> R.drawable.ic_weather_04d
            "09d" -> R.drawable.ic_weather_09d
            "10d" -> R.drawable.ic_weather_10d
            "11d" -> R.drawable.ic_weather_11d
            "13d" -> R.drawable.ic_weather_13d
            "50d" -> R.drawable.ic_weather_50d

            else -> {
                R.drawable.ic_meteo
            }
        }
    }

    suspend fun showNewsNotification(article: GNewsArticle) {
        // Scarica immagine da URL (in background)
        val bigPicture: Bitmap? = article.image?.let { imageUrl ->
            withContext(Dispatchers.IO) {
                try { BitmapFactory.decodeStream(URL(imageUrl).openStream()) }
                catch (_: Exception) { null }
            }
        }

        val builder = createBasicBuilder(
            ChannelID.NEWS,
            R.drawable.ic_expandable,
            article.title
        )
            .setContentText(article.description ?: "")
            .setDestinationUrl(article.url)
            .setBigText(article.content ?: article.description ?: "")
            .setAutoCancel(true)


        if (bigPicture != null)
            builder.setBigPicture(bigPicture)
                .setLargeIcon(bigPicture)

        safeNotify(getUniqueId(), builder)
    }

    fun showMessageNotification(messages: List<Message>) {
        val extras = Bundle().apply {
            putInt(IntentExtras.NOTIFICATION_ID, NotificationID.CHAT)
            putBoolean(IntentExtras.IS_DEMO, false)
        }

        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Assistente").build())
        messages.forEach {
            style.addMessage(it.content, it.timestamp, Person.Builder().setName(it.role).build())
        }

        val builder = createBasicBuilder(
            ChannelID.CHAT,
            R.drawable.ic_chat,
            ctx.getString(R.string.notif_chat_title)
        )
            .addReplyAction(extras)
            .setDestinationFragment(R.id.chatNotificationFragment)
            .setAutoCancel(true)
            .setStyle(style)

        safeNotify(NotificationID.CHAT, builder)
    }
}