package me.mavenried.Ryde.domain.util

import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import kotlin.math.*

object TrackStats {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val ACCURACY_THRESHOLD_M = 25f
    // GPS altitude jitter on consumer devices is typically ±5–15 m.
    // A gate below ~8 m accumulates significant false gain on flat ground.
    private const val ELEVATION_NOISE_GATE_M = 8.0

    // Distance-based energy cost (kcal/kg/km) — won't accumulate while stationary
    // Sources: Margaria 1963 (running), Givoni & Goldman 1971 (walking), Whitt & Wilson 1982 (cycling)
    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val KCAL_PER_KG_KM_RUNNING = 1.04
    private const val KCAL_PER_KG_KM_WALKING = 0.80
    private const val KCAL_PER_KG_KM_CYCLING = 0.35

    fun filterPoints(points: List<LocationPoint>): List<LocationPoint> =
        points.filter { it.accuracy <= ACCURACY_THRESHOLD_M }

    fun totalDistanceKm(points: List<LocationPoint>): Double {
        val filtered = filterPoints(points)
        if (filtered.size < 2) return 0.0
        return filtered.zipWithNext().sumOf { (a, b) -> haversineKm(a.lat, a.lng, b.lat, b.lng) }
    }

    fun elevationGainM(points: List<LocationPoint>): Double {
        val filtered = filterPoints(points)
        if (filtered.size < 2) return 0.0
        return filtered.zipWithNext()
            .sumOf { (a, b) -> max(0.0, b.altitude - a.altitude - ELEVATION_NOISE_GATE_M) }
    }

    fun avgPaceMinPerKm(distanceKm: Double, durationMs: Long): Double {
        if (distanceKm <= 0.0 || durationMs <= 0) return 0.0
        return (durationMs / 60_000.0) / distanceKm
    }

    fun avgSpeedKmh(distanceKm: Double, durationMs: Long): Double {
        if (durationMs <= 0) return 0.0
        return distanceKm / (durationMs / 3_600_000.0)
    }

    fun topSpeedKmh(points: List<LocationPoint>): Double =
        (points.maxOfOrNull { it.speed } ?: 0f) * 3.6

    /** Moving time: sum of intervals between consecutive points where the gap is < 30 s (excludes auto-pause gaps). */
    fun movingTimeSec(points: List<LocationPoint>): Long {
        if (points.size < 2) return 0L
        return points.zipWithNext().sumOf { (a, b) ->
            val gap = b.timestamp - a.timestamp
            if (gap in 1L..30_000L) gap else 0L
        } / 1000L
    }

    /** Stopped time = total duration minus moving time derived from point timestamps. */
    fun stoppedTimeSec(totalDurationMs: Long, points: List<LocationPoint>): Long =
        ((totalDurationMs / 1000L) - movingTimeSec(points)).coerceAtLeast(0L)

    fun estimatedCaloriesKcal(
        activityType: ActivityType,
        distanceKm: Double,
        weightKg: Double = DEFAULT_WEIGHT_KG
    ): Double {
        val rate = when (activityType) {
            ActivityType.RUNNING -> KCAL_PER_KG_KM_RUNNING
            ActivityType.WALKING -> KCAL_PER_KG_KM_WALKING
            ActivityType.CYCLING -> KCAL_PER_KG_KM_CYCLING
        }
        return rate * weightKg * distanceKm
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }
}
