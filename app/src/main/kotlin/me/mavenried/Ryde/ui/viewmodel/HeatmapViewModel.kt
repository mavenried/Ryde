package me.mavenried.Ryde.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.repository.RouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeatmapViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    private val _allTrails = MutableStateFlow<List<List<LocationPoint>>>(emptyList())
    val allTrails: StateFlow<List<List<LocationPoint>>> = _allTrails.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            val routes = repository.getAllRoutesOnce().filter { it.completed }
            _allTrails.value = routes.mapNotNull { route ->
                val pts = repository.getPointsForRoute(route.id)
                pts.ifEmpty { null }
            }
            _loading.value = false
        }
    }
}
