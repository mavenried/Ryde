package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import me.mavenried.Ryde.R
import me.mavenried.Ryde.ui.theme.LocalIsDarkTheme
import me.mavenried.Ryde.ui.viewmodel.HeatmapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    onNavigateBack: () -> Unit,
    vm: HeatmapViewModel = viewModel()
) {
    val trails by vm.allTrails.collectAsState()
    val loading by vm.loading.collectAsState()
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    // Compute bounding box of all points to set initial camera
    val bounds = remember(trails) {
        if (trails.isEmpty() || trails.all { it.isEmpty() }) return@remember null
        val allPts = trails.flatten()
        val builder = LatLngBounds.Builder()
        allPts.forEach { builder.include(LatLng(it.lat, it.lng)) }
        builder.build()
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            bounds?.center ?: LatLng(0.0, 0.0),
            if (bounds != null) 12f else 2f
        )
    }

    LaunchedEffect(bounds) {
        if (bounds != null) {
            cameraState.position = CameraPosition.fromLatLngZoom(bounds.center, 11f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heatmap") },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                properties = MapProperties(
                    mapStyleOptions = if (isDark) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) else null
                )
            ) {
                trails.forEach { trail ->
                    if (trail.size >= 2) {
                        Polyline(
                            points = trail.map { LatLng(it.lat, it.lng) },
                            color = Color(0x996C63FF.toInt()),
                            width = 6f
                        )
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (!loading && trails.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Text(
                        "No routes recorded yet",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Route count badge
            if (!loading && trails.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        "${trails.size} route${if (trails.size == 1) "" else "s"} shown",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
