package me.mavenried.Ryde.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import me.mavenried.Ryde.ui.theme.LocalIsDarkTheme
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint

private fun speedColor(speedMs: Float, activityType: ActivityType): Color {
    val (slowMs, fastMs) = when (activityType) {
        ActivityType.RUNNING -> 2f to 4.5f
        ActivityType.CYCLING -> 3f to 9f
        ActivityType.WALKING -> 0.8f to 2f
    }
    val t = ((speedMs - slowMs) / (fastMs - slowMs)).coerceIn(0f, 1f)
    val r: Int; val g: Int; val b: Int
    if (t < 0.5f) {
        val s = t * 2f
        r = lerpInt(0x29, 0x00, s); g = lerpInt(0x79, 0xE6, s); b = lerpInt(0xFF, 0x76, s)
    } else {
        val s = (t - 0.5f) * 2f
        r = lerpInt(0x00, 0xFF, s); g = lerpInt(0xE6, 0x17, s); b = lerpInt(0x76, 0x44, s)
    }
    return Color(red = r / 255f, green = g / 255f, blue = b / 255f)
}

private fun lerpInt(a: Int, b: Int, t: Float) = (a + (b - a) * t).toInt().coerceIn(0, 255)

@Composable
fun RouteMapView(
    points: List<LocationPoint>,
    activityType: ActivityType,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(points, mapLoaded) {
        if (mapLoaded && points.size >= 2) {
            val bounds = LatLngBounds.Builder()
                .apply { points.forEach { include(LatLng(it.lat, it.lng)) } }
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = { mapLoaded = true },
        mapColorScheme = if (LocalIsDarkTheme.current) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        properties = MapProperties(),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            scrollGesturesEnabled = true,
            zoomGesturesEnabled = true,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false
        )
    ) {
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            Polyline(
                points = listOf(LatLng(p1.lat, p1.lng), LatLng(p2.lat, p2.lng)),
                color = speedColor(((p1.speed + p2.speed) / 2f), activityType),
                width = 14f
            )
        }
    }
}
