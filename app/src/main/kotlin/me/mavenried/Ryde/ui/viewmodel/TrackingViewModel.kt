package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.service.TrackingService
import me.mavenried.Ryde.service.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(app: Application) : AndroidViewModel(app) {

    private var trackingService: TrackingService? = null

    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? TrackingService.TrackingBinder ?: return
            trackingService = b.getService()
            viewModelScope.launch {
                b.getService().state.collect { _trackingState.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
        }
    }

    init {
        app.bindService(
            Intent(app, TrackingService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun startTracking(activityType: ActivityType) {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, TrackingService::class.java))
        trackingService?.startTracking(activityType)
    }

    fun pauseTracking() { trackingService?.pauseTracking() }
    fun resumeTracking() { trackingService?.resumeTracking() }
    fun stopTracking() { trackingService?.stopTracking() }

    override fun onCleared() {
        try { getApplication<Application>().unbindService(connection) } catch (_: Exception) {}
        super.onCleared()
    }
}
