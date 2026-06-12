package me.mavenried.Ryde.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.ui.components.ElevationChart
import me.mavenried.Ryde.ui.components.RouteMapView
import me.mavenried.Ryde.ui.viewmodel.RouteDetailViewModel
import me.mavenried.Ryde.util.GpxExporter

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
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            RouteMapView(
                points = points,
                activityType = route?.activityType ?: ActivityType.RUNNING,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

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
                val mins = durationMs / 60_000
                val secs = (durationMs % 60_000) / 1000

                val stats = buildList {
                    add("Distance" to "%.2f km".format(r.distanceKm))
                    add("Duration" to "%d:%02d".format(mins, secs))
                    add("Elevation" to "+%.0f m".format(r.elevationGainM))
                    if (r.activityType == ActivityType.CYCLING) {
                        add("Avg Speed" to if (r.avgSpeedKmh > 0) "%.1f km/h".format(r.avgSpeedKmh) else "--")
                    } else {
                        val pace = r.avgPace
                        val paceStr = if (pace > 0 && pace <= 60) {
                            val pm = pace.toInt()
                            val ps = ((pace - pm) * 60).toInt().coerceIn(0, 59)
                            "%d:%02d /km".format(pm, ps)
                        } else "--"
                        add("Avg Pace" to paceStr)
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
