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
import me.mavenried.Ryde.util.FileLogger
import me.mavenried.Ryde.util.ReverseGeocoder
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
        val isPaused: Boolean = false,
        val isAutoPaused: Boolean = false
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
    private var isAutoPaused = false
    private var currentActivityType = ActivityType.RUNNING
    private var lastSpeedMs = 0f
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildTrackingNotification(this)
        startForeground(NotificationHelper.TRACKING_NOTIFICATION_ID, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun startTracking(activityType: ActivityType) {
        FileLogger.log(this, "Starting tracking for $activityType")
        currentActivityType = activityType
        currentRouteId = UUID.randomUUID().toString()
        startTimeMs = System.currentTimeMillis()
        totalPausedMs = 0L
        pausedAtMs = 0L
        isPaused = false
        lastSpeedMs = 0f
        pointBuffer.clear()
        pendingDbPoints.clear()

        val initialRoute = Route(
            id = currentRouteId,
            activityType = currentActivityType,
            name = autoName(currentActivityType, startTimeMs),
            startTime = startTimeMs,
            endTime = startTimeMs,
            distanceKm = 0.0,
            avgPace = 0.0,
            avgSpeedKmh = 0.0,
            elevationGainM = 0.0,
            calories = 0.0,
            category = "Other",
            completed = false
        )
        lifecycleScope.launch {
            try {
                repository.saveRoute(initialRoute)
                FileLogger.log(this@TrackingService, "Initial route saved: $currentRouteId")
            } catch (e: Exception) {
                FileLogger.logError(this@TrackingService, "Failed to save initial route", e)
            }
        }

        requestLocationUpdates()
        startTimer()
        pushState()
    }

    fun resumeFromCrash(routeId: String, activityType: ActivityType, savedStartTime: Long) {
        FileLogger.log(this, "Resuming crashed route: $routeId")
        currentActivityType = activityType
        currentRouteId = routeId
        startTimeMs = savedStartTime
        totalPausedMs = 0L
        pausedAtMs = 0L
        isPaused = false
        isAutoPaused = false
        lastSpeedMs = 0f
        pointBuffer.clear()
        pendingDbPoints.clear()
        lifecycleScope.launch {
            try {
                val existing = repository.getPointsForRoute(routeId)
                pointBuffer.addAll(existing)
                FileLogger.log(this@TrackingService, "Loaded ${existing.size} existing points for crash resume")
            } catch (e: Exception) {
                FileLogger.logError(this@TrackingService, "Failed to load existing points on resume", e)
            }
        }
        requestLocationUpdates()
        startTimer()
        pushState()
    }

    fun pauseTracking() {
        if (isPaused) return
        if (isAutoPaused) {
            totalPausedMs += System.currentTimeMillis() - pausedAtMs
            isAutoPaused = false
        }
        isPaused = true
        pausedAtMs = System.currentTimeMillis()
        fusedClient.removeLocationUpdates(locationCallback)
        stopTimer()
        pushState()
    }

    fun resumeTracking() {
        if (!isPaused && !isAutoPaused) return
        totalPausedMs += System.currentTimeMillis() - pausedAtMs
        isPaused = false
        isAutoPaused = false
        requestLocationUpdates()
        startTimer()
        pushState()
    }

    fun discardTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        stopTimer()
        val routeId = currentRouteId
        lifecycleScope.launch { repository.deleteRoute(routeId) }
        _state.value = TrackingState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun stopTracking() {
        FileLogger.log(this, "Stopping tracking for $currentRouteId")
        fusedClient.removeLocationUpdates(locationCallback)
        stopTimer()
        val endTime = System.currentTimeMillis()
        val durationMs = (endTime - startTimeMs - totalPausedMs).coerceAtLeast(0L)
        val distanceKm = TrackStats.totalDistanceKm(pointBuffer)
        val elevGain = TrackStats.elevationGainM(pointBuffer)
        val avgPace = if (currentActivityType != ActivityType.CYCLING)
            TrackStats.avgPaceMinPerKm(distanceKm, durationMs) else 0.0
        val avgSpeed = if (currentActivityType == ActivityType.CYCLING)
            TrackStats.avgSpeedKmh(distanceKm, durationMs) else 0.0
        val calories = TrackStats.estimatedCaloriesKcal(currentActivityType, distanceKm, UserPrefs.getWeightKg(this))

        lifecycleScope.launch {
            val locationName = if (pointBuffer.isNotEmpty()) {
                val last = pointBuffer.last()
                ReverseGeocoder.getPlaceName(this@TrackingService, last.lat, last.lng)
            } else null

            val routeName = if (locationName != null) {
                "${autoName(currentActivityType, startTimeMs)} in $locationName"
            } else {
                autoName(currentActivityType, startTimeMs)
            }

            val route = Route(
                id = currentRouteId,
                activityType = currentActivityType,
                name = routeName,
                startTime = startTimeMs,
                endTime = endTime,
                distanceKm = distanceKm,
                avgPace = avgPace,
                avgSpeedKmh = avgSpeed,
                elevationGainM = elevGain,
                calories = calories,
                category = "Other",
                completed = true
            )
            val allPoints = pointBuffer.toList()
            val routeId = currentRouteId

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

    private fun requestLocationUpdatesStationary() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(0f)
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
            accuracy = location.accuracy,
            bearing = if (location.hasBearing()) location.bearing else 0f
        )

        if (point.accuracy <= 25f) {
            lastSpeedMs = point.speed
            if (point.speed < 0.5f && !isAutoPaused && !isPaused) {
                isAutoPaused = true
                pausedAtMs = System.currentTimeMillis()
                requestLocationUpdatesStationary()
                FileLogger.log(this, "Auto-paused (speed ${point.speed})")
            } else if (point.speed > 1.0f && isAutoPaused) {
                totalPausedMs += System.currentTimeMillis() - pausedAtMs
                isAutoPaused = false
                requestLocationUpdates()
                FileLogger.log(this, "Auto-resumed (speed ${point.speed})")
            }

            if (!isAutoPaused) {
                pointBuffer.add(point)
                pendingDbPoints.add(point)
                if (pendingDbPoints.size >= 10) flushPendingPoints()
            }
        } else {
            FileLogger.log(this, "Location rejected: accuracy ${point.accuracy}m")
        }
        pushState()
        updateNotification()
    }

    private fun flushPendingPoints() {
        if (pendingDbPoints.isEmpty()) return
        val toSave = pendingDbPoints.toList()
        val routeId = currentRouteId
        pendingDbPoints.clear()
        lifecycleScope.launch {
            try {
                repository.savePoints(routeId, toSave)
            } catch (e: Exception) {
                FileLogger.logError(this@TrackingService, "Failed to flush points for $routeId", e)
            }
        }
    }

    private fun pushState() {
        val now = System.currentTimeMillis()
        val elapsed = (if (isPaused || isAutoPaused) {
            pausedAtMs - startTimeMs - totalPausedMs
        } else {
            now - startTimeMs - totalPausedMs
        }).coerceAtLeast(0L)
        val dist = TrackStats.totalDistanceKm(pointBuffer)
        val liveSpeedKmh = lastSpeedMs * 3.6
        _state.value = TrackingState.Active(
            activityType = currentActivityType,
            elapsedMs = elapsed,
            distanceKm = dist,
            currentPace = if (currentActivityType != ActivityType.CYCLING && lastSpeedMs > 0.1f)
                1000.0 / (lastSpeedMs * 60.0) else 0.0,
            currentSpeed = if (currentActivityType == ActivityType.CYCLING) liveSpeedKmh else 0.0,
            calories = TrackStats.estimatedCaloriesKcal(currentActivityType, dist, UserPrefs.getWeightKg(this@TrackingService)),
            points = pointBuffer.toList(),
            isPaused = isPaused || isAutoPaused,
            isAutoPaused = isAutoPaused
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
