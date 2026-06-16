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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class StatsPeriod { WEEKLY, MONTHLY }

data class PeriodBar(
    val label: String,
    val cyclingKm: Double,
    val runningKm: Double,
    val walkingKm: Double
) {
    val totalKm get() = cyclingKm + runningKm + walkingKm
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    private val _period = MutableStateFlow(StatsPeriod.WEEKLY)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun setPeriod(p: StatsPeriod) { _period.value = p }

    val bars: StateFlow<List<PeriodBar>> = combine(
        repository.getAllRoutes(), _period
    ) { routes, period ->
        buildBars(routes.filter { it.completed }, period)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun buildBars(routes: List<Route>, period: StatsPeriod): List<PeriodBar> {
        val count = if (period == StatsPeriod.WEEKLY) 8 else 6
        val bars = mutableListOf<PeriodBar>()

        val cal = Calendar.getInstance()

        repeat(count) { i ->
            cal.timeInMillis = System.currentTimeMillis()
            if (period == StatsPeriod.WEEKLY) {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.add(Calendar.WEEK_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val weekStart = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                val weekEnd = cal.timeInMillis

                val label = if (i == 0) "This wk"
                            else "W${cal.apply { add(Calendar.WEEK_OF_YEAR, -1) }.get(Calendar.WEEK_OF_YEAR)}"
                cal.timeInMillis = weekStart

                val periodRoutes = routes.filter { it.startTime in weekStart until weekEnd }
                bars.add(0, PeriodBar(
                    label = if (i == 0) "This wk" else "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}",
                    cyclingKm = periodRoutes.filter { it.activityType == ActivityType.CYCLING }.sumOf { it.distanceKm },
                    runningKm = periodRoutes.filter { it.activityType == ActivityType.RUNNING }.sumOf { it.distanceKm },
                    walkingKm = periodRoutes.filter { it.activityType == ActivityType.WALKING }.sumOf { it.distanceKm }
                ))
            } else {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val monthEnd = cal.timeInMillis

                val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                val label = monthNames[cal.apply { add(Calendar.MONTH, -1) }.get(Calendar.MONTH)]

                val periodRoutes = routes.filter { it.startTime in monthStart until monthEnd }
                bars.add(0, PeriodBar(
                    label = label,
                    cyclingKm = periodRoutes.filter { it.activityType == ActivityType.CYCLING }.sumOf { it.distanceKm },
                    runningKm = periodRoutes.filter { it.activityType == ActivityType.RUNNING }.sumOf { it.distanceKm },
                    walkingKm = periodRoutes.filter { it.activityType == ActivityType.WALKING }.sumOf { it.distanceKm }
                ))
            }
        }

        return bars
    }
}
