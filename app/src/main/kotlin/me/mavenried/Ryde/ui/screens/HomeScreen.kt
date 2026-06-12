package me.mavenried.Ryde.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.service.TrackingState
import me.mavenried.Ryde.ui.components.*
import me.mavenried.Ryde.ui.viewmodel.TrackingViewModel
import me.mavenried.Ryde.util.PermissionHelper
import me.mavenried.Ryde.util.UserPrefs

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    vm: TrackingViewModel = viewModel()
) {
    val state by vm.trackingState.collectAsState()
    var selectedActivity by remember { mutableStateOf(ActivityType.CYCLING) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!UserPrefs.isOnboarded(context)) showOnboarding = true
    }

    if (showOnboarding) {
        OnboardingDialog(onDone = { showOnboarding = false })
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.startTracking(selectedActivity) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !PermissionHelper.hasBackgroundLocationPermission(context)
            ) {
                bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                vm.startTracking(selectedActivity)
            }
        }
    }

    fun onStartPressed() {
        when {
            !PermissionHelper.hasLocationPermission(context) ->
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !PermissionHelper.hasBackgroundLocationPermission(context) ->
                bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else -> vm.startTracking(selectedActivity)
        }
    }

    when (val s = state) {
        is TrackingState.Idle -> IdleContent(
            selected = selectedActivity,
            onSelect = { selectedActivity = it },
            onStart = ::onStartPressed,
            onHistory = onNavigateToHistory,
            onSettings = onNavigateToSettings
        )
        is TrackingState.Active -> ActiveContent(
            state = s,
            onPauseResume = { if (s.isPaused) vm.resumeTracking() else vm.pauseTracking() },
            onStop = { showStopDialog = true }
        )
    }

    if (showStopDialog) {
        val activeState = state as? TrackingState.Active
        val isShort = activeState != null &&
                activeState.elapsedMs < 60_000L && activeState.distanceKm < 0.1

        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(if (isShort) "Short ride" else "End ride?") },
            text = {
                if (isShort) {
                    val mins = (activeState!!.elapsedMs / 60_000L)
                    val secs = (activeState.elapsedMs % 60_000L) / 1_000L
                    Text("Only ${"%d:%02d".format(mins, secs)} and ${"%.2f".format(activeState.distanceKm)} km — worth saving?")
                } else {
                    Text("Save your route to history, or discard it entirely.")
                }
            },
            confirmButton = {
                if (isShort) {
                    TextButton(onClick = { showStopDialog = false; vm.stopTracking() }) {
                        Text("Save anyway")
                    }
                } else {
                    TextButton(onClick = { showStopDialog = false; vm.stopTracking() }) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showStopDialog = false; vm.discardTracking() }) {
                        Text(
                            if (isShort) "Discard" else "Discard",
                            color = if (isShort) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = { showStopDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun OnboardingDialog(onDone: () -> Unit) {
    val context = LocalContext.current
    var useLbs by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("70") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Welcome to RYDE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter your body weight so we can calculate accurate calorie burn.")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it; isError = false },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (useLbs) "lbs" else "kg") },
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    @OptIn(ExperimentalMaterial3Api::class)
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !useLbs,
                            onClick = {
                                if (useLbs) {
                                    useLbs = false
                                    inputText.toDoubleOrNull()?.let {
                                        inputText = "%.1f".format(UserPrefs.lbsToKg(it))
                                    }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            label = { Text("kg") }
                        )
                        SegmentedButton(
                            selected = useLbs,
                            onClick = {
                                if (!useLbs) {
                                    useLbs = true
                                    inputText.toDoubleOrNull()?.let {
                                        inputText = "%.1f".format(UserPrefs.kgToLbs(it))
                                    }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            label = { Text("lbs") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = inputText.toDoubleOrNull()
                if (parsed == null || parsed <= 0) { isError = true; return@Button }
                val kg = if (useLbs) UserPrefs.lbsToKg(parsed) else parsed
                UserPrefs.setWeightKg(context, kg)
                UserPrefs.setUseLbs(context, useLbs)
                UserPrefs.setOnboarded(context)
                onDone()
            }) {
                Text("Get started")
            }
        }
    )
}

@Composable
private fun IdleContent(
    selected: ActivityType,
    onSelect: (ActivityType) -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TrackingMapView(
            points = emptyList(),
            activityType = selected,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.9f)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RydeLogo(
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ActivityPicker(selected = selected, onSelect = onSelect)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start", style = MaterialTheme.typography.labelLarge)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MusicRow()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                TextButton(
                    onClick = onHistory,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("History")
                }
            }
        }
    }
}

@Composable
private fun ActiveContent(
    state: TrackingState.Active,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    var recenterTrigger by remember { mutableStateOf(0) }
    var panelHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        TrackingMapView(
            points = state.points,
            activityType = state.activityType,
            modifier = Modifier.fillMaxSize(),
            recenterTrigger = recenterTrigger
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            RydeLogo(
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(22.dp)
            )
        }
        val panelHeightDp = with(density) { panelHeightPx.toDp() }
        SmallFloatingActionButton(
            onClick = { recenterTrigger++ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = panelHeightDp + 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "Recenter")
        }
        ActiveBottomPanel(
            state = state,
            onPauseResume = onPauseResume,
            onStop = onStop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { panelHeightPx = it.height }
        )
    }
}
