package me.mavenried.Ryde.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import me.mavenried.Ryde.ui.MainActivity

object NotificationHelper {
    const val TRACKING_NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "ryde_tracking"

    fun buildTrackingNotification(
        context: Context,
        distanceKm: Double = 0.0,
        elapsedMs: Long = 0L
    ): Notification {
        ensureChannel(context)
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val minutes = elapsedMs / 60_000
        val seconds = (elapsedMs % 60_000) / 1000
        val content = "%.2f km  •  %02d:%02d".format(distanceKm, minutes, seconds)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ryde — Tracking active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "GPS tracking active" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
