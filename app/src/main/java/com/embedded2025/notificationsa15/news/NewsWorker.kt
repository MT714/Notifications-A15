package com.embedded2025.notificationsa15.news

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationsHelper
import com.embedded2025.notificationsa15.utils.NotificationsHelper.setBigPicture
import com.embedded2025.notificationsa15.utils.NotificationsHelper.setBigText
import com.embedded2025.notificationsa15.utils.PendingIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

class NewsWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    private val api: NewsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }

    override suspend fun doWork(): Result {
        Log.i("NewsWorker", "NewsWorker called")

        return try {
            val response = api.getTopHeadlines(API_KEY)

            if (response.articles.isEmpty()) return Result.failure()

            val article = response.articles.first()

            showNotification(article)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun showNotification(article: Article) {
        NotificationsHelper.initialize(applicationContext)

        // Scarica immagine da URL (in background)
        val bigPicture: Bitmap? = article.image?.let { imageUrl ->
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeStream(URL(imageUrl).openStream())
                } catch (_: Exception) {
                    null
                }
            }
        }

        val notif = NotificationsHelper.createBasicNotificationBuilder(
            NotificationsHelper.ChannelID.DEFAULT,
            article.title,
            article.description ?: "",
            R.id.expandableNotificationFragment
        )
            .setBigText(article.content ?: article.description ?: "")
            .setAutoCancel(true)
            .setContentIntent(PendingIntentHelper.createOpenUrlIntent(article.url))

        if (bigPicture != null) {
            notif.setBigPicture(bigPicture)
                .setLargeIcon(bigPicture)
        }

        NotificationsHelper.safeNotify(NotificationsHelper.getUniqueId(), notif)
    }

    companion object {
        const val WORKER_NAME = "news_worker"

        private const val BASE_URL = "https://gnews.io/api/"
        private const val API_KEY = "a1132ed12f858cb217c24f92d51874a2"
    }
}
