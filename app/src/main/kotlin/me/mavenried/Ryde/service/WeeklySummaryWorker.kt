package me.mavenried.Ryde.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.mavenried.Ryde.R
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.repository.RouteRepository
import java.util.concurrent.TimeUnit

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = RouteRepository(AppDatabase.getInstance(applicationContext))
        val allRoutes = repository.getAllRoutesOnce().filter { it.completed }

        val weekMs = 7 * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val lastWeekRoutes = allRoutes.filter { it.startTime >= now - weekMs }

        if (lastWeekRoutes.isEmpty()) return Result.success()

        val totalKm = lastWeekRoutes.sumOf { it.distanceKm }
        val count = lastWeekRoutes.size
        val prevWeekRoutes = allRoutes.filter { it.startTime in (now - 2 * weekMs) until (now - weekMs) }
        val prevKm = prevWeekRoutes.sumOf { it.distanceKm }

        val trend = when {
            prevKm <= 0 -> ""
            totalKm > prevKm -> " · ↑ %.0f%% vs last week".format((totalKm - prevKm) / prevKm * 100)
            totalKm < prevKm -> " · ↓ %.0f%% vs last week".format((prevKm - totalKm) / prevKm * 100)
            else -> ""
        }

        val title = "Weekly summary"
        val body = "$count ${if (count == 1) "activity" else "activities"} · %.1f km$trend".format(totalKm)

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weekly_summary"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Weekly Summary", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        nm.notify(2001, notification)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "weekly_summary"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
