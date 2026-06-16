package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.domain.model.LocationPoint

@Composable
fun SpeedChart(
    points: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val speeds = points.map { it.speed * 3.6f } // m/s → kmh
    val maxSpeed = speeds.max().coerceAtLeast(1f)

    val lineColor = MaterialTheme.colorScheme.tertiary
    val fillColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val path = Path()
        val fillPath = Path()

        speeds.forEachIndexed { index, speed ->
            val x = (index.toFloat() / (speeds.size - 1)) * size.width
            val y = size.height - (speed / maxSpeed) * size.height

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == speeds.size - 1) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(fillPath, color = fillColor, style = Fill)
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}
