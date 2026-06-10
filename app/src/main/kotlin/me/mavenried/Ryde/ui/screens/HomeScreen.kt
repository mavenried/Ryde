package me.mavenried.Ryde.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var selectedActivity by remember { mutableStateOf(ActivityType.RUNNING) }
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
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("End ride?") },
            text = { Text("Your route will be saved automatically.") },
            confirmButton = {
                TextButton(onClick = { showStopDialog = false; vm.stopTracking() }) {
                    Text("Save & Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Cancel") }
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
        title = { Text("Welcome to Ryde") },
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ryde",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(40.dp))
            ActivityPicker(selected = selected, onSelect = onSelect)
            Spacer(modifier = Modifier.height(52.dp))
            FloatingActionButton(
                onClick = onStart,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "START",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MusicControlBar()
            TextButton(
                onClick = onHistory,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("History")
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
    Box(modifier = Modifier.fillMaxSize()) {
        TrackingMapView(
            points = state.points,
            activityType = state.activityType,
            modifier = Modifier.fillMaxSize()
        )
        ActiveBottomPanel(
            state = state,
            onPauseResume = onPauseResume,
            onStop = onStop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
