package com.embedded2025.notificationsa15.chat

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ChatDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastMessages(limit: Int): List<Message>

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
