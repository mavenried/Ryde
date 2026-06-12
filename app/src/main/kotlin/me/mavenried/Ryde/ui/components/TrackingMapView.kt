package me.mavenried.Ryde.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
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
fun TrackingMapView(
    points: List<LocationPoint>,
    activityType: ActivityType,
    modifier: Modifier = Modifier,
    recenterTrigger: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasLocation = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 17f)
    }

    // Before any tracking points arrive, center on last-known location
    LaunchedEffect(Unit) {
        if (hasLocation) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { loc ->
                        loc?.let {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(it.latitude, it.longitude), 17f
                                    )
                                )
                            }
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    // Follow the latest tracking point
    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(points.last().lat, points.last().lng))
            )
        }
    }

    // Recenter on demand
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger == 0) return@LaunchedEffect
        val target = points.lastOrNull()
        if (target != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(target.lat, target.lng))
            )
        } else if (hasLocation) {
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { loc ->
                        loc?.let {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 17f)
                                )
                            }
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    val darkTheme = isSystemInDarkTheme()
    val mapModifier = if (darkTheme) {
        // invert(100%) hue-rotate(180deg) — keeps map colours natural in dark mode
        val invertPaint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix(floatArrayOf(
                     0.574f, -1.430f, -0.144f, 0f, 255f,
                    -0.426f, -0.430f, -0.144f, 0f, 255f,
                    -0.426f, -1.430f,  0.856f, 0f, 255f,
                     0f,      0f,      0f,     1f,   0f
                ))
            )
        }
        modifier.drawWithContent {
            drawContext.canvas.saveLayer(
                Rect(0f, 0f, size.width, size.height), invertPaint
            )
            drawContent()
            drawContext.canvas.restore()
        }
    } else modifier

    GoogleMap(
        modifier = mapModifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocation
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            Polyline(
                points = listOf(LatLng(p1.lat, p1.lng), LatLng(p2.lat, p2.lng)),
                color = speedColor(((p1.speed + p2.speed) / 2f).toFloat(), activityType),
                width = 14f
            )
        }
    }
}
