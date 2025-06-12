package com.embedded2025.notificationsa15.chat

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

object TogetherClient {
    private const val BASE_URL = "https://api.together.xyz/"

    val api: TogetherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TogetherApi::class.java)
    }
}

interface TogetherApi {
    @Headers(
        "Authorization: Bearer e28c10a8164656d110e1bf3d42cf8e4b6a33308f4dc76af9101e58d6f6b6086e",
        "content-type: application/json"
    )
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Body request: TogetherRequest
    ): TogetherResponse
}

data class TogetherRequest(
    val model: String = "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",
    val messages: List<TogetherMessage>
)

data class TogetherResponse(
    val choices: List<TogetherChoice>
)

data class TogetherChoice(
    val message: TogetherMessage
)

data class TogetherMessage(
    val role: String, // "user" o "assistant"
    val content: String
)
