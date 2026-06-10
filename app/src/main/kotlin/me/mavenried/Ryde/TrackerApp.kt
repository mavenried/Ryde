package me.mavenried.Ryde

import android.app.Application
import org.osmdroid.config.Configuration

class TrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = getExternalFilesDir("osmdroid")
                ?: getDir("osmdroid", MODE_PRIVATE)
        }
    }

    companion object {
        lateinit var instance: TrackerApp
            private set
    }
}
