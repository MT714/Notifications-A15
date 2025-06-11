package com.embedded2025.notificationsa15

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.embedded2025.notificationsa15.chat.MessageDao
import com.embedded2025.notificationsa15.chat.RepositoryMessage

@Database(entities = [RepositoryMessage::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?:
        synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chat_database"
            )
                .build()
            INSTANCE = instance
            instance
        }
    }
}