package me.mavenried.Ryde.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.mavenried.Ryde.ui.theme.LocalIsMetric
import me.mavenried.Ryde.ui.viewmodel.PeriodBar
import me.mavenried.Ryde.ui.viewmodel.StatsPeriod
import me.mavenried.Ryde.ui.viewmodel.StatsViewModel
import me.mavenried.Ryde.util.UserPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    vm: StatsViewModel = viewModel()
) {
    val period by vm.period.collectAsState()
    val bars by vm.bars.collectAsState()
    val isMetric = LocalIsMetric.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Stats") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = period == StatsPeriod.WEEKLY,
                    onClick = { vm.setPeriod(StatsPeriod.WEEKLY) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Weekly") }
                SegmentedButton(
                    selected = period == StatsPeriod.MONTHLY,
                    onClick = { vm.setPeriod(StatsPeriod.MONTHLY) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Monthly") }
            }

            if (bars.isEmpty() || bars.all { it.totalKm == 0.0 }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No activity data yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            } else {
                val maxKm = bars.maxOf { it.totalKm }.coerceAtLeast(1.0)
                val cyclingColor = MaterialTheme.colorScheme.primary
                val runningColor = MaterialTheme.colorScheme.secondary
                val walkingColor = MaterialTheme.colorScheme.tertiary
                val surfaceVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

                ActivityBarChart(
                    bars = bars,
                    maxKm = maxKm,
                    cyclingColor = cyclingColor,
                    runningColor = runningColor,
                    walkingColor = walkingColor,
                    gridColor = surfaceVariant,
                    isMetric = isMetric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                ChartLegend(
                    cyclingColor = cyclingColor,
                    runningColor = runningColor,
                    walkingColor = walkingColor
                )

                SummaryTotals(bars = bars, isMetric = isMetric)
            }
        }
    }
}

@Composable
private fun ActivityBarChart(
    bars: List<PeriodBar>,
    maxKm: Double,
    cyclingColor: Color,
    runningColor: Color,
    walkingColor: Color,
    gridColor: Color,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = bars.size
        if (barCount == 0) return@Canvas

        val labelHeight = 28f
        val chartHeight = size.height - labelHeight
        val barWidth = size.width / barCount * 0.55f
        val gap = size.width / barCount

        repeat(3) { i ->
            val y = chartHeight * (1f - (i + 1) / 3f)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        bars.forEachIndexed { idx, bar ->
            val centerX = gap * idx + gap / 2f

            fun drawSegment(km: Double, bottomY: Float, color: Color): Float {
                if (km <= 0.0) return bottomY
                val h = (km / maxKm * chartHeight).toFloat().coerceAtLeast(1f)
                val top = bottomY - h
                drawRect(color, Offset(centerX - barWidth / 2f, top), Size(barWidth, h))
                return top
            }

            val base = chartHeight
            val afterWalk = drawSegment(bar.walkingKm, base, walkingColor)
            val afterRun = drawSegment(bar.runningKm, afterWalk, runningColor)
            drawSegment(bar.cyclingKm, afterRun, cyclingColor)
        }
    }
}

@Composable
private fun ChartLegend(cyclingColor: Color, runningColor: Color, walkingColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = cyclingColor, label = "Cycling", icon = Icons.AutoMirrored.Rounded.DirectionsBike)
        LegendItem(color = runningColor, label = "Running", icon = Icons.AutoMirrored.Rounded.DirectionsRun)
        LegendItem(color = walkingColor, label = "Walking", icon = Icons.AutoMirrored.Rounded.DirectionsWalk)
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun SummaryTotals(bars: List<PeriodBar>, isMetric: Boolean) {
    val totalCycling = bars.sumOf { it.cyclingKm }
    val totalRunning = bars.sumOf { it.runningKm }
    val totalWalking = bars.sumOf { it.walkingKm }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryItem("Cycling", UserPrefs.formatDistance(totalCycling, isMetric))
            SummaryItem("Running", UserPrefs.formatDistance(totalRunning, isMetric))
            SummaryItem("Walking", UserPrefs.formatDistance(totalWalking, isMetric))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
