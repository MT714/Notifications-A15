package com.embedded2025.notificationsa15.chat

import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChatRepository(private val messageDao: MessageDao) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val messages: StateFlow<List<RepositoryMessage>> = messageDao.getAllMessages()
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun addUserMessage(text: String, immediateNotify: Boolean = false) {
        val message = RepositoryMessage(role = "user", content = text)
        messageDao.insertMessage(message)
        if (immediateNotify) DemoNotificationsHelper.showMessageNotification(getLastMessages(4))

        val request = TogetherRequest(
            messages = messages.value.map {
                Message(role = it.role, content = it.content)
            } + Message(role = "user", content = text)
        )

        try {
            val response = TogetherClient.api.getChatCompletion(request)

            val assistantMsg = response.choices.firstOrNull()?.message
            if (assistantMsg != null) {
                val assistantMessage =
                    RepositoryMessage(role = assistantMsg.role, content = assistantMsg.content)
                messageDao.insertMessage(assistantMessage)
                DemoNotificationsHelper.showMessageNotification(getLastMessages(4))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            messageDao.insertMessage(RepositoryMessage(
                role = "assistant",
                content = "Sorry, something went wrong. Please try again later."
            ))
        }
    }

    suspend fun getLastMessages(limit: Int) = messageDao.getLastMessages(limit).reversed()

    suspend fun clearChat() = messageDao.clearAllMessages()
}