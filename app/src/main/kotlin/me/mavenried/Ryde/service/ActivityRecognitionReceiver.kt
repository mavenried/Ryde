package me.mavenried.Ryde.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import me.mavenried.Ryde.R
import me.mavenried.Ryde.ui.MainActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return

        val probable = result.probableActivities
            .filter { it.confidence >= 75 }
            .maxByOrNull { it.confidence } ?: return

        val activityLabel = when (probable.type) {
            DetectedActivity.RUNNING -> "running"
            DetectedActivity.ON_BICYCLE -> "cycling"
            DetectedActivity.WALKING -> "walking"
            else -> return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "auto_start"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Activity Detection", NotificationManager.IMPORTANCE_HIGH)
        )

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Looks like you're $activityLabel")
            .setContentText("Tap to start tracking in RYDE")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(2002, notification)
    }

    companion object {
        private fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ActivityRecognitionReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun enable(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            try {
                val transitions = listOf(
                    DetectedActivity.RUNNING,
                    DetectedActivity.ON_BICYCLE,
                    DetectedActivity.WALKING
                ).map { type ->
                    ActivityTransition.Builder()
                        .setActivityType(type)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                }
                ActivityRecognition.getClient(context)
                    .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(context))
            } catch (_: Exception) {}
        }

        fun disable(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            try {
                ActivityRecognition.getClient(context)
                    .removeActivityTransitionUpdates(pendingIntent(context))
            } catch (_: Exception) {}
        }
    }
}
