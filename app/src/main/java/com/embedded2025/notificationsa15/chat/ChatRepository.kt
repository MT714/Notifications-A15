package com.embedded2025.notificationsa15.chat

import android.util.Log
import com.embedded2025.notificationsa15.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChatRepository(private val chatDao: ChatDao) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val messages: StateFlow<List<Message>> = chatDao.getAllMessages()
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun processUserMessageAndGetResponse(userInput: String, immediateNotify: Boolean = false): Message? {
        val userMessage = Message(role = "user", content = userInput)
        chatDao.insertMessage(userMessage)

        if (immediateNotify) NotificationHelper.showMessageNotification(getLastMessages(4))

        val request = TogetherRequest(
            messages = messages.value.map {
                TogetherMessage(role = it.role, content = it.content)
            } + TogetherMessage(role = "user", content = userInput)
        )

        try {
            val response: TogetherResponse = TogetherClient.api.getChatCompletion(request)
            val botReply = response.choices.firstOrNull()?.message

            if (botReply != null) {
                val botMessage = Message(role = botReply.role, content = botReply.content)
                chatDao.insertMessage(botMessage)
                return botMessage
            } else {
                chatDao.insertMessage(Message(
                    role = "system",
                    content = "Sorry, something went wrong. Please try again later."
                ))
                return null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching bot response", e)
            chatDao.insertMessage(Message(
                role = "system",
                content = "Sorry, something went wrong. Please try again later."
            ))
            return null
        }
    }

    suspend fun getLastMessages(limit: Int) = chatDao.getLastMessages(limit).reversed()

    suspend fun clearChat() = chatDao.clearAllMessages()
}