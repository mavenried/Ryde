package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.Route
import me.mavenried.Ryde.ui.theme.LocalIsMetric
import me.mavenried.Ryde.ui.viewmodel.ActivityStats
import me.mavenried.Ryde.ui.viewmodel.HistoryViewModel
import me.mavenried.Ryde.util.UserPrefs
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onRouteClick: (String) -> Unit,
    vm: HistoryViewModel = viewModel()
) {
    val routes by vm.routes.collectAsState()
    val stats by vm.totalStats.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all rides?") },
            text = { Text("This will permanently delete all ${routes.size} rides. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteAll(); showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (routes.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear all",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.SentimentDissatisfied,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No routes yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TotalStatsHeader(stats = stats)
                }
                items(routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        onClick = { onRouteClick(route.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalStatsHeader(stats: me.mavenried.Ryde.ui.viewmodel.TotalStats) {
    val order = listOf(ActivityType.CYCLING, ActivityType.RUNNING, ActivityType.WALKING)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        order.forEach { type ->
            val s = stats.byActivity[type] ?: return@forEach
            ActivityStatsCard(type = type, stats = s)
        }
    }
}

@Composable
private fun ActivityStatsCard(type: ActivityType, stats: ActivityStats) {
    val isMetric = LocalIsMetric.current
    val label = when (type) {
        ActivityType.CYCLING -> "Cycling"
        ActivityType.RUNNING -> "Running"
        ActivityType.WALKING -> "Walking"
    }
    val countLabel = when (type) {
        ActivityType.CYCLING -> if (stats.count == 1) "ride" else "rides"
        ActivityType.RUNNING -> if (stats.count == 1) "run" else "runs"
        ActivityType.WALKING -> if (stats.count == 1) "walk" else "walks"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${stats.count} $countLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "DISTANCE",
                    value = UserPrefs.formatDistance(stats.distanceKm, isMetric),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "TIME",
                    value = run {
                        val h = stats.durationMs / 3_600_000
                        val m = (stats.durationMs % 3_600_000) / 60_000
                        val s = (stats.durationMs % 60_000) / 1000
                        if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
                    },
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "CALORIES",
                    value = "%.0f kcal".format(stats.calories),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RouteCard(route: Route, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isMetric = LocalIsMetric.current
    val dateStr = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale.getDefault())
        .format(Date(route.startTime))
    val durationMin = (route.endTime - route.startTime) / 60_000

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = route.activityType.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = route.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(UserPrefs.formatDistance(route.distanceKm, isMetric), style = MaterialTheme.typography.titleSmall)
                Text(
                    "%d min".format(durationMin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

private val ActivityType.icon: ImageVector
    get() = when (this) {
        ActivityType.RUNNING -> Icons.AutoMirrored.Rounded.DirectionsRun
        ActivityType.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
        ActivityType.WALKING -> Icons.AutoMirrored.Rounded.DirectionsWalk
    }
