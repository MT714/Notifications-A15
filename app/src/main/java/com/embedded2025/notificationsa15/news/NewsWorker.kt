package com.embedded2025.notificationsa15.news

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.embedded2025.notificationsa15.utils.NotificationHelper

/**
 * Classe worker per l'aggiornamento delle notizie di cronaca.
 */
class NewsWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    /**
     * Reperisce l'ultima notizia disponibile e pubblica la relativa notifica.
     */
    override suspend fun doWork(): Result {
        Log.i("NewsWorker", "NewsWorker called")

        return try {
            val response = GNewsClient.api.getTopHeadlines(API_KEY)

            if (response.articles.isEmpty()) return Result.failure()

            val article = response.articles.first()

            NotificationHelper.showNewsNotification(article)

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
