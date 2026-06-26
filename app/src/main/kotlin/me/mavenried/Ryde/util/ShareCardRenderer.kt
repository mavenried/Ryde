package me.mavenried.Ryde.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import me.mavenried.Ryde.domain.model.ActivityType
import me.mavenried.Ryde.domain.model.LocationPoint
import me.mavenried.Ryde.domain.model.Route
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareCardRenderer {

    private const val W = 1080
    private const val H = 1350
    private const val PADDING = 72f
    private const val CORNER = 40f

    fun buildShareIntent(context: Context, route: Route, points: List<LocationPoint>): Intent {
        val bitmap = render(route, points)
        val file = File(context.cacheDir, "share/route_${route.id}.png").also {
            it.parentFile?.mkdirs()
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun render(route: Route, points: List<LocationPoint>): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B1B2F") }
        c.drawRoundRect(RectF(0f, 0f, W.toFloat(), H.toFloat()), CORNER, CORNER, bgPaint)

        val accentColor = Color.parseColor("#6C63FF")
        val white = Color.WHITE
        val dim = Color.argb(153, 255, 255, 255)

        // Header label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val actLabel = when (route.activityType) {
            ActivityType.CYCLING -> "CYCLING"
            ActivityType.RUNNING -> "RUNNING"
            ActivityType.WALKING -> "WALKING"
        }
        c.drawText("RYDE  ·  $actLabel", PADDING, PADDING + 42f, labelPaint)

        // Date
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim
            textSize = 36f
        }
        val dateStr = SimpleDateFormat("EEE, dd MMM yyyy · HH:mm", Locale.getDefault())
            .format(Date(route.startTime))
        c.drawText(dateStr, PADDING, PADDING + 42f + 52f, datePaint)

        // Route name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(route.name, PADDING, PADDING + 180f, namePaint)

        // Route line drawing
        val mapTop = PADDING + 220f
        val mapHeight = 520f
        val mapRect = RectF(PADDING, mapTop, W - PADDING, mapTop + mapHeight)
        drawRouteMap(c, points, mapRect, accentColor)

        // Stats grid
        val statsTop = mapTop + mapHeight + 60f
        val durationMs = route.endTime - route.startTime
        val h = durationMs / 3_600_000; val m = (durationMs % 3_600_000) / 60_000
        val durationStr = if (h > 0) "%d:%02d h".format(h, m) else "%d min".format(m)
        val stats = buildList<Pair<String, String>> {
            add("DISTANCE" to "%.2f km".format(route.distanceKm))
            add("DURATION" to durationStr)
            add("ELEVATION" to "+%.0f m".format(route.elevationGainM))
            val movStr = when {
                route.activityType == ActivityType.CYCLING && route.avgSpeedKmh > 0 ->
                    "%.1f km/h".format(route.avgSpeedKmh)
                route.avgPace in 1.0..60.0 -> {
                    val pm = route.avgPace.toInt(); val ps = ((route.avgPace - pm) * 60).toInt()
                    "%d:%02d /km".format(pm, ps)
                }
                else -> "--"
            }
            add((if (route.activityType == ActivityType.CYCLING) "AVG SPEED" else "AVG PACE") to movStr)
            add("CALORIES" to "%.0f kcal".format(route.calories))
            if (route.category != "Other") add("TAG" to route.category)
        }

        val colW = (W - PADDING * 2) / 3f
        stats.take(6).forEachIndexed { i, (label, value) ->
            val col = i % 3
            val row = i / 3
            val x = PADDING + col * colW
            val y = statsTop + row * 130f

            val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dim; textSize = 30f
            }
            val statValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = white; textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            c.drawText(label, x, y, statLabelPaint)
            c.drawText(value, x, y + 58f, statValPaint)
        }

        // Branding footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(89, 255, 255, 255); textSize = 32f
        }
        c.drawText("Tracked with RYDE", PADDING, H - PADDING, footerPaint)

        return bmp
    }

    private fun drawRouteMap(c: Canvas, points: List<LocationPoint>, rect: RectF, color: Int) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#0D0D1F")
        }
        c.drawRoundRect(rect, 20f, 20f, bgPaint)

        if (points.size < 2) return

        val lats = points.map { it.lat }
        val lngs = points.map { it.lng }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLng = lngs.min(); val maxLng = lngs.max()

        val latRange = (maxLat - minLat).coerceAtLeast(0.001)
        val lngRange = (maxLng - minLng).coerceAtLeast(0.001)

        val pad = 40f
        val scaleX = (rect.width() - pad * 2) / lngRange
        val scaleY = (rect.height() - pad * 2) / latRange

        fun ptX(lng: Double) = rect.left + pad + ((lng - minLng) * scaleX).toFloat()
        fun ptY(lat: Double) = rect.bottom - pad - ((lat - minLat) * scaleY).toFloat()

        val path = Path()
        path.moveTo(ptX(points[0].lng), ptY(points[0].lat))
        points.drop(1).forEach { path.lineTo(ptX(it.lng), ptY(it.lat)) }

        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        c.drawPath(path, routePaint)

        // Start dot (green)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = Color.parseColor("#4CAF50")
        c.drawCircle(ptX(points.first().lng), ptY(points.first().lat), 16f, dotPaint)

        // End dot (accent)
        dotPaint.color = color
        c.drawCircle(ptX(points.last().lng), ptY(points.last().lat), 16f, dotPaint)
    }
}
