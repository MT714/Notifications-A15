package com.embedded2025.notificationsa15.chat

import com.embedded2025.notificationsa15.utils.DemoNotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

class ChatRepository(private val messageDao: MessageDao) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val messages: StateFlow<List<RepositoryMessage>> = messageDao.getAllMessages()
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun addUserMessage(text: String) {
        val message = RepositoryMessage(role = "user", content = text)
        messageDao.insertMessage(message)

        val request = TogetherRequest(
            messages = messages.value.map {
                Message(role = it.role, content = it.content)
            } + Message(role = "user", content = text)
        )

        val response = TogetherClient.api.getChatCompletion(request)

        val assistantMsg = response.choices.firstOrNull()?.message
        if (assistantMsg != null) {
            messageDao.insertMessage(RepositoryMessage(
                role = assistantMsg.role,
                content = assistantMsg.content
            ))
            DemoNotificationsHelper.showMessageNotification(assistantMsg.content)
        }
    }

    suspend fun clearChat() = messageDao.clearAllMessages()
}