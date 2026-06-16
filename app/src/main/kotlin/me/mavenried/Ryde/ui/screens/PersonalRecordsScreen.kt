package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.ui.viewmodel.PersonalRecord
import me.mavenried.Ryde.ui.viewmodel.PersonalRecordsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    onNavigateBack: () -> Unit,
    onRouteClick: (String) -> Unit,
    vm: PersonalRecordsViewModel = viewModel()
) {
    val records by vm.records.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Records") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val isEmpty = records.cycling.isEmpty() && records.running.isEmpty() &&
                records.walking.isEmpty() && records.overall.isEmpty()

        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No records yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        "Complete some rides to set records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (records.overall.isNotEmpty()) {
                    item {
                        PRSection(
                            title = "Overall",
                            icon = Icons.Rounded.EmojiEvents,
                            records = records.overall,
                            onRouteClick = onRouteClick
                        )
                    }
                }
                if (records.cycling.isNotEmpty()) {
                    item {
                        PRSection(
                            title = "Cycling",
                            icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                            records = records.cycling,
                            onRouteClick = onRouteClick
                        )
                    }
                }
                if (records.running.isNotEmpty()) {
                    item {
                        PRSection(
                            title = "Running",
                            icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                            records = records.running,
                            onRouteClick = onRouteClick
                        )
                    }
                }
                if (records.walking.isNotEmpty()) {
                    item {
                        PRSection(
                            title = "Walking",
                            icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                            records = records.walking,
                            onRouteClick = onRouteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PRSection(
    title: String,
    icon: ImageVector,
    records: List<PersonalRecord>,
    onRouteClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            records.forEach { record ->
                PRCard(record = record, onClick = { onRouteClick(record.routeId) })
            }
        }
    }
}

@Composable
private fun PRCard(record: PersonalRecord, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    record.routeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            Text(
                record.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
