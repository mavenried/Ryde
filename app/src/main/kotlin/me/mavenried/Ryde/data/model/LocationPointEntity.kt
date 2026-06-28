package me.mavenried.Ryde.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val lat: Double,
    val lng: Double,
    val altitude: Double,
    val timestamp: Long,
    val speed: Float,
    val accuracy: Float,
    val bearing: Float = 0f,
    val heartRate: Int? = null,
)
