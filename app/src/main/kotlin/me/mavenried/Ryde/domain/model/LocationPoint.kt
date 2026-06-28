package me.mavenried.Ryde.domain.model

data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double,
    val timestamp: Long,
    val speed: Float,
    val accuracy: Float,
    val bearing: Float = 0f,
    val heartRate: Int? = null,
)
