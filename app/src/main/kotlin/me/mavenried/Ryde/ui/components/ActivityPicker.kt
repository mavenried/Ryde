package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.domain.model.ActivityType

@Composable
fun ActivityPicker(
    selected: ActivityType,
    onSelect: (ActivityType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        ActivityType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.label) },
                leadingIcon = {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

private val ActivityType.icon: ImageVector
    get() = when (this) {
        ActivityType.RUNNING -> Icons.AutoMirrored.Rounded.DirectionsRun
        ActivityType.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
        ActivityType.WALKING -> Icons.AutoMirrored.Rounded.DirectionsWalk
    }

private val ActivityType.label: String
    get() = when (this) {
        ActivityType.RUNNING -> "Run"
        ActivityType.CYCLING -> "Ride"
        ActivityType.WALKING -> "Walk"
    }

