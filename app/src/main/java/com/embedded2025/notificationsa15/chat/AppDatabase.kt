package com.embedded2025.notificationsa15.chat

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<RepositoryMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RepositoryMessage)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Entity(tableName = "messages")
data class RepositoryMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timeStamp: Long = System.currentTimeMillis()
)
