package me.mavenried.Ryde

import android.app.Application

class TrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TrackerApp
            private set
    }
}
