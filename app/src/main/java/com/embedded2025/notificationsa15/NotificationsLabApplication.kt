package com.embedded2025.notificationsa15

import android.app.Application
import com.embedded2025.notificationsa15.chat.ChatRepository
import com.embedded2025.notificationsa15.utils.NotificationHelper

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