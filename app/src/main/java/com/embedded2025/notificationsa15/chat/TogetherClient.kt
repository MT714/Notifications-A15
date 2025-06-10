package com.embedded2025.notificationsa15.chat

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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