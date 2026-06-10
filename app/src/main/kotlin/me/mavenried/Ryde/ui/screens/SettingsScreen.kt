package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.util.UserPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val useLbs = remember { mutableStateOf(UserPrefs.useLbs(context)) }
    val weightKg = remember { mutableStateOf(UserPrefs.getWeightKg(context)) }

    val displayValue = remember(useLbs.value, weightKg.value) {
        if (useLbs.value) "%.1f".format(UserPrefs.kgToLbs(weightKg.value))
        else "%.1f".format(weightKg.value)
    }
    var inputText by remember(displayValue) { mutableStateOf(displayValue) }
    var isError by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
