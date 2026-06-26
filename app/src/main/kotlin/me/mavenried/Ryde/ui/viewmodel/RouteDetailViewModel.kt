package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.domain.util.LapSplit
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

    private val _lapSplits = MutableStateFlow<List<LapSplit>>(emptyList())
    val lapSplits: StateFlow<List<LapSplit>> = _lapSplits.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            val r = repository.getRouteById(id)
            val pts = repository.getPointsForRoute(id)
            _route.value = r
            _points.value = pts
            _topSpeedKmh.value = TrackStats.topSpeedKmh(pts)
            _lapSplits.value = TrackStats.computeLapSplits(pts)
            r?.let { route ->
                _stoppedTimeSec.value = TrackStats.stoppedTimeSec(route.endTime - route.startTime, pts)
            }
        }
    }

    fun renameRoute(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameRoute(id, newName)
            _route.value = _route.value?.copy(name = newName)
        }
    }

    fun updateTag(id: String, tag: String) {
        viewModelScope.launch {
            repository.updateTag(id, tag)
            _route.value = _route.value?.copy(category = tag)
        }
    }

    fun deleteRoute(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoute(id)
            onDeleted()
        }
    }
}
