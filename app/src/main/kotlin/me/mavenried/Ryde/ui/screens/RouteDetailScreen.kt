package me.mavenried.Ryde.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.ui.components.ElevationChart
import me.mavenried.Ryde.ui.components.RouteMapView
import me.mavenried.Ryde.ui.theme.LocalIsMetric
import me.mavenried.Ryde.ui.viewmodel.RouteDetailViewModel
import me.mavenried.Ryde.util.GpxExporter
import me.mavenried.Ryde.util.UserPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeId: String,
    onNavigateBack: () -> Unit,
    vm: RouteDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val route by vm.route.collectAsState()
    val points by vm.points.collectAsState()
    val topSpeedKmh by vm.topSpeedKmh.collectAsState()
    val stoppedTimeSec by vm.stoppedTimeSec.collectAsState()
    val isMetric = LocalIsMetric.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        uri?.let {
            val r = route ?: return@let
            val p = points
            if (GpxExporter.exportToUri(context, it, r, p)) {
                Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(routeId) { vm.load(routeId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ride?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteRoute(routeId, onNavigateBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val name = route?.name?.replace(" ", "_") ?: "route"
                        exportLauncher.launch("Ryde_$name.gpx")
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Export GPX")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete ride",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Box {
                RouteMapView(
                    points = points,
                    activityType = route?.activityType ?: ActivityType.RUNNING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
                SpeedLegend(
                    activityType = route?.activityType ?: ActivityType.RUNNING,
                    isMetric = isMetric,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                )
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (points.isNotEmpty()) {
                    Text(
                        "Elevation Profile",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    ElevationChart(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                route?.let { r ->
                    val durationMs = r.endTime - r.startTime
                    val hours = durationMs / 3_600_000
                    val mins = (durationMs % 3_600_000) / 60_000
                    val secs = (durationMs % 60_000) / 1000

                    val stats = buildList {
                        add("Distance" to UserPrefs.formatDistance(r.distanceKm, isMetric))
                        add("Duration" to if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs)
                                          else "%d:%02d".format(mins, secs))
                        add("Elevation" to "+%.0f m".format(r.elevationGainM))
                        if (r.activityType == ActivityType.CYCLING) {
                            add("Avg Speed" to if (r.avgSpeedKmh > 0)
                                UserPrefs.formatSpeed(r.avgSpeedKmh, isMetric) else "--")
                        } else {
                            val pace = r.avgPace
                            add("Avg Pace" to if (pace > 0 && pace <= 60)
                                UserPrefs.formatPace(pace, isMetric) else "--")
                        }
                        if (topSpeedKmh > 0) {
                            add("Top Speed" to UserPrefs.formatSpeed(topSpeedKmh, isMetric))
                        }
                        if (stoppedTimeSec > 0) {
                            val sh = stoppedTimeSec / 3600
                            val sm = (stoppedTimeSec % 3600) / 60
                            val ss = stoppedTimeSec % 60
                            add("Stopped" to if (sh > 0) "%d:%02d:%02d".format(sh, sm, ss)
                                            else "%d:%02d".format(sm, ss))
                        }
                        add("Calories" to "%.0f kcal".format(r.calories))
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        stats.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (label, value) ->
                                    ElevatedCard(modifier = Modifier.weight(1f)) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(value, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SpeedLegend(activityType: ActivityType, isMetric: Boolean, modifier: Modifier = Modifier) {
    val (slowMs, fastMs) = when (activityType) {
        ActivityType.RUNNING -> 2f to 4.5f
        ActivityType.CYCLING -> 3f to 9f
        ActivityType.WALKING -> 0.8f to 2f
    }
    val slowLabel = if (isMetric) "${(slowMs * 3.6).toInt()} kmph"
                    else "${(slowMs * 3.6 * 0.621371).toInt()} mph"
    val fastLabel = if (isMetric) "${(fastMs * 3.6).toInt()} kmph"
                    else "${(fastMs * 3.6 * 0.621371).toInt()} mph"
    val gradientColors = listOf(
        Color(0xFF2979FF.toInt()),
        Color(0xFF00E676.toInt()),
        Color(0xFFFF1744.toInt()),
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(gradientColors))
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.width(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    slowLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    fastLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
