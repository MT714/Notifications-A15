package com.embedded2025.notificationsa15.news

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.embedded2025.notificationsa15.R
import com.embedded2025.notificationsa15.utils.NotificationHelper
import com.embedded2025.notificationsa15.utils.NotificationHelper.setBigPicture
import com.embedded2025.notificationsa15.utils.NotificationHelper.setBigText
import com.embedded2025.notificationsa15.utils.NotificationHelper.setDestinationUrl
import com.embedded2025.notificationsa15.utils.ChannelID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class NewsWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("NewsWorker", "NewsWorker called")

        return try {
            val response = GNewsClient.api.getTopHeadlines(API_KEY)

            if (response.articles.isEmpty()) return Result.failure()

            val article = response.articles.first()

            NotificationHelper.showNotification(article)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORKER_NAME = "news_worker"

        private const val API_KEY = "a1132ed12f858cb217c24f92d51874a2"
    }
}
