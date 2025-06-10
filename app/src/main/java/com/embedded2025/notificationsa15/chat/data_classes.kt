package com.embedded2025.notificationsa15.chat

data class TogetherRequest(
    val model: String = "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",
    val messages: List<Message>
)

data class TogetherResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class Message(
    val role: String, // "user" o "assistant"
    val content: String
)
