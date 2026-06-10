package me.mavenried.Ryde.domain.model

data class Route(
    val id: String,
    val activityType: ActivityType,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val avgPace: Double,
    val avgSpeedKmh: Double,
    val elevationGainM: Double,
    val calories: Double = 0.0
)
