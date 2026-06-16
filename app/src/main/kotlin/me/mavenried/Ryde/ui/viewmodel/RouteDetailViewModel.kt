package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.domain.util.TrackStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouteDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    private val _points = MutableStateFlow<List<LocationPoint>>(emptyList())
    val points: StateFlow<List<LocationPoint>> = _points.asStateFlow()

    private val _topSpeedKmh = MutableStateFlow(0.0)
    val topSpeedKmh: StateFlow<Double> = _topSpeedKmh.asStateFlow()

    private val _stoppedTimeSec = MutableStateFlow(0L)
    val stoppedTimeSec: StateFlow<Long> = _stoppedTimeSec.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            val r = repository.getRouteById(id)
            val pts = repository.getPointsForRoute(id)
            _route.value = r
            _points.value = pts
            _topSpeedKmh.value = TrackStats.topSpeedKmh(pts)
            r?.let { route ->
                _stoppedTimeSec.value = TrackStats.stoppedTimeSec(route.endTime - route.startTime, pts)
            }
        }
    }

    fun deleteRoute(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoute(id)
            onDeleted()
        }
    }
}
