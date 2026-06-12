package me.mavenried.Ryde.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import me.mavenried.Ryde.R

@Composable
fun RydeLogo(
    tint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageBitmap = remember {
        val svg = SVG.getFromResource(context, R.raw.ryde_logo)
        // SVG dimensions are in mm; documentWidth/Height gives the px equivalent at renderDPI.
        // Scale the canvas so that intrinsic rendering fills our target bitmap.
        val docW = svg.documentWidth   // ~111 px at 96 dpi
        val docH = svg.documentHeight  // ~42 px at 96 dpi
        val w = docW.coerceAtLeast(1f).toInt()
        val h = docH.coerceAtLeast(1f).toInt()
        val targetW = 800f
        val targetH = (targetW * docH / docW).coerceAtLeast(1f)
        val bmp = Bitmap.createBitmap(targetW.toInt(), targetH.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(targetW / docW, targetH / docH)
        svg.renderToCanvas(canvas)
        bmp.asImageBitmap()
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = "RYDE",
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint)
    )
}
