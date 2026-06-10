package me.mavenried.Ryde.service

import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.domain.util.TrackStats
import me.mavenried.Ryde.util.UserPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

sealed class TrackingState {
    data object Idle : TrackingState()
    data class Active(
        val activityType: ActivityType,
        val elapsedMs: Long,
        val distanceKm: Double,
        val currentPace: Double,
        val currentSpeed: Double,
        val calories: Double,
        val points: List<LocationPoint>,
        val isPaused: Boolean = false
    ) : TrackingState()
}

class TrackingService : LifecycleService() {

    inner class TrackingBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    private val binder = TrackingBinder()
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: RouteRepository

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private var startTimeMs = 0L
    private var totalPausedMs = 0L
    private var pausedAtMs = 0L
    private var isPaused = false
    private var currentActivityType = ActivityType.RUNNING
    private val pointBuffer = mutableListOf<LocationPoint>()
    private val pendingDbPoints = mutableListOf<LocationPoint>()
    private var currentRouteId = ""
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        repository = RouteRepository(AppDatabase.getInstance(this))
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onNewLocation(it) }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun startTracking(activityType: ActivityType) {
        currentActivityType = activityType
        currentRouteId = UUID.randomUUID().toString()
        startTimeMs = System.currentTimeMillis()
        totalPausedMs = 0L
        pausedAtMs = 0L
        isPaused = false
        pointBuffer.clear()
        pendingDbPoints.clear()

        val notification = NotificationHelper.buildTrackingNotification(this)
        startForeground(NotificationHelper.TRACKING_NOTIFICATION_ID, notification)
        requestLocationUpdates()
        startTimer()
        pushState()
    }

    fun pauseTracking() {
        if (isPaused) return
        isPaused = true
        pausedAtMs = System.currentTimeMillis()
        fusedClient.removeLocationUpdates(locationCallback)
        stopTimer()
        pushState()
    }

    fun resumeTracking() {
        if (!isPaused) return
        totalPausedMs += System.currentTimeMillis() - pausedAtMs
        isPaused = false
        requestLocationUpdates()
        startTimer()
        pushState()
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        stopTimer()
        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTimeMs - totalPausedMs
        val distanceKm = TrackStats.totalDistanceKm(pointBuffer)
        val elevGain = TrackStats.elevationGainM(pointBuffer)
        val avgPace = if (currentActivityType != ActivityType.CYCLING)
            TrackStats.avgPaceMinPerKm(distanceKm, durationMs) else 0.0
        val avgSpeed = if (currentActivityType == ActivityType.CYCLING)
            TrackStats.avgSpeedKmh(distanceKm, durationMs) else 0.0
        val calories = TrackStats.estimatedCaloriesKcal(currentActivityType, distanceKm, UserPrefs.getWeightKg(this))

        val route = Route(
            id = currentRouteId,
            activityType = currentActivityType,
            name = autoName(currentActivityType, startTimeMs),
            startTime = startTimeMs,
            endTime = endTime,
            distanceKm = distanceKm,
            avgPace = avgPace,
            avgSpeedKmh = avgSpeed,
            elevationGainM = elevGain,
            calories = calories
        )
        val allPoints = pointBuffer.toList()
        val routeId = currentRouteId

        lifecycleScope.launch {
            repository.saveRoute(route)
            repository.savePoints(routeId, allPoints)
        }

        _state.value = TrackingState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (true) {
                delay(1_000L)
                pushState()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun onNewLocation(location: Location) {
        val point = LocationPoint(
            lat = location.latitude,
            lng = location.longitude,
            altitude = location.altitude,
            timestamp = location.time,
            speed = location.speed,
            accuracy = location.accuracy
        )
        if (point.accuracy <= 25f) {
            pointBuffer.add(point)
            pendingDbPoints.add(point)
            if (pendingDbPoints.size >= 10) flushPendingPoints()
        }
        pushState()
        updateNotification()
    }

    private fun flushPendingPoints() {
        if (pendingDbPoints.isEmpty()) return
        val toSave = pendingDbPoints.toList()
        val routeId = currentRouteId
        pendingDbPoints.clear()
        lifecycleScope.launch { repository.savePoints(routeId, toSave) }
    }

    private fun pushState() {
        val elapsed = if (isPaused) {
            pausedAtMs - startTimeMs - totalPausedMs
        } else {
            System.currentTimeMillis() - startTimeMs - totalPausedMs
        }
        val dist = TrackStats.totalDistanceKm(pointBuffer)
        _state.value = TrackingState.Active(
            activityType = currentActivityType,
            elapsedMs = elapsed,
            distanceKm = dist,
            currentPace = if (currentActivityType != ActivityType.CYCLING)
                TrackStats.avgPaceMinPerKm(dist, elapsed) else 0.0,
            currentSpeed = if (currentActivityType == ActivityType.CYCLING)
                TrackStats.avgSpeedKmh(dist, elapsed) else 0.0,
            calories = TrackStats.estimatedCaloriesKcal(currentActivityType, dist, UserPrefs.getWeightKg(this@TrackingService)),
            points = pointBuffer.toList(),
            isPaused = isPaused
        )
    }

    private fun updateNotification() {
        val s = _state.value as? TrackingState.Active ?: return
        val notification = NotificationHelper.buildTrackingNotification(
            this, s.distanceKm, s.elapsedMs
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NotificationHelper.TRACKING_NOTIFICATION_ID, notification)
    }

    private fun autoName(type: ActivityType, startMs: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = startMs }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dayName = when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> "Weekend"
            else -> "Weekday"
        }

        val timeOfDay = when {
            hour < 6 -> "Night"
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            hour < 21 -> "Evening"
            else -> "Night"
        }
        val activity = when (type) {
            ActivityType.RUNNING -> "Run"
            ActivityType.CYCLING -> "Ride"
            ActivityType.WALKING -> "Walk"
        }
        return "$dayName $timeOfDay $activity"
    }
}
