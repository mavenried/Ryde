package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.service.TrackingService
import me.mavenried.Ryde.service.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DestinationPoint(val lat: Double, val lng: Double, val label: String = "")

class TrackingViewModel(app: Application) : AndroidViewModel(app) {

    private var trackingService: TrackingService? = null
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _crashRecoveryRoute = MutableStateFlow<Route?>(null)
    val crashRecoveryRoute: StateFlow<Route?> = _crashRecoveryRoute.asStateFlow()

    private val _overlayPoints = MutableStateFlow<List<List<LocationPoint>>>(emptyList())
    val overlayPoints: StateFlow<List<List<LocationPoint>>> = _overlayPoints.asStateFlow()

    private val _destination = MutableStateFlow<DestinationPoint?>(null)
    val destination: StateFlow<DestinationPoint?> = _destination.asStateFlow()

    val allRoutes: StateFlow<List<Route>> = repository.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Holds a route to resume once the service connects
    private var pendingResume: Route? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? TrackingService.TrackingBinder ?: return
            trackingService = b.getService()
            viewModelScope.launch {
                b.getService().state.collect { _trackingState.value = it }
            }
            pendingResume?.let { route ->
                val ctx = getApplication<Application>()
                ctx.startForegroundService(Intent(ctx, TrackingService::class.java))
                b.getService().resumeFromCrash(route.id, route.activityType, route.startTime)
                pendingResume = null
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
        viewModelScope.launch {
            try {
                val last = repository.getLastIncompleteRoute()
                if (last != null) {
                    val elapsed = System.currentTimeMillis() - last.startTime
                    if (elapsed < 30 * 60 * 1000L) {
                        _crashRecoveryRoute.value = last
                    } else {
                        repository.deleteRoute(last.id)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun startTracking(
        activityType: ActivityType,
        goalDistanceKm: Double? = null,
        goalDurationMs: Long? = null
    ) {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, TrackingService::class.java))
        trackingService?.startTracking(activityType, goalDistanceKm, goalDurationMs)
    }

    fun resumeCrashedTracking() {
        val route = _crashRecoveryRoute.value ?: return
        _crashRecoveryRoute.value = null
        val service = trackingService
        if (service != null) {
            val ctx = getApplication<Application>()
            ctx.startForegroundService(Intent(ctx, TrackingService::class.java))
            service.resumeFromCrash(route.id, route.activityType, route.startTime)
        } else {
            pendingResume = route
        }
    }

    fun discardCrashedRoute() {
        val route = _crashRecoveryRoute.value ?: return
        _crashRecoveryRoute.value = null
        viewModelScope.launch { repository.deleteRoute(route.id) }
    }

    fun pauseTracking() { trackingService?.pauseTracking() }
    fun resumeTracking() { trackingService?.resumeTracking() }
    fun stopTracking(tag: String = "Other") { trackingService?.stopTracking(tag) }
    fun discardTracking() { trackingService?.discardTracking() }

    fun setOverlayRoute(routeId: String) {
        viewModelScope.launch {
            val pts = repository.getPointsForRoute(routeId)
            _overlayPoints.value = if (pts.isNotEmpty()) listOf(pts) else emptyList()
        }
    }

    fun clearOverlay() { _overlayPoints.value = emptyList() }

    fun setDestination(lat: Double, lng: Double, label: String = "") {
        _destination.value = DestinationPoint(lat, lng, label)
    }

    fun clearDestination() { _destination.value = null }

    override fun onCleared() {
        try { getApplication<Application>().unbindService(connection) } catch (_: Exception) {}
        super.onCleared()
    }
}
