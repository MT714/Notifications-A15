package com.embedded2025.notificationsa15.news

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Client di GNews.
 */
object GNewsClient {
    private const val BASE_URL = "https://gnews.io/api/"

    val api: GNewsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GNewsApi::class.java)
    }
}

/**
 * Interfaccia API di GNews.
 */
interface GNewsApi {
    @GET("v4/top-headlines")
    suspend fun getTopHeadlines(
        @Query("token") apiKey: String,
        @Query("lang") lang: String = "it",
        @Query("max") max: Int = 1
    ): GNewsResponse
}

data class GNewsResponse(
    val articles: List<GNewsArticle>
)

data class GNewsArticle(
    val title: String,
    val description: String?,
    val content: String?,
    val image: String?,
    val url: String
)