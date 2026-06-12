package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TotalStats(
    val totalDistanceKm: Double,
    val totalCalories: Double,
    val totalDurationMs: Long,
    val rideCount: Int,
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    val routes: StateFlow<List<Route>> = repository.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalStats: StateFlow<TotalStats> = routes.map { list ->
        TotalStats(
            totalDistanceKm = list.sumOf { it.distanceKm },
            totalCalories = list.sumOf { it.calories },
            totalDurationMs = list.sumOf { it.endTime - it.startTime },
            rideCount = list.size
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, TotalStats(0.0, 0.0, 0, 0))

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAllRoutes() }
    }
}
