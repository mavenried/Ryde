package me.mavenried.Ryde

import android.app.Application
import me.mavenried.Ryde.service.ActivityRecognitionReceiver
import me.mavenried.Ryde.service.WeeklySummaryWorker
import me.mavenried.Ryde.util.UserPrefs

class TrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (UserPrefs.isWeeklyNotificationEnabled(this)) WeeklySummaryWorker.schedule(this)
        if (UserPrefs.isAutoStartEnabled(this)) ActivityRecognitionReceiver.enable(this)
    }

    companion object {
        lateinit var instance: TrackerApp
            private set
    }
}
