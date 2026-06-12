package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import me.mavenried.Ryde.domain.model.LocationPoint

@Composable
fun ElevationChart(
    points: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val minAlt = points.minOf { it.altitude }.toFloat()
        val maxAlt = points.maxOf { it.altitude }.toFloat()
        val altRange = (maxAlt - minAlt).coerceAtLeast(10f)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, point ->
            val x = (index.toFloat() / (points.size - 1)) * size.width
            val y = size.height - ((point.altitude.toFloat() - minAlt) / altRange) * size.height

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == points.size - 1) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(fillPath, color = surfaceColor, style = Fill)
        drawPath(path, color = primaryColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}
