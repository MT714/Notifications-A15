package com.embedded2025.notificationsa15

import android.app.Application
import com.embedded2025.notificationsa15.chat.ChatRepository
import com.embedded2025.notificationsa15.utils.NotificationHelper

/**
 * Classe che estende Application e gestisce l'inizializzazione dell'applicazione.
 * In particolare, viene chiamata la funzione initialize di NotificationHelper per
 * l'inizializzazione dei canali di notifica, e viene inizializzata la variabile
 * database con l'istanza del database Room utilizzando [AppDatabase.getDatabase].
 */
class NotificationsLabApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.initialize(this)
        database = AppDatabase.getDatabase(this)
        chatRepository = ChatRepository(database.messageDao())
    }

    companion object {
        lateinit var database: AppDatabase
            private set

        lateinit var chatRepository: ChatRepository
            private set
    }
}