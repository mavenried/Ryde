package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.mavenried.Ryde.util.FileLogger
import me.mavenried.Ryde.util.UserPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val useLbs = remember { mutableStateOf(UserPrefs.useLbs(context)) }
    val weightKg = remember { mutableStateOf(UserPrefs.getWeightKg(context)) }
    var theme by remember { mutableStateOf(UserPrefs.getTheme(context)) }
    var isMetric by remember { mutableStateOf(UserPrefs.isMetric(context)) }
    var showLogsDialog by remember { mutableStateOf(false) }

    val displayValue = remember(useLbs.value, weightKg.value) {
        if (useLbs.value) "%.1f".format(UserPrefs.kgToLbs(weightKg.value))
        else "%.1f".format(weightKg.value)
    }
    var inputText by remember(displayValue) { mutableStateOf(displayValue) }
    var isError by remember { mutableStateOf(false) }

    if (showLogsDialog) {
        LogsDialog(onDismiss = { showLogsDialog = false })
    }

    fun save() {
        val parsed = inputText.toDoubleOrNull()
        if (parsed == null || parsed <= 0) { isError = true; return }
        isError = false
        val kg = if (useLbs.value) UserPrefs.lbsToKg(parsed) else parsed
        weightKg.value = kg
        UserPrefs.setWeightKg(context, kg)
        UserPrefs.setUseLbs(context, useLbs.value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { save(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Profile", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Body weight", style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it; isError = false },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (useLbs.value) "lbs" else "kg") },
                        isError = isError,
                        supportingText = if (isError) {{ Text("Enter a valid weight") }} else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    UnitToggle(
                        useLbs = useLbs.value,
                        onToggle = { newUseLbs ->
                            save()
                            useLbs.value = newUseLbs
                            UserPrefs.setUseLbs(context, newUseLbs)
                            inputText = if (newUseLbs)
                                "%.1f".format(UserPrefs.kgToLbs(weightKg.value))
                            else
                                "%.1f".format(weightKg.value)
                        }
                    )
                }
                Text(
                    "Used to estimate calorie burn during activities.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Units", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distance & speed", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true to "Metric", false to "Imperial")
                        .forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = isMetric == value,
                                onClick = {
                                    isMetric = value
                                    UserPrefs.setMetric(context, value)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                label = { Text(label) }
                            )
                        }
                }
                Text(
                    if (isMetric) "Showing km and kmph." else "Showing miles and mph.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Appearance", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        .forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = theme == value,
                                onClick = {
                                    theme = value
                                    UserPrefs.setTheme(context, value)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                                label = { Text(label) }
                            )
                        }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Diagnostics", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)

            OutlinedButton(
                onClick = { showLogsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("View Application Logs")
            }
        }
    }
}

@Composable
private fun LogsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var logText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        logText = FileLogger.getLogs(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text("Application Logs") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                FileLogger.clearLogs(context)
                                logText = "Logs cleared."
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear logs")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitToggle(useLbs: Boolean, onToggle: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = !useLbs,
            onClick = { if (useLbs) onToggle(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            label = { Text("kg") }
        )
        SegmentedButton(
            selected = useLbs,
            onClick = { if (!useLbs) onToggle(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            label = { Text("lbs") }
        )
    }
}
