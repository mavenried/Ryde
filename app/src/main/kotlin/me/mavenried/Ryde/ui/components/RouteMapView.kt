package me.mavenried.Ryde.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

private const val STADIA_KEY = "5c3cb3e2-58f0-4dea-818c-fa112e9683a6"

private val STADIA_LIGHT = XYTileSource(
    "StadiaAlidadeSmooth", 0, 20, 256, ".png?api_key=$STADIA_KEY",
    arrayOf("https://tiles.stadiamaps.com/tiles/alidade_smooth/")
)

private val STADIA_DARK = XYTileSource(
    "StadiaAlidadeSmoothDark", 0, 20, 256, ".png?api_key=$STADIA_KEY",
    arrayOf("https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/")
)

private fun speedColor(speedMs: Float, activityType: ActivityType): Int {
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
    return android.graphics.Color.argb(255, r, g, b)
}

private fun lerpInt(a: Int, b: Int, t: Float) = (a + (b - a) * t).toInt().coerceIn(0, 255)

@Composable
fun RouteMapView(
    points: List<LocationPoint>,
    activityType: ActivityType,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(if (darkTheme) STADIA_DARK else STADIA_LIGHT)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setScrollableAreaLimitDouble(null)
        }
    }

    LaunchedEffect(points) {
        mapView.overlays.removeAll { it is Polyline }

        if (points.size >= 2) {
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val color = speedColor(((p1.speed + p2.speed) / 2f).toFloat(), activityType)
                val seg = Polyline().apply {
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.isAntiAlias = true
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    outlinePaint.color = color
                    setPoints(listOf(GeoPoint(p1.lat, p1.lng), GeoPoint(p2.lat, p2.lng)))
                }
                mapView.overlays.add(seg)
            }

            val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
            val maxLat = geoPoints.maxOf { it.latitude }
            val minLat = geoPoints.minOf { it.latitude }
            val maxLng = geoPoints.maxOf { it.longitude }
            val minLng = geoPoints.minOf { it.longitude }
            val latPad = (maxLat - minLat).coerceAtLeast(0.002) * 0.2
            val lngPad = (maxLng - minLng).coerceAtLeast(0.002) * 0.2
            val box = BoundingBox(maxLat + latPad, maxLng + lngPad, minLat - latPad, minLng - lngPad)
            mapView.post { mapView.zoomToBoundingBox(box, true) }
            mapView.invalidate()
        }
    }

    LaunchedEffect(darkTheme) {
        mapView.setTileSource(if (darkTheme) STADIA_DARK else STADIA_LIGHT)
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
