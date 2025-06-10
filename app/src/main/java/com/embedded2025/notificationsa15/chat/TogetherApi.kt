package com.embedded2025.notificationsa15.chat

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

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