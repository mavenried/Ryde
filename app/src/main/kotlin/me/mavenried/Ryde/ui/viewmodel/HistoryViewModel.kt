package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

val ROUTE_TAGS = listOf("Other", "Commute", "Exercise", "Leisure", "Race", "Training")

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

    private val _allRoutes: StateFlow<List<Route>> = repository.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val routes: StateFlow<List<Route>> = combine(_allRoutes, _selectedTag, _searchQuery) { all, tag, query ->
        var list = if (tag == null) all else all.filter { it.category == tag }
        if (query.isNotBlank()) list = list.filter { it.name.contains(query.trim(), ignoreCase = true) }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableTags: StateFlow<List<String>> = _allRoutes.map { all ->
        all.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val streak: StateFlow<Int> = _allRoutes.map { list -> computeStreak(list) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalStats: StateFlow<TotalStats> = _allRoutes.map { list ->
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

    fun setTagFilter(tag: String?) { _selectedTag.value = tag }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    private fun computeStreak(routes: List<Route>): Int {
        val msPerDay = 86_400_000L
        val completedDays = routes.filter { it.completed }.mapTo(mutableSetOf()) { route ->
            val cal = Calendar.getInstance().apply { timeInMillis = route.startTime }
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis / msPerDay
        }
        val today = System.currentTimeMillis() / msPerDay
        var streak = 0
        var day = today
        while (day in completedDays) { streak++; day-- }
        return streak
    }
}
