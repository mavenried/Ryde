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

data class PersonalRecord(
    val label: String,
    val value: String,
    val routeId: String,
    val routeName: String
)

data class PersonalRecords(
    val cycling: List<PersonalRecord> = emptyList(),
    val running: List<PersonalRecord> = emptyList(),
    val walking: List<PersonalRecord> = emptyList(),
    val overall: List<PersonalRecord> = emptyList()
)

class PersonalRecordsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RouteRepository(AppDatabase.getInstance(app))

    val records: StateFlow<PersonalRecords> = repository.getAllRoutes()
        .map { routes -> computeRecords(routes.filter { it.completed }) }
        .stateIn(viewModelScope, SharingStarted.Lazily, PersonalRecords())

    private fun computeRecords(routes: List<Route>): PersonalRecords {
        if (routes.isEmpty()) return PersonalRecords()

        val cycling = routes.filter { it.activityType == ActivityType.CYCLING }
        val running = routes.filter { it.activityType == ActivityType.RUNNING }
        val walking = routes.filter { it.activityType == ActivityType.WALKING }

        fun durationStr(ms: Long): String {
            val h = ms / 3_600_000; val m = (ms % 3_600_000) / 60_000; val s = (ms % 60_000) / 1000
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }

        fun paceStr(minPerKm: Double): String {
            if (minPerKm <= 0 || minPerKm > 60) return "--"
            val m = minPerKm.toInt(); val s = ((minPerKm - m) * 60).toInt()
            return "%d:%02d /km".format(m, s)
        }

        fun List<Route>.prDistance() = maxByOrNull { it.distanceKm }?.let {
            PersonalRecord("Longest distance", "%.2f km".format(it.distanceKm), it.id, it.name)
        }

        fun List<Route>.prDuration() = maxByOrNull { it.endTime - it.startTime }?.let {
            PersonalRecord("Longest duration", durationStr(it.endTime - it.startTime), it.id, it.name)
        }

        fun List<Route>.prElevation() = maxByOrNull { it.elevationGainM }?.let {
            PersonalRecord("Most elevation", "+%.0f m".format(it.elevationGainM), it.id, it.name)
        }

        val cyclingPRs = buildList {
            cycling.prDistance()?.let { add(it) }
            cycling.prDuration()?.let { add(it) }
            cycling.prElevation()?.let { add(it) }
            cycling.filter { it.avgSpeedKmh > 0 }.maxByOrNull { it.avgSpeedKmh }?.let {
                add(PersonalRecord("Fastest avg speed", "%.1f kmph".format(it.avgSpeedKmh), it.id, it.name))
            }
        }

        val runningPRs = buildList {
            running.prDistance()?.let { add(it) }
            running.prDuration()?.let { add(it) }
            running.prElevation()?.let { add(it) }
            running.filter { it.avgPace > 0 && it.avgPace <= 60 }.minByOrNull { it.avgPace }?.let {
                add(PersonalRecord("Best pace", paceStr(it.avgPace), it.id, it.name))
            }
        }

        val walkingPRs = buildList {
            walking.prDistance()?.let { add(it) }
            walking.prDuration()?.let { add(it) }
            walking.prElevation()?.let { add(it) }
        }

        val overallPRs = buildList {
            routes.prDistance()?.let { add(it) }
            routes.prDuration()?.let { add(it) }
            routes.prElevation()?.let { add(it) }
            routes.maxByOrNull { it.calories }?.let {
                add(PersonalRecord("Most calories", "%.0f kcal".format(it.calories), it.id, it.name))
            }
        }

        return PersonalRecords(cyclingPRs, runningPRs, walkingPRs, overallPRs)
    }
}
