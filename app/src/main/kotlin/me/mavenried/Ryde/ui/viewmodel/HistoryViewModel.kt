package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActivityStats(
    val distanceKm: Double,
    val durationMs: Long,
    val calories: Double,
    val count: Int,
)

data class TotalStats(
    val totalDistanceKm: Double,
    val totalCalories: Double,
    val totalDurationMs: Long,
    val rideCount: Int,
    val byActivity: Map<ActivityType, ActivityStats> = emptyMap(),
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    val routes: StateFlow<List<Route>> = repository.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalStats: StateFlow<TotalStats> = routes.map { list ->
        val byActivity = list.groupBy { it.activityType }.mapValues { (_, routes) ->
            ActivityStats(
                distanceKm = routes.sumOf { it.distanceKm },
                durationMs = routes.sumOf { it.endTime - it.startTime },
                calories = routes.sumOf { it.calories },
                count = routes.size,
            )
        }
        TotalStats(
            totalDistanceKm = list.sumOf { it.distanceKm },
            totalCalories = list.sumOf { it.calories },
            totalDurationMs = list.sumOf { it.endTime - it.startTime },
            rideCount = list.size,
            byActivity = byActivity,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, TotalStats(0.0, 0.0, 0, 0))

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAllRoutes() }
    }
}
