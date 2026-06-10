package me.mavenried.Ryde.domain.repository

import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.data.model.LocationPointEntity
import me.mavenried.Ryde.data.model.RouteEntity
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RouteRepository(private val db: AppDatabase) {

    fun getAllRoutes(): Flow<List<Route>> =
        db.routeDao().getAllRoutes().map { entities -> entities.map { it.toDomain() } }

    suspend fun getRouteById(id: String): Route? =
        db.routeDao().getRouteById(id)?.toDomain()

    suspend fun getPointsForRoute(id: String): List<LocationPoint> =
        db.locationPointDao().getPointsForRoute(id).map { it.toDomain() }

    suspend fun saveRoute(route: Route) {
        db.routeDao().insert(route.toEntity())
    }

    suspend fun savePoints(routeId: String, points: List<LocationPoint>) {
        if (points.isEmpty()) return
        db.locationPointDao().insertAll(points.map { it.toEntity(routeId) })
    }

    suspend fun deleteRoute(id: String) {
        val entity = db.routeDao().getRouteById(id) ?: return
        db.routeDao().delete(entity)
    }

    suspend fun deleteAllRoutes() = db.routeDao().deleteAll()

    private fun RouteEntity.toDomain() = Route(
        id = id,
        activityType = ActivityType.valueOf(activityType),
        name = name,
        startTime = startTime,
        endTime = endTime,
        distanceKm = distanceKm,
        avgPace = avgPace,
        avgSpeedKmh = avgSpeedKmh,
        elevationGainM = elevationGainM,
        calories = calories
    )

    private fun Route.toEntity() = RouteEntity(
        id = id,
        activityType = activityType.name,
        name = name,
        startTime = startTime,
        endTime = endTime,
        distanceKm = distanceKm,
        avgPace = avgPace,
        avgSpeedKmh = avgSpeedKmh,
        elevationGainM = elevationGainM,
        calories = calories
    )

    private fun LocationPointEntity.toDomain() = LocationPoint(
        lat = lat, lng = lng, altitude = altitude,
        timestamp = timestamp, speed = speed, accuracy = accuracy
    )

    private fun LocationPoint.toEntity(routeId: String) = LocationPointEntity(
        routeId = routeId, lat = lat, lng = lng, altitude = altitude,
        timestamp = timestamp, speed = speed, accuracy = accuracy
    )
}
