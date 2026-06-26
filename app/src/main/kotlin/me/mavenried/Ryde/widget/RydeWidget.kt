package me.mavenried.Ryde.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.domain.util.TrackStats
import me.mavenried.Ryde.ui.MainActivity

private val BG = Color(0xFF1B1B2F)
private val ACCENT = Color(0xFF6C63FF)
private val GREEN = Color(0xFF4CAF50)
private val WHITE = Color.White
private val DIM = Color(0x99FFFFFF)
private val DIMMER = Color(0x59FFFFFF)

private data class RecordRow(val label: String, val value: String)

class RydeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = RouteRepository(AppDatabase.getInstance(context))
        val activeRoute = try { repository.getLastIncompleteRoute() } catch (_: Exception) { null }

        if (activeRoute != null) {
            val points = try { repository.getPointsForRoute(activeRoute.id) } catch (_: Exception) { emptyList() }
            val distanceKm = TrackStats.totalDistanceKm(points)
            val durationMs = System.currentTimeMillis() - activeRoute.startTime
            val lastSpeed = points.lastOrNull()?.speed ?: 0f
            provideContent {
                ActiveContent(
                    activityType = activeRoute.activityType,
                    distanceKm = distanceKm,
                    durationMs = durationMs,
                    lastSpeedMs = lastSpeed
                )
            }
        } else {
            val routes = try { repository.getAllRoutesOnce().filter { it.completed } } catch (_: Exception) { emptyList() }
            val totalKm = routes.sumOf { it.distanceKm }
            val records = buildRecordRows(routes)
            provideContent {
                IdleContent(totalKm = totalKm, totalRides = routes.size, records = records)
            }
        }
    }
}

private fun buildRecordRows(routes: List<Route>): List<RecordRow> {
    if (routes.isEmpty()) return emptyList()
    val rows = mutableListOf<RecordRow>()

    routes.maxByOrNull { it.distanceKm }?.let {
        rows.add(RecordRow("Longest activity", "%.1f km".format(it.distanceKm)))
    }

    val bestPace = routes
        .filter { it.activityType != ActivityType.CYCLING && it.avgPace in 1.0..60.0 }
        .minByOrNull { it.avgPace }
    val bestSpeed = routes
        .filter { it.activityType == ActivityType.CYCLING && it.avgSpeedKmh > 0 }
        .maxByOrNull { it.avgSpeedKmh }

    when {
        bestPace != null -> {
            val m = bestPace.avgPace.toInt()
            val s = ((bestPace.avgPace - m) * 60).toInt()
            rows.add(RecordRow("Best pace", "%d:%02d /km".format(m, s)))
        }
        bestSpeed != null ->
            rows.add(RecordRow("Fastest ride", "%.1f km/h".format(bestSpeed.avgSpeedKmh)))
    }

    routes.maxByOrNull { it.elevationGainM }?.takeIf { it.elevationGainM > 0 }?.let {
        rows.add(RecordRow("Most elevation", "+%.0f m".format(it.elevationGainM)))
    }

    return rows.take(3)
}

@Composable
private fun ActiveContent(
    activityType: ActivityType,
    distanceKm: Double,
    durationMs: Long,
    lastSpeedMs: Float
) {
    val activityLabel = when (activityType) {
        ActivityType.CYCLING -> "CYCLING"
        ActivityType.RUNNING -> "RUNNING"
        ActivityType.WALKING -> "WALKING"
    }
    val isCycling = activityType == ActivityType.CYCLING
    val speedKmh = lastSpeedMs * 3.6

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BG)
            .clickable(actionRunCallback<OpenAppCallback>())
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                "● LIVE",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(GREEN),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                activityLabel,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(ACCENT),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "%.2f km".format(distanceKm),
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(WHITE),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            formatDuration(durationMs),
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(DIM),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (speedKmh > 0.5) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                if (isCycling) "%.1f km/h".format(speedKmh) else formatPace(speedKmh),
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(DIMMER),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun IdleContent(
    totalKm: Double,
    totalRides: Int,
    records: List<RecordRow>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BG)
            .clickable(actionRunCallback<OpenAppCallback>())
            .padding(14.dp)
    ) {
        Text(
            "RYDE",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(ACCENT),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (records.isEmpty()) {
            Spacer(GlanceModifier.height(10.dp))
            Text(
                "No activities yet",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(DIM),
                    fontSize = 13.sp
                )
            )
        } else {
            Text(
                "%.0f km · %d %s".format(totalKm, totalRides, if (totalRides == 1) "ride" else "rides"),
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(DIM),
                    fontSize = 11.sp
                )
            )
            Spacer(GlanceModifier.height(10.dp))
            records.forEach { record ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(
                        record.label,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(DIMMER),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        record.value,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(WHITE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(GlanceModifier.height(5.dp))
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    val s = (ms % 60_000) / 1000
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatPace(speedKmh: Double): String {
    if (speedKmh < 0.5) return "--"
    val minPerKm = 60.0 / speedKmh
    val m = minPerKm.toInt()
    val s = ((minPerKm - m) * 60).toInt()
    return "%d:%02d /km".format(m, s)
}

class OpenAppCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }
}
