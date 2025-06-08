package com.embedded2025.notificationsa15.news

data class NewsResponse(
    val articles: List<Article>
)

data class Article(
    val title: String,
    val description: String?,
    val content: String?,
    val image: String?,
    val url: String
)