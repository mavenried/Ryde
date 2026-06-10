package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.service.TrackingState

@Composable
fun LiveStatsCard(state: TrackingState.Active, modifier: Modifier = Modifier) {
    val minutes = state.elapsedMs / 60_000
    val seconds = (state.elapsedMs % 60_000) / 1000

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            StatItem(label = "TIME", value = "%02d:%02d".format(minutes, seconds))
            StatItem(label = "DIST", value = "%.2f km".format(state.distanceKm))
            if (state.activityType == ActivityType.CYCLING) {
                StatItem(label = "SPEED", value = "%.1f km/h".format(state.currentSpeed))
            } else {
                val pace = state.currentPace
                StatItem(label = "PACE", value = if (pace > 0) formatPace(pace) else "--:--")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatPace(minPerKm: Double): String {
    val mins = minPerKm.toInt()
    val secs = ((minPerKm - mins) * 60).toInt()
    return "%d:%02d".format(mins, secs)
}
