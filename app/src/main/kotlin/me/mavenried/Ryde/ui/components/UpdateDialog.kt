package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.util.UpdateInfo

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    downloading: Boolean,
    downloadProgress: Float,
    readyToInstall: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Version ${info.version} is ready to download.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (info.releaseNotes.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
                if (downloading) {
                    if (downloadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            when {
                readyToInstall -> TextButton(onClick = onInstall) { Text("Install now") }
                downloading -> TextButton(onClick = {}, enabled = false) { Text("Downloading…") }
                else -> TextButton(onClick = onDownload) { Text("Download") }
            }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
