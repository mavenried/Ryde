package me.mavenried.Ryde.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.mavenried.Ryde.data.db.AppDatabase
import me.mavenried.Ryde.domain.repository.RouteRepository
import me.mavenried.Ryde.ui.MainActivity

class RydeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = RouteRepository(AppDatabase.getInstance(context))
        val routes = try { repository.getAllRoutesOnce() } catch (_: Exception) { emptyList() }
        val totalDistanceKm = routes.sumOf { it.distanceKm }
        val totalRides = routes.size
        val lastRoute = routes.firstOrNull()

        provideContent {
            WidgetContent(
                totalDistanceKm = totalDistanceKm,
                totalRides = totalRides,
                lastRouteName = lastRoute?.name
            )
        }
    }
}

@Composable
private fun WidgetContent(
    totalDistanceKm: Double,
    totalRides: Int,
    lastRouteName: String?
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1B1B2F))
            .clickable(actionRunCallback<OpenAppCallback>())
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            "RYDE",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "%.1f km".format(totalDistanceKm),
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color.White),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            "$totalRides ${if (totalRides == 1) "ride" else "rides"} total",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                fontSize = 12.sp
            )
        )
        if (lastRouteName != null) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                "Last: $lastRouteName",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.45f)),
                    fontSize = 11.sp
                )
            )
        }
    }
}

class OpenAppCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }
}
